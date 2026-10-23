package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.mine.MineLayout.Sector;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.zone.ZoneData;
import com.mmorpg.zone.ZoneManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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

/**
 * Les Mines d'Aeloria : une immense caverne partagee (dimension des donjons) dont les minerais se mangent a la pioche. Extraire un minerai
 * donne directement le lingot ; le bloc devient une « veine epuisee » puis repousse apres un temps de recharge (conserve dans le monde).
 * Le gestionnaire construit la mine au premier demarrage, enregistre ses zones, fait repousser les minerais et fait apparaitre des
 * monstres dont le niveau suit la profondeur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MineManager {
    /** A incrementer quand le plan change : la mine est alors reconstruite au demarrage. */
    public static final int VERSION = 1;
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
        if (job != null) {
            if (job.step(22_000_000L)) {
                MineData.get(server).setVersion(VERSION);
                registerZones(server);
                MMORPG.LOGGER.info("[MMORPG] Mines d'Aeloria construites : {} blocs en {} s", job.blocksPlaced(), (System.nanoTime() - jobStart) / 1_000_000_000L);
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
        PlayerData d = RpgPlayers.get(p);
        if (d.level < ore.minLevel()) {
            p.sendOverlayMessage(Component.literal("Niveau " + ore.minLevel() + " requis pour extraire la " + ore.name() + ".").withColor(0xFF7060));
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.6F);
            return false;
        }
        var random = level.getRandom();
        int count = 1 + (random.nextInt(100) < 12 + ore.tier() * 2 ? 1 : 0);                  // veine riche : un lingot de plus
        ItemStack ingot = new ItemStack(ModItems.get(ore.ingotId()), count);
        if (!p.getInventory().add(ingot)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), ingot));
        tool.hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
        RpgPlayers.giveXp(p, 3 + ore.minLevel() / 2L, false);
        p.sendOverlayMessage(Component.literal("+" + count + " Lingot de " + ore.name() + (count > 1 ? "  (veine riche !)" : "")).withColor(ore.color()));
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

    // ================================================================== entree

    /** Envoie le joueur sur la place d'entree de la mine. */
    public static boolean enter(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = dim(server);
        if (level == null) return false;
        if (!built(server)) {
            if (job == null) start(server);
            p.sendSystemMessage(Component.literal("Les mines sont en construction (" + percent() + " %) : revenez dans quelques minutes.").withColor(0xFFC060));
            return false;
        }
        MineLayout layout = MineLayout.get();
        double x = MineLayout.X0 + MineLayout.PLAZA_X + 0.5, z = MineLayout.Z0 + MineLayout.PLAZA_Z - 7 + 0.5;
        double y = layout.floor(MineLayout.PLAZA_X, MineLayout.PLAZA_Z - 7) + 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk((int) Math.floor(x + dx * 16) >> 4, (int) Math.floor(z + dz * 16) >> 4);
        }
        ZoneManager.forgetAnnouncements(p.getUUID());
        p.teleportTo(level, x, y, z, Set.of(), 0F, 6F, true);
        p.resetFallDistance();
        return true;
    }

    // ================================================================== commandes

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("mine")
                .executes(c -> enter(c.getSource().getPlayerOrException()) ? 1 : 0)
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
