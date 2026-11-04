package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.registry.DecorBlocks;
import com.mmorpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Persistance des modeles 3D poses (-PmodelChecks=spawn puis -PmodelChecks=verify avec le meme monde) : monstres poses par oeuf,
 * PNJ aux nouvelles apparences et blocs de decor doivent encore etre la apres un redemarrage du serveur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevModelPersistChecks {
    private static final String MODE = System.getProperty("mmorpg.modelChecks", "");
    private static final String[] MOBS = {"fernling", "champion_gobelin", "voyageur_squelette", "sentinelle_hibou", "minotaure", "seigneur_squelette", "morghul", "gardien_citadelle"};
    private static final int SX = 40, SZ = 40;
    private static int ticks;
    private static boolean done;

    private static boolean ok = true;

    private static void check(boolean good, String what) {
        if (!good) ok = false;
        MMORPG.LOGGER.info("[CODEX CHECKS] MODELES {} : {}", good ? "ok" : "ECHEC", what);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (MODE.isEmpty() || done) return;
        ticks++;
        if (ticks == 1) {                                  // les entites ne se chargent qu'avec leur chunk : on le force
            for (int cx = (SX - 2) >> 4; cx <= (SX + 30) >> 4; cx++)
                for (int cz = (SZ - 2) >> 4; cz <= (SZ + 10) >> 4; cz++) event.getServer().overworld().setChunkForced(cx, cz, true);
        }
        if (ticks < (MODE.equals("spawn") ? 60 : 400)) return;
        done = true;
        var server = event.getServer();
        ServerLevel level = server.overworld();
        level.getChunk(SX >> 4, SZ >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SX, SZ);
        if (MODE.equals("spawn")) {
            for (int i = 0; i < MOBS.length; i++) {
                var pos = new BlockPos(SX + i * 2, y, SZ);
                var e = ModEntities.IMPORTED.get(MOBS[i]).get().spawn(level, pos, EntitySpawnReason.SPAWN_ITEM_USE);
                check(e != null && e.isPersistenceRequired(), MOBS[i] + " pose par oeuf, persistant");
                if (e != null) e.setNoAi(true);
            }
            for (int i = 0; i < 4; i++) {
                var n = new NpcEntity(ModEntities.NPC.get(), level);
                n.setup(NpcEntity.Role.QUETES, "PNJ " + i, 14 + i, "");
                n.setPos(SX + i * 2 + .5, y, SZ + 3);
                level.addFreshEntity(n);
            }
            String[] decor = {"chaise_aeloria", "table_aeloria", "armoire_aeloria", "feu_de_camp", "buche_camp", "jarre_ancienne", "pot_decore", "panneau_feuille", "viande_deco", "potion_etoilee", "panneau_quetes", "drapeau_aeloria", "camp_aeloria"};
            for (int i = 0; i < decor.length; i++) level.setBlock(new BlockPos(SX + i * 2, y, SZ + 6), DecorBlocks.state(decor[i]), 3);
            server.saveEverything(false, true, true);
            MMORPG.LOGGER.info("[CODEX CHECKS] MODELES {} : lot pose et sauvegarde", ok ? "PASS" : "ECHEC");
        } else {
            var box = new AABB(SX - 2, y - 40, SZ - 2, SX + 30, y + 60, SZ + 10);
            var mobs = level.getEntitiesOfClass(Mob.class, box);
            for (String key : MOBS) {
                check(mobs.stream().anyMatch(m -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath().equals(key)), key + " toujours present apres redemarrage");
            }
            for (int i = 0; i < 4; i++) {
                int skin = 14 + i;
                check(mobs.stream().anyMatch(m -> m instanceof NpcEntity n && n.skin() == skin), "PNJ apparence " + skin + " toujours present");
            }
            int blocks = 0;
            for (int i = 0; i < 13; i++) {
                var s = level.getBlockState(new BlockPos(SX + i * 2, y, SZ + 6));
                if (s.getBlock() instanceof com.mmorpg.block.DecorPropBlock || s.getBlock() instanceof com.mmorpg.block.DecorModelBlock) blocks++;
            }
            check(blocks == 13, "blocs de decor toujours poses (" + blocks + "/13)");
            MMORPG.LOGGER.info("[CODEX CHECKS] MODELES {} : persistance apres redemarrage", ok ? "PASS" : "ECHEC");
        }
        server.halt(false);
    }
}
