// Genere automatiquement par art/structures_blockbench/generer_structures.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.structure;

import com.mmorpg.MMORPG;
import com.mmorpg.client.model.BlockbenchAnimations;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Modele et animations de Forge arcanique (format Modded Entity de Blockbench, art/structures_blockbench/forge_arcanique). */
public final class ForgeArcaniqueModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("forge_arcanique"), "main");

    private ForgeArcaniqueModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(0, 0).addBox(-40F, -4F, -16F, 80F, 4F, 56F, new CubeDeformation(0F)).texOffs(0, 141).addBox(-22F, -2F, -24F, 44F, 2F, 8F, new CubeDeformation(0F)).texOffs(480, 98).addBox(-40F, -10F, -18F, 8F, 10F, 8F, new CubeDeformation(0F)).texOffs(480, 98).addBox(32F, -10F, -18F, 8F, 10F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_four = p_racine.addOrReplaceChild("four", CubeListBuilder.create().texOffs(80, 60).addBox(-16F, -6F, -16F, 32F, 6F, 30F, new CubeDeformation(0F)).texOffs(0, 60).addBox(-16F, -36F, 6F, 32F, 30F, 8F, new CubeDeformation(0F)).texOffs(272, 0).addBox(-16F, -36F, -16F, 8F, 30F, 22F, new CubeDeformation(0F)).texOffs(332, 0).addBox(10F, -36F, -16F, 6F, 30F, 22F, new CubeDeformation(0F)).texOffs(104, 141).addBox(-8F, -36F, -16F, 18F, 6F, 4F, new CubeDeformation(0F)).texOffs(204, 60).addBox(-16F, -42F, -16F, 32F, 6F, 30F, new CubeDeformation(0F)).texOffs(24, 98).addBox(-8F, -30F, 5F, 18F, 24F, 1F, new CubeDeformation(0F)).texOffs(396, 60).addBox(-10F, -32F, -18F, 3F, 26F, 3F, new CubeDeformation(0F)).texOffs(396, 60).addBox(9F, -32F, -18F, 3F, 26F, 3F, new CubeDeformation(0F)).texOffs(218, 141).addBox(-10F, -33F, -18F, 22F, 4F, 3F, new CubeDeformation(0F)).texOffs(144, 124).addBox(-8F, -8F, -12F, 18F, 2F, 14F, new CubeDeformation(0F)).texOffs(290, 98).addBox(-13F, -10F, -33F, 12F, 10F, 10F, new CubeDeformation(0F)).texOffs(424, 141).addBox(-8F, -7F, -33.5F, 2F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(-24F, -4F, 24F));
        p_four.addOrReplaceChild("four_cadre_coin_gauche_r", CubeListBuilder.create().texOffs(402, 141).addBox(0F, 2F, -1F, 5F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-7F, -29F, -16.5F, 0F, 0F, -0.7854F));
        p_four.addOrReplaceChild("four_cadre_coin_droit_r", CubeListBuilder.create().texOffs(402, 141).addBox(-5F, 2F, -1F, 5F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(9F, -29F, -16.5F, 0F, 0F, 0.7854F));
        PartDefinition p_cheminee = p_racine.addOrReplaceChild("cheminee", CubeListBuilder.create().texOffs(328, 60).addBox(-7F, -22F, -7F, 14F, 22F, 14F, new CubeDeformation(0F)).texOffs(378, 98).addBox(-8F, -25F, -8F, 16F, 3F, 16F, new CubeDeformation(0F)), PartPose.offset(-25F, -46F, 23F));
        PartDefinition p_foyer = p_racine.addOrReplaceChild("foyer", CubeListBuilder.create(), PartPose.offset(-23F, -12F, 19F));
        PartDefinition p_braises = p_foyer.addOrReplaceChild("braises", CubeListBuilder.create().texOffs(240, 124).addBox(-8F, -2F, -6F, 16F, 2F, 12F, new CubeDeformation(0F)).texOffs(416, 141).addBox(4F, -3.5F, -4F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(416, 141).addBox(-1F, -3.5F, 0F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(416, 141).addBox(-6F, -3.5F, -3F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(416, 141).addBox(2F, -3.5F, 3F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(416, 141).addBox(-5F, -3.5F, 3F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_flammes = p_foyer.addOrReplaceChild("flammes", CubeListBuilder.create().texOffs(208, 124).addBox(-8F, -16F, 0F, 16F, 16F, 0F, new CubeDeformation(0F)).texOffs(480, 60).addBox(0F, -14F, -6F, 0F, 14F, 12F, new CubeDeformation(0F)), PartPose.offset(0F, -2F, 0F));
        PartDefinition p_enclume = p_racine.addOrReplaceChild("enclume", CubeListBuilder.create().texOffs(408, 60).addBox(-10F, -12F, -8F, 20F, 12F, 16F, new CubeDeformation(0F)).texOffs(150, 98).addBox(-11F, -14F, -9F, 22F, 2F, 18F, new CubeDeformation(0F)).texOffs(432, 124).addBox(-6F, -17F, -4F, 12F, 3F, 8F, new CubeDeformation(0F)).texOffs(292, 141).addBox(-3F, -20F, -2F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(296, 124).addBox(-8F, -25F, -4F, 16F, 5F, 8F, new CubeDeformation(0F)).texOffs(164, 141).addBox(-13F, -24F, -3F, 5F, 3F, 6F, new CubeDeformation(0F)).texOffs(388, 141).addBox(-16F, -23F, -2F, 3F, 1F, 4F, new CubeDeformation(0F)).texOffs(148, 141).addBox(8F, -25F, -3F, 2F, 4F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        PartDefinition p_cristal = p_racine.addOrReplaceChild("cristal", CubeListBuilder.create().texOffs(230, 98).addBox(-8F, -6F, -7F, 16F, 6F, 14F, new CubeDeformation(0F)).texOffs(384, 60).addBox(-8F, -36F, -7F, 3F, 30F, 3F, new CubeDeformation(0F)).texOffs(384, 60).addBox(5F, -36F, -7F, 3F, 30F, 3F, new CubeDeformation(0F)).texOffs(384, 60).addBox(-8F, -36F, 4F, 3F, 30F, 3F, new CubeDeformation(0F)).texOffs(384, 60).addBox(5F, -36F, 4F, 3F, 30F, 3F, new CubeDeformation(0F)).texOffs(0, 124).addBox(-8F, -39F, -7F, 16F, 3F, 14F, new CubeDeformation(0F)), PartPose.offset(4F, -4F, 33F));
        PartDefinition p_cristal_source = p_cristal.addOrReplaceChild("cristal_source", CubeListBuilder.create().texOffs(0, 98).addBox(-3F, -10F, -3F, 6F, 20F, 6F, new CubeDeformation(0F)).texOffs(186, 141).addBox(-2F, -15F, -2F, 4F, 5F, 4F, new CubeDeformation(0F)).texOffs(330, 141).addBox(-2F, 10F, -2F, 4F, 2F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -18F, 0F));
        PartDefinition p_conduits = p_racine.addOrReplaceChild("conduits", CubeListBuilder.create().texOffs(202, 141).addBox(-8F, -24F, 11F, 4F, 4F, 4F, new CubeDeformation(0F)).texOffs(416, 124).addBox(-8F, -25F, 10F, 2F, 6F, 6F, new CubeDeformation(0F)).texOffs(334, 98).addBox(2F, 0F, -12F, 4F, 2F, 18F, new CubeDeformation(0F)).texOffs(268, 141).addBox(0F, -1F, -6F, 8F, 3F, 4F, new CubeDeformation(0F)).texOffs(346, 141).addBox(12F, -23F, 11F, 2F, 3F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -6F, 20F));
        PartDefinition p_etabli = p_racine.addOrReplaceChild("etabli", CubeListBuilder.create().texOffs(62, 98).addBox(-13F, -17F, -9F, 26F, 3F, 18F, new CubeDeformation(0F)).texOffs(60, 124).addBox(-13F, -14F, -8F, 3F, 14F, 3F, new CubeDeformation(0F)).texOffs(60, 124).addBox(9F, -14F, -8F, 3F, 14F, 3F, new CubeDeformation(0F)).texOffs(60, 124).addBox(-13F, -14F, 5F, 3F, 14F, 3F, new CubeDeformation(0F)).texOffs(60, 124).addBox(9F, -14F, 5F, 3F, 14F, 3F, new CubeDeformation(0F)).texOffs(72, 124).addBox(-11F, -6F, -7F, 22F, 2F, 14F, new CubeDeformation(0F)).texOffs(442, 98).addBox(0F, -14F, -4F, 9F, 8F, 10F, new CubeDeformation(0F)).texOffs(442, 98).addBox(-10F, -14F, -4F, 9F, 8F, 10F, new CubeDeformation(0F)).texOffs(388, 0).addBox(-13F, -32F, 19F, 26F, 32F, 8F, new CubeDeformation(0F)).texOffs(344, 124).addBox(-14F, -35F, 18F, 27F, 3F, 9F, new CubeDeformation(0F)), PartPose.offset(27F, -4F, 13F));
        PartDefinition p_outils = p_racine.addOrReplaceChild("outils", CubeListBuilder.create().texOffs(312, 141).addBox(3F, -3F, -5F, 6F, 3F, 3F, new CubeDeformation(0F)).texOffs(430, 141).addBox(-6F, -2F, -4F, 9F, 1F, 1F, new CubeDeformation(0F)).texOffs(356, 141).addBox(6F, -2F, 2F, 5F, 2F, 3F, new CubeDeformation(0F)).texOffs(356, 141).addBox(5.5F, -4F, 2.5F, 5F, 2F, 3F, new CubeDeformation(0F)).texOffs(372, 141).addBox(-12F, -2F, -4F, 5F, 2F, 3F, new CubeDeformation(0F)), PartPose.offset(27F, -21F, 13F));
        p_outils.addOrReplaceChild("outils_pinces_branche_1_r", CubeListBuilder.create().texOffs(450, 141).addBox(-8F, -1F, -0.5F, 8F, 1F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 1.5F, 0F, -0.2094F, 0F));
        p_outils.addOrReplaceChild("outils_pinces_branche_2_r", CubeListBuilder.create().texOffs(450, 141).addBox(-8F, -1F, -0.5F, 8F, 1F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 2.5F, 0F, 0.2094F, 0F));
        return LayerDefinition.create(mesh, 512, 256);
    }

    /** Dans l'ordre des indices de ForgeArcaniqueAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "cristal_source|p|0,0,0,0,c;2,0,0.6,0,c;4,0,0,0,c\nflammes|s|0,0.6,0.15,0.6,l;4,0.6,0.15,0.6,l"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "cristal_source|p|0,0,0,0,l;0.2,0,0.208,0,l;0.5,0,1,0,l;0.8,0,1.792,0,l;1,0,2,0,l\nflammes|s|0,0.6,0.15,0.6,l;0.08,0.652,0.2696,0.652,l;0.2,0.85,0.725,0.85,l;0.32,1.048,1.1804,1.048,l;0.4,1.1,1.3,1.1,l;0.52,1.0896,1.2688,1.0896,l;0.7,1.05,1.15,1.05,l;0.88,1.0104,1.0312,1.0104,l;1,1,1,1,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "cristal_source|p|0,0,2,0,c;0.6,0,2.8,0,c;1.2,0,2,0,c\ncristal_source|r|0,0,0,0,l;1.2,0,-90,0,l\nflammes|r|0,0,0,0,c;0.4,0,-6,0,c;0.8,0,6,0,c;1.2,0,0,0,c\nflammes|s|0,1,1,1,c;0.3,1.06,1.18,1.06,c;0.6,0.96,0.92,0.96,c;0.9,1.04,1.12,1.04,c;1.2,1,1,1,c"
        }, 1.2F, true)
        };
    }
}
