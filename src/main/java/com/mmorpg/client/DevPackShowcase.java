package com.mmorpg.client;

import com.mmorpg.entity.NpcEntity;
import com.mmorpg.entity.mob.ImportedMobs;
import com.mmorpg.registry.DecorBlocks;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;

/** Vitrine du 2e lot de modeles (-Pshowcase=modeles2) : decor, PNJ et monstres, rendus cote client uniquement. */
final class DevPackShowcase {
    private static int tick, wait;
    private static boolean loadingSeen;
    private static final String[] DECOR = {"chaise_aeloria", "table_aeloria", "armoire_aeloria", "feu_de_camp", "buche_camp", "jarre_ancienne",
            "pot_decore", "panneau_feuille", "viande_deco", "potion_etoilee", "panneau_quetes"};

    private DevPackShowcase() {
    }

    static void phase(Minecraft mc, String me) {
        if (com.mmorpg.client.LoadingClient.active()) {
            loadingSeen = true;
            wait = 0;
            return;
        }
        if (!loadingSeen && ++wait < 1800) return;
        int t = ++tick;
        var level = mc.level;
        if (level == null || mc.player == null) return;
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.player.setYRot(0);
            mc.player.setXRot(12);
        }
        var look = Direction.fromYRot(mc.player.getYRot());
        Direction right = look.getClockWise();
        BlockPos base = mc.player.blockPosition().relative(look, 7);
        if (t == 20) {
            for (int i = 0; i < DECOR.length; i++) {
                BlockPos pos = base.relative(right, (i - 5) * 3);
                level.setBlock(pos, DecorBlocks.state(DECOR[i]).setValue(com.mmorpg.block.DecorPropBlock.FACING, look.getOpposite()), 3);
            }
        }
        if (t == 30) {
            for (int i = 0; i < 4; i++) {
                var n = new NpcEntity(ModEntities.NPC.get(), level);
                n.setId(-6200 - i);
                n.setup(NpcEntity.Role.QUETES, "PNJ", 14 + i, "");
                var np = base.relative(look.getOpposite(), 4).relative(right, (i - 2) * 3);
                place(mc, n, np.getX() + .5, base.getY(), np.getZ() + .5, look);
            }
            int i = 0;
            for (var key : new String[]{"fernling", "champion_gobelin", "voyageur_squelette", "sentinelle_hibou", "minotaure", "seigneur_squelette", "morghul", "gardien_citadelle"}) {
                var e = ModEntities.IMPORTED.get(key).get().create(level, EntitySpawnReason.COMMAND);
                if (e == null) continue;
                e.setId(-6300 - i);
                var p = base.relative(look.getOpposite(), 9).relative(right, (i - 3) * 6);
                place(mc, e, p.getX() + .5, base.getY(), p.getZ() + .5, look);
                i++;
            }
        }
        if (t == 90) Minecraft.getInstance().gui.hud.toggle();
        if (t == 130) DevShowcase.shot(mc, "modeles2_vue");
        if (t == 140) {
            mc.player.setXRot(35);
            mc.player.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
        if (t == 170) DevShowcase.shot(mc, "modeles2_haut");
        if (t == 180) DevShowcase.stop(mc);
    }

    private static void place(Minecraft mc, net.minecraft.world.entity.Entity e, double x, double y, double z, Direction look) {
        e.setPos(x, y, z);
        e.setYRot(look.getOpposite().toYRot());
        e.setYHeadRot(e.getYRot());
        if (e instanceof net.minecraft.world.entity.LivingEntity l) {
            l.setYBodyRot(e.getYRot());
            l.yBodyRotO = l.yBodyRot;
        }
        if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
        mc.level.addEntity(e);
    }
}
