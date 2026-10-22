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

/** Modele et animations de Maître Archer (format Modded Entity de Blockbench, art/classes_blockbench/maitre_archer). */
public final class MaitreArcherModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("maitre_archer"), "main");

    private MaitreArcherModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(32, 28).addBox(-4F, -12F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(34, 44).addBox(-4.6F, -9F, -2.4F, 9F, 8F, 5F, new CubeDeformation(0F)).texOffs(24, 84).addBox(-4.4F, -2F, -2.6F, 9F, 2F, 5F, new CubeDeformation(0F)).texOffs(28, 100).addBox(-1F, -2.2F, -3.1F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 28).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)).texOffs(16, 60).addBox(-4.5F, -9.5F, -4.5F, 9F, 2F, 9F, new CubeDeformation(0F)).texOffs(0, 73).addBox(-4.5F, -8F, 3.5F, 9F, 9F, 2F, new CubeDeformation(0F)).texOffs(0, 44).addBox(-5F, -7.5F, -4F, 1F, 8F, 8F, new CubeDeformation(0F)).texOffs(0, 44).mirror().addBox(4F, -7.5F, -4F, 1F, 8F, 8F, new CubeDeformation(0F)).mirror(false).texOffs(34, 100).addBox(-4.5F, -8F, -5F, 9F, 1F, 1F, new CubeDeformation(0F)).texOffs(0, 93).addBox(-1.5F, -11F, -1.5F, 3F, 2F, 5F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(18, 44).mirror().addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(16, 93).addBox(-3F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(46, 73).mirror().addBox(-2.9F, 5F, -2.2F, 4F, 5F, 4F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(-5F, -10F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(18, 44).addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).texOffs(30, 93).addBox(-1F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(46, 73).addBox(-0.9F, 5F, -2.2F, 4F, 5F, 4F, new CubeDeformation(0.2F)), PartPose.offset(5F, -10F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(0, 60).mirror().addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(28, 73).mirror().addBox(-1.8F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-2F, -12F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(0, 60).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).texOffs(28, 73).addBox(-2.2F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)), PartPose.offset(2F, -12F, 0F));
        PartDefinition p_cape = p_corps.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(2, 0).addBox(-4.5F, 0F, 0F, 9F, 17F, 1F, new CubeDeformation(0F)).texOffs(0, 100).addBox(-3.5F, 17F, 0.1F, 7F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -11F, 2F));
        PartDefinition p_carquois = p_corps.addOrReplaceChild("carquois", CubeListBuilder.create().texOffs(22, 0).addBox(-2F, -2F, 0F, 4F, 14F, 3F, new CubeDeformation(0F)).texOffs(44, 93).addBox(0.5F, -6F, 0.5F, 1F, 4F, 1F, new CubeDeformation(0F)).texOffs(16, 100).addBox(0.25F, -3F, 0.7F, 1F, 3F, 1F, new CubeDeformation(-0.25F)).texOffs(48, 93).addBox(-0.5F, -6F, 0.9F, 1F, 4F, 1F, new CubeDeformation(0F)).texOffs(20, 100).addBox(-0.75F, -3F, 1.1F, 1F, 3F, 1F, new CubeDeformation(-0.25F)).texOffs(52, 93).addBox(-1.5F, -6F, 1.3F, 1F, 4F, 1F, new CubeDeformation(0F)).texOffs(24, 100).addBox(-1.75F, -3F, 1.5F, 1F, 3F, 1F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(2F, -8F, 3F, 0F, 0F, -0.2094F));
        PartDefinition p_arme = p_bras_gauche.addOrReplaceChild("arme", CubeListBuilder.create().texOffs(22, 73).addBox(0F, -4F, -2F, 1F, 8F, 2F, new CubeDeformation(0F)).texOffs(0, 0).addBox(-1.5F, -14F, 2.3F, 1F, 28F, 0F, new CubeDeformation(0F)), PartPose.offset(1F, 10F, -2F));
        p_arme.addOrReplaceChild("arme_arc_haut_1_r", CubeListBuilder.create().texOffs(0, 84).addBox(-1F, -7F, 0F, 1F, 7F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.5F, -4F, -1F, 0.2443F, 0F, 0F));
        p_arme.addOrReplaceChild("arme_arc_haut_2_r", CubeListBuilder.create().texOffs(12, 84).addBox(-1F, -6F, 0F, 1F, 6F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -10F, 0.8F, 0.5236F, 0F, 0F));
        p_arme.addOrReplaceChild("arme_arc_bas_1_r", CubeListBuilder.create().texOffs(6, 84).addBox(-1F, 0F, 0F, 1F, 7F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0.5F, 4F, -1F, -0.2443F, 0F, 0F));
        p_arme.addOrReplaceChild("arme_arc_bas_2_r", CubeListBuilder.create().texOffs(18, 84).addBox(-1F, 0F, 0F, 1F, 6F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 10F, 0.8F, -0.5236F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de MaitreArcherAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,c;0.9,-2,0,1.5,c;1.8,0,0,0,c;2.7,2,0,1.5,c;3.6,0,0,0,c\nbras_gauche|r|0,0,0,0,c;0.9,2,0,-1.5,c;1.8,0,0,0,c;2.7,-2,0,-1.5,c;3.6,0,0,0,c\ncape|r|0,0,0,0,c;0.9,-4,0,2,c;1.8,0,0,0,c;2.7,-4,0,-2,c;3.6,0,0,0,c\ncarquois|r|0,0,0,0,c;1.8,-1.5,0,0,c;3.6,0,0,0,c\ncorps|r|0,0,0,0,c;0.9,0.8,0,0.6,c;1.8,0,0,0,c;2.7,0.8,0,-0.6,c;3.6,0,0,0,c\ncorps|s|0,1,1,1,c;0.9,1,1.012,1,c;1.8,1,1,1,c;2.7,1,1.012,1,c;3.6,1,1,1,c\ntete|r|0,0,0,0,c;0.9,-0.8,-8,0,c;1.8,0,0,0,c;2.7,-0.8,8,0,c;3.6,0,0,0,c"
        }, 3.6F, true),
        BlockbenchAnimations.parse(new String[]{
                "arme|r|0,0,0,0,c;0.5,0,0,0,c;1.4,0,0,0,c;2.4,0,0,0,c\nbras_droit|r|0,0,0,0,l;0.1,-6.24,2.6,0,l;0.25,-30,12.5,0,l;0.4,-53.76,22.4,0,l;0.5,-60,25,0,l;0.68,-61.56,26.04,0,l;0.95,-67.5,30,0,l;1.22,-73.44,33.96,0,l;1.4,-75,35,0,l;1.6,-67.2,31.36,0,l;1.9,-37.5,17.5,0,l;2.2,-7.8,3.64,0,l;2.4,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.1,-8.32,0,-0.416,l;0.25,-40,0,-2,l;0.4,-71.68,0,-3.584,l;0.5,-80,0,-4,l;0.68,-81.04,0,-4,l;0.95,-85,0,-4,l;1.22,-88.96,0,-4,l;1.4,-90,0,-4,l;1.6,-80.64,0,-3.584,l;1.9,-45,0,-2,l;2.2,-9.36,0,-0.416,l;2.4,0,0,0,l\ncorps|r|0,0,0,0,l;0.1,0,1.04,0,l;0.25,0,5,0,l;0.4,0,8.96,0,l;0.5,0,10,0,l;0.68,0,10.208,0,l;0.95,0,11,0,l;1.22,0,11.792,0,l;1.4,0,12,0,l;1.6,0,10.752,0,l;1.9,0,6,0,l;2.2,0,1.248,0,l;2.4,0,0,0,l\ntete|r|0,0,0,0,l;0.1,0,-1.248,0,l;0.25,0,-6,0,l;0.4,0,-10.752,0,l;0.5,0,-12,0,l;0.68,0,-12,0,l;0.95,0,-12,0,l;1.22,0,-12,0,l;1.4,0,-12,0,l;1.6,0,-10.752,0,l;1.9,0,-6,0,l;2.2,0,-1.248,0,l;2.4,0,0,0,l"
        }, 2.4F, false)
        };
    }
}
