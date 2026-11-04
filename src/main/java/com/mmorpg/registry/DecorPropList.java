package com.mmorpg.registry;

import net.minecraft.world.level.block.SoundType;

/** Genere par tools/import_pack2.py : accessoires de decor (modeles Blockbench) avec leur boite de collision (pixels). */
final class DecorPropList {
    record Prop(String id, SoundType sound, int light, double x0, double y0, double z0, double x1, double y1, double z1) {
    }

    static final Prop[] ALL = {
        new Prop("chaise_aeloria", SoundType.WOOD, 0, 0.0, 0.0, 0.0, 16.0, 16.0, 16.0),
        new Prop("table_aeloria", SoundType.WOOD, 0, 0.0, 0.0, 2, 16.0, 11.5, 14),
        new Prop("armoire_aeloria", SoundType.WOOD, 0, 0.0, 0.0, 2.3, 16.0, 16.0, 15.6),
        new Prop("feu_de_camp", SoundType.WOOD, 13, 0.0, 0.0, 0.0, 16.0, 16.0, 16.0),
        new Prop("buche_camp", SoundType.WOOD, 0, 0.0, 0.0, 4.7075, 16.0, 6.9125, 11.9125),
        new Prop("jarre_ancienne", SoundType.STONE, 0, 0.0, 0.0, 0.0, 16.0, 16.0, 16.0),
        new Prop("pot_decore", SoundType.STONE, 0, 4, 0.0, 4, 12, 12, 12),
        new Prop("panneau_feuille", SoundType.WOOD, 0, 3.2, 0.0, 7.2, 11.9, 16.0, 8.7),
        new Prop("viande_deco", SoundType.WOOL, 0, 3, 0.0, 0.0, 13, 5, 16.0),
        new Prop("potion_etoilee", SoundType.GLASS, 9, 3.69, 1, 4, 12.31, 14.75, 12),
        new Prop("panneau_quetes", SoundType.WOOD, 0, 0, 0, 6, 16, 16, 10)
    };
}
