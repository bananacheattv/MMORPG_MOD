package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Each imported model has its own persistent type, configuration, loot and bestiary entry. */
public final class ImportedMobEntity extends RpgMonster {
    public int attackStarted = -100;
    public ImportedMobEntity(EntityType<? extends ImportedMobEntity> type, Level level) {
        super(type,level,BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==4) attackStarted=tickCount;
        else super.handleEntityEvent(event);
    }
    @Override public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        level.broadcastEntityEvent(this,(byte)4);
        return super.doHurtTarget(level,target);
    }
}
