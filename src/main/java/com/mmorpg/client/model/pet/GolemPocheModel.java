// Genere automatiquement par art/familiers_blockbench/generer_familiers.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.pet;

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

/** Modele et animations de Golem de poche (format Modded Entity de Blockbench, art/familiers_blockbench/golem_poche). */
public final class GolemPocheModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("golem_poche"), "main");

    private GolemPocheModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(32, 24).addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)).texOffs(16, 24).addBox(-2F, 2F, -2.5F, 4F, 2F, 4F, new CubeDeformation(0F)), PartPose.offset(-2F, -4F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0F)).mirror(false).texOffs(16, 24).mirror().addBox(-2F, 2F, -2.5F, 4F, 2F, 4F, new CubeDeformation(0F)).mirror(false), PartPose.offset(2F, -4F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(30, 0).addBox(-4F, -6F, -2.5F, 8F, 6F, 5F, new CubeDeformation(0F)).texOffs(20, 14).addBox(-4.5F, -6F, -3F, 9F, 2F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        PartDefinition p_noyau = p_corps.addOrReplaceChild("noyau", CubeListBuilder.create().texOffs(44, 24).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 1F, new CubeDeformation(0F)).texOffs(12, 32).addBox(-1.5F, -1.5F, -0.3F, 3F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -3F, -3F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -7F, -3.5F, 8F, 7F, 7F, new CubeDeformation(0F)).texOffs(20, 32).addBox(-4.5F, -6F, -4.5F, 9F, 1F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -6F, 0F));
        PartDefinition p_paupieres = p_tete.addOrReplaceChild("paupieres", CubeListBuilder.create().texOffs(42, 32).addBox(-4F, 0F, 0F, 8F, 2F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -5F, -3.55F, 1.5708F, 0F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(0, 24).addBox(-3F, -1F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-5.5F, -5F, 0F, 0F, 0F, 0.0873F));
        PartDefinition p_avant_bras_droit = p_bras_droit.addOrReplaceChild("avant_bras_droit", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0F)).texOffs(0, 14).addBox(-3F, 1F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F)), PartPose.offset(-1F, 2.5F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-1F, -1F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)).mirror(false), PartPose.offsetAndRotation(5.5F, -5F, 0F, 0F, 0F, -0.0873F));
        PartDefinition p_avant_bras_gauche = p_bras_gauche.addOrReplaceChild("avant_bras_gauche", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0F)).mirror(false).texOffs(0, 14).mirror().addBox(-2F, 1F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(1F, 2.5F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Dans l'ordre des indices de GolemPocheAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,c;1.5,0,0,3,c;3,0,0,0,c\nbras_gauche|r|0,0,0,0,c;1.5,0,0,-3,c;3,0,0,0,c\ncorps|p|0,0,0,0,c;1.5,0,-0.3,0,c;3,0,0,0,c\ncorps|r|0,0,0,0,c;1.5,-2,0,0,c;3,0,0,0,c\nnoyau|s|0,1,1,1,c;1.5,1.25,1.25,1.25,c;3,1,1,1,c\ntete|r|0,0,0,0,c;1.5,3,0,0,c;3,0,0,0,c"
        }, 3F, true),
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,-18,0,0,c;0.6,18,0,0,c;1.2,-18,0,0,c\nbras_gauche|r|0,18,0,0,c;0.6,-18,0,0,c;1.2,18,0,0,c\ncorps|r|0,0,0,-4,c;0.6,0,0,4,c;1.2,0,0,-4,c\njambe_droite|r|0,22,0,0,c;0.6,-22,0,0,c;1.2,22,0,0,c\njambe_gauche|r|0,-22,0,0,c;0.6,22,0,0,c;1.2,-22,0,0,c\nracine|p|0,0,0.7,0,c;0.3,0,1.1,0,c;0.6,0,0.7,0,c;0.9,0,1.1,0,c;1.2,0,0.7,0,c"
        }, 1.2F, true),
        BlockbenchAnimations.parse(new String[]{
                "avant_bras_droit|r|0,0,0,0,l;0.06,0,0,1.04,l;0.15,0,0,5,l;0.24,0,0,8.96,l;0.3,0,0,10,l;0.35,0,0,10,l;0.425,0,0,10,l;0.5,0,0,10,l;0.55,0,0,10,l;0.6,0,0,10,l;0.675,0,0,10,l;0.75,0,0,10,l;0.8,0,0,10,l;0.92,0,0,8.96,l;1.1,0,0,5,l;1.28,0,0,1.04,l;1.4,0,0,0,l\navant_bras_gauche|r|0,0,0,0,l;0.06,0,0,-1.04,l;0.15,0,0,-5,l;0.24,0,0,-8.96,l;0.3,0,0,-10,l;0.35,0,0,-10,l;0.425,0,0,-10,l;0.5,0,0,-10,l;0.55,0,0,-10,l;0.6,0,0,-10,l;0.675,0,0,-10,l;0.75,0,0,-10,l;0.8,0,0,-10,l;0.92,0,0,-8.96,l;1.1,0,0,-5,l;1.28,0,0,-1.04,l;1.4,0,0,0,l\nbras_droit|r|0,0,0,0,l;0.06,0,0,15.6,l;0.15,0,0,75,l;0.24,0,0,134.4,l;0.3,0,0,150,l;0.35,0,0,150,l;0.425,0,0,150,l;0.5,0,0,150,l;0.55,0,0,150,l;0.6,0,0,150,l;0.675,0,0,150,l;0.75,0,0,150,l;0.8,0,0,150,l;0.92,0,0,134.4,l;1.1,0,0,75,l;1.28,0,0,15.6,l;1.4,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.06,0,0,-15.6,l;0.15,0,0,-75,l;0.24,0,0,-134.4,l;0.3,0,0,-150,l;0.35,0,0,-150,l;0.425,0,0,-150,l;0.5,0,0,-150,l;0.55,0,0,-150,l;0.6,0,0,-150,l;0.675,0,0,-150,l;0.75,0,0,-150,l;0.8,0,0,-150,l;0.92,0,0,-134.4,l;1.1,0,0,-75,l;1.28,0,0,-15.6,l;1.4,0,0,0,l\ncorps|r|0,0,0,0,l;0.06,0,0,0,l;0.15,0,0,0,l;0.24,0,0,0,l;0.3,0,0,0,l;0.35,-0.624,0,0,l;0.425,-3,0,0,l;0.5,-5.376,0,0,l;0.55,-6,0,0,l;0.6,-5.376,0,0,l;0.675,-3,0,0,l;0.75,-0.624,0,0,l;0.8,0,0,0,l;0.92,0,0,0,l;1.1,0,0,0,l;1.28,0,0,0,l;1.4,0,0,0,l\nnoyau|s|0,1,1,1,l;0.08,1.052,1.052,1.052,l;0.2,1.25,1.25,1.25,l;0.32,1.448,1.448,1.448,l;0.4,1.5,1.5,1.5,l;0.52,1.4688,1.4688,1.4688,l;0.7,1.35,1.35,1.35,l;0.88,1.2312,1.2312,1.2312,l;1,1.2,1.2,1.2,l;1.08,1.1792,1.1792,1.1792,l;1.2,1.1,1.1,1.1,l;1.32,1.0208,1.0208,1.0208,l;1.4,1,1,1,l\nracine|p|0,0,0,0,l;0.06,0,0,0,l;0.15,0,0,0,l;0.24,0,0,0,l;0.3,0,0,0,l;0.34,0,0.312,0,l;0.4,0,1.5,0,l;0.46,0,2.688,0,l;0.5,0,3,0,l;0.54,0,2.688,0,l;0.6,0,1.5,0,l;0.66,0,0.312,0,l;0.7,0,0,0,l;0.84,0,0,0,l;1.05,0,0,0,l;1.26,0,0,0,l;1.4,0,0,0,l\ntete|r|0,0,0,0,l;0.06,-1.248,0,0,l;0.15,-6,0,0,l;0.24,-10.752,0,0,l;0.3,-12,0,0,l;0.35,-12,0,0,l;0.425,-12,0,0,l;0.5,-12,0,0,l;0.55,-12,0,0,l;0.6,-12,0,0,l;0.675,-12,0,0,l;0.75,-12,0,0,l;0.8,-12,0,0,l;0.92,-10.752,0,0,l;1.1,-6,0,0,l;1.28,-1.248,0,0,l;1.4,0,0,0,l"
        }, 1.4F, false),
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,l;0.09,-15.6,0,0,l;0.225,-75,0,0,l;0.36,-134.4,0,0,l;0.45,-150,0,0,l;0.6,-55,0,0,l;0.64,-55,0,0,l;0.7,-55,0,0,l;0.76,-55,0,0,l;0.8,-55,0,0,l;0.88,-49.28,0,0,l;1,-27.5,0,0,l;1.12,-5.72,0,0,l;1.2,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.09,-15.6,0,0,l;0.225,-75,0,0,l;0.36,-134.4,0,0,l;0.45,-150,0,0,l;0.6,-55,0,0,l;0.64,-55,0,0,l;0.7,-55,0,0,l;0.76,-55,0,0,l;0.8,-55,0,0,l;0.88,-49.28,0,0,l;1,-27.5,0,0,l;1.12,-5.72,0,0,l;1.2,0,0,0,l\ncorps|p|0,0,0,0,l;0.09,0,0.0416,0,l;0.225,0,0.2,0,l;0.36,0,0.3584,0,l;0.45,0,0.4,0,l;0.6,0,-0.1,0,l;0.64,0,-0.1,0,l;0.7,0,-0.1,0,l;0.76,0,-0.1,0,l;0.8,0,-0.1,0,l;0.88,0,-0.0896,0,l;1,0,-0.05,0,l;1.12,0,-0.0104,0,l;1.2,0,0,0,l\ncorps|r|0,0,0,0,l;0.09,-1.04,0,0,l;0.225,-5,0,0,l;0.36,-8.96,0,0,l;0.45,-10,0,0,l;0.6,22,0,0,l;0.64,22,0,0,l;0.7,22,0,0,l;0.76,22,0,0,l;0.8,22,0,0,l;0.88,19.712,0,0,l;1,11,0,0,l;1.12,2.288,0,0,l;1.2,0,0,0,l\njambe_droite|r|0,0,0,0,l;0.09,0,0,0,l;0.225,0,0,0,l;0.36,0,0,0,l;0.45,0,0,0,l;0.6,-5,0,0,l;0.64,-5,0,0,l;0.7,-5,0,0,l;0.76,-5,0,0,l;0.8,-5,0,0,l;0.88,-4.48,0,0,l;1,-2.5,0,0,l;1.12,-0.52,0,0,l;1.2,0,0,0,l\njambe_gauche|r|0,0,0,0,l;0.09,0,0,0,l;0.225,0,0,0,l;0.36,0,0,0,l;0.45,0,0,0,l;0.6,-5,0,0,l;0.64,-5,0,0,l;0.7,-5,0,0,l;0.76,-5,0,0,l;0.8,-5,0,0,l;0.88,-4.48,0,0,l;1,-2.5,0,0,l;1.12,-0.52,0,0,l;1.2,0,0,0,l\ntete|r|0,0,0,0,l;0.09,0.624,0,0,l;0.225,3,0,0,l;0.36,5.376,0,0,l;0.45,6,0,0,l;0.6,-12,0,0,l;0.64,-12,0,0,l;0.7,-12,0,0,l;0.76,-12,0,0,l;0.8,-12,0,0,l;0.88,-10.752,0,0,l;1,-6,0,0,l;1.12,-1.248,0,0,l;1.2,0,0,0,l"
        }, 1.2F, false),
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,-46,0,10,c;2,-46,0,10,c;4,-46,0,10,c\nbras_gauche|r|0,-46,0,-10,c;2,-46,0,-10,c;4,-46,0,-10,c\ncorps|r|0,-6,0,0,c;2,-6,0,0,c;4,-6,0,0,c\njambe_droite|r|0,-80,0,8,c;2,-80,0,8,c;4,-80,0,8,c\njambe_gauche|r|0,-80,0,-8,c;2,-80,0,-8,c;4,-80,0,-8,c\nnoyau|s|0,0.8,0.8,0.8,c;2,0.9,0.9,0.9,c;4,0.8,0.8,0.8,c\npaupieres|r|0,-90,0,0,c;2,-90,0,0,c;4,-90,0,0,c\nracine|p|0,0,-1.55,0,c;2,0,-1.55,0,c;4,0,-1.55,0,c\ntete|r|0,20,0,0,c;2,44,0,0,c;4,20,0,0,c"
        }, 4F, true)
        };
    }
}
