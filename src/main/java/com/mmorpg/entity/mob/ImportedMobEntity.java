package com.mmorpg.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Each imported model has its own persistent type, configuration, loot and bestiary entry. L'IA et les attaques viennent de
 * {@link SmartMonster} : chaque monstre a les siennes ({@link ImportedSpells}), chacune avec son clip d'animation.
 */
public final class ImportedMobEntity extends SmartMonster {
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
}
