package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.mine.MineLayout.Sector;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.zone.ZoneData;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.zone.ZoneManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Les Mines d'Aeloria : une immense caverne partagee (dimension des donjons) dont les minerais se mangent a la pioche. Extraire un minerai
 * donne directement le lingot ; le bloc devient une « veine epuisee » puis repousse apres un temps de recharge (conserve dans le monde).
 * Le gestionnaire construit la mine au premier demarrage, enregistre ses zones, fait repousser les minerais et fait apparaitre des
 * monstres dont le niveau suit la profondeur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MineManager {
    /** A incrementer quand le plan change : la mine est alors reconstruite au demarrage. */
    public static final int VERSION = 4;
    private static final int MOB_CAP = 14;

    private static MineBuilder job;
    private static long jobStart;
    private static int lastLogged = -1;

    private MineManager() {
    }

    public static ServerLevel dim(MinecraftServer server) {
        return server.getLevel(InstanceManager.DIM);
    }

    /** Numero de la mine ou se trouve l'entite, ou -1. */
    public static int regionOf(Entity e) {
        return e.level().dimension().equals(InstanceManager.DIM) ? MineLayout.regionAt(e.getX(), e.getZ()) : -1;
    }

    public static boolean inMine(Entity e) {
        return regionOf(e) >= 0;
    }

    public static boolean built(MinecraftServer server, int region) {
        return MineData.get(server).version(region) == VERSION;
    }

    public static boolean allBuilt(MinecraftServer server) {
        for (int r = 0; r < MineLayout.SECTORS; r++) if (!built(server, r)) return false;
        return true;
    }

    public static boolean building() {
        return job != null;
    }

    public static int percent() {
        return job == null ? 100 : job.percent();
    }

    // ================================================================== construction et zones

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        job = null;
        ZoneData zones = ZoneData.get(server);
        for (Sector s : MineLayout.STYLES) zones.remove(s.id());                // anciennes zones de la mine unique
        registerZones(server);
    }

    /** Lance la construction d'une mine (la zone et les minerais existants sont remplaces). */
    public static boolean start(MinecraftServer server, int region) {
        ServerLevel level = dim(server);
        if (level == null || job != null) return false;
        MineData data = MineData.get(server);
        data.setVersion(region, 0);
        int x0 = MineLayout.get(region).x0();
        data.respawns().removeIf(r -> r.x() >= x0 && r.x() < x0 + MineLayout.SIZE);
        data.setDirty();
        job = new MineBuilder(level, region);
        jobStart = System.nanoTime();
        lastLogged = -1;
        MMORPG.LOGGER.info("[MMORPG] Construction de la mine {} ({}) : {} x {} blocs, {} blocs de haut", region + 1, MineLayout.STYLES.get(region).name(), MineLayout.SIZE, MineLayout.SIZE, MineLayout.HEIGHT);
        return true;
    }

    /** Deux zones par mine (la place d'entree, puis la caverne) ; la construction et la casse y sont interdites, pas les monstres. */
    public static void registerZones(MinecraftServer server) {
        ZoneData zones = ZoneData.get(server);
        for (Sector s : MineLayout.STYLES) {
            int x0 = MineLayout.get(s.index()).x0();
            int px = x0 + MineLayout.PLAZA_X;
            zones.add(new ZoneData.Zone("mines_" + s.index() + "_entree", "Place de " + s.name(), InstanceManager.DIM, px - 24, MineLayout.Z0, px + 24, MineLayout.Z0 + MineLayout.PLAZA_Z + 22,
                    true, true, true, ZoneData.Zone.STYLE_ZONE, "Le portail de retour vers le Spawn d'Aeloria"));
            zones.add(new ZoneData.Zone("mines_" + s.index(), s.name(), InstanceManager.DIM, x0, MineLayout.Z0, x0 + MineLayout.SIZE - 1, MineLayout.Z0 + MineLayout.SIZE - 1,
                    true, true, false, ZoneData.Zone.STYLE_DUNGEON, s.lore() + "|Mine " + (s.index() + 1) + " · niveaux " + s.minLevel() + " à " + s.maxLevel()));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        transitions(server);
        if (job != null) {
            if (job.step(22_000_000L)) {
                int r = job.region();
                MineData.get(server).setVersion(r, VERSION);
                MMORPG.LOGGER.info("[MMORPG] Mine {} construite : {} blocs en {} s ({} case(s) de caverne au contact du vide, attendu : 0)", r + 1, job.blocksPlaced(),
                        (System.nanoTime() - jobStart) / 1_000_000_000L, MineLayout.get(r).leaks());
                job = null;
            } else if (job.percent() / 20 != lastLogged) {
                lastLogged = job.percent() / 20;
                MMORPG.LOGGER.info("[MMORPG] Mine {} : {} %", job.region() + 1, job.percent());
            }
            return;
        }
        for (int r = 0; r < MineLayout.SECTORS && job == null && !com.mmorpg.plains.PlainsManager.building(); r++) {          // une construction a la fois dans la dimension
            if (!built(server, r)) start(server, r);
        }
        if (job != null) return;
        ServerLevel level = dim(server);
        if (level == null || level.players().isEmpty()) return;
        long t = server.getTickCount();
        if (t % 8 == 0) portals(server, level);
        if (t % 20 == 0) respawns(server, level);
        if (t % 40 == 0) spawnMobs(level);
    }

    // ================================================================== portails : retour au Spawn et passage cache a la mine suivante

    private static final java.util.Map<UUID, Long> PORTAL_COOLDOWN = new java.util.HashMap<>();

    private static AABB returnArch(MineLayout lay) {
        int f = lay.floor(MineLayout.PLAZA_X, MineLayout.PLAZA_Z);
        return new AABB(lay.x0() + MineLayout.PLAZA_X - 5, f + 1, MineLayout.Z0 + MineLayout.PLAZA_Z - 14.5, lay.x0() + MineLayout.PLAZA_X + 6, f + 10, MineLayout.Z0 + MineLayout.PLAZA_Z - 8.6);
    }

    private static AABB exitGate(MineLayout lay) {
        int f = lay.floor(lay.exitX, lay.exitZ);
        return new AABB(lay.x0() + lay.exitX - 3.5, f, MineLayout.Z0 + lay.exitZ - 2.4, lay.x0() + lay.exitX + 4.5, f + 9, MineLayout.Z0 + lay.exitZ + 1.0);
    }

    private static boolean cooling(ServerPlayer p, long now) {
        Long last = PORTAL_COOLDOWN.get(p.getUUID());
        if (last != null && now - last < 60) return true;
        PORTAL_COOLDOWN.put(p.getUUID(), now);
        return false;
    }

    private static void portals(MinecraftServer server, ServerLevel level) {
        long now = server.getTickCount();
        for (ServerPlayer p : new ArrayList<>(level.players())) {                  // copie : un portail deplace le joueur pendant le parcours
            int region = regionOf(p);
            if (region < 0 || p.isSpectator() || p.isPassenger()) continue;
            MineLayout lay = MineLayout.get(region);
            if (region < MineLayout.SECTORS - 1) {
                double ex = lay.x0() + lay.exitX + 0.5, ez = MineLayout.Z0 + lay.exitZ + 0.5, ey = lay.floor(lay.exitX, lay.exitZ) + 4;
                if (p.distanceToSqr(ex, ey, ez) < 40 * 40) level.sendParticles(ParticleTypes.PORTAL, ex, ey, ez, 8, 2.5, 3.5, 0.3, 0.2);
                if (exitGate(lay).contains(p.getX(), p.getY(), p.getZ()) && !transitioning(p) && !cooling(p, now)) {
                    level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.2f);
                    discover(p, region + 1);
                    startTransition(p, region + 1);
                    continue;
                }
            }
            if (returnArch(lay).contains(p.getX(), p.getY(), p.getZ()) && !cooling(p, now)) {
                level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.5f);
                com.mmorpg.hub.HubManager.goToSpawn(p);
            }
        }
    }

    /** Premiere traversee du portail cache : la mine suivante est ouverte au joueur pour toujours. */
    private static void discover(ServerPlayer p, int region) {
        PlayerData d = RpgPlayers.get(p);
        if (d.minesUnlocked > region) return;
        d.minesUnlocked = region + 1;
        d.dirty = true;
        com.mmorpg.db.Audit.log(p, com.mmorpg.db.Audit.UNLOCK, region + 1, "mine " + (region + 1));
        Sector s = MineLayout.STYLES.get(region);
        p.sendSystemMessage(Component.literal("✦ Nouvelle mine découverte : " + s.name() + " (niveaux " + s.minLevel() + " à " + s.maxLevel() + ")").withColor(MineBlocks.ore(s.ore()).color()));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    // ================================================================== minage

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof MineOreBlock ore)) return;
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (sp.isCreative() && sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;      // un batisseur peut casser le bloc
        event.setCanceled(true);
        mineAndVein(level, sp, event.getPos(), ore.ore());
    }

    /** Extrait le minerai casse, puis (pioches d'Ambrasite et au-dela, accroupi) toute sa veine. */
    public static boolean mineAndVein(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore) {
        boolean ok = mine(level, p, pos, ore);
        if (ok) vein(level, p, pos, ore);
        return ok;
    }

    /** Minerais d'une meme veine extraits d'un coup (accroupi) selon le palier de la pioche : Ambrasite 4, Glacium 8, Neantite 14. */
    public static int veinLimit(int pickTier) {
        return pickTier >= 5 ? 14 : pickTier == 4 ? 8 : pickTier == 3 ? 4 : 0;
    }

    /** Pioches d'Ambrasite et au-dela : accroupi, extrait aussi les minerais de meme espece qui touchent celui qu'on vient de briser. */
    private static void vein(ServerLevel level, ServerPlayer p, BlockPos start, MineBlocks.Ore ore) {
        int limit = veinLimit(MineTools.pickTier(p.getMainHandItem()));
        if (limit <= 0 || !p.isShiftKeyDown()) return;
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        seen.add(start);
        queue.add(start);
        int mined = 0;
        while (!queue.isEmpty() && mined < limit) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1 && mined < limit; dx++) {
                for (int dy = -1; dy <= 1 && mined < limit; dy++) {
                    for (int dz = -1; dz <= 1 && mined < limit; dz++) {
                        BlockPos n = at.offset(dx, dy, dz);
                        if (!seen.add(n)) continue;
                        if (!(level.getBlockState(n).getBlock() instanceof MineOreBlock other) || !other.ore().id().equals(ore.id())) continue;
                        if (!mine(level, p, n, ore)) return;                                   // pioche cassee ou refus : on s'arrete
                        mined++;
                        queue.add(n);
                    }
                }
            }
        }
        if (mined > 0) p.sendOverlayMessage(Component.literal("Veine : +" + mined + " minerai" + (mined > 1 ? "s" : "") + " extrait" + (mined > 1 ? "s" : "") + " d'un coup").withColor(ore.color()));
    }

    /** Extrait un minerai : le lingot va dans le sac, le bloc s'epuise puis repousse. @return vrai si le minerai a ete extrait */
    public static boolean mine(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore) {
        ItemStack tool = p.getMainHandItem();
        if (!tool.is(ItemTags.PICKAXES)) {
            p.sendOverlayMessage(Component.literal("Il faut une pioche pour extraire la " + ore.name() + ".").withColor(0xFFC060));
            return false;
        }
        int pick = MineTools.pickTier(tool);
        if (pick < ore.tier()) {
            String need = MineTools.TIER_ID[Math.min(ore.tier(), MineTools.TIER_ID.length - 1)];
            p.sendOverlayMessage(Component.literal("Votre pioche est trop fragile pour la " + ore.name() + " : il faut une pioche de palier « " + need + " » ou mieux.").withColor(0xFF7060));
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.4F);
            return false;
        }
        PlayerData d = RpgPlayers.get(p);
        if (d.level < ore.minLevel()) {
            p.sendOverlayMessage(Component.literal("Niveau " + ore.minLevel() + " requis pour extraire la " + ore.name() + ".").withColor(0xFF7060));
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.6F);
            return false;
        }
        var random = level.getRandom();
        int count = 1 + (random.nextInt(100) < 12 + ore.tier() * 2 + pick * 4 ? 1 : 0);      // veine riche : un lingot de plus (la pioche aide)
        ItemStack ingot = new ItemStack(ModItems.get(ore.ingotId()), count);
        if (!p.getInventory().add(ingot)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), ingot));
        tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        RpgPlayers.giveGatherXp(p, 3 + ore.minLevel() / 2L);
        p.sendOverlayMessage(Component.literal("+" + count + " Lingot de " + ore.name() + (count > 1 ? "  (veine riche !)" : "")).withColor(ore.color()));
        rareDrop(level, p, pos, ore, pick);
        BlockState old = level.getBlockState(pos);
        level.setBlock(pos, MineBlocks.EPUISE.get().defaultBlockState(), 3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, old), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F + random.nextFloat() * 0.3F);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7F, 1.1F);
        long wait = (long) (ore.cooldown() * 20 * (0.8 + random.nextDouble() * 0.4));
        MineData.get(level.getServer()).add(new MineData.Respawn(pos.getX(), pos.getY(), pos.getZ(), ore.tier(), level.getGameTime() + wait));
        return true;
    }

    /** Chances (en %) par minerai extrait, avant bonus de pioche : {Aelium, Orichalque, Astralium} selon le palier du minerai. */
    private static final double[][] RARE_CHANCE = {
            {3.0, 0, 0}, {4.5, 0.5, 0}, {6.0, 1.2, 0.05}, {7.0, 2.0, 0.1}, {8.0, 3.0, 0.35}};

    /** Lingots rares, epiques et legendaires : tirage independant du lingot de base ; la pioche augmente toutes les chances de 25 % par palier. */
    private static void rareDrop(ServerLevel level, ServerPlayer p, BlockPos pos, MineBlocks.Ore ore, int pick) {
        double[] chance = RARE_CHANCE[Math.min(ore.tier(), RARE_CHANCE.length - 1)];
        double mult = 1.0 + 0.25 * pick;
        String[] ids = {"lingot_aelium", "lingot_orichalque", "lingot_astralium"};
        String[] names = {"Lingot d'Aelium", "Lingot d'Orichalque", "Lingot d'Astralium"};
        int[] colors = {0x6EF0E0, 0xFF8A5A, 0xFFE070};
        var random = level.getRandom();
        for (int i = 2; i >= 0; i--) {
            if (chance[i] <= 0 || random.nextDouble() * 100.0 >= chance[i] * mult) continue;
            ItemStack drop = new ItemStack(ModItems.get(ids[i]));
            if (!p.getInventory().add(drop)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), drop));
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12 + i * 14, 0.4, 0.4, 0.4, 0.05 + 0.03 * i);
            level.playSound(null, pos, i == 0 ? SoundEvents.AMETHYST_CLUSTER_BREAK : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F + 0.2F * i, 1.4F - 0.2F * i);
            p.sendSystemMessage(Component.literal("✦ Trouvaille " + (i == 0 ? "rare" : i == 1 ? "épique" : "légendaire") + " : " + names[i] + " !").withColor(colors[i]));
            if (i == 2) {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + p.getName().getString() + " a extrait un Lingot d'Astralium dans les Mines d'Aeloria !").withColor(colors[2]), false);
            }
            return;                                                                          // un seul lingot rare par minerai (le plus precieux d'abord)
        }
    }

    private static void respawns(MinecraftServer server, ServerLevel level) {
        MineData data = MineData.get(server);
        if (data.respawns().isEmpty()) return;
        long now = level.getGameTime();
        List<MineData.Respawn> due = new ArrayList<>();
        for (MineData.Respawn r : data.respawns()) {                         // seules les entrees dont le chunk est charge comptent : les autres n'en bloquent pas d'autres
            if (r.due() <= now && level.hasChunkAt(r.pos())) due.add(r);
            if (due.size() >= 120) break;
        }
        for (MineData.Respawn r : due) {
            BlockPos pos = r.pos();
            if (!level.hasChunkAt(pos)) continue;                          // il repoussera au prochain passage dans le secteur
            data.remove(r);
            if (!level.getBlockState(pos).is(MineBlocks.EPUISE.get())) continue;
            MineBlocks.Ore ore = MineBlocks.ORES.get(Math.min(MineBlocks.ORES.size() - 1, r.tier()));
            level.setBlock(pos, MineBlocks.block(ore).defaultBlockState(), 3);
            if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false) != null) {
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
    }

    /** Test : fait repousser tout de suite les minerais epuises. */
    public static int respawnAllNow(MinecraftServer server) {
        ServerLevel level = dim(server);
        MineData data = MineData.get(server);
        int n = data.respawns().size();
        List<MineData.Respawn> all = new ArrayList<>(data.respawns());
        data.clear();
        for (MineData.Respawn r : all) data.add(new MineData.Respawn(r.x(), r.y(), r.z(), r.tier(), level == null ? 0 : level.getGameTime() - 1));
        return n;
    }

    // ================================================================== monstres

    private static void spawnMobs(ServerLevel level) {
        var random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            int region = regionOf(p);
            if (region < 0 || p.isSpectator()) continue;
            MineLayout layout = MineLayout.get(region);
            int px = (int) p.getX() - layout.x0(), pz = (int) p.getZ() - MineLayout.Z0;
            if (Math.hypot(px - MineLayout.PLAZA_X, pz - MineLayout.PLAZA_Z) < 42) continue;           // la place d'entree est sure
            AABB area = p.getBoundingBox().inflate(36, 30, 36);
            if (level.getEntitiesOfClass(Mob.class, area, m -> m instanceof com.mmorpg.entity.RpgMob).size() >= MOB_CAP) continue;
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double dist = 16 + random.nextDouble() * 18;
                int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
                int lx = x - layout.x0(), lz = z - MineLayout.Z0;
                if (layout.kind(lx, lz) != 1 || layout.isPillar(lx, lz) || layout.liquid(lx, lz) > layout.floor(lx, lz)) continue;
                if (Math.hypot(lx - MineLayout.PLAZA_X, lz - MineLayout.PLAZA_Z) < 34 || Math.hypot(lx - layout.exitX, lz - layout.exitZ) < 14) continue;
                BlockPos pos = new BlockPos(x, layout.floor(lx, lz) + 1, z);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                Sector s = layout.style;
                String key = s.mobs().get(random.nextInt(s.mobs().size()));
                int lvl = layout.mobLevel(lx, lz, new Random(random.nextLong()));
                MobSpawner.spawnForCommand(level, key, pos, lvl);
            }
        }
    }

    // ================================================================== menu, chargement et arrivee

    /** Temps minimal de l'ecran de chargement (ticks) : assez pour lire le titre et l'ambiance de la mine. */
    private static final int LOADING_TICKS = 80;
    /** Marque des titres de chargement des mines (voir LoadingClient.MINE_MARK : elle n'est jamais affichee). */
    private static final String MARK = "\u26CF ";

    private record Transition(UUID player, int region, long teleportAt, String title, String lore, int color) {
    }

    private static final List<Transition> TRANSITIONS = new ArrayList<>();

    /** Menu du Maitre des mines (ou de la commande /mine quand {@code npc} est nul) : les cinq mines, leurs niveaux et leurs minerais ; celles qu'on n'a pas decouvertes sont verrouillees. */
    public static void open(ServerPlayer p, NpcEntity npc) {
        PlayerData d = RpgPlayers.get(p);
        ListTag list = new ListTag();
        for (Sector s : MineLayout.STYLES) {
            MineBlocks.Ore ore = MineBlocks.ore(s.ore());
            CompoundTag e = new CompoundTag();
            e.putInt("index", s.index());
            e.putString("label", s.name());
            e.putString("lore", s.lore());
            e.putInt("color", ore.color());
            e.putInt("min", s.minLevel());
            e.putInt("max", s.maxLevel());
            e.putBoolean("locked", s.index() >= d.minesUnlocked);
            e.putString("icon", ore.ingotId());
            e.putString("lockedHint", s.index() == 0 ? "" : "Trouvez le portail caché de « " + MineLayout.STYLES.get(s.index() - 1).name() + " » : il se cache au fond de la caverne.");
            ListTag info = new ListTag();
            info.add(net.minecraft.nbt.StringTag.valueOf("Minerai : " + ore.name() + ", extrait à partir du niveau " + ore.minLevel() + " (pioche en main). Il repousse après " + ore.cooldown() + " secondes."));
            info.add(net.minecraft.nbt.StringTag.valueOf("Monstres de niveau " + s.minLevel() + " à " + s.maxLevel() + ", répartis en zones de niveau (un totem annonce chaque zone)."));
            info.add(net.minecraft.nbt.StringTag.valueOf("Le lingot arrive directement dans votre sac : il sert à forger armes et armures à la Forge Arcanique."));
            info.add(net.minecraft.nbt.StringTag.valueOf(s.index() < MineLayout.SECTORS - 1 ? "Un portail caché mène à « " + MineLayout.STYLES.get(s.index() + 1).name() + " » : à vous de le trouver." : "La dernière mine : son autel garde encore ses secrets."));
            e.put("info", info);
            list.add(e);
        }
        CompoundTag data = new CompoundTag();
        data.putInt("npc", npc == null ? -1 : npc.getId());
        data.putInt("base", 0);
        data.putString("name", npc == null ? "Les mines d'Aeloria" : npc.displayName());
        data.putString("unit", "Mine");
        data.putString("tagline", "Trouvez le portail caché de chaque mine pour ouvrir la suivante");
        data.putInt("playerLevel", d.level);
        data.putBoolean("built", allBuilt(p.level().getServer()));
        data.putInt("percent", percent());
        data.put("sectors", list);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MINE_MASTER, data));
    }

    /** Choix du joueur dans le menu : verifications cote serveur puis ecran de chargement et arrivee a la mine. */
    public static void enter(ServerPlayer p, Payloads.MineEnter packet) {
        if (packet.sector() >= com.mmorpg.plains.PlainsManager.PACKET_BASE) {                // le meme menu sert aux plaines
            com.mmorpg.plains.PlainsManager.enterChosen(p, packet.sector() - com.mmorpg.plains.PlainsManager.PACKET_BASE);
            return;
        }
        if (packet.npc() >= 0) {
            Entity e = p.level().getEntity(packet.npc());
            if (!(e instanceof NpcEntity npc) || npc.role() != NpcEntity.Role.MINES || npc.distanceToSqr(p) > 100) return;
        }
        int k = packet.sector();
        if (k < 0 || k >= MineLayout.SECTORS) return;
        Sector s = MineLayout.STYLES.get(k);
        if (k >= RpgPlayers.get(p).minesUnlocked) {
            p.sendOverlayMessage(Component.literal("Cette mine n'est pas encore découverte : trouvez le portail caché de la précédente.").withColor(0xFF5050));
            return;
        }
        startTransition(p, k);
    }

    /** Ecran de chargement puis arrivee a la mine (menu, portail cache). */
    public static void startTransition(ServerPlayer p, int k) {
        MinecraftServer server = p.level().getServer();
        if (dim(server) == null || k < 0 || k >= MineLayout.SECTORS) return;
        Sector s = MineLayout.STYLES.get(k);
        MineBlocks.Ore ore = MineBlocks.ore(s.ore());
        String title = MARK + s.name();
        long at = server.overworld().getGameTime() + LOADING_TICKS;
        TRANSITIONS.removeIf(x -> x.player().equals(p.getUUID()));
        TRANSITIONS.add(new Transition(p.getUUID(), k, at, title, s.lore(), ore.color()));
        com.mmorpg.dungeon.BossReveal.hold(p, LOADING_TICKS + 400);
        Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.START, title, s.lore(), ore.color(), built(server, k) ? 0 : (job != null && job.region() == k ? percent() : 0)));
    }

    private static boolean transitioning(ServerPlayer p) {
        for (Transition x : TRANSITIONS) if (x.player().equals(p.getUUID())) return true;
        return false;
    }

    private static void transitions(MinecraftServer server) {
        if (TRANSITIONS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        var it = TRANSITIONS.iterator();
        while (it.hasNext()) {
            Transition x = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(x.player());
            if (p == null) {
                it.remove();
                continue;
            }
            com.mmorpg.dungeon.BossReveal.hold(p, 40);
            boolean ready = built(server, x.region());
            if (!ready) {
                if (now % 4 == 0) Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), job != null && job.region() == x.region() ? Math.min(99, percent()) : 0));
            } else if (now >= x.teleportAt()) {
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), 100));
                com.mmorpg.dungeon.BossReveal.release(p);
                arrive(p, x.region());
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.END, x.title(), x.lore(), x.color(), 100));
                it.remove();
            } else if (now % 4 == 0) {
                int pct = (int) Math.max(0, 100 - 100L * (x.teleportAt() - now) / LOADING_TICKS);
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), pct));
            }
        }
    }

    /** Point d'arrivee d'une mine : la place d'entree, sur un sol bien plat. */
    public static double[] spawnPoint(int region) {
        MineLayout layout = MineLayout.get(region);
        int cx = MineLayout.PLAZA_X, cz = MineLayout.PLAZA_Z - 7;
        int ox = layout.x0();
        for (int r = 0; r < 70; r += 2) {
            int steps = r == 0 ? 1 : 12;
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps;
                int lx = cx + (int) Math.round(Math.cos(a) * r), lz = cz + (int) Math.round(Math.sin(a) * r);
                if (layout.kind(lx, lz) != 1 || layout.isPillar(lx, lz) || layout.liquid(lx, lz) > layout.floor(lx, lz)) continue;
                if (lz < MineLayout.PLAZA_Z - 6) continue;                                   // jamais devant l'arche du retour (elle ramene au Spawn)
                boolean flat = true;
                for (int dx = -2; dx <= 2 && flat; dx++) {
                    for (int dz = -2; dz <= 2 && flat; dz++) {
                        flat = layout.kind(lx + dx, lz + dz) == 1 && !layout.isPillar(lx + dx, lz + dz)
                                && Math.abs(layout.floor(lx + dx, lz + dz) - layout.floor(lx, lz)) <= 1
                                && layout.liquid(lx + dx, lz + dz) <= layout.floor(lx + dx, lz + dz);
                    }
                }
                if (flat) return new double[]{ox + lx + 0.5, layout.floor(lx, lz) + 1, MineLayout.Z0 + lz + 0.5};
            }
        }
        return new double[]{ox + cx + 0.5, layout.floor(cx, cz) + 1, MineLayout.Z0 + cz + 0.5};
    }

    private static void arrive(ServerPlayer p, int region) {
        ServerLevel level = dim(p.level().getServer());
        if (level == null) return;
        double[] at = spawnPoint(region);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(((int) at[0] + dx * 16) >> 4, ((int) at[2] + dz * 16) >> 4);
        }
        ZoneManager.forgetAnnouncements(p.getUUID());               // l'annonce de la mine (titre et ambiance) se rejoue a l'arrivee
        PORTAL_COOLDOWN.put(p.getUUID(), (long) p.level().getServer().getTickCount());
        p.teleportTo(level, at[0], at[1], at[2], Set.of(), 0F, 6F, true);
        p.resetFallDistance();
        level.sendParticles(ParticleTypes.END_ROD, at[0], at[1] + 1, at[2], 30, 0.6, 0.9, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.RISE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Envoie directement le joueur a une mine, sans menu ni chargement (portail cache, tests et administration). */
    public static boolean enterNow(ServerPlayer p, int region) {
        MinecraftServer server = p.level().getServer();
        if (region < 0 || region >= MineLayout.SECTORS || !built(server, region) || dim(server) == null) return false;
        arrive(p, region);
        return true;
    }

    // ================================================================== commandes

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("mine")
                .executes(c -> {
                    open(c.getSource().getPlayerOrException(), null);
                    return 1;
                })
                .then(Commands.literal("etat").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    MinecraftServer s = c.getSource().getServer();
                    int n = 0;
                    for (int r = 0; r < MineLayout.SECTORS; r++) if (built(s, r)) n++;
                    String msg = (building() ? "Construction en cours : mine " + (job.region() + 1) + ", " + percent() + " %. " : "") + n + "/" + MineLayout.SECTORS + " mines construites (plan " + VERSION + "), "
                            + MineData.get(s).respawns().size() + " minerai(s) en attente de repousse.";
                    c.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
                .then(Commands.literal("reconstruire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("numero", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, MineLayout.SECTORS)).executes(c -> {
                            boolean ok = start(c.getSource().getServer(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "numero") - 1);
                            c.getSource().sendSuccess(() -> Component.literal(ok ? "Reconstruction de la mine lancée." : "Impossible : une construction est déjà en cours."), true);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("aller").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("numero", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, MineLayout.SECTORS)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return enterNow(p, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "numero") - 1) ? 1 : 0;
                        })))
                .then(Commands.literal("debloquer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgPlayers.get(p).minesUnlocked = MineLayout.SECTORS;
                    RpgPlayers.get(p).dirty = true;
                    c.getSource().sendSuccess(() -> Component.literal("Toutes les mines sont ouvertes."), false);
                    return 1;
                })));
    }
}
