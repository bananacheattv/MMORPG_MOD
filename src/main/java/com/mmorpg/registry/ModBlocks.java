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
    public static final DeferredBlock<com.mmorpg.block.PresseBlock> PRESSE_COMPRESSION = BLOCKS.registerBlock("presse_compression", com.mmorpg.block.PresseBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.ANVIL).lightLevel(s -> 5).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<com.mmorpg.block.BanqueBlock> BANQUE = BLOCKS.registerBlock("banque_aeloria", com.mmorpg.block.BanqueBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(3f, 1200f).sound(SoundType.METAL).lightLevel(s -> 8).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<com.mmorpg.block.HdvBlock> HDV = BLOCKS.registerBlock("hdv_aeloria", com.mmorpg.block.HdvBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(3f, 1200f).sound(SoundType.WOOD).lightLevel(s -> 8).noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<TeleporterBlock> TELEPORTEUR = BLOCKS.registerBlock("teleporteur", TeleporterBlock::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(3f, 1200f).sound(SoundType.AMETHYST).lightLevel(s -> 12).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<AltarBlock> AUTEL_INVOCATION = BLOCKS.registerBlock("autel_invocation", AltarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 1200f).sound(SoundType.STONE).lightLevel(s -> s.getValue(AltarBlock.ACTIVE) ? 13 : 8).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    public static final DeferredBlock<com.mmorpg.block.LuckyBlock> LUCKY_BLOCK = BLOCKS.registerBlock("lucky_block", com.mmorpg.block.LuckyBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(0.8f, 1200f).sound(SoundType.METAL).lightLevel(s -> 5).noLootTable().pushReaction(PushReaction.IMMOVEABLE));

    public static final java.util.List<DeferredBlock<com.mmorpg.block.LuckyBlock>> LUCKY_BLOCKS=new java.util.ArrayList<>();
    static {for(var tier:com.mmorpg.block.LuckyTier.values()){final int index=tier.ordinal();LUCKY_BLOCKS.add(BLOCKS.registerBlock(tier.id,p->new com.mmorpg.block.LuckyBlock(p,index),p->p.mapColor(MapColor.COLOR_PURPLE).strength(.8f,1200f).sound(SoundType.METAL).lightLevel(s->9).noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)));}}

    /** Caisses en bloc 3D (une par type), posees par les admins et ouvertes avec la cle correspondante. */
    public static final java.util.Map<com.mmorpg.block.crate.CrateTier, DeferredBlock<com.mmorpg.block.crate.CrateBlock>> CRATES =
            new java.util.EnumMap<>(com.mmorpg.block.crate.CrateTier.class);

    static {
        for (var tier : com.mmorpg.block.crate.CrateTier.values()) {
            CRATES.put(tier, BLOCKS.registerBlock(tier.blockId(), p -> new com.mmorpg.block.crate.CrateBlock(p, tier),
                    p -> p.mapColor(MapColor.WOOD).strength(-1.0f, 3600000f).sound(SoundType.WOOD).lightLevel(s -> 4).noOcclusion()
                            .noLootTable().pushReaction(PushReaction.IMMOVEABLE)));
        }
    }

    /** Panneaux de classement (un bloc et un modèle par type), posés par les admins. */
    public static final java.util.Map<com.mmorpg.ranking.RankingKind, DeferredBlock<com.mmorpg.block.ranking.RankingBoardBlock>> RANKING_BOARDS =
            new java.util.EnumMap<>(com.mmorpg.ranking.RankingKind.class);

    static {
        for (var kind : com.mmorpg.ranking.RankingKind.values()) {
            RANKING_BOARDS.put(kind, BLOCKS.registerBlock(kind.blockId(), p -> new com.mmorpg.block.ranking.RankingBoardBlock(p, kind),
                    p -> p.mapColor(MapColor.WOOD).strength(3f, 1200f).sound(SoundType.WOOD).lightLevel(s -> 6).noOcclusion()
                            .pushReaction(PushReaction.IMMOVEABLE)));
        }
    }

    /** Partie invisible des structures multiblocs (collision et renvoi des clics vers le bloc fonctionnel). */
    public static final DeferredBlock<StructurePartBlock> STRUCTURE_PART = BLOCKS.registerBlock("structure_part", StructurePartBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5f, 1200f).sound(SoundType.STONE).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE).isSuffocating(ModBlocks::never));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MMORPG.MODID);
    /** Entite des blocs fonctionnels de structure (rendu des modeles 3D). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureBlockEntity>> STRUCTURE = BLOCK_ENTITIES.register("structure",
            () -> new BlockEntityType<>(StructureBlockEntity::new, FORGE_ARCANIQUE.get(), TELEPORTEUR.get(), AUTEL_INVOCATION.get(), PRESSE_COMPRESSION.get(), BANQUE.get(), HDV.get()));

    /** Caisses : roue d'ouverture animee (etat synchronise). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.mmorpg.block.crate.CrateBlockEntity>> CRATE = BLOCK_ENTITIES.register("caisse",
            () -> new BlockEntityType<>(com.mmorpg.block.crate.CrateBlockEntity::new,
                    CRATES.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    /** Panneaux de classement : lignes du top affichées sur le panneau (état synchronisé). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.mmorpg.block.ranking.RankingBoardBlockEntity>> RANKING_BOARD = BLOCK_ENTITIES.register("classement",
            () -> new BlockEntityType<>(com.mmorpg.block.ranking.RankingBoardBlockEntity::new,
                    RANKING_BOARDS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    private ModBlocks() {
    }

    private static boolean never(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
                                 net.minecraft.core.BlockPos pos) {
        return false;
    }
}
