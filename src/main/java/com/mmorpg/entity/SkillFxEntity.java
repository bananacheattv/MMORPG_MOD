package com.mmorpg.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Effet visuel ephemere d'une competence. Cree par le serveur (synchronise automatiquement avec les joueurs proches),
 * sans collision ni sauvegarde ; dessine cote client par {@code SkillFxRenderer} selon son {@link FxKind}.
 */
public class SkillFxEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FOLLOW = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> POINTS = SynchedEntityData.defineId(SkillFxEntity.class, EntityDataSerializers.STRING);

    /** Positions recentes de l'entite suivie (cote client, pour les trainees). */
    public final Deque<Vec3> history = new ArrayDeque<>();

    public SkillFxEntity(EntityType<? extends SkillFxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static SkillFxEntity create(ServerLevel level, FxKind kind, Vec3 pos, double radius, int color, int life) {
        SkillFxEntity e = new SkillFxEntity(com.mmorpg.registry.ModEntities.SKILL_FX.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.entityData.set(KIND, kind.ordinal());
        e.entityData.set(RADIUS, (float) radius);
        e.entityData.set(COLOR, color);
        e.entityData.set(LIFE, Math.max(1, life));
        return e;
    }

    public SkillFxEntity follow(Entity target) {
        this.entityData.set(FOLLOW, target.getId());
        return this;
    }

    public SkillFxEntity angle(float yawDegrees) {
        this.entityData.set(ANGLE, yawDegrees);
        return this;
    }

    /** Points relatifs a l'effet (chaine d'eclairs, rayon). */
    public SkillFxEntity points(List<Vec3> world) {
        StringBuilder sb = new StringBuilder();
        for (Vec3 v : world) {
            if (sb.length() > 0) sb.append(';');
            sb.append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f", v.x - getX(), v.y - getY(), v.z - getZ()));
        }
        this.entityData.set(POINTS, sb.toString());
        return this;
    }

    public FxKind kind() {
        return FxKind.byId(this.entityData.get(KIND));
    }

    public int color() {
        return this.entityData.get(COLOR);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    public float radius() {
        return this.entityData.get(RADIUS);
    }

    public float yaw() {
        return this.entityData.get(ANGLE);
    }

    public int followId() {
        return this.entityData.get(FOLLOW);
    }

    public List<Vec3> relativePoints() {
        List<Vec3> out = new ArrayList<>();
        String s = this.entityData.get(POINTS);
        if (s.isEmpty()) return out;
        for (String part : s.split(";")) {
            String[] c = part.split(",");
            if (c.length == 3) {
                try {
                    out.add(new Vec3(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
        builder.define(COLOR, 0xFFFFFF);
        builder.define(LIFE, 20);
        builder.define(FOLLOW, -1);
        builder.define(RADIUS, 1.0F);
        builder.define(ANGLE, 0.0F);
        builder.define(POINTS, "");
    }

    @Override
    public void tick() {
        super.tick();
        int f = followId();
        if (f >= 0) {
            Entity target = this.level().getEntity(f);
            if (target != null) {
                this.setPos(target.getX(), target.getY(), target.getZ());
                if (this.level().isClientSide()) {
                    history.addFirst(target.position());
                    while (history.size() > 10) history.removeLast();
                }
            } else if (!this.level().isClientSide() && this.tickCount > 2) {
                this.discard();
                return;
            }
        }
        if (!this.level().isClientSide() && this.tickCount > life()) this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
