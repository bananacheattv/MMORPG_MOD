package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.Level;

/** Archetype RPG utilisant l'IA et les animations natives de Pillager. */
public final class BanditEntity extends Pillager implements RpgMob {
    public BanditEntity(EntityType<? extends Pillager> type, Level level) { super(type, level); }
    @Override public String mobKey() { return "bandit_arbaletrier"; }
}
