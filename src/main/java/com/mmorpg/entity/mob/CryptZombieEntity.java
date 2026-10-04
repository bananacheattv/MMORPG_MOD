package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** Archetype RPG utilisant l'IA et les animations natives de Zombie. */
public final class CryptZombieEntity extends Zombie implements RpgMob {
    public CryptZombieEntity(EntityType<? extends Zombie> type, Level level) { super(type, level); }
    @Override public String mobKey() { return "zombie_des_cryptes"; }
}
