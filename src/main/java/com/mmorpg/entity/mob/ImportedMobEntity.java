package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;

/**
 * Each imported model has its own persistent type, configuration, loot and bestiary entry. Chaque monstre a aussi ses sorts
 * ({@link ImportedSpells}) : ils se lancent avec son animation d'attaque, puis l'effet tombe au moment du geste.
 */
public final class ImportedMobEntity extends RpgMonster {
    /** Ticks entre le debut du geste et l'effet du sort (l'impact de l'animation tombe vers 0,3 s). */
    private static final int CAST_DELAY = 6;
    public int attackStarted = -100;
    private long[] nextCast;
    private long globalCooldown;
    private ImportedSpells.Spell pending;
    private int pendingAt;

    public ImportedMobEntity(EntityType<? extends ImportedMobEntity> type, Level level) {
        super(type, level, BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }

    public ImportedMobs.Entry entry() {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
        for (ImportedMobs.Entry e : ImportedMobs.ALL) {
            if (e.key().equals(key)) return e;
        }
        return null;
    }

    /** Les monstres en lévitation flottent juste au-dessus du sol (rendu) mais se déplacent comme les autres. */
    public boolean isLevitating() {
        ImportedMobs.Entry e = entry();
        return e != null && e.levitating();
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (!isLevitating()) super.checkFallDamage(ya, onGround, onState, pos);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isLevitating()) super.playStepSound(pos, state);
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

    // ------------------------------------------------------------------ sorts

    private List<ImportedSpells.Spell> spells() {
        return ImportedSpells.of(mobKey());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new CasterStanceGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) spellTick();
    }

    private double range(ImportedSpells.Spell s) {
        return s.ranged() || s.type() == ImportedSpells.Type.CHARGE ? ImportedSpells.param(mobKey(), s, "Range", s.range()) : ImportedSpells.param(mobKey(), s, "Radius", s.radius());
    }

    private void spellTick() {
        List<ImportedSpells.Spell> list = spells();
        if (list.isEmpty() || !this.isAlive()) return;                     // pas de sort pendant l'animation de mort
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            pending = null;
            return;
        }
        if (pending != null) {
            if (this.tickCount >= pendingAt) {
                ImportedSpells.Spell s = pending;
                pending = null;
                ImportedSpells.cast(this, s, target);
            }
            return;
        }
        if (this.tickCount < globalCooldown) return;
        if (nextCast == null) {
            nextCast = new long[list.size()];
            for (int i = 0; i < nextCast.length; i++) nextCast[i] = this.tickCount + 30 + this.random.nextInt(60);
        }
        double dist = this.distanceTo(target);
        for (int i = 0; i < list.size(); i++) {
            ImportedSpells.Spell s = list.get(i);
            if (this.tickCount < nextCast[i]) continue;
            double range = range(s);
            boolean ok = switch (s.type()) {
                case BOLT, VOLLEY -> dist <= range && dist >= 2 && this.hasLineOfSight(target);
                case SLAM, NOVA, CLOUD, ROOTS -> dist <= range * 0.85;
                case CHARGE -> dist >= 4 && dist <= range && this.hasLineOfSight(target);
                case BLINK -> dist >= 5 && dist <= 14;
                case SUMMON -> dist <= 16 && this.hasLineOfSight(target);
            };
            if (!ok) continue;
            double cd = ImportedSpells.param(mobKey(), s, "Cooldown", s.cooldown());
            nextCast[i] = this.tickCount + (long) (cd * (0.85 + this.random.nextDouble() * 0.3));
            globalCooldown = this.tickCount + 30;
            pending = s;
            pendingAt = this.tickCount + CAST_DELAY;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 60F, 60F);
            this.level().broadcastEntityEvent(this, (byte) 4);
            break;
        }
    }

    /** Un monstre qui lance ses sorts de loin s'arrete a bonne distance (3,5 blocs ou plus) tant qu'il a la cible en vue. */
    private final class CasterStanceGoal extends Goal {
        CasterStanceGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private ImportedSpells.Spell ranged() {
            for (ImportedSpells.Spell s : spells()) if (s.ranged()) return s;
            return null;
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ImportedMobEntity.this.getTarget();
            ImportedSpells.Spell s = ranged();
            if (t == null || !t.isAlive() || s == null) return false;
            double d = ImportedMobEntity.this.distanceTo(t);
            return d >= 3.5 && d <= range(s) * 0.9 && ImportedMobEntity.this.hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity t = ImportedMobEntity.this.getTarget();
            if (t == null) return;
            ImportedMobEntity.this.getNavigation().stop();
            ImportedMobEntity.this.getLookControl().setLookAt(t, 30F, 30F);
        }
    }
}
