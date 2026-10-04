package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMob;
import com.mmorpg.server.MobLevels;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/** Squelette Maudit : archer mort-vivant aux flammes violettes, ne brule pas au soleil. */
public class CursedSkeletonEntity extends Skeleton implements RpgMob {
    public CursedSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public String mobKey() {
        return "squelette_maudit";
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (level.getLevel() instanceof ServerLevel sl) MobLevels.initialize(this, sl);
        return data;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0.02, 0);
        }
    }
}
