package com.mmorpg.server;

import com.mmorpg.entity.MountEntity;
import com.mmorpg.mount.MountType;
import com.mmorpg.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class MountManager {
    private static boolean playable(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && RpgPlayers.get(p).playerClass.isPlayable(); }
    /** Les montures ne s'achetent plus : elles s'obtiennent dans les caisses et les Lucky Blocks (sceaux de monture). */
    public static boolean unlock(ServerPlayer p, String id) {
        message(p, "Les montures s’obtiennent dans les caisses et les Lucky Blocks.");
        return false;
    }
    /** Ajoute une monture a la collection (sceau de monture) ; faux si elle est deja possedee. */
    public static boolean grant(ServerPlayer p, MountType type) {
        var d = RpgPlayers.get(p);
        if (!p.isAlive() || p.isSpectator()) return false;
        if (d.mounts.contains(type.id)) { message(p, "Vous possédez déjà " + type.label + "."); return false; }
        d.mounts.add(type.id); RpgPlayers.sync(p);
        message(p, type.label + " débloqué définitivement !");
        return true;
    }
    /** Invoque la monture et place directement le joueur dessus. */
    public static boolean summon(ServerPlayer p, String id) {
        var type = MountType.byId(id); var d = RpgPlayers.get(p);
        if (!playable(p) || type == null || !d.mounts.contains(id)) return false;
        long now = p.level().getServer().getTickCount();
        if (now < d.pvpUntilTick) { message(p, "Montures indisponibles en combat contre un joueur (" + (d.pvpUntilTick - now) / 20 + " s)."); return false; }
        if (now < d.nextMountTick) { message(p, "Patientez quelques secondes avant de réinvoquer."); return false; }
        if (p.isPassenger() || p.isInWater()) { message(p, "Impossible d’invoquer une monture ici."); return false; }
        var mount = new MountEntity(ModEntities.MOUNTS.get(type).get(), p.level());
        mount.setup(p, type);
        boolean room = false;
        for (int[] offset : new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}}) {
            mount.setPos(p.getX() + offset[0], p.getY(), p.getZ() + offset[1]);
            if (p.level().noCollision(mount) && !p.level().containsAnyLiquid(mount.getBoundingBox())
                    && p.level().getWorldBorder().isWithinBounds(mount.blockPosition())) { room = true; break; }
        }
        if (!room) { message(p, "Pas assez de place pour la monture."); return false; }
        mount.setYRot(p.getYRot());
        mount.yBodyRot = p.getYRot();
        despawn(p);
        if (!p.level().addFreshEntity(mount)) return false;
        d.mountEntity = mount.getUUID(); d.activeMount = id; d.lastMount = id; d.nextMountTick = now + 40;
        p.startRiding(mount, true, true);
        RpgPlayers.sync(p);
        return true;
    }
    /** Touche Monture : descendre (la monture disparait) ou invoquer la derniere monture utilisee. */
    public static void toggle(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        if (d.mountEntity != null) { despawn(p); RpgPlayers.sync(p); return; }
        String id = d.mounts.contains(d.lastMount) ? d.lastMount : d.mounts.stream().findFirst().orElse("");
        if (id.isEmpty()) { message(p, "Vous n’avez aucune monture (Sceau mystère dans les caisses)."); return; }
        summon(p, id);
    }
    /** Degats d'un joueur (ou attaque d'un joueur) : monture renvoyee et interdite pendant 15 s. */
    public static void pvpTag(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        d.pvpUntilTick = p.level().getServer().getTickCount() + 300;
        if (d.mountEntity != null) {
            despawn(p);
            RpgPlayers.sync(p);
            message(p, "Combat contre un joueur : monture renvoyée.");
        }
    }
    public static void despawn(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        if (d.mountEntity != null) for (var level : p.level().getServer().getAllLevels()) {
            var entity = level.getEntity(d.mountEntity);
            if (entity instanceof MountEntity) entity.discard();
        }
        d.mountEntity = null; d.activeMount = ""; d.dirty = true;
    }
    public static void maintain(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        // les monstres qui visent le cavalier attaquent la monture (les coups sont redirigés sur le cavalier)
        if (p.getVehicle() instanceof MountEntity m) {
            for (var mob : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(24), e -> e.getTarget() == p))
                mob.setTarget(m);
        }
        if (d.mountEntity != null && (!p.isAlive() || p.isSpectator() || !(p.level().getEntity(d.mountEntity) instanceof MountEntity m) || !m.isAlive())) despawn(p);
    }
    private static void message(ServerPlayer p, String text) { p.sendOverlayMessage(Component.literal(text).withColor(0xFFD040)); }
}
