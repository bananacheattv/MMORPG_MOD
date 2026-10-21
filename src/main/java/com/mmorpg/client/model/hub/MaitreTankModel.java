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

/** Modele et animations de Maître Tank (format Modded Entity de Blockbench, art/classes_blockbench/maitre_tank). */
public final class MaitreTankModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("maitre_tank"), "main");

    private MaitreTankModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(32, 51).addBox(-4F, -12F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(0, 33).addBox(-5F, -12F, -3F, 10F, 12F, 6F, new CubeDeformation(0.2F)).texOffs(32, 94).addBox(-4.4F, -1F, -2.8F, 9F, 3F, 5F, new CubeDeformation(0F)).texOffs(0, 94).addBox(-5F, -1F, -3.2F, 10F, 2F, 6F, new CubeDeformation(0F)).texOffs(46, 102).addBox(-2F, -9F, -3.25F, 4F, 6F, 0F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(32, 33).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)).texOffs(0, 51).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.5F)).texOffs(46, 110).addBox(-3.5F, -4.5F, -4.7F, 7F, 1F, 1F, new CubeDeformation(0F)).texOffs(26, 83).addBox(-0.5F, -11F, -4F, 1F, 3F, 8F, new CubeDeformation(0F)).texOffs(0, 102).addBox(-1F, -12F, -3F, 2F, 2F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(0, 67).mirror().addBox(-3.5F, -2F, -2F, 5F, 12F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(0, 110).addBox(-3.5F, 9F, -1.5F, 5F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(0, 83).mirror().addBox(-3.7F, -3F, -3.5F, 6F, 4F, 7F, new CubeDeformation(0F)).mirror(false).texOffs(28, 102).addBox(-2.7F, -4F, -2.5F, 4F, 1F, 5F, new CubeDeformation(0F)), PartPose.offset(-5.5F, -10F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(0, 67).addBox(-1.5F, -2F, -2F, 5F, 12F, 4F, new CubeDeformation(0F)).texOffs(16, 110).addBox(-1.5F, 9F, -1.5F, 5F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(0, 83).addBox(-2.3F, -3F, -3.5F, 6F, 4F, 7F, new CubeDeformation(0F)).texOffs(28, 102).addBox(-1.3F, -4F, -2.5F, 4F, 1F, 5F, new CubeDeformation(0F)), PartPose.offset(5.5F, -10F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(28, 67).mirror().addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(44, 83).mirror().addBox(-1.8F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-2F, -12F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(28, 67).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).texOffs(44, 83).addBox(-2.2F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)), PartPose.offset(2F, -12F, 0F));
        PartDefinition p_arme = p_bras_gauche.addOrReplaceChild("arme", CubeListBuilder.create().texOffs(30, 0).addBox(0F, -10F, -3F, 3F, 20F, 11F, new CubeDeformation(0F)).texOffs(0, 0).addBox(0.4F, -10.5F, -3.4F, 3F, 21F, 12F, new CubeDeformation(0F)).texOffs(18, 67).addBox(3.55F, -4F, 0F, 0F, 9F, 5F, new CubeDeformation(0F)).texOffs(32, 110).addBox(0.5F, 10F, 1F, 2F, 2F, 3F, new CubeDeformation(0F)), PartPose.offset(3.5F, 6F, -3F));
        PartDefinition p_effet = p_corps.addOrReplaceChild("effet", CubeListBuilder.create().texOffs(44, 67).addBox(-0.5F, -3F, 1.5F, 1F, 12F, 1F, new CubeDeformation(0F)).texOffs(16, 102).addBox(-1.5F, -6F, 0.5F, 3F, 4F, 3F, new CubeDeformation(0F)).texOffs(42, 110).addBox(-0.5F, -8F, 1.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 0F, 1F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de MaitreTankAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "arme|r|0,0,0,0,c;1.8,0,0,1,c;3.6,0,0,0,c\narme|s|0,1,1,1,c;0.9,1,1.004,1,c;1.8,1,1,1,c;3.6,1,1,1,c\nbras_droit|r|0,0,0,0,c;0.9,-2,0,1.5,c;1.8,0,0,0,c;2.7,2,0,1.5,c;3.6,0,0,0,c\nbras_gauche|r|0,0,0,0,c;0.9,2,0,-1.5,c;1.8,0,0,0,c;2.7,-2,0,-1.5,c;3.6,0,0,0,c\ncorps|r|0,0,0,0,c;0.9,0.8,0,0.6,c;1.8,0,0,0,c;2.7,0.8,0,-0.6,c;3.6,0,0,0,c\ncorps|s|0,1,1,1,c;0.9,1,1.012,1,c;1.8,1,1,1,c;2.7,1,1.012,1,c;3.6,1,1,1,c\ntete|r|0,0,0,0,c;0.9,-0.8,-8,0,c;1.8,0,0,0,c;2.7,-0.8,8,0,c;3.6,0,0,0,c"
        }, 3.6F, true),
        BlockbenchAnimations.parse(new String[]{
                "arme|r|0,0,0,0,l;0.09,2.08,0,0,l;0.225,10,0,0,l;0.36,17.92,0,0,l;0.45,20,0,0,l;0.7,-10,0,0,l;0.8,-8.96,0,0,l;0.95,-5,0,0,l;1.1,-1.04,0,0,l;1.2,0,0,0,l;1.44,0,0,0,l;1.8,0,0,0,l;2.16,0,0,0,l;2.4,0,0,0,l\narme|s|0,1,1,1,c;0.7,1.12,1.12,1.12,c;1.4,1,1,1,c;2.4,1,1,1,c\nbras_gauche|r|0,0,0,0,l;0.09,-4.68,0,1.04,l;0.225,-22.5,0,5,l;0.36,-40.32,0,8.96,l;0.45,-45,0,10,l;0.7,20,0,6,l;0.8,18.96,0,6,l;0.95,15,0,6,l;1.1,11.04,0,6,l;1.2,10,0,6,l;1.44,8.96,0,5.376,l;1.8,5,0,3,l;2.16,1.04,0,0.624,l;2.4,0,0,0,l\ncorps|r|0,0,0,0,l;0.09,0.624,0,0,l;0.225,3,0,0,l;0.36,5.376,0,0,l;0.45,6,0,0,l;0.7,-8,0,0,l;0.8,-7.584,0,0,l;0.95,-6,0,0,l;1.1,-4.416,0,0,l;1.2,-4,0,0,l;1.44,-3.584,0,0,l;1.8,-2,0,0,l;2.16,-0.416,0,0,l;2.4,0,0,0,l\ntete|r|0,0,0,0,l;0.09,0.832,0,0,l;0.225,4,0,0,l;0.36,7.168,0,0,l;0.45,8,0,0,l;0.7,-6,0,0,l;0.8,-5.376,0,0,l;0.95,-3,0,0,l;1.1,-0.624,0,0,l;1.2,0,0,0,l;1.44,0,0,0,l;1.8,0,0,0,l;2.16,0,0,0,l;2.4,0,0,0,l"
        }, 2.4F, false)
        };
    }
}
