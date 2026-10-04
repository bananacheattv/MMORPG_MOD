package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import com.mmorpg.block.AltarBlock;
import com.mmorpg.block.ForgeBlock;
import com.mmorpg.block.TeleporterBlock;
import com.mmorpg.block.structure.StructureBlockEntity;
import com.mmorpg.block.structure.StructurePartBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MMORPG.MODID);

    public static final DeferredBlock<ForgeBlock> FORGE_ARCANIQUE = BLOCKS.registerBlock("forge_arcanique", ForgeBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.ANVIL).lightLevel(s -> 6).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<TeleporterBlock> TELEPORTEUR = BLOCKS.registerBlock("teleporteur", TeleporterBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(3f, 1200f).sound(SoundType.AMETHYST).lightLevel(s -> 12).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<AltarBlock> AUTEL_INVOCATION = BLOCKS.registerBlock("autel_invocation", AltarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 1200f).sound(SoundType.STONE).lightLevel(s -> s.getValue(AltarBlock.ACTIVE) ? 13 : 8).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    /** Partie invisible des structures multiblocs (collision et renvoi des clics vers le bloc fonctionnel). */
    public static final DeferredBlock<StructurePartBlock> STRUCTURE_PART = BLOCKS.registerBlock("structure_part", StructurePartBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5f, 1200f).sound(SoundType.STONE).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE).isSuffocating(ModBlocks::never));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MMORPG.MODID);
    /** Entite des blocs fonctionnels de structure (rendu des modeles 3D). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureBlockEntity>> STRUCTURE = BLOCK_ENTITIES.register("structure",
            () -> new BlockEntityType<>(StructureBlockEntity::new, FORGE_ARCANIQUE.get(), TELEPORTEUR.get(), AUTEL_INVOCATION.get()));

    private ModBlocks() {
    }

    private static boolean never(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
                                 net.minecraft.core.BlockPos pos) {
        return false;
    }
}
