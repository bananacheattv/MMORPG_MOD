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

/** Modele et animations de Hôtel des ventes (format Modded Entity de Blockbench, art/structures_blockbench/hdv_aeloria). */
public final class HdvAeloriaModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("hdv_aeloria"), "main");

    private HdvAeloriaModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(0, 0).addBox(-48F, -4F, -20F, 96F, 4F, 56F, new CubeDeformation(0F)).texOffs(384, 107).addBox(-24F, -2F, -26F, 48F, 2F, 6F, new CubeDeformation(0F)).texOffs(414, 60).addBox(-44F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)).texOffs(414, 60).addBox(36F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_estrade = p_racine.addOrReplaceChild("estrade", CubeListBuilder.create().texOffs(186, 60).addBox(-26F, -6F, -10F, 52F, 6F, 24F, new CubeDeformation(0F)).texOffs(18, 60).addBox(-28F, -8F, -12F, 56F, 2F, 28F, new CubeDeformation(0F)).texOffs(338, 60).addBox(-7F, -20F, -3F, 14F, 12F, 8F, new CubeDeformation(0F)).texOffs(326, 117).addBox(-8F, -20F, -5F, 16F, 2F, 2F, new CubeDeformation(0F)).texOffs(240, 117).addBox(-3F, -16F, -4F, 6F, 6F, 1F, new CubeDeformation(0F)).texOffs(204, 117).addBox(-8F, -22F, 3F, 5F, 2F, 5F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        p_estrade.addOrReplaceChild("estrade_pupitre_pente_r", CubeListBuilder.create().texOffs(220, 91).addBox(-8F, -2F, -4F, 16F, 2F, 10F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -20F, 0F, 0.3927F, 0F, 0F));
        PartDefinition p_marteau = p_racine.addOrReplaceChild("marteau", CubeListBuilder.create().texOffs(362, 117).addBox(-4F, -1F, -1F, 7F, 1F, 1F, new CubeDeformation(0F)).texOffs(274, 117).addBox(-6F, -2F, -2F, 3F, 3F, 3F, new CubeDeformation(0F)).texOffs(298, 117).addBox(-3F, -2F, -2F, 1F, 3F, 3F, new CubeDeformation(0F)), PartPose.offset(-6F, -26F, 2F));
        PartDefinition p_portique = p_racine.addOrReplaceChild("portique", CubeListBuilder.create().texOffs(304, 0).addBox(-44F, -46F, -4F, 4F, 54F, 4F, new CubeDeformation(0F)).texOffs(360, 107).addBox(-45F, -15F, -5F, 6F, 3F, 6F, new CubeDeformation(0F)).texOffs(304, 0).addBox(40F, -46F, -4F, 4F, 54F, 4F, new CubeDeformation(0F)).texOffs(360, 107).addBox(39F, -15F, -5F, 6F, 3F, 6F, new CubeDeformation(0F)).texOffs(292, 91).addBox(-50F, -50F, -5F, 96F, 4F, 6F, new CubeDeformation(0F)).texOffs(0, 117).addBox(-50F, -51F, -5F, 96F, 1F, 6F, new CubeDeformation(0F)).texOffs(128, 91).addBox(-17F, -46F, -4F, 3F, 12F, 3F, new CubeDeformation(0F)).texOffs(128, 91).addBox(17F, -46F, -4F, 3F, 12F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 10F));
        PartDefinition p_bannieres = p_racine.addOrReplaceChild("bannieres", CubeListBuilder.create().texOffs(0, 60).addBox(-38F, 0F, -4F, 8F, 30F, 1F, new CubeDeformation(0F)).texOffs(306, 117).addBox(-36F, 30F, -4F, 4F, 4F, 1F, new CubeDeformation(0F)).texOffs(316, 117).addBox(-36F, 14F, -5F, 4F, 4F, 1F, new CubeDeformation(0F)).texOffs(0, 60).addBox(28F, 0F, -4F, 8F, 30F, 1F, new CubeDeformation(0F)).texOffs(306, 117).addBox(30F, 30F, -4F, 4F, 4F, 1F, new CubeDeformation(0F)).texOffs(316, 117).addBox(30F, 14F, -5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -56F, 8F));
        PartDefinition p_cloche = p_racine.addOrReplaceChild("cloche", CubeListBuilder.create().texOffs(286, 117).addBox(-2F, -4F, -1F, 3F, 3F, 3F, new CubeDeformation(0F)).texOffs(188, 91).addBox(-5F, -1F, -3F, 9F, 7F, 7F, new CubeDeformation(0F)).texOffs(446, 60).addBox(-7F, 6F, -5F, 13F, 6F, 11F, new CubeDeformation(0F)).texOffs(72, 91).addBox(-8F, 12F, -6F, 15F, 2F, 13F, new CubeDeformation(0F)).texOffs(254, 117).addBox(-3F, 13F, -1F, 3F, 4F, 3F, new CubeDeformation(0F)), PartPose.offset(2F, -50F, 8F));
        PartDefinition p_tableau = p_racine.addOrReplaceChild("tableau", CubeListBuilder.create().texOffs(340, 0).addBox(-40F, -32F, -2F, 80F, 40F, 3F, new CubeDeformation(0F)).texOffs(0, 107).addBox(-42F, -36F, -4F, 84F, 4F, 6F, new CubeDeformation(0F)).texOffs(180, 107).addBox(-42F, 7F, -4F, 84F, 3F, 6F, new CubeDeformation(0F)).texOffs(320, 0).addBox(-42F, -32F, -4F, 4F, 39F, 6F, new CubeDeformation(0F)).texOffs(320, 0).addBox(38F, -32F, -4F, 4F, 39F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -22F, 26F));
        PartDefinition p_parchemins = p_racine.addOrReplaceChild("parchemins", CubeListBuilder.create().texOffs(378, 117).addBox(29F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(17F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(5F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-7F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-19F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-31F, -14F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(29F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(17F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(5F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-7F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-19F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(378, 117).addBox(-31F, 0F, -4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -34F, 24F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_0_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(30F, -14F, -3F, 0F, 0F, -0.1047F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_1_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(18F, -14F, -3F, 0F, 0F, 0.0698F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_2_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(6F, -14F, -3F, 0F, 0F, -0.0349F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_3_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-6F, -14F, -3F, 0F, 0F, 0.1047F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_4_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-18F, -14F, -3F, 0F, 0F, -0.0698F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_5_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-30F, -14F, -3F, 0F, 0F, 0.0349F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_6_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(30F, 0F, -3F, 0F, 0F, 0.0698F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_7_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(18F, 0F, -3F, 0F, 0F, -0.0349F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_8_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(6F, 0F, -3F, 0F, 0F, 0.1047F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_9_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-6F, 0F, -3F, 0F, 0F, -0.0698F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_10_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-18F, 0F, -3F, 0F, 0F, 0.0349F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_11_r", CubeListBuilder.create().texOffs(272, 91).addBox(-5F, -1F, 0F, 9F, 11F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-30F, 0F, -3F, 0F, 0F, -0.1047F));
        PartDefinition p_piedestal_g = p_racine.addOrReplaceChild("piedestal_g", CubeListBuilder.create().texOffs(0, 91).addBox(-6F, -12F, -6F, 12F, 4F, 12F, new CubeDeformation(0F)).texOffs(382, 60).addBox(-4F, -24F, -4F, 8F, 12F, 8F, new CubeDeformation(0F)).texOffs(140, 91).addBox(-6F, -26F, -6F, 12F, 2F, 12F, new CubeDeformation(0F)), PartPose.offset(-30F, -4F, 8F));
        PartDefinition p_objets_g = p_piedestal_g.addOrReplaceChild("objets_g", CubeListBuilder.create().texOffs(48, 91).addBox(-3F, -7F, -3F, 6F, 10F, 6F, new CubeDeformation(0F)).texOffs(224, 117).addBox(-2F, -10F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -30F, 0F));
        p_objets_g.addOrReplaceChild("objets_g_objet_g_eclat_r", CubeListBuilder.create().texOffs(266, 117).addBox(-1F, -5F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -1F, 0F, 0F, 0F, 0.3927F));
        PartDefinition p_piedestal_d = p_racine.addOrReplaceChild("piedestal_d", CubeListBuilder.create().texOffs(0, 91).addBox(-6F, -12F, -6F, 12F, 4F, 12F, new CubeDeformation(0F)).texOffs(382, 60).addBox(-4F, -24F, -4F, 8F, 12F, 8F, new CubeDeformation(0F)).texOffs(140, 91).addBox(-6F, -26F, -6F, 12F, 2F, 12F, new CubeDeformation(0F)), PartPose.offset(30F, -4F, 8F));
        PartDefinition p_objets_d = p_piedestal_d.addOrReplaceChild("objets_d", CubeListBuilder.create().texOffs(48, 91).addBox(-3F, -7F, -3F, 6F, 10F, 6F, new CubeDeformation(0F)).texOffs(224, 117).addBox(-2F, -10F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -30F, 0F));
        p_objets_d.addOrReplaceChild("objets_d_objet_d_eclat_r", CubeListBuilder.create().texOffs(266, 117).addBox(-1F, -5F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -1F, 0F, 0F, 0F, 0.3927F));
        return LayerDefinition.create(mesh, 512, 128);
    }

    /** Dans l'ordre des indices de HdvAeloriaAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "cloche|r|0,0,0,-1.5,l;2,0,0,1.5,l;4,0,0,-1.5,l\nobjets_d|p|0,0,0,0,c;2,0,0.8,0,c;4,0,0,0,c\nobjets_g|p|0,0,0,0,c;2,0,0.8,0,c;4,0,0,0,c"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "cloche|r|0,0,0,0,l;0.5,0,0,14,l;1,0,0,-8,l\nmarteau|r|0,0,0,0,l;0.3,0,0,40,l;0.5,0,0,-10,l;1,0,0,0,l\nobjets_d|p|0,0,0,0,l;0.2,0,0.208,0,l;0.5,0,1,0,l;0.8,0,1.792,0,l;1,0,2,0,l\nobjets_g|p|0,0,0,0,l;0.2,0,0.208,0,l;0.5,0,1,0,l;0.8,0,1.792,0,l;1,0,2,0,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "cloche|r|0,0,0,0,l;1.2,0,0,12,l;1.6,0,0,-9,l;2,0,0,0,l\nmarteau|r|0,0,0,0,l;0.8,0,0,0,l;1,0,0,40,l;1.2,0,0,-8,l;1.4,0,0,0,l;2,0,0,0,l\nobjets_d|p|0,0,2,0,c;1,0,3,0,c;2,0,2,0,c\nobjets_d|r|0,0,-45,0,l;2,0,-225,0,l\nobjets_g|p|0,0,2,0,c;1,0,3,0,c;2,0,2,0,c\nobjets_g|r|0,0,0,0,l;2,0,-180,0,l"
        }, 2F, true)
        };
    }
}
