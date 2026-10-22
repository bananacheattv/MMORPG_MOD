// Genere automatiquement par art/classes_blockbench/generer_classes.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.hub;

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

/** Modele et animations de Gardien de l'épreuve (format Modded Entity de Blockbench, art/classes_blockbench/gardien_epreuve). */
public final class GardienEpreuveModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("gardien_epreuve"), "main");

    private GardienEpreuveModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(32, 39).addBox(-4F, -12F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(16, 55).addBox(-4.5F, -10F, -2.6F, 9F, 9F, 5F, new CubeDeformation(0F)).texOffs(12, 0).addBox(-5F, 0F, -3F, 10F, 11F, 6F, new CubeDeformation(0F)).texOffs(26, 82).addBox(-4.8F, 10.5F, -3.2F, 10F, 1F, 6F, new CubeDeformation(0F)).texOffs(4, 0).addBox(-3F, -10F, -3.1F, 2F, 20F, 0F, new CubeDeformation(0F)).texOffs(8, 0).addBox(1F, -10F, -3.1F, 2F, 20F, 0F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 39).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)).texOffs(18, 71).addBox(-4F, -9F, -4F, 8F, 2F, 8F, new CubeDeformation(0.3F)).texOffs(0, 71).addBox(-4F, -8F, 3.5F, 8F, 10F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(0, 55).mirror().addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(40, 91).addBox(-3F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(0, 91).mirror().addBox(-3.6F, -1F, -2.6F, 5F, 2F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-5F, -10F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(0, 55).addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).texOffs(0, 98).addBox(-1F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(0, 91).addBox(-1.4F, -1F, -2.6F, 5F, 2F, 5F, new CubeDeformation(0F)), PartPose.offset(5F, -10F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(44, 55).mirror().addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(0, 82).mirror().addBox(-1.8F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-2F, -12F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(44, 55).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).texOffs(0, 82).addBox(-2.2F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)), PartPose.offset(2F, -12F, 0F));
        PartDefinition p_effet = p_tete.addOrReplaceChild("effet", CubeListBuilder.create().texOffs(0, 103).addBox(-6F, -5.5F, -0.3F, 12F, 1F, 1F, new CubeDeformation(0F)).texOffs(26, 103).addBox(-6F, 3.5F, -0.3F, 12F, 1F, 1F, new CubeDeformation(0F)).texOffs(18, 82).addBox(5.5F, -4.5F, -0.3F, 1F, 8F, 1F, new CubeDeformation(0F)).texOffs(22, 82).addBox(-6.5F, -4.5F, -0.3F, 1F, 8F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 4.5F));
        PartDefinition p_arme = p_bras_droit.addOrReplaceChild("arme", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -26F, -0.5F, 1F, 38F, 1F, new CubeDeformation(0F)).texOffs(28, 91).addBox(-1.5F, -29F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)), PartPose.offset(-1F, 10F, -1F));
        PartDefinition p_sablier = p_corps.addOrReplaceChild("sablier", CubeListBuilder.create().texOffs(14, 98).addBox(-2F, -2F, -1F, 4F, 1F, 3F, new CubeDeformation(0F)).texOffs(28, 98).addBox(-2F, 4F, -1F, 4F, 1F, 3F, new CubeDeformation(0F)).texOffs(20, 91).addBox(-1F, -1F, -0.5F, 2F, 5F, 2F, new CubeDeformation(0F)), PartPose.offset(9F, -10F, -3F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de GardienEpreuveAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,c;1,-2,0,1.5,c;2,0,0,0,c;3,2,0,1.5,c;4,0,0,0,c\nbras_gauche|r|0,0,0,0,c;1,2,0,-1.5,c;2,0,0,0,c;3,-2,0,-1.5,c;4,0,0,0,c\ncorps|r|0,0,0,0,c;1,0.8,0,0.6,c;2,0,0,0,c;3,0.8,0,-0.6,c;4,0,0,0,c\ncorps|s|0,1,1,1,c;1,1,1.012,1,c;2,1,1,1,c;3,1,1.012,1,c;4,1,1,1,c\neffet|r|0,0,0,0,l;2,0,0,180,l;4,0,0,360,l\nsablier|p|0,0,0,0,c;1,0,1,0,c;2,0,0,0,c;3,0,-1,0,c;4,0,0,0,c\nsablier|r|0,0,0,0,l;4,0,-360,0,l\ntete|r|0,0,0,0,c;1,-0.8,-8,0,c;2,0,0,0,c;3,-0.8,8,0,c;4,0,0,0,c"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,l;0.12,0,0,-0.624,l;0.3,0,0,-3,l;0.48,0,0,-5.376,l;0.6,0,0,-6,l;0.8,0,0,-5.376,l;1.1,0,0,-3,l;1.4,0,0,-0.624,l;1.6,0,0,0,l;1.76,0,0,0,l;2,0,0,0,l;2.24,0,0,0,l;2.4,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.12,6.24,0,2.08,l;0.3,30,0,10,l;0.48,53.76,0,17.92,l;0.6,60,0,20,l;0.8,60,0,20,l;1.1,60,0,20,l;1.4,60,0,20,l;1.6,60,0,20,l;1.76,53.76,0,17.92,l;2,30,0,10,l;2.24,6.24,0,2.08,l;2.4,0,0,0,l\ntete|r|0,0,0,0,l;0.12,0.624,0,0,l;0.3,3,0,0,l;0.48,5.376,0,0,l;0.6,6,0,0,l;0.8,6,0,0,l;1.1,6,0,0,l;1.4,6,0,0,l;1.6,6,0,0,l;1.76,5.376,0,0,l;2,3,0,0,l;2.24,0.624,0,0,l;2.4,0,0,0,l"
        }, 2.4F, false)
        };
    }
}
