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

/** Modele et animations de Presse de compression (format Modded Entity de Blockbench, art/structures_blockbench/presse_compression). */
public final class PresseCompressionModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("presse_compression"), "main");

    private PresseCompressionModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(0, 0).addBox(-40F, -4F, -20F, 80F, 4F, 64F, new CubeDeformation(0F)).texOffs(372, 178).addBox(-20F, -2F, -28F, 40F, 2F, 8F, new CubeDeformation(0F)).texOffs(182, 159).addBox(-38F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)).texOffs(182, 159).addBox(30F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_socle_presse = p_racine.addOrReplaceChild("socle_presse", CubeListBuilder.create().texOffs(144, 68).addBox(-14F, -12F, -12F, 28F, 12F, 24F, new CubeDeformation(0F)).texOffs(312, 68).addBox(-16F, -15F, -14F, 32F, 3F, 28F, new CubeDeformation(0F)).texOffs(348, 134).addBox(-9F, -17F, -9F, 18F, 2F, 18F, new CubeDeformation(0F)).texOffs(380, 191).addBox(-10F, -18F, -11F, 20F, 3F, 2F, new CubeDeformation(0F)).texOffs(380, 191).addBox(-10F, -18F, 9F, 20F, 3F, 2F, new CubeDeformation(0F)).texOffs(308, 134).addBox(-10F, -18F, -9F, 2F, 3F, 18F, new CubeDeformation(0F)).texOffs(308, 134).addBox(8F, -18F, -9F, 2F, 3F, 18F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        PartDefinition p_portique = p_racine.addOrReplaceChild("portique", CubeListBuilder.create().texOffs(288, 0).addBox(-18F, -41F, -14F, 4F, 41F, 4F, new CubeDeformation(0F)).texOffs(288, 0).addBox(14F, -41F, -14F, 4F, 41F, 4F, new CubeDeformation(0F)).texOffs(288, 0).addBox(-18F, -41F, 12F, 4F, 41F, 4F, new CubeDeformation(0F)).texOffs(288, 0).addBox(14F, -41F, 12F, 4F, 41F, 4F, new CubeDeformation(0F)).texOffs(0, 68).addBox(-20F, -46F, -16F, 40F, 5F, 32F, new CubeDeformation(0F)).texOffs(0, 105).addBox(-17F, -49F, -13F, 34F, 3F, 26F, new CubeDeformation(0F)).texOffs(0, 134).addBox(-8F, -58F, -8F, 16F, 9F, 16F, new CubeDeformation(0F)).texOffs(420, 134).addBox(-9F, -53F, -9F, 18F, 2F, 18F, new CubeDeformation(0F)).texOffs(420, 134).addBox(-9F, -57F, -9F, 18F, 2F, 18F, new CubeDeformation(0F)).texOffs(180, 178).addBox(-5F, -60F, -5F, 10F, 2F, 10F, new CubeDeformation(0F)), PartPose.offset(0F, -19F, 0F));
        PartDefinition p_piston = p_racine.addOrReplaceChild("piston", CubeListBuilder.create().texOffs(432, 68).addBox(-3F, -31F, -3F, 6F, 24F, 6F, new CubeDeformation(0F)).texOffs(312, 159).addBox(-5F, -22F, -5F, 10F, 6F, 10F, new CubeDeformation(0F)).texOffs(144, 134).addBox(-8F, -7F, -8F, 16F, 7F, 16F, new CubeDeformation(0F)).texOffs(64, 134).addBox(-10F, 0F, -10F, 20F, 3F, 20F, new CubeDeformation(0F)).texOffs(336, 191).addBox(3F, 3F, -7F, 4F, 2F, 4F, new CubeDeformation(0F)).texOffs(336, 191).addBox(-7F, 3F, -7F, 4F, 2F, 4F, new CubeDeformation(0F)).texOffs(336, 191).addBox(3F, 3F, 3F, 4F, 2F, 4F, new CubeDeformation(0F)).texOffs(336, 191).addBox(-7F, 3F, 3F, 4F, 2F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -36F, 0F));
        PartDefinition p_tremie = p_racine.addOrReplaceChild("tremie", CubeListBuilder.create().texOffs(292, 105).addBox(-5F, -24F, -8F, 3F, 24F, 3F, new CubeDeformation(0F)).texOffs(292, 105).addBox(-13F, -24F, -8F, 3F, 24F, 3F, new CubeDeformation(0F)).texOffs(292, 105).addBox(-5F, -24F, 6F, 3F, 24F, 3F, new CubeDeformation(0F)).texOffs(292, 105).addBox(-13F, -24F, 6F, 3F, 24F, 3F, new CubeDeformation(0F)).texOffs(0, 159).addBox(-13F, -12F, -8F, 11F, 2F, 17F, new CubeDeformation(0F)).texOffs(120, 105).addBox(-17F, -32F, -10F, 19F, 8F, 21F, new CubeDeformation(0F)).texOffs(56, 159).addBox(-14F, -24F, -7F, 13F, 4F, 15F, new CubeDeformation(0F)).texOffs(124, 178).addBox(-11F, -20F, -4F, 7F, 4F, 9F, new CubeDeformation(0F)).texOffs(396, 105).addBox(-18F, -29F, -11F, 21F, 2F, 23F, new CubeDeformation(0F)), PartPose.offset(-28F, -4F, 0F));
        p_tremie.addOrReplaceChild("tremie_goulotte_r", CubeListBuilder.create().texOffs(0, 191).addBox(-1F, 0F, -3.5F, 18F, 3F, 7F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -18F, 0.5F, 0F, 0F, 0.3142F));
        PartDefinition p_minerais = p_tremie.addOrReplaceChild("minerais", CubeListBuilder.create().texOffs(244, 134).addBox(-8F, -4F, -8F, 15F, 4F, 17F, new CubeDeformation(0F)), PartPose.offset(-7F, -32F, 0F));
        p_minerais.addOrReplaceChild("minerais_minerai_1_r", CubeListBuilder.create().texOffs(94, 191).addBox(-2F, -4F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(3F, -4F, -3F, 0F, -0.3491F, 0F));
        p_minerais.addOrReplaceChild("minerais_minerai_2_r", CubeListBuilder.create().texOffs(50, 191).addBox(-2.5F, -5F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-3.5F, -4F, 3.5F, 0F, -0.6109F, 0F));
        p_minerais.addOrReplaceChild("minerais_minerai_3_r", CubeListBuilder.create().texOffs(368, 191).addBox(-1.5F, -3F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(1.5F, -5F, 5.5F, 0F, -0.8727F, 0F));
        p_minerais.addOrReplaceChild("minerais_minerai_4_r", CubeListBuilder.create().texOffs(368, 191).addBox(-1.5F, -3F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-5.5F, -4F, -2.5F, 0F, -1.1345F, 0F));
        p_minerais.addOrReplaceChild("minerais_minerai_5_r", CubeListBuilder.create().texOffs(368, 191).addBox(-1.5F, -3F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.5F, -7F, 0.5F, 0F, -1.3963F, 0F));
        PartDefinition p_sortie = p_racine.addOrReplaceChild("sortie", CubeListBuilder.create().texOffs(200, 105).addBox(-12F, -3F, -12F, 22F, 3F, 24F, new CubeDeformation(0F)).texOffs(304, 105).addBox(-12F, -5F, -12F, 22F, 2F, 24F, new CubeDeformation(0F)).texOffs(366, 159).addBox(-12F, -17F, 10F, 22F, 12F, 2F, new CubeDeformation(0F)).texOffs(248, 68).addBox(8F, -17F, -12F, 2F, 12F, 22F, new CubeDeformation(0F)), PartPose.offset(28F, -4F, 0F));
        p_sortie.addOrReplaceChild("sortie_glissiere_r", CubeListBuilder.create().texOffs(272, 178).addBox(-2F, -1F, -4F, 20F, 3F, 8F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-16F, -17F, 0F, 0F, 0F, -0.2793F));
        PartDefinition p_blocs_compresses = p_sortie.addOrReplaceChild("blocs_compresses", CubeListBuilder.create().texOffs(156, 178).addBox(0F, -7F, -8F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(-7F, -7F, -8F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(0F, -7F, -1F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(-7F, -7F, -1F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(0F, -7F, 5F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(-7F, -7F, 5F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(-3F, -14.5F, -5F, 6F, 7F, 6F, new CubeDeformation(0F)).texOffs(156, 178).addBox(-3F, -14.5F, 2F, 6F, 7F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -5F, 0F));
        PartDefinition p_reacteur = p_racine.addOrReplaceChild("reacteur", CubeListBuilder.create().texOffs(304, 0).addBox(-24F, -30F, -6F, 48F, 30F, 8F, new CubeDeformation(0F)).texOffs(0, 178).addBox(-26F, -33F, -7F, 52F, 3F, 10F, new CubeDeformation(0F)).texOffs(126, 159).addBox(-8F, -6F, -10F, 16F, 6F, 12F, new CubeDeformation(0F)).texOffs(232, 134).addBox(-9F, -26F, -10F, 3F, 20F, 3F, new CubeDeformation(0F)).texOffs(232, 134).addBox(6F, -26F, -10F, 3F, 20F, 3F, new CubeDeformation(0F)).texOffs(214, 159).addBox(-9F, -29F, -11F, 18F, 3F, 14F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 30F));
        PartDefinition p_cristal_source = p_reacteur.addOrReplaceChild("cristal_source", CubeListBuilder.create().texOffs(208, 134).addBox(-3F, -9F, -3F, 6F, 17F, 6F, new CubeDeformation(0F)).texOffs(110, 191).addBox(-2F, -13F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)).texOffs(352, 191).addBox(-2F, 8F, -2F, 4F, 2F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -16F, -4F));
        PartDefinition p_conduits = p_racine.addOrReplaceChild("conduits", CubeListBuilder.create().texOffs(126, 191).addBox(-2F, -17F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)).texOffs(296, 68).addBox(-2F, -47F, -2F, 4F, 30F, 4F, new CubeDeformation(0F)).texOffs(278, 159).addBox(-2F, -48F, -11F, 4F, 4F, 13F, new CubeDeformation(0F)).texOffs(70, 191).addBox(-3F, -28F, -3F, 6F, 3F, 6F, new CubeDeformation(0F)).texOffs(70, 191).addBox(-3F, -39F, -3F, 6F, 3F, 6F, new CubeDeformation(0F)).texOffs(220, 178).addBox(-2F, 14F, -8F, 4F, 2F, 10F, new CubeDeformation(0F)).texOffs(314, 191).addBox(-4F, 13F, -10F, 8F, 3F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, 20F));
        PartDefinition p_engrenage_g = p_racine.addOrReplaceChild("engrenage_g", CubeListBuilder.create().texOffs(208, 191).addBox(-8F, -2F, -1.5F, 16F, 4F, 3F, new CubeDeformation(0F)).texOffs(112, 159).addBox(-2F, -8F, -1.5F, 4F, 16F, 3F, new CubeDeformation(0F)).texOffs(328, 178).addBox(-3F, -3F, -2.5F, 6F, 6F, 5F, new CubeDeformation(0F)), PartPose.offset(-16F, -52F, 19.5F));
        p_engrenage_g.addOrReplaceChild("engrenage_g_engrenage_g_diag_1_r", CubeListBuilder.create().texOffs(158, 191).addBox(-11F, -2F, -1.5F, 22F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        p_engrenage_g.addOrReplaceChild("engrenage_g_engrenage_g_diag_2_r", CubeListBuilder.create().texOffs(158, 191).addBox(-11F, -2F, -1.5F, 22F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.7854F));
        PartDefinition p_engrenage_d = p_racine.addOrReplaceChild("engrenage_d", CubeListBuilder.create().texOffs(284, 191).addBox(-6F, -2F, -1.5F, 12F, 4F, 3F, new CubeDeformation(0F)).texOffs(352, 159).addBox(-2F, -6F, -1.5F, 4F, 12F, 3F, new CubeDeformation(0F)).texOffs(350, 178).addBox(-3F, -3F, -2.5F, 6F, 6F, 5F, new CubeDeformation(0F)), PartPose.offset(16F, -44F, 19.5F));
        p_engrenage_d.addOrReplaceChild("engrenage_d_engrenage_d_diag_1_r", CubeListBuilder.create().texOffs(246, 191).addBox(-8F, -2F, -1.5F, 16F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        p_engrenage_d.addOrReplaceChild("engrenage_d_engrenage_d_diag_2_r", CubeListBuilder.create().texOffs(246, 191).addBox(-8F, -2F, -1.5F, 16F, 4F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.7854F));
        PartDefinition p_manometre = p_racine.addOrReplaceChild("manometre", CubeListBuilder.create().texOffs(248, 178).addBox(7F, -25F, -1F, 9F, 9F, 3F, new CubeDeformation(0F)).texOffs(142, 191).addBox(8F, -24F, 1.5F, 7F, 7F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -40F, 22F));
        PartDefinition p_aiguille = p_manometre.addOrReplaceChild("aiguille", CubeListBuilder.create().texOffs(424, 191).addBox(-0.5F, -3.5F, 0.5F, 1F, 4F, 1F, new CubeDeformation(0F)), PartPose.offset(11.5F, -20.5F, 1F));
        return LayerDefinition.create(mesh, 512, 256);
    }

    /** Dans l'ordre des indices de PresseCompressionAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "aiguille|r|0,0,0,-30,l;4,0,0,-30,l\ncristal_source|p|0,0,0,0,c;2,0,0.6,0,c;4,0,0,0,c"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "aiguille|r|0,0,0,-30,l;1,0,0,40,l\ncristal_source|p|0,0,0,0,l;0.2,0,0.208,0,l;0.5,0,1,0,l;0.8,0,1.792,0,l;1,0,2,0,l\nengrenage_d|r|0,0,0,0,l;1,0,0,-120,l\nengrenage_g|r|0,0,0,0,l;1,0,0,90,l\npiston|p|0,0,0,0,l;0.2,0,0.416,0,l;0.5,0,2,0,l;0.8,0,3.584,0,l;1,0,4,0,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "aiguille|r|0,0,0,40,l;0.8,0,0,70,l;1.6,0,0,40,l\nblocs_compresses|s|0,1,1,1,c;0.9,1,1.06,1,c;1.6,1,1,1,c\ncristal_source|p|0,0,2,0,c;0.8,0,2.8,0,c;1.6,0,2,0,c\ncristal_source|r|0,0,0,0,l;1.6,0,-90,0,l\nengrenage_d|r|0,0,0,-120,l;1.6,0,0,-480,l\nengrenage_g|r|0,0,0,90,l;1.6,0,0,450,l\npiston|p|0,0,4,0,c;0.5,0,4,0,c;0.8,0,-11,0,c;1,0,-11,0,c;1.6,0,4,0,c"
        }, 1.6F, true)
        };
    }
}
