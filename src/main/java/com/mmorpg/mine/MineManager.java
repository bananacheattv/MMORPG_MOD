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
    public static final int VERSION = 3;
    private static final int MOB_CAP = 14;

    private static MineBuilder job;
    private static long jobStart;
    private static int lastLogged = -1;

    private MineManager() {
    }

    public static ServerLevel dim(MinecraftServer server) {
        return server.getLevel(InstanceManager.DIM);
    }

    public static boolean inMine(Entity e) {
        if (!e.level().dimension().equals(InstanceManager.DIM)) return false;
        return e.getX() >= MineLayout.X0 - 8 && e.getX() <= MineLayout.X0 + MineLayout.SIZE + 8
                && e.getZ() >= MineLayout.Z0 - 8 && e.getZ() <= MineLayout.Z0 + MineLayout.SIZE + 8;
    }

    public static boolean built(MinecraftServer server) {
        return MineData.get(server).version() == VERSION;
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
        if (!built(server)) start(server);
        else registerZones(server);
    }

    /** Lance la construction de la mine (la zone et les minerais existants sont remplaces). */
    public static boolean start(MinecraftServer server) {
        ServerLevel level = dim(server);
        if (level == null || job != null) return false;
        MineData data = MineData.get(server);
        data.setVersion(0);
        data.clear();
        job = new MineBuilder(level);
        jobStart = System.nanoTime();
        lastLogged = -1;
        MMORPG.LOGGER.info("[MMORPG] Construction des Mines d'Aeloria : {} x {} blocs, {} blocs de haut (quelques minutes)", MineLayout.SIZE, MineLayout.SIZE, MineLayout.HEIGHT);
        return true;
    }

    /** Une zone par etage (nom, ambiance et niveaux annonces a l'entree) ; la construction et la casse y sont interdites, pas les monstres. */
    public static void registerZones(MinecraftServer server) {
        ZoneData zones = ZoneData.get(server);
        for (Sector s : MineLayout.STYLES) {
            int z0 = MineLayout.Z0 + MineLayout.sectorStart(s.index());
            int z1 = MineLayout.Z0 + MineLayout.sectorEnd(s.index());
            zones.add(new ZoneData.Zone(s.id(), s.name(), InstanceManager.DIM, MineLayout.X0, z0, MineLayout.X0 + MineLayout.SIZE - 1, z1,
                    true, true, false, ZoneData.Zone.STYLE_DUNGEON, s.lore() + "|Mines d'Aeloria · niveaux " + s.minLevel() + " à " + s.maxLevel()));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        transitions(server);
        if (job != null) {
            if (job.step(22_000_000L)) {
                MineData.get(server).setVersion(VERSION);
                registerZones(server);
                MMORPG.LOGGER.info("[MMORPG] Mines d'Aeloria construites : {} blocs en {} s", job.blocksPlaced(), (System.nanoTime() - jobStart) / 1_000_000_000L);
                MMORPG.LOGGER.info("[MMORPG] Mines d'Aeloria : {} case(s) de caverne au contact du vide (attendu : 0)", MineLayout.get().leaks());
                job = null;
            } else if (job.percent() / 10 != lastLogged) {
                lastLogged = job.percent() / 10;
                MMORPG.LOGGER.info("[MMORPG] Mines d'Aeloria : {} %", job.percent());
            }
            return;
        }
        ServerLevel level = dim(server);
        if (level == null || level.players().isEmpty()) return;
        long t = server.getTickCount();
        if (t % 20 == 0) respawns(server, level);
        if (t % 40 == 0 && built(server)) spawnMobs(level);
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
        mine(level, sp, event.getPos(), ore.ore());
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
        RpgPlayers.giveXp(p, 3 + ore.minLevel() / 2L, false);
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
            {3.0, 0, 0}, {4.5, 0.5, 0}, {6.0, 1.2, 0.05}, {7.0, 2.0, 0.2}, {8.0, 3.0, 0.6}};

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
        for (MineData.Respawn r : data.respawns()) {
            if (r.due() <= now) due.add(r);
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
        MineLayout layout = MineLayout.get();
        var random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (!inMine(p) || p.isSpectator()) continue;
            int px = (int) p.getX() - MineLayout.X0, pz = (int) p.getZ() - MineLayout.Z0;
            if (Math.hypot(px - MineLayout.PLAZA_X, pz - MineLayout.PLAZA_Z) < 42) continue;           // la place d'entree est sure
            AABB area = p.getBoundingBox().inflate(36, 30, 36);
            if (level.getEntitiesOfClass(Mob.class, area, m -> m instanceof com.mmorpg.entity.RpgMob).size() >= MOB_CAP) continue;
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double dist = 16 + random.nextDouble() * 18;
                int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
                int lx = x - MineLayout.X0, lz = z - MineLayout.Z0;
                if (layout.kind(lx, lz) != 1 || layout.isPillar(lx, lz) || layout.liquid(lx, lz) > layout.floor(lx, lz)) continue;
                if (Math.hypot(lx - MineLayout.PLAZA_X, lz - MineLayout.PLAZA_Z) < 34) continue;
                BlockPos pos = new BlockPos(x, layout.floor(lx, lz) + 1, z);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                Sector s = layout.style(lx, lz);
                String key = s.mobs().get(random.nextInt(s.mobs().size()));
                int lvl = layout.mobLevel(lz, new Random(random.nextLong()));
                MobSpawner.spawnForCommand(level, key, pos, lvl);
            }
        }
    }

    // ================================================================== menu, chargement et arrivee

    /** Temps minimal de l'ecran de chargement (ticks) : assez pour lire le titre et l'ambiance de l'etage. */
    private static final int LOADING_TICKS = 80;
    /** Marque des titres de chargement des mines (voir LoadingClient.MINE_MARK : elle n'est jamais affichee). */
    private static final String MARK = "\u26CF ";

    private record Transition(UUID player, int sector, long teleportAt, String title, String lore, int color) {
    }

    private static final List<Transition> TRANSITIONS = new ArrayList<>();

    /** Menu du Maitre des mines (ou de la commande /mine quand {@code npc} est nul) : les cinq etages, leurs niveaux et leurs minerais. */
    public static void open(ServerPlayer p, NpcEntity npc) {
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
            e.putInt("need", needed(s));
            e.putString("ore", ore.name());
            e.putInt("oreLevel", ore.minLevel());
            e.putInt("cooldown", ore.cooldown());
            list.add(e);
        }
        CompoundTag data = new CompoundTag();
        data.putInt("npc", npc == null ? -1 : npc.getId());
        data.putString("name", npc == null ? "Les mines d'Aeloria" : npc.displayName());
        data.putInt("playerLevel", RpgPlayers.get(p).level);
        data.putBoolean("built", built(p.level().getServer()) && !building());
        data.putInt("percent", percent());
        data.put("sectors", list);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MINE_MASTER, data));
    }

    /** Niveau requis pour rejoindre un etage directement (le premier est ouvert a tous). */
    public static int needed(Sector s) {
        return s.index() == 0 ? 1 : s.minLevel();
    }

    /** Choix du joueur dans le menu : verifications cote serveur puis ecran de chargement et arrivee a l'etage. */
    public static void enter(ServerPlayer p, Payloads.MineEnter packet) {
        if (packet.npc() >= 0) {
            Entity e = p.level().getEntity(packet.npc());
            if (!(e instanceof NpcEntity npc) || npc.role() != NpcEntity.Role.MINES || npc.distanceToSqr(p) > 100) return;
        }
        int k = packet.sector();
        if (k < 0 || k >= MineLayout.SECTORS) return;
        Sector s = MineLayout.STYLES.get(k);
        if (RpgPlayers.get(p).level < needed(s) && !p.hasInfiniteMaterials()) {
            p.sendOverlayMessage(Component.literal("Niveau " + needed(s) + " requis pour descendre à cet étage.").withColor(0xFF5050));
            return;
        }
        MinecraftServer server = p.level().getServer();
        if (dim(server) == null) return;
        if (!built(server) && job == null) start(server);
        MineBlocks.Ore ore = MineBlocks.ore(s.ore());
        String title = MARK + s.name();
        long at = server.overworld().getGameTime() + LOADING_TICKS;
        TRANSITIONS.removeIf(x -> x.player().equals(p.getUUID()));
        TRANSITIONS.add(new Transition(p.getUUID(), k, at, title, s.lore(), ore.color()));
        com.mmorpg.dungeon.BossReveal.hold(p, LOADING_TICKS + 400);
        Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.START, title, s.lore(), ore.color(), built(server) && !building() ? 0 : percent()));
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
            boolean ready = built(server) && !building();
            if (!ready) {
                if (now % 4 == 0) Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), Math.min(99, percent())));
            } else if (now >= x.teleportAt()) {
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), 100));
                com.mmorpg.dungeon.BossReveal.release(p);
                arrive(p, x.sector());
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.END, x.title(), x.lore(), x.color(), 100));
                it.remove();
            } else if (now % 4 == 0) {
                int pct = (int) Math.max(0, 100 - 100L * (x.teleportAt() - now) / LOADING_TICKS);
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), pct));
            }
        }
    }

    /** Point d'arrivee d'un etage : la place d'entree pour le premier, sinon un sol bien plat pres de l'entree de l'etage. */
    public static double[] spawnPoint(int sector) {
        MineLayout layout = MineLayout.get();
        int cx = MineLayout.PLAZA_X, cz = sector == 0 ? MineLayout.PLAZA_Z - 7 : MineLayout.sectorStart(sector) + 16;
        for (int r = 0; r < 70; r += 2) {
            int steps = r == 0 ? 1 : 12;
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps;
                int lx = cx + (int) Math.round(Math.cos(a) * r), lz = cz + (int) Math.round(Math.sin(a) * r);
                if (layout.kind(lx, lz) != 1 || layout.isPillar(lx, lz) || layout.liquid(lx, lz) > layout.floor(lx, lz)) continue;
                boolean flat = true;
                for (int dx = -2; dx <= 2 && flat; dx++) {
                    for (int dz = -2; dz <= 2 && flat; dz++) {
                        flat = layout.kind(lx + dx, lz + dz) == 1 && !layout.isPillar(lx + dx, lz + dz)
                                && Math.abs(layout.floor(lx + dx, lz + dz) - layout.floor(lx, lz)) <= 1
                                && layout.liquid(lx + dx, lz + dz) <= layout.floor(lx + dx, lz + dz);
                    }
                }
                if (flat) return new double[]{MineLayout.X0 + lx + 0.5, layout.floor(lx, lz) + 1, MineLayout.Z0 + lz + 0.5};
            }
        }
        return new double[]{MineLayout.X0 + cx + 0.5, layout.floor(cx, cz) + 1, MineLayout.Z0 + cz + 0.5};
    }

    private static void arrive(ServerPlayer p, int sector) {
        ServerLevel level = dim(p.level().getServer());
        if (level == null) return;
        double[] at = spawnPoint(sector);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(((int) at[0] + dx * 16) >> 4, ((int) at[2] + dz * 16) >> 4);
        }
        ZoneManager.forgetAnnouncements(p.getUUID());               // l'annonce de l'etage (titre et ambiance) se rejoue a l'arrivee
        p.teleportTo(level, at[0], at[1], at[2], Set.of(), 0F, 6F, true);
        p.resetFallDistance();
        level.sendParticles(ParticleTypes.END_ROD, at[0], at[1] + 1, at[2], 30, 0.6, 0.9, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.RISE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Envoie directement le joueur a un etage, sans menu ni chargement (tests et administration). */
    public static boolean enterNow(ServerPlayer p, int sector) {
        MinecraftServer server = p.level().getServer();
        if (!built(server) || building() || dim(server) == null) return false;
        arrive(p, sector);
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
                    String msg = building() ? "Construction en cours : " + percent() + " %" : built(s) ? "Mines construites (plan " + VERSION + "), "
                            + MineData.get(s).respawns().size() + " minerai(s) en attente de repousse." : "Mines non construites.";
                    c.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
                .then(Commands.literal("reconstruire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    boolean ok = start(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Reconstruction des mines lancée (quelques minutes)." : "Impossible : construction déjà en cours ou dimension absente."), true);
                    return ok ? 1 : 0;
                })));
    }
}
