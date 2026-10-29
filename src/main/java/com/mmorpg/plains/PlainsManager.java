package com.mmorpg.plains;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.farm.AeloriaCropBlock;
import com.mmorpg.farm.FarmCrops;
import com.mmorpg.hub.HubManager;
import com.mmorpg.hub.HubMarket;
import com.mmorpg.hub.SpawnBuilder;
import com.mmorpg.hub.SpawnData;
import com.mmorpg.mine.MineTools;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.plains.PlainsLayout.Sector;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.zone.ZoneData;
import com.mmorpg.zone.ZoneManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.mmorpg.plains.PlainsLayout.*;

/**
 * Les Plaines d'Aeloria : six grandes plaines a ciel ouvert (une par palier de niveau, dimension des donjons) pour farmer les monstres
 * sans descendre dans la mine. La premiere est ouverte d'office ; chacune cache un portail vers la suivante, que le joueur doit trouver en
 * se promenant (sa decouverte debloque la plaine pour toujours). Les monstres reapparaissent autour des joueurs, par zones de niveau, et les
 * champs de cultures se recoltent comme des veines : la plante est donnee directement puis repousse. Rien n'y peut etre casse ou pose.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlainsManager {
    /** A incrementer quand le plan change : les plaines sont alors reconstruites au demarrage. */
    public static final int VERSION = 2;
    private static final int MOB_CAP = 48;
    /** Portail du Spawn : colonne locale (relative a l'origine du spawn) du voile vert, face a l'est. */
    public static final int SPAWN_GATE_X = HubMarket.CX - 24, SPAWN_GATE_Z = -18;
    /** Decalage ajoute au numero de plaine dans le paquet {@code MineEnter} (les mines gardent 0 a 4). */
    public static final int PACKET_BASE = 100;
    private static final int[] COLORS = {0x5AE05A, 0xE8E8E8, 0x8CD8FF, 0xFFA040, 0xC080FF, 0xFF70D0};
    /** Secondes avant qu'une culture recoltee soit de nouveau mure, par rang de plante (Lueur a Neant). */
    private static final int[] CROP_COOLDOWN = {40, 55, 75, 95, 120};

    /** Temps minimal de l'ecran de chargement (ticks) et marque de ses titres (voir LoadingClient.PLAIN_MARK : elle n'est jamais affichee). */
    private static final int LOADING_TICKS = 80;
    private static final String MARK = "\u2698 ";

    private record Transition(UUID player, int region, long teleportAt, String title, String lore, int color) {
    }

    private static final java.util.List<Transition> TRANSITIONS = new ArrayList<>();
    private static PlainsBuilder job;
    private static long jobStart;
    private static int lastLogged = -1;
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private PlainsManager() {
    }

    public static ServerLevel dim(MinecraftServer server) {
        return server.getLevel(InstanceManager.DIM);
    }

    /** Numero de la plaine ou se trouve l'entite, ou -1. */
    public static int regionOf(Entity e) {
        return e.level().dimension().equals(InstanceManager.DIM) ? regionAt(e.getX(), e.getZ()) : -1;
    }

    public static boolean inPlains(Entity e) {
        return regionOf(e) >= 0;
    }

    /** Vrai si la position est dans une plaine (utilise par les cultures pour ne pas pousser d'elles-memes). */
    public static boolean inPlains(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.dimension().equals(InstanceManager.DIM) && regionAt(pos.getX() + 0.5, pos.getZ() + 0.5) >= 0;
    }

    public static boolean built(MinecraftServer server, int region) {
        return PlainsData.get(server).version(region) == VERSION;
    }

    public static boolean allBuilt(MinecraftServer server) {
        for (int r = 0; r < REGIONS; r++) if (!built(server, r)) return false;
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
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.PLAINS)) return;
        MinecraftServer server = event.getServer();
        job = null;
        ZoneData zones = ZoneData.get(server);
        for (String old : new String[]{"plaines_entree", "plaines_prairies", "plaines_marais", "plaines_os", "plaines_givre", "plaines_cendres", "plaines_astre"}) zones.remove(old);
        registerZones(server);
    }

    /** Lance la construction d'une plaine (quelques secondes a quelques minutes). */
    public static boolean start(MinecraftServer server, int region) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.PLAINS)) return false;
        ServerLevel level = dim(server);
        if (level == null || job != null) return false;
        PlainsData data = PlainsData.get(server);
        data.setVersion(region, 0);
        int x0 = PlainsLayout.get(region).x0();
        data.respawns().removeIf(r -> r.x() >= x0 && r.x() < x0 + W);
        data.setDirty();
        job = new PlainsBuilder(level, region);
        jobStart = System.nanoTime();
        lastLogged = -1;
        MMORPG.LOGGER.info("[MMORPG] Construction de la plaine {} ({}) : {} x {} blocs", region + 1, STYLES.get(region).name(), W, L);
        return true;
    }

    /** Deux zones par plaine (place d'entree, puis le reste) ; on n'y construit pas et on ne s'y bat pas entre joueurs, mais les monstres y vivent. */
    public static void registerZones(MinecraftServer server) {
        ZoneData zones = ZoneData.get(server);
        for (Sector s : STYLES) {
            int x0 = PlainsLayout.get(s.index()).x0();
            zones.add(new ZoneData.Zone("plaines_" + s.index() + "_entree", "Place de " + s.name(), InstanceManager.DIM, x0, Z0, x0 + W - 1, Z0 + ENTRY - 1,
                    true, true, true, ZoneData.Zone.STYLE_ZONE, "Le portail de retour vers le Spawn d'Aeloria"));
            zones.add(new ZoneData.Zone("plaines_" + s.index(), s.name(), InstanceManager.DIM, x0, Z0 + ENTRY, x0 + W - 1, Z0 + L - 1,
                    true, true, false, ZoneData.Zone.STYLE_DUNGEON, s.lore() + "|Plaine " + (s.index() + 1) + " · niveaux " + s.minLevel() + " à " + s.maxLevel()));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.PLAINS)) return;
        MinecraftServer server = event.getServer();
        transitions(server);
        if (job != null) {
            if (job.step(22_000_000L)) {
                int r = job.region();
                PlainsData.get(server).setVersion(r, VERSION);
                MMORPG.LOGGER.info("[MMORPG] Plaine {} construite : {} blocs en {} s", r + 1, job.blocksPlaced(), (System.nanoTime() - jobStart) / 1_000_000_000L);
                job = null;
            } else if (job.percent() / 25 != lastLogged) {
                lastLogged = job.percent() / 25;
                MMORPG.LOGGER.info("[MMORPG] Plaine {} : {} %", job.region() + 1, job.percent());
            }
            return;
        }
        for (int r = 0; r < REGIONS && job == null && !com.mmorpg.mine.MineManager.building(); r++) {          // une construction a la fois dans la dimension
            if (!built(server, r)) start(server, r);
        }
        if (job != null) return;
        if (server.getTickCount() % 8 == 0) portals(server);
        ServerLevel level = dim(server);
        if (level == null || level.players().isEmpty()) return;
        long t = server.getTickCount();
        if (t % 20 == 0) respawns(server, level);
        if (t % 40 == 0) spawnMobs(level);
    }

    // ================================================================== menu, portails et arrivee

    /** Menu de selection des six plaines (portail du Spawn, /plaines). */
    public static void open(ServerPlayer p) {
        if (!com.mmorpg.server.PortalAccess.require(p,"plaines")) return;
        PlayerData d = RpgPlayers.get(p);
        MinecraftServer server = p.level().getServer();
        ListTag list = new ListTag();
        for (Sector s : STYLES) {
            boolean locked = s.index() >= d.plainsUnlocked;
            CompoundTag e = new CompoundTag();
            e.putInt("index", s.index());
            e.putString("label", s.name());
            e.putString("lore", s.lore());
            e.putInt("color", COLORS[s.index()]);
            e.putInt("min", s.minLevel());
            e.putInt("max", s.maxLevel());
            e.putBoolean("locked", locked);
            FarmCrops.Crop a = FarmCrops.crop(s.cropA()), b = FarmCrops.crop(s.cropB());
            e.putString("icon", a.product());
            e.putString("lockedHint", s.index() == 0 ? "" : "Trouvez le portail caché de « " + STYLES.get(s.index() - 1).name() + " » : il se promène hors des chemins.");
            ListTag info = new ListTag();
            info.add(StringTag.valueOf("Monstres de niveau " + s.minLevel() + " à " + s.maxLevel() + ", répartis en zones de niveau (un totem annonce chaque zone)."));
            info.add(StringTag.valueOf("Champs de cultures : " + (a == b ? a.name() : a.name() + " et " + b.name()) + ". La plante se récolte d'un coup et repousse (houe : récoltes en plus et récolte en zone)."));
            info.add(StringTag.valueOf("Campements au bord du chemin, lacs, bosquets et rochers."));
            info.add(StringTag.valueOf(s.index() < REGIONS - 1 ? "Un portail caché mène à « " + STYLES.get(s.index() + 1).name() + " » : à vous de le trouver." : "La dernière plaine : son autel garde encore ses secrets."));
            e.put("info", info);
            list.add(e);
        }
        CompoundTag data = new CompoundTag();
        data.putInt("npc", -1);
        data.putInt("base", PACKET_BASE);
        data.putString("name", "Plaines d'Aeloria");
        data.putString("unit", "Plaine");
        data.putString("tagline", "Trouvez le portail caché de chaque plaine pour ouvrir la suivante");
        data.putInt("playerLevel", d.level);
        data.putBoolean("built", allBuilt(server));
        data.putInt("percent", percent());
        data.put("sectors", list);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MINE_MASTER, data));
    }

    /** Ecran de chargement (comme les mines) puis arrivee sur la place d'entree de la plaine : menu, portail cache. */
    public static void startTransition(ServerPlayer p, int region) {
        if(!com.mmorpg.server.FeatureFlags.allow(p,"plaines"))return;
        MinecraftServer server = p.level().getServer();
        if (dim(server) == null || region < 0 || region >= REGIONS) return;
        if (built(server,region)) {
            var point=new BlockPos(PlainsLayout.get(region).x0()+PLAZA_X,BASE,Z0+PLAZA_Z);var world=dim(server);world.getChunkAt(point);
            int missing=0;for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(world.getBlockState(point.offset(dx,0,dz)).isAir())missing++;
            if(missing==25){PlainsData.get(server).setVersion(region,0);com.mmorpg.MMORPG.LOGGER.warn("Plain {} marked built but entry terrain missing: scheduling reconstruction",region);}
        }
        Sector s = STYLES.get(region);
        String title = MARK + s.name();
        long at = server.overworld().getGameTime() + LOADING_TICKS;
        TRANSITIONS.removeIf(x -> x.player().equals(p.getUUID()));
        TRANSITIONS.add(new Transition(p.getUUID(), region, at, title, s.lore(), COLORS[region]));
        com.mmorpg.dungeon.BossReveal.hold(p, LOADING_TICKS + 400);
        Net.toPlayer(p, new com.mmorpg.network.Payloads.Loading(com.mmorpg.network.Payloads.Loading.START, title, s.lore(), COLORS[region], 0));
    }

    public static boolean transitioning(ServerPlayer p) {
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
            if (!built(server, x.region())) {                                                    // carte pas encore construite : la barre suit la construction
                if (now % 4 == 0) Net.toPlayer(p, new com.mmorpg.network.Payloads.Loading(com.mmorpg.network.Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(),
                        job != null && job.region() == x.region() ? Math.min(99, percent()) : 0));
            } else if (now >= x.teleportAt()) {
                Net.toPlayer(p, new com.mmorpg.network.Payloads.Loading(com.mmorpg.network.Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), 100));
                com.mmorpg.dungeon.BossReveal.release(p);
                enter(p, x.region());
                Net.toPlayer(p, new com.mmorpg.network.Payloads.Loading(com.mmorpg.network.Payloads.Loading.END, x.title(), x.lore(), x.color(), 100));
                it.remove();
            } else if (now % 4 == 0) {
                int pct = (int) Math.max(0, 100 - 100L * (x.teleportAt() - now) / LOADING_TICKS);
                Net.toPlayer(p, new com.mmorpg.network.Payloads.Loading(com.mmorpg.network.Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), pct));
            }
        }
    }

    /** Choix du joueur dans le menu : verifications cote serveur puis arrivee sur la place d'entree de la plaine. */
    public static void enterChosen(ServerPlayer p, int region) {
        if (!com.mmorpg.server.PortalAccess.consume(p,"plaines")) return;
        if (com.mmorpg.network.GroupTravel.begin(p,"plaines",Integer.toString(region))) return;
        com.mmorpg.server.AdventureSummary.begin(p,"plaines");
        if (region < 0 || region >= REGIONS) return;
        if (region >= RpgPlayers.get(p).plainsUnlocked) {
            p.sendOverlayMessage(Component.literal("Cette plaine n'est pas encore découverte : trouvez le portail caché de la précédente.").withColor(0xFF7060));
            return;
        }
        if (com.mmorpg.network.NetworkPlayers.route(p, com.mmorpg.network.NetworkConfig.Role.PLAINS, "plains:" + region)) return;
        startTransition(p, region);
    }

    private static AABB spawnGate(BlockPos o) {
        double x = o.getX() + SPAWN_GATE_X;
        double z = o.getZ() + SPAWN_GATE_Z;
        return new AABB(x - 0.5, o.getY(), z - 2, x + 3.2, o.getY() + 9, z + 3);
    }

    private static AABB arch(PlainsLayout lay) {
        return new AABB(lay.x0() + PLAZA_X - 5, BASE + 1, Z0 + PlainsBuilder.ARCH_Z - 1, lay.x0() + PLAZA_X + 6, BASE + 10, Z0 + PlainsBuilder.ARCH_Z + 3.2);
    }

    private static AABB exitGate(PlainsLayout lay) {
        double x = lay.x0() + lay.exitX;
        int gh = lay.height(lay.exitX, lay.exitZ);
        return new AABB(x - 3.5, gh, Z0 + lay.exitZ - 2.4, x + 4.5, gh + 9, Z0 + lay.exitZ + 1.0);
    }

    private static void portals(MinecraftServer server) {
        SpawnData spawn = SpawnData.get(server);
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {     // copie : un portail deplace le joueur pendant le parcours
            if (p.isSpectator() || p.isPassenger() || com.mmorpg.network.NetworkPlayers.portalBlocked(p)) continue;
            ServerLevel level = p.level();
            boolean inSpawn = spawn.exists() && level.dimension().equals(SpawnBuilder.DIM);
            int region = inSpawn ? -1 : regionOf(p);
            if (!inSpawn && region < 0) continue;
            long now = server.getTickCount();
            if (inSpawn) {
                BlockPos o = spawn.origin();
                if (p.distanceToSqr(o.getX() + SPAWN_GATE_X + 0.5, o.getY() + 4, o.getZ() + SPAWN_GATE_Z + 0.5) < 26 * 26) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX() + SPAWN_GATE_X + 0.5, o.getY() + 3.5, o.getZ() + SPAWN_GATE_Z + 0.5, 6, 0.3, 2.5, 1.8, 0.0);
                }
                if (!spawnGate(o).contains(p.getX(), p.getY(), p.getZ()) || cooling(p, now)) continue;
                level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.5f);
                if (com.mmorpg.config.ConfigManager.spawn().manualBuild) continue;
                com.mmorpg.server.PortalAccess.grant(p,"plaines");
                open(p);
                continue;
            }
            PlainsLayout lay = PlainsLayout.get(region);
            // portail cache : particules a l'approche, decouverte et passage a la plaine suivante
            if (region < REGIONS - 1) {
                double ex = lay.x0() + lay.exitX + 0.5, ez = Z0 + lay.exitZ + 0.5, ey = lay.height(lay.exitX, lay.exitZ) + 4;
                if (p.distanceToSqr(ex, ey, ez) < 46 * 46) level.sendParticles(ParticleTypes.PORTAL, ex, ey, ez, 8, 2.5, 3.5, 0.3, 0.2);
                if (exitGate(lay).contains(p.getX(), p.getY(), p.getZ()) && !transitioning(p) && !cooling(p, now)) {
                    level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.2f);
                    discover(p, region + 1);
                    com.mmorpg.server.PortalAccess.grant(p,"plaines");
                    enterChosen(p, region + 1);
                    continue;
                }
            }
            if (arch(lay).contains(p.getX(), p.getY(), p.getZ()) && !cooling(p, now)) {
                level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.5f);
                HubManager.goToSpawn(p);
            }
        }
    }

    private static boolean cooling(ServerPlayer p, long now) {
        Long last = COOLDOWN.get(p.getUUID());
        if (last != null && now - last < 60) return true;
        COOLDOWN.put(p.getUUID(), now);
        return false;
    }

    /** Premiere fois que le joueur traverse le portail cache : la plaine suivante lui est ouverte pour toujours. */
    private static void discover(ServerPlayer p, int region) {
        PlayerData d = RpgPlayers.get(p);
        if (d.plainsUnlocked > region) return;
        d.plainsUnlocked = region + 1;
        d.dirty = true;
        Sector s = STYLES.get(region);
        p.sendSystemMessage(Component.literal("✦ Nouvelle plaine découverte : " + s.name() + " (niveaux " + s.minLevel() + " à " + s.maxLevel() + ")").withColor(COLORS[region]));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    /** Envoie le joueur sur la place d'entree d'une plaine. @return faux si elle n'est pas encore construite */
    public static boolean enter(ServerPlayer p, int region) {
        if(!com.mmorpg.server.FeatureFlags.allow(p,"plaines"))return false;
        MinecraftServer server = p.level().getServer();
        ServerLevel level = dim(server);
        if (level == null || region < 0 || region >= REGIONS) return false;
        if (!built(server, region)) {
            p.sendOverlayMessage(Component.literal("Cette plaine est en construction" + (job != null && job.region() == region ? " (" + percent() + " %)" : "") + ", revenez dans un instant.").withColor(0xFFC060));
            return false;
        }
        if (com.mmorpg.hub.CinematicManager.isLocked(p)) return true;
        if (com.mmorpg.hub.ClassTrial.active(p)) com.mmorpg.hub.ClassTrial.end(p, false, null);
        p.stopRiding();
        PlainsLayout lay = PlainsLayout.get(region);
        double x = lay.x0() + PLAZA_X + 0.5, y = BASE + 1, z = Z0 + PLAZA_Z + 0.5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(((int) x + dx * 16) >> 4, ((int) z + dz * 16) >> 4);
        }
        ZoneManager.forgetAnnouncements(p.getUUID());
        COOLDOWN.put(p.getUUID(), (long) server.getTickCount());
        p.teleportTo(level, x, y, z, Set.of(), 0F, 4F, true);
        p.resetFallDistance();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 24, 0.6, 0.9, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.RISE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    // ================================================================== monstres

    private static void spawnMobs(ServerLevel level) {
        var random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            int region = regionOf(p);
            if (region < 0 || p.isSpectator()) continue;
            PlainsLayout lay = PlainsLayout.get(region);
            int lz0 = (int) p.getZ() - Z0;
            if (lz0 < ENTRY + 4) continue;                                                           // la place d'entree est sure
            AABB area = p.getBoundingBox().inflate(40, 26, 40);
            int near = level.getEntitiesOfClass(Mob.class, area, m -> m instanceof com.mmorpg.entity.RpgMob).size();
            for (int i = 0; i < 9 && near < MOB_CAP; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double dist = 16 + random.nextDouble() * 20;
                int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
                int lx = x - lay.x0(), lz = z - Z0;
                if (!inside(lx, lz) || edge(lx, lz) < WALL + 4 || lz < ENTRY + 4) continue;
                int h = lay.height(lx, lz);
                if (h < BASE) continue;                                                              // pas dans les lacs
                if (Math.hypot(lx - lay.exitX, lz - lay.exitZ) < 12) continue;                       // ni dans le sanctuaire du portail
                BlockPos pos = new BlockPos(x, h + 1, z);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                String key = lay.style.mobs().get(random.nextInt(lay.style.mobs().size()));
                if (MobSpawner.spawnForCommand(level, key, pos, lay.mobLevel(lx, lz, new Random(random.nextLong()))) != null) near++;
            }
        }
    }

    // ================================================================== cultures : recolte a la facon des veines

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof AeloriaCropBlock crop)) return;
        if (!(event.getPlayer() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!inPlains(level, event.getPos())) return;
        if (sp.isCreative() && sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;      // un batisseur peut casser le bloc
        event.setCanceled(true);
        harvest(level, sp, event.getPos(), crop, state);
    }

    /** Les champs ne se piétinent pas : la terre labourée reste intacte. */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && inPlains(level, event.getPos())) event.setCanceled(true);
    }

    /** Recolte une culture mure (et, avec une houe d'Argentite ou mieux, celles qui l'entourent) ; elle repoussera comme une veine. */
    public static boolean harvest(ServerLevel level, ServerPlayer p, BlockPos pos, AeloriaCropBlock block, BlockState st) {
        ItemStack tool = p.getMainHandItem();
        int hoe = MineTools.hoeTier(tool);
        if (!gather(level, p, pos, block, st, hoe)) return false;
        if (hoe >= 1) tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        if (hoe < 2) return true;
        int r = Math.min(4,(hoe >= 4 ? 2 : 1)+com.mmorpg.mine.ToolRunes.rank(p.getMainHandItem(),2)/2);
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos at = pos.offset(dx, 0, dz);
                BlockState s2 = level.getBlockState(at);
                if (s2.getBlock() instanceof AeloriaCropBlock cb && cb.isMaxAge(s2) && gather(level, p, at, cb, s2, hoe)) n++;
            }
        }
        if (n > 0) p.sendOverlayMessage(Component.literal("Récolte en zone : +" + n + " plante" + (n > 1 ? "s" : "")).withColor(block.crop().color()));
        return true;
    }

    private static boolean gather(ServerLevel level, ServerPlayer p, BlockPos pos, AeloriaCropBlock block, BlockState st, int hoe) {
        FarmCrops.Crop crop = block.crop();
        if (!block.isMaxAge(st)) {
            p.sendOverlayMessage(Component.literal(crop.name() + " : pas encore mûre.").withColor(0xFFC060));
            return false;
        }
        PlayerData d = RpgPlayers.get(p);
        if (d.level < crop.minLevel()) {
            p.sendOverlayMessage(Component.literal("Niveau " + crop.minLevel() + " requis pour récolter la " + crop.name() + ".").withColor(0xFF7060));
            return false;
        }
        var random = level.getRandom();
        int rank = FarmCrops.CROPS.indexOf(crop);
        int count = 1 + (random.nextInt(100) < 12 + rank * 2 + hoe * 4 + com.mmorpg.mine.ToolRunes.rank(p.getMainHandItem(),0)*10 ? 1 : 0);
        for (int i = 0; i < 2; i++) if (random.nextInt(100) < hoe * 10) count++;
        if (com.mmorpg.job.JobSystem.rollDouble(p, com.mmorpg.job.Job.FERMIER, random)) count *= 2;
        ItemStack drop = new ItemStack(ModItems.get(crop.product()), count);
        if (!p.getInventory().add(drop)) level.addFreshEntity(new ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), drop));
        com.mmorpg.server.AdventureSummary.item(p,ModItems.get(crop.product()),count-drop.getCount());
        RpgPlayers.giveGatherXp(p, 2 + rank * 3L);
        com.mmorpg.job.JobSystem.addXp(p, com.mmorpg.job.Job.FERMIER, com.mmorpg.job.Job.cropXp(rank));
        com.mmorpg.job.JobSystem.farmerMeal(p, random);
        p.sendOverlayMessage(Component.literal("+" + count + " " + crop.name()).withColor(crop.color()));
        level.setBlock(pos, block.getStateForAge(0), 3);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.3F);
        long wait = (long) (CROP_COOLDOWN[Math.min(rank, CROP_COOLDOWN.length - 1)] * 20 * (0.8 + random.nextDouble() * 0.4));
        PlainsData.get(level.getServer()).add(new PlainsData.Respawn(pos.getX(), pos.getY(), pos.getZ(), level.getGameTime() + wait));
        return true;
    }

    private static void respawns(MinecraftServer server, ServerLevel level) {
        PlainsData data = PlainsData.get(server);
        if (data.respawns().isEmpty()) return;
        long now = level.getGameTime();
        List<PlainsData.Respawn> due = new ArrayList<>();
        for (PlainsData.Respawn r : data.respawns()) {                       // seules les entrees dont le chunk est charge comptent : les autres n'en bloquent pas d'autres
            if (r.due() <= now && level.hasChunkAt(r.pos())) due.add(r);
            if (due.size() >= 120) break;
        }
        for (PlainsData.Respawn r : due) {
            BlockPos pos = r.pos();
            if (!level.hasChunkAt(pos)) continue;                          // elle repoussera au prochain passage
            data.remove(r);
            if (level.getBlockState(pos).getBlock() instanceof AeloriaCropBlock cb) {
                level.setBlock(pos, cb.getStateForAge(cb.getMaxAge()), 3);
                if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 24, false) != null) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.0);
                }
            }
        }
    }

    /** Test : fait repousser tout de suite les cultures recoltees. */
    public static int respawnAllNow(MinecraftServer server) {
        ServerLevel level = dim(server);
        PlainsData data = PlainsData.get(server);
        int n = data.respawns().size();
        List<PlainsData.Respawn> all = new ArrayList<>(data.respawns());
        data.clear();
        for (PlainsData.Respawn r : all) data.add(new PlainsData.Respawn(r.x(), r.y(), r.z(), level == null ? 0 : level.getGameTime() - 1));
        return n;
    }

    // ================================================================== commandes

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("plaines")
                .executes(c -> {
                    open(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("etat").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    MinecraftServer s = c.getSource().getServer();
                    int n = 0;
                    for (int r = 0; r < REGIONS; r++) if (built(s, r)) n++;
                    String msg = (building() ? "Construction en cours : plaine " + (job.region() + 1) + ", " + percent() + " %. " : "") + n + "/" + REGIONS + " plaines construites (plan " + VERSION + "), "
                            + PlainsData.get(s).respawns().size() + " culture(s) en attente de repousse.";
                    c.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
                .then(Commands.literal("reconstruire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("numero", IntegerArgumentType.integer(1, REGIONS)).executes(c -> {
                            boolean ok = start(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "numero") - 1);
                            c.getSource().sendSuccess(() -> Component.literal(ok ? "Reconstruction de la plaine lancée." : "Impossible : une construction est déjà en cours."), true);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("aller").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("numero", IntegerArgumentType.integer(1, REGIONS)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return enter(p, IntegerArgumentType.getInteger(c, "numero") - 1) ? 1 : 0;
                        })))
                .then(Commands.literal("debloquer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgPlayers.get(p).plainsUnlocked = REGIONS;
                    RpgPlayers.get(p).dirty = true;
                    c.getSource().sendSuccess(() -> Component.literal("Toutes les plaines sont ouvertes."), false);
                    return 1;
                })));
    }
}
