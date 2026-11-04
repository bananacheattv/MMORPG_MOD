package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.network.SharedLedger;
import com.mmorpg.registry.ModItems;
import com.mmorpg.world.DungeonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Progression d'une expedition de donjon : trois etages fermes par une grille qui ne s'ouvre qu'avec la cle de l'etage, chrono et
 * classement de vitesse, coffre bonus si personne n'est mort, reprise apres plantage du serveur et carte des salles decouvertes.
 * L'etat de chaque expedition est enregistre (un exemplaire = une expedition) et efface quand l'exemplaire est remis a neuf.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DungeonRun extends SavedData {
    private static final String[] ROMAN = {"I", "II", "III"};
    /** Dernier couloir de chaque etage : la grille y est posee (z relatif a l'origine de l'exemplaire). */
    public static final int[] DOOR_Z = {Layout.C2.z1(), Layout.C4.z1(), Layout.C6.z1()};
    /** Les epreuves situees avant cette ligne doivent etre reussies pour que la cle de l'etage soit remise. */
    private static final int[] FLOOR_END = {Layout.R2.z1(), Layout.R4.z1(), Layout.ELITE.z1()};
    /** Delai de grace apres un redemarrage du serveur avant de liberer un exemplaire en cours (en ticks). */
    public static final int CRASH_GRACE_TICKS = 20 * 60 * 20;
    public static final int RANK_SIZE = 10;

    /** Noeuds de la carte : {nom, z0, z1, etage (3 = boss)}. */
    public static final Object[][] NODES = {
            {"Hall d'entrée", Integer.MIN_VALUE, Layout.HALL.z1(), 0},
            {"Couloir", Layout.C0.z0(), Layout.C0.z1(), 0},
            {"Salle I", Layout.R1.z0(), Layout.R1.z1(), 0},
            {"Fosse I", Layout.P1.z0(), Layout.P1.z1(), 0},
            {"Salle II", Layout.R2.z0(), Layout.R2.z1(), 0},
            {"Couloir", Layout.C2.z0(), Layout.C2.z1(), 0},
            {"Salle III", Layout.R3.z0(), Layout.R3.z1(), 1},
            {"Fosse II", Layout.P2.z0(), Layout.P2.z1(), 1},
            {"Salle IV", Layout.R4.z0(), Layout.R4.z1(), 1},
            {"Couloir", Layout.C4.z0(), Layout.C4.z1(), 1},
            {"Salle V", Layout.R5.z0(), Layout.R5.z1(), 2},
            {"Couloir", Layout.C5.z0(), Layout.C5.z1(), 2},
            {"Gardien d'élite", Layout.ELITE.z0(), Layout.ELITE.z1(), 2},
            {"Couloir", Layout.C6.z0(), Layout.C6.z1(), 2},
            {"Galerie", Layout.GALLERY.z0(), Layout.GALLERY.z1(), 3},
            {"Antre du boss", Layout.ARENA.z0(), Layout.ARENA.z1(), 3},
            {"Trésor", Layout.TREASURE.z0(), Integer.MAX_VALUE, 3},
    };

    // ------------------------------------------------------------------ etat enregistre

    public static final class Run {
        public final String pool;
        public final int index;
        public String owner;
        public long start;
        public int deaths;
        /** Bits : cles deja remises / grilles deja ouvertes / noeuds de la carte decouverts. */
        public int keys, opened, seen;
        public boolean done;
        public boolean hasBonus;
        public long bonusPos;
        public List<String> party = new ArrayList<>();

        Run(String pool, int index, String owner, long start) {
            this.pool = pool;
            this.index = index;
            this.owner = owner;
            this.start = start;
        }

        static final Codec<Run> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("pool").forGetter(r -> r.pool),
                Codec.INT.fieldOf("index").forGetter(r -> r.index),
                Codec.STRING.fieldOf("owner").forGetter(r -> r.owner),
                Codec.LONG.fieldOf("start").forGetter(r -> r.start),
                Codec.INT.optionalFieldOf("deaths", 0).forGetter(r -> r.deaths),
                Codec.INT.optionalFieldOf("keys", 0).forGetter(r -> r.keys),
                Codec.INT.optionalFieldOf("opened", 0).forGetter(r -> r.opened),
                Codec.INT.optionalFieldOf("seen", 0).forGetter(r -> r.seen),
                Codec.BOOL.optionalFieldOf("done", false).forGetter(r -> r.done),
                Codec.BOOL.optionalFieldOf("hasBonus", false).forGetter(r -> r.hasBonus),
                Codec.LONG.optionalFieldOf("bonusPos", 0L).forGetter(r -> r.bonusPos),
                Codec.STRING.listOf().optionalFieldOf("party", List.of()).forGetter(r -> r.party)
        ).apply(i, (pool, index, owner, start, deaths, keys, opened, seen, done, bonus, bonusPos, party) -> {
            Run r = new Run(pool, index, owner, start);
            r.deaths = deaths; r.keys = keys; r.opened = opened; r.seen = seen; r.done = done; r.hasBonus = bonus; r.bonusPos = bonusPos;
            r.party = new ArrayList<>(party);
            return r;
        }));
    }

    private static final Codec<DungeonRun> CODEC = RecordCodecBuilder.create(i -> i.group(
            Run.CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter(d -> d.runs)
    ).apply(i, DungeonRun::new));
    public static final SavedDataType<DungeonRun> TYPE = new SavedDataType<>(MMORPG.id("dungeon_runs"), DungeonRun::new, CODEC);

    final List<Run> runs;

    public DungeonRun() {
        this.runs = new ArrayList<>();
    }

    private DungeonRun(List<Run> runs) {
        this.runs = new ArrayList<>(runs);
    }

    public static DungeonRun get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static @Nullable Run find(MinecraftServer server, InstanceData.Slot s) {
        for (Run r : get(server).runs) if (r.pool.equals(s.pool) && r.index == s.index) return r;
        return null;
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // ------------------------------------------------------------------ entree, reprise

    /** Un joueur arrive dans l'exemplaire : le chrono demarre a la premiere arrivee, les suivantes reprennent la meme expedition. */
    public static void enter(ServerPlayer p, InstanceData.Slot slot) {
        MinecraftServer server = p.level().getServer();
        DungeonRun data = get(server);
        Run r = find(server, slot);
        boolean fresh = r == null || r.done || !r.owner.equals(slot.owner);
        if (fresh) {
            data.runs.removeIf(x -> x.pool.equals(slot.pool) && x.index == slot.index);
            r = new Run(slot.pool, slot.index, slot.owner, now(server));
            data.runs.add(r);
        }
        String name = p.getName().getString();
        if (!r.party.contains(name)) r.party.add(name);
        data.setDirty();
        p.sendSystemMessage(Component.literal(fresh ? "⏱ Le chrono de l'expédition démarre. Carte du donjon : /donjon carte"
                : "⏱ Expédition reprise (" + time(now(server) - r.start) + "). Carte : /donjon carte").withColor(0x9CD8FF));
        reissueKeys(p, r);
    }

    /** Reconnexion dans un exemplaire encore reserve (apres un plantage par exemple) : les cles perdues sont redonnees. */
    public static void rejoin(ServerPlayer p, InstanceData.Slot slot) {
        MinecraftServer server = p.level().getServer();
        Run r = find(server, slot);
        if (r == null || r.done) return;
        String name = p.getName().getString();
        if (!r.party.contains(name)) { r.party.add(name); get(server).setDirty(); }
        p.sendSystemMessage(Component.literal("↺ Reconnexion : votre expédition continue (" + time(now(server) - r.start) + ", " + r.deaths + " mort"
                + (r.deaths > 1 ? "s" : "") + "). Carte : /donjon carte").withColor(0x9CD8FF));
        reissueKeys(p, r);
    }

    /** Au demarrage : un exemplaire en cours n'est libere qu'apres un long delai de grace, les autres comme d'habitude. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        DungeonRun data = get(server);
        long tick = server.getTickCount();
        for (InstanceData.Slot s : InstanceData.get(server).slots) {
            if (s.owner.isEmpty()) continue;
            Run r = find(server, s);
            boolean live = r != null && !r.done && r.owner.equals(s.owner);
            s.lastBusy = live ? tick + CRASH_GRACE_TICKS - InstanceManager.RELEASE_TICKS : tick;
        }
        InstanceData.get(server).dirty();
        data.setDirty();
    }

    /** L'exemplaire est remis a neuf : on oublie l'expedition et on retire le coffre bonus. */
    static void cleanup(MinecraftServer server, InstanceData.Slot s) {
        DungeonRun data = get(server);
        Run r = find(server, s);
        if (r == null) return;
        if (r.hasBonus) {
            ServerLevel level = server.getLevel(InstanceManager.DIM);
            if (level != null) {
                BlockPos at = BlockPos.of(r.bonusPos);
                if (level.getBlockState(at).is(com.mmorpg.registry.ModBlocks.DUNGEON_CHEST.get())) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        data.runs.remove(r);
        data.setDirty();
    }

    // ------------------------------------------------------------------ grilles et cles

    private static boolean doorBlock(net.minecraft.world.level.block.state.BlockState s) {
        return s.is(Blocks.IRON_BARS) || s.is(Blocks.GOLD_BLOCK);
    }

    /** Pose les trois grilles (sur l'air uniquement : la voute du couloir reste intacte) ; appelee a chaque construction / remise a neuf. */
    public static void buildDoors(ServerLevel level, BlockPos origin) {
        for (int k = 0; k < 3; k++) {
            for (int x = -6; x <= 6; x++) {
                for (int y = 0; y <= 9; y++) {
                    BlockPos at = origin.offset(x, y, DOOR_Z[k]);
                    if (!level.isEmptyBlock(at)) continue;
                    level.setBlock(at, (x == 0 && y <= 1 ? Blocks.GOLD_BLOCK : Blocks.IRON_BARS).defaultBlockState(), 2);
                }
            }
        }
    }

    private static ItemStack makeKey(int floor, Run r) {
        ItemStack s = new ItemStack(Items.TRIPWIRE_HOOK);
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Clé d'étage " + ROMAN[floor]).withColor(0xFFD060));
        CompoundTag tag = new CompoundTag();
        tag.putInt("mmo_key", floor + 1);
        tag.putLong("mmo_run", r.start);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return s;
    }

    private static boolean isKey(ItemStack s, int floor, Run r) {
        if (!s.is(Items.TRIPWIRE_HOOK)) return false;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag tag = cd.copyTag();
        return tag.getIntOr("mmo_key", 0) == floor + 1 && tag.getLongOr("mmo_run", -1L) == r.start;
    }

    private static boolean hasKey(ServerPlayer p, int floor, Run r) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (isKey(p.getInventory().getItem(i), floor, r)) return true;
        return false;
    }

    private static boolean takeKey(ServerPlayer p, int floor, Run r) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (isKey(s, floor, r)) { s.shrink(1); return true; }
        }
        return false;
    }

    private static boolean giveKey(ServerPlayer p, int floor, Run r) {
        return p.getInventory().add(makeKey(floor, r));
    }

    private static void reissueKeys(ServerPlayer p, Run r) {
        for (int f = 0; f < 3; f++) {
            if ((r.keys >> f & 1) == 1 && (r.opened >> f & 1) == 0 && !hasKey(p, f, r)) giveKey(p, f, r);
        }
    }

    /** Les epreuves de l'etage (hors embuscades) sont-elles toutes reussies ? */
    static boolean floorDone(MinecraftServer server, InstanceData.Slot slot, int floor) {
        var trials = TrialData.get(server).inGroup(InstanceManager.group(slot.pool, slot));
        boolean any = false;
        for (var t : trials) {
            if (t.kind().equals(TrialData.AMBUSH)) continue;
            int rel = t.pos().get(2) - slot.z;
            if (rel > FLOOR_END[floor] + 4) continue;
            any = true;
            if (!t.done()) return false;
        }
        return any;
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(p.level() instanceof ServerLevel level) || !level.dimension().equals(InstanceManager.DIM)) return;
        if (!doorBlock(level.getBlockState(e.getPos()))) return;
        MinecraftServer server = level.getServer();
        var slot = InstanceManager.slotAt(server, level, e.getPos().getX(), e.getPos().getZ());
        if (slot == null || slot.owner.isEmpty()) return;
        int rz = e.getPos().getZ() - slot.z, rx = e.getPos().getX() - slot.x;
        int door = -1;
        for (int k = 0; k < 3; k++) if (rz == DOOR_Z[k] && Math.abs(rx) <= 6) door = k;
        if (door < 0) return;
        e.setCanceled(true);
        Run r = find(server, slot);
        if (r == null) return;
        if ((r.opened >> door & 1) == 1) return;
        if (takeKey(p, door, r)) {
            openDoor(level, slot, r, door);
        } else if ((r.keys >> door & 1) == 1) {
            if (giveKey(p, door, r)) p.sendOverlayMessage(Component.literal("Clé d'étage " + ROMAN[door] + " retrouvée : cliquez à nouveau sur la grille.").withColor(0xFFD060));
            else p.sendOverlayMessage(Component.literal("Inventaire plein : faites de la place pour recevoir la clé.").withColor(0xFF8060));
        } else {
            p.sendOverlayMessage(Component.literal("Grille scellée : terminez les épreuves de l'étage " + ROMAN[door] + " pour obtenir la clé.").withColor(0xFF8060));
        }
    }

    private static void openDoor(ServerLevel level, InstanceData.Slot slot, Run r, int door) {
        BlockPos origin = new BlockPos(slot.x, slot.y, slot.z);
        for (int x = -6; x <= 6; x++) {
            for (int y = 0; y <= 9; y++) {
                BlockPos at = origin.offset(x, y, DOOR_Z[door]);
                if (doorBlock(level.getBlockState(at))) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        BlockPos mid = origin.offset(0, 1, DOOR_Z[door]);
        level.playSound(null, mid, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.4F, 0.7F);
        level.sendParticles(ParticleTypes.CLOUD, mid.getX() + 0.5, mid.getY() + 1, mid.getZ() + 0.5, 30, 2, 1.5, 0.3, 0.02);
        r.opened |= 1 << door;
        get(level.getServer()).setDirty();
        String next = door < 2 ? "L'étage " + ROMAN[door + 1] + " s'ouvre devant vous." : "La voie vers l'antre du boss est ouverte !";
        for (ServerPlayer q : inside(level, slot)) q.sendSystemMessage(Component.literal("🗝 Grille de l'étage " + ROMAN[door] + " ouverte. " + next).withColor(0xFFD060));
    }

    // ------------------------------------------------------------------ joueurs presents, mort, tick

    static List<ServerPlayer> inside(ServerLevel level, InstanceData.Slot s) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && Math.abs(p.getX() - s.x) < 40 && p.getZ() > s.z - 6 && p.getZ() < s.z + Layout.LENGTH + 6) out.add(p);
        }
        return out;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || !level.dimension().equals(InstanceManager.DIM)) return;
        var slot = InstanceManager.slotAt(level.getServer(), level, p.getX(), p.getZ());
        if (slot == null) return;
        Run r = find(level.getServer(), slot);
        if (r == null || r.done) return;
        r.deaths++;
        get(level.getServer()).setDirty();
        for (ServerPlayer q : inside(level, slot)) q.sendSystemMessage(Component.literal("☠ " + p.getName().getString() + " est tombé : le coffre bonus sans mort est perdu.").withColor(0xFF7070));
    }

    /** Numero du noeud de la carte contenant cette ligne (z relatif). */
    public static int nodeAt(int rz) {
        for (int i = 0; i < NODES.length; i++) if (rz <= (int) NODES[i][2]) return i;
        return NODES.length - 1;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.getLevel(InstanceManager.DIM);
        if (level == null || level.players().isEmpty()) return;
        boolean keyTick = server.getTickCount() % 20 == 0;
        DungeonRun data = get(server);
        for (InstanceData.Slot s : InstanceData.get(server).slots) {
            if (s.owner.isEmpty() || !s.ready || InstanceManager.isSetup(s.pool)) continue;
            List<ServerPlayer> in = inside(level, s);
            if (in.isEmpty()) continue;
            Run r = find(server, s);
            if (r == null || r.done) continue;
            for (ServerPlayer p : in) {
                int bit = 1 << nodeAt((int) p.getZ() - s.z);
                if ((r.seen & bit) == 0) {
                    r.seen |= bit;
                    data.setDirty();
                    p.sendOverlayMessage(Component.literal("Carte : " + NODES[nodeAt((int) p.getZ() - s.z)][0] + " découverte").withColor(0x9CD8FF));
                }
            }
            if (!keyTick) continue;
            for (int f = 0; f < 3; f++) {
                if ((r.keys >> f & 1) == 1 || !floorDone(server, s, f)) continue;
                r.keys |= 1 << f;
                data.setDirty();
                for (ServerPlayer p : in) {
                    boolean ok = giveKey(p, f, r);
                    p.sendSystemMessage(Component.literal("🗝 Étage " + ROMAN[f] + " terminé ! Clé d'étage " + ROMAN[f] + (ok ? " reçue : ouvrez la grille au bout du couloir."
                            : " : inventaire plein, la serrure vous la redonnera.")).withColor(0xFFD060));
                    level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            }
        }
    }

    // ------------------------------------------------------------------ victoire : chrono, classement, coffre bonus

    public static final class Entry {
        public String party = "";
        public long ticks;
        public int deaths;
        public int size;
        public long at;
    }

    public static final class Ranking {
        public java.util.Map<String, List<Entry>> best = new java.util.HashMap<>();
    }

    public static String time(long ticks) {
        long sec = Math.max(0, ticks / 20);
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }

    public static String poolLabel(String pool) {
        int d = InstanceManager.difficultyOf(pool);
        return InstanceManager.typeOf(pool).label + (d == 1 ? " (héroïque)" : d >= 2 ? " (mythique)" : "");
    }

    public static List<Entry> ranking(MinecraftServer server, String pool) {
        return SharedLedger.read(server, "donjon-vitesse", Ranking.class, Ranking::new,
                s -> new ArrayList<>(s.best.getOrDefault(pool, List.of())));
    }

    /** Le boss d'un antre vient de tomber : le chrono s'arrete, le temps entre au classement, le coffre bonus apparait si personne n'est mort. */
    public static void victory(ServerLevel level, BlockPos bossPos) {
        MinecraftServer server = level.getServer();
        var slot = InstanceManager.slotAt(server, level, bossPos.getX(), bossPos.getZ());
        if (slot == null || InstanceManager.isSetup(slot.pool)) return;
        Run r = find(server, slot);
        if (r == null || r.done) return;
        r.done = true;
        long ticks = now(server) - r.start;
        List<ServerPlayer> in = inside(level, slot);
        List<String> names = new ArrayList<>();
        for (ServerPlayer p : in) names.add(p.getName().getString());
        if (names.isEmpty()) names.addAll(r.party);
        Entry entry = new Entry();
        entry.party = String.join(", ", names);
        entry.ticks = ticks;
        entry.deaths = r.deaths;
        entry.size = Math.max(1, names.size());
        entry.at = System.currentTimeMillis();
        int rank;
        try {
            rank = SharedLedger.access(server, "donjon-vitesse", Ranking.class, Ranking::new, s -> {
                List<Entry> list = s.best.computeIfAbsent(slot.pool, k -> new ArrayList<>());
                list.add(entry);
                list.sort(java.util.Comparator.comparingLong(x -> x.ticks));
                while (list.size() > RANK_SIZE) list.remove(list.size() - 1);
                return list.indexOf(entry) + 1;
            });
        } catch (RuntimeException ex) {
            MMORPG.LOGGER.warn("[MMORPG] Classement de vitesse indisponible : {}", ex.toString());
            rank = 0;
        }
        String line = "⏱ " + poolLabel(slot.pool) + " vaincu en " + time(ticks) + (rank > 0 ? " — classement de vitesse : n°" + rank : "");
        for (ServerPlayer p : in) p.sendSystemMessage(Component.literal(line).withColor(0x9CD8FF));
        if (rank == 1) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal("🏆 Record de vitesse : " + entry.party + " — " + poolLabel(slot.pool) + " en " + time(ticks) + " !").withColor(0xFFD060));
            }
        }
        if (r.deaths == 0) spawnBonus(level, slot, r, bossPos, in);
        else for (ServerPlayer p : in) p.sendSystemMessage(Component.literal("Coffre bonus : il fallait terminer sans aucune mort (" + r.deaths + ").").withColor(0xB9ABCF));
        get(server).setDirty();
    }

    private static void spawnBonus(ServerLevel level, InstanceData.Slot slot, Run r, BlockPos bossPos, List<ServerPlayer> in) {
        BlockPos at = bossPos;
        int[][] around = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 3}, {-3, -3}};
        for (int[] d : around) {
            BlockPos c = bossPos.offset(d[0], 0, d[1]);
            if (level.isEmptyBlock(c) && !level.isEmptyBlock(c.below())) { at = c; break; }
        }
        var type = InstanceManager.typeOf(slot.pool);
        var rnd = level.getRandom();
        List<ItemStack> loot = new ArrayList<>(DungeonLoot.treasure(type, rnd, 1));
        loot.add(new ItemStack(ModItems.PIECE_OR.get(), 20 + type.level / 2 + rnd.nextInt(20)));
        if (loot.size() > 27) loot = new ArrayList<>(loot.subList(0, 27));
        DungeonBuilder.chest(level, at, loot, rnd);
        r.hasBonus = true;
        r.bonusPos = at.asLong();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 40, 0.6, 0.8, 0.6, 0.3);
        level.playSound(null, at, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 1.0F);
        for (ServerPlayer p : in) p.sendSystemMessage(Component.literal("✦ Aucun mort : un coffre bonus est apparu près du boss !").withColor(0xFFE080));
    }

    // ------------------------------------------------------------------ carte et commandes

    public static void sendMap(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !level.dimension().equals(InstanceManager.DIM)) {
            p.sendSystemMessage(Component.literal("La carte n'est disponible qu'à l'intérieur d'un donjon.").withColor(0xFF8060));
            return;
        }
        MinecraftServer server = level.getServer();
        var slot = InstanceManager.slotAt(server, level, p.getX(), p.getZ());
        Run r = slot == null ? null : find(server, slot);
        if (r == null) {
            p.sendSystemMessage(Component.literal("Aucune expédition en cours ici.").withColor(0xFF8060));
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("title", poolLabel(slot.pool));
        t.putInt("seen", r.seen);
        t.putInt("keys", r.keys);
        t.putInt("opened", r.opened);
        t.putInt("deaths", r.deaths);
        t.putInt("secs", (int) ((r.done ? 0 : now(server) - r.start) / 20));
        t.putBoolean("done", r.done);
        t.putInt("at", nodeAt((int) p.getZ() - slot.z));
        int have = 0;
        for (int f = 0; f < 3; f++) if (hasKey(p, f, r)) have |= 1 << f;
        t.putInt("have", have);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.DUNGEON_MAP, t));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("donjon")
                .then(Commands.literal("carte").executes(c -> { sendMap(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("classement").executes(c -> { ranking(c.getSource().getPlayerOrException()); return 1; })));
    }

    private static void ranking(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        String only = null;
        if (p.level() instanceof ServerLevel level && level.dimension().equals(InstanceManager.DIM)) {
            var slot = InstanceManager.slotAt(server, level, p.getX(), p.getZ());
            if (slot != null) only = slot.pool;
        }
        p.sendSystemMessage(Component.literal("⏱ Classement de vitesse des donjons").withColor(0xFFD060));
        boolean any = false;
        for (var type : DungeonBuilder.DungeonType.values()) {
            for (int diff = 0; diff < 3; diff++) {
                String pool = InstanceManager.pool(type, diff);
                if (only != null && !only.equals(pool)) continue;
                List<Entry> list = ranking(server, pool);
                if (list.isEmpty()) continue;
                any = true;
                p.sendSystemMessage(Component.literal(poolLabel(pool)).withColor(0x9CD8FF));
                for (int i = 0; i < Math.min(list.size(), only == null ? 3 : RANK_SIZE); i++) {
                    Entry en = list.get(i);
                    p.sendSystemMessage(Component.literal(" " + (i + 1) + ". " + time(en.ticks) + " — " + en.party + (en.deaths == 0 ? " ✦ sans mort" : "")).withColor(0xEDE8F5));
                }
            }
        }
        if (!any) p.sendSystemMessage(Component.literal("Aucun temps enregistré pour l'instant.").withColor(0xB9ABCF));
    }
}
