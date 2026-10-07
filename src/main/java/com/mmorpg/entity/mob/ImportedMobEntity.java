package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Each imported model has its own persistent type, configuration, loot and bestiary entry. */
public final class ImportedMobEntity extends RpgMonster {
    /** Hauteur de vol visee au-dessus du sol quand le monstre n'a pas de cible. */
    private static final double HOVER_HEIGHT = 2.5;
    public int attackStarted = -100;

    public ImportedMobEntity(EntityType<? extends ImportedMobEntity> type, Level level) {
        super(type, level, BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
        if (isFlyer()) {
            this.moveControl = new FlyingMoveControl<>(this, 20, true);
            this.setNoGravity(true);
        }
    }

    public ImportedMobs.Entry entry() {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
        for (ImportedMobs.Entry e : ImportedMobs.ALL) {
            if (e.key().equals(key)) return e;
        }
        return null;
    }

    /** Les monstres dont le modele est anime en vol se deplacent dans les airs. */
    public boolean isFlyer() {
        ImportedMobs.Entry e = entry();
        return e != null && e.flying();
    }

    @Override
    protected void registerGoals() {
        if (!isFlyer()) {
            super.registerGoals();
            return;
        }
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (!isFlyer()) return super.createNavigation(level);
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlyer()) this.travelFlying(input, this.getSpeed());
        else super.travel(input);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (!isFlyer()) super.checkFallDamage(ya, onGround, onState, pos);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!isFlyer() || this.level().isClientSide()) return;
        // sans cible, le monstre reste en vol a quelques blocs du sol au lieu de se poser
        if (this.getTarget() == null && this.getNavigation().isDone() && groundDistance() < HOVER_HEIGHT) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, Math.max(v.y, 0.08), v.z);
        }
    }

    /** Distance au premier bloc solide sous le monstre (plafonnee a 8). */
    private double groundDistance() {
        BlockPos.MutableBlockPos p = this.blockPosition().mutable();
        for (int i = 0; i <= 8; i++) {
            if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) {
                return this.getY() - (p.getY() + 1);
            }
            p.move(0, -1, 0);
        }
        return 8;
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 4) attackStarted = tickCount;
        else super.handleEntityEvent(event);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        level.broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(level, target);
    }
}
