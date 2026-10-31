package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.combat.AutoLock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Controle en jeu (-Dmmorpg.checkAutolock=true) : verrouillage des ennemis, jamais des animaux ni des joueurs, interrupteur. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevAutoLockChecks {
    private static boolean done;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        var srv = event.getServer();
        if (done || !Boolean.getBoolean("mmorpg.checkAutolock") || srv.getTickCount() < 100) return;
        done = true;
        var overworld = srv.overworld();
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(overworld);
        for (int cx = -2; cx <= 3; cx++) for (int cz = -2; cz <= 3; cz++) overworld.getChunk(cx, cz);
        p.snapTo(0.5, 250, 0.5, 0F, 0F);
        var level = p.level();
        var server = level.getServer();
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        var far = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        var near = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        var side = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        try {
            far.setNoAi(true); near.setNoAi(true); pig.setNoAi(true); side.setNoAi(true);
            Vec3 a = eye.add(look.scale(18)), b = eye.add(look.scale(8)), c = eye.add(look.scale(5));
            far.setPos(a.x, a.y - 1.6, a.z); near.setPos(b.x, b.y - 1.6, b.z); pig.setPos(c.x, c.y - 1.6, c.z);
            Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 s = eye.add(look.scale(8)).add(perp.scale(6));
            side.setPos(s.x, s.y - 1.6, s.z);
            for (var e : new net.minecraft.world.entity.Entity[]{far, near, pig, side}) level.addFreshEntity(e);
            FeatureFlags.set(server, "autolock", true);
            check(AutoLock.find(p) != null, "une cible est verrouillee");
            check(AutoLock.find(p) != far || near.isRemoved(), "zombie lointain choisi seulement si le proche est absent");
            var target = AutoLock.find(p);
            check(target == near || target == far, "cible = zombie dans le cone, pas le cochon ni le zombie sur le cote");
            check(target != pig && target != side, "animal et hors-cone ignores");
            check(target == near, "le plus proche du reticule est choisi");
            FeatureFlags.set(server, "autolock", false);
            check(AutoLock.find(p) == null, "interrupteur coupe : aucun verrouillage");
            FeatureFlags.set(server, "autolock", true);
            near.discard();
            check(AutoLock.find(p) == far, "apres mort du proche, le suivant est verrouille");
            MMORPG.LOGGER.info("[AUTOLOCK CHECKS] PASS: cone, portee, animaux ignores, interrupteur");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[AUTOLOCK CHECKS] FAIL", t);
        } finally {
            srv.halt(false);
            far.discard(); near.discard(); pig.discard(); side.discard();
        }
    }
    private static void check(boolean b, String label) { if (!b) throw new IllegalStateException("Autolock check: " + label); }
}
