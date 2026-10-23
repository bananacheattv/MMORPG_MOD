package com.mmorpg.mine;

import net.minecraft.world.level.block.Block;

/** Bloc de minerai pose dans les mines : il ne se detruit jamais, il s'epuise puis repousse (voir {@link MineManager}). */
public class MineOreBlock extends Block {
    private final MineBlocks.Ore ore;

    public MineOreBlock(Properties properties, MineBlocks.Ore ore) {
        super(properties);
        this.ore = ore;
    }

    public MineBlocks.Ore ore() {
        return ore;
    }
}
