package com.mmorpg.skill;

import com.mmorpg.entity.CastAnim;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Raccourcis serveur pour declencher les effets visuels et les animations de lancement. */
public final class Fx {
    private Fx() {
    }

    /** Effet pose a un endroit fixe. */
    public static SkillFxEntity spawn(ServerLevel level, FxKind kind, Vec3 pos, double radius, int color, int life) {
        SkillFxEntity e = SkillFxEntity.create(level, kind, pos, radius, color, life);
        level.addFreshEntity(e);
        return e;
    }

    /** Effet qui suit une entite (aura, bouclier, tourbillon...). */
    public static SkillFxEntity attach(ServerLevel level, FxKind kind, Entity target, double radius, int color, int life) {
        SkillFxEntity e = SkillFxEntity.create(level, kind, target.position(), radius, color, life).follow(target);
        level.addFreshEntity(e);
        return e;
    }

    /** Effet oriente selon le regard du lanceur (arc de lame, bouclier projete). */
    public static SkillFxEntity facing(ServerLevel level, FxKind kind, Entity caster, double radius, int color, int life) {
        SkillFxEntity e = SkillFxEntity.create(level, kind, caster.position(), radius, color, life).angle(caster.getYRot());
        level.addFreshEntity(e);
        return e;
    }

    /** Effet reliant une suite de points (chaine d'eclairs, rayon). */
    public static SkillFxEntity path(ServerLevel level, FxKind kind, List<Vec3> points, double width, int color, int life) {
        SkillFxEntity e = SkillFxEntity.create(level, kind, points.get(0), width, color, life);
        e.points(points);
        level.addFreshEntity(e);
        return e;
    }

    /** Animation du personnage visible par tous les joueurs proches. */
    public static void cast(ServerPlayer p, CastAnim anim, int durationTicks) {
        Net.toTrackingAndSelf(p, new Payloads.CastAnimation(p.getId(), anim.ordinal(), durationTicks));
    }
}
