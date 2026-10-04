package com.mmorpg.entity.mob;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.RpgMonster;
import com.mmorpg.item.ItemDefs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Monstre lanceur de sorts : attaque a distance avec des projectiles elementaires. */
public abstract class CasterMonster extends RpgMonster implements RangedAttackMob {
    private final ItemDefs.Element element;
    private final SoundEvent castSound;

    protected CasterMonster(EntityType<? extends Monster> type, Level level, String key, ItemDefs.Element element, SoundEvent castSound) {
        super(type, level, key);
        this.element = element;
        this.castSound = castSound;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 60, 16.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        MobAbilities.bolt(this, target, element, 1.0, 1.2F, element == ItemDefs.Element.FEU ? 1.5 : 0);
        this.level().playSound(null, this.blockPosition(), castSound, SoundSource.HOSTILE, 1f, 1.2f);
    }
}
