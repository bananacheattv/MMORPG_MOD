package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.level.Level;

/** Archetype RPG utilisant l'IA et les animations natives de CaveSpider. */
public final class VenomSpiderEntity extends CaveSpider implements RpgMob {
    public VenomSpiderEntity(EntityType<? extends CaveSpider> type, Level level) { super(type, level); }
    @Override public String mobKey() { return "araignee_venimeuse"; }
}
