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

/** Modele et animations de Pancarte des quêtes hebdomadaires (format Modded Entity de Blockbench, art/structures_blockbench/quetes_hebdo). */
public final class QuetesHebdoModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("quetes_hebdo"), "main");

    private QuetesHebdoModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(0, 105).addBox(-44F, -3F, -14F, 88F, 3F, 28F, new CubeDeformation(0F)).texOffs(0, 206).addBox(-20F, -2F, -18F, 40F, 2F, 4F, new CubeDeformation(0F)).texOffs(0, 136).addBox(-38F, -9F, -6F, 8F, 6F, 12F, new CubeDeformation(0F)).texOffs(0, 136).addBox(30F, -9F, -6F, 8F, 6F, 12F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_poteaux = p_racine.addOrReplaceChild("poteaux", CubeListBuilder.create().texOffs(0, 0).addBox(-37F, -70F, -3F, 6F, 64F, 6F, new CubeDeformation(0F)).texOffs(120, 167).addBox(-38F, -14F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(120, 167).addBox(-38F, -62F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(152, 167).addBox(-38F, -73F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(0, 0).addBox(31F, -70F, -3F, 6F, 64F, 6F, new CubeDeformation(0F)).texOffs(120, 167).addBox(30F, -14F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(120, 167).addBox(30F, -62F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(152, 167).addBox(30F, -73F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, -3F, 0F));
        PartDefinition p_lanternes = p_racine.addOrReplaceChild("lanternes", CubeListBuilder.create().texOffs(88, 206).addBox(-35F, -2F, -1F, 2F, 4F, 2F, new CubeDeformation(0F)).texOffs(88, 206).addBox(33F, -2F, -1F, 2F, 4F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -78F, 0F));
        PartDefinition p_panneau = p_racine.addOrReplaceChild("panneau", CubeListBuilder.create().texOffs(42, 0).addBox(-32F, -22F, -2F, 64F, 46F, 4F, new CubeDeformation(0F)).texOffs(0, 178).addBox(-35F, -26F, -3F, 70F, 4F, 6F, new CubeDeformation(0F)).texOffs(0, 188).addBox(-35F, 24F, -3F, 70F, 3F, 6F, new CubeDeformation(0F)).texOffs(24, 0).addBox(-35F, -22F, -3F, 3F, 46F, 6F, new CubeDeformation(0F)).texOffs(24, 0).addBox(32F, -22F, -3F, 3F, 46F, 6F, new CubeDeformation(0F)).texOffs(40, 136).addBox(-40F, -29F, -6F, 80F, 3F, 12F, new CubeDeformation(0F)).texOffs(0, 154).addBox(-40F, -30F, -6F, 80F, 1F, 12F, new CubeDeformation(0F)).texOffs(0, 167).addBox(-26F, -33F, -4F, 52F, 3F, 8F, new CubeDeformation(0F)).texOffs(0, 197).addBox(-26F, -34F, -4F, 52F, 1F, 8F, new CubeDeformation(0F)).texOffs(152, 197).addBox(-12F, -21F, -4F, 24F, 5F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -44F, 0F));
        PartDefinition p_cristal = p_racine.addOrReplaceChild("cristal", CubeListBuilder.create().texOffs(120, 197).addBox(-4F, -1F, -4F, 8F, 1F, 8F, new CubeDeformation(0F)).texOffs(224, 136).addBox(-3F, -9F, -3F, 6F, 8F, 6F, new CubeDeformation(0F)).texOffs(204, 197).addBox(-2F, -12F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -78F, 0F));
        PartDefinition p_parchemins = p_racine.addOrReplaceChild("parchemins", CubeListBuilder.create().texOffs(214, 206).addBox(26F, 27F, -1F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(214, 206).addBox(18F, 27F, -1F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(214, 206).addBox(-18F, 27F, -1F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(214, 206).addBox(-26F, 27F, -1F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -60F, -3F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_0_r", CubeListBuilder.create().texOffs(152, 178).addBox(-4F, 0F, 0F, 7F, 9F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(27F, 27F, 0F, 0F, 0F, -0.0873F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_1_r", CubeListBuilder.create().texOffs(152, 178).addBox(-4F, 0F, 0F, 7F, 9F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(19F, 27F, 0F, 0F, 0F, 0.0698F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_2_r", CubeListBuilder.create().texOffs(152, 178).addBox(-4F, 0F, 0F, 7F, 9F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-17F, 27F, 0F, 0F, 0F, -0.0524F));
        p_parchemins.addOrReplaceChild("parchemins_parchemin_3_r", CubeListBuilder.create().texOffs(152, 178).addBox(-4F, 0F, 0F, 7F, 9F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-25F, 27F, 0F, 0F, 0F, 0.1047F));
        PartDefinition p_etendard_facile = p_racine.addOrReplaceChild("etendard_facile", CubeListBuilder.create().texOffs(162, 206).addBox(-11F, -2F, -1F, 22F, 2F, 2F, new CubeDeformation(0F)).texOffs(178, 0).addBox(-9F, 0F, -1F, 18F, 34F, 1F, new CubeDeformation(0F)).texOffs(96, 206).addBox(-5F, 34F, -1F, 10F, 4F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(6F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(-7F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(-23F, -62F, -4F));
        PartDefinition p_etendard_difficile = p_racine.addOrReplaceChild("etendard_difficile", CubeListBuilder.create().texOffs(162, 206).addBox(-11F, -2F, -1F, 22F, 2F, 2F, new CubeDeformation(0F)).texOffs(216, 0).addBox(-9F, 0F, -1F, 18F, 34F, 1F, new CubeDeformation(0F)).texOffs(118, 206).addBox(-5F, 34F, -1F, 10F, 4F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(6F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(-7F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -62F, -4F));
        PartDefinition p_etendard_tres_difficile = p_racine.addOrReplaceChild("etendard_tres_difficile", CubeListBuilder.create().texOffs(162, 206).addBox(-11F, -2F, -1F, 22F, 2F, 2F, new CubeDeformation(0F)).texOffs(0, 70).addBox(-9F, 0F, -1F, 18F, 34F, 1F, new CubeDeformation(0F)).texOffs(140, 206).addBox(-5F, 34F, -1F, 10F, 4F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(6F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)).texOffs(210, 206).addBox(-7F, -2F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(23F, -62F, -4F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** Dans l'ordre des indices de QuetesHebdoAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "cristal|p|0,0,0,0,l;2,0,1,0,l;4,0,0,0,l\netendard_difficile|r|0,0,0,-0.8,l;2,0,0,0.8,l;4,0,0,-0.8,l\netendard_facile|r|0,0,0,-1.2,l;2,0,0,1.2,l;4,0,0,-1.2,l\netendard_tres_difficile|r|0,0,0,-0.4,l;2,0,0,0.4,l;4,0,0,-0.4,l"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "cristal|p|0,0,0,0,l;1,0,2,0,l\netendard_difficile|r|0,0,0,0,l;0.4,0,0,10,l;0.7,0,0,-6,l;1,0,0,0,l\netendard_facile|r|0,0,0,0,l;0.3,0,0,10,l;0.7,0,0,-6,l;1,0,0,0,l\netendard_tres_difficile|r|0,0,0,0,l;0.5,0,0,10,l;0.7,0,0,-6,l;1,0,0,0,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "cristal|p|0,0,2,0,l;1,0,3,0,l;2,0,2,0,l\ncristal|r|0,0,0,0,l;2,0,-180,0,l\netendard_difficile|r|0,0,0,0,l;0.6,0,0,5,l;1.6,0,0,-5,l;2,0,0,0,l\netendard_facile|r|0,0,0,0,l;0.5,0,0,5,l;1.5,0,0,-5,l;2,0,0,0,l\netendard_tres_difficile|r|0,0,0,0,l;0.7,0,0,5,l;1.7,0,0,-5,l;2,0,0,0,l"
        }, 2F, true)
        };
    }
}
