package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Each imported model has its own persistent type, configuration, loot and bestiary entry. */
public final class ImportedMobEntity extends RpgMonster {
    public int attackStarted = -100;

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
}
