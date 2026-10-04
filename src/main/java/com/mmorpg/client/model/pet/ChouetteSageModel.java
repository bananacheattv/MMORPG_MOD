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

/** Modele et animations de Chouette sage (format Modded Entity de Blockbench, art/familiers_blockbench/chouette_sage). */
public final class ChouetteSageModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("chouette_sage"), "main");

    private ChouetteSageModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(30, 0).addBox(-3.5F, -5F, -3F, 7F, 5F, 6F, new CubeDeformation(0F)).texOffs(48, 13).addBox(-1F, -3.5F, -3.5F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -1F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -6F, -3.5F, 8F, 6F, 7F, new CubeDeformation(0F)), PartPose.offset(0F, -5F, -0.5F));
        p_tete.addOrReplaceChild("tete_aigrette_droite_r", CubeListBuilder.create().texOffs(32, 13).addBox(-1F, -2F, -1F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-3F, -6F, -1F, 0F, 0F, -0.384F));
        p_tete.addOrReplaceChild("tete_aigrette_gauche_r", CubeListBuilder.create().texOffs(32, 13).mirror().addBox(-1F, -2F, -1F, 2F, 2F, 2F, new CubeDeformation(0F)).mirror(false), PartPose.offsetAndRotation(3F, -6F, -1F, 0F, 0F, 0.384F));
        PartDefinition p_bec = p_tete.addOrReplaceChild("bec", CubeListBuilder.create().texOffs(54, 13).addBox(-1F, -0.5F, -1F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -2.5F, -3.5F));
        PartDefinition p_paupieres = p_tete.addOrReplaceChild("paupieres", CubeListBuilder.create().texOffs(0, 22).addBox(-4F, 0F, 0F, 8F, 2F, 0F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -5F, -3.55F, 1.5708F, 0F, 0F));
        PartDefinition p_aile_droite = p_corps.addOrReplaceChild("aile_droite", CubeListBuilder.create().texOffs(0, 13).addBox(-1F, 0F, -1.5F, 1F, 4F, 5F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-3.5F, -4.5F, -1F, 0F, 0F, 0.0698F));
        PartDefinition p_aile_droite_bout = p_aile_droite.addOrReplaceChild("aile_droite_bout", CubeListBuilder.create().texOffs(12, 13).addBox(-0.5F, -2F, -1F, 1F, 3F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-0.5F, 3F, 3.5F, 0.1396F, 0F, 0F));
        PartDefinition p_aile_gauche = p_corps.addOrReplaceChild("aile_gauche", CubeListBuilder.create().texOffs(0, 13).mirror().addBox(0F, 0F, -1.5F, 1F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offsetAndRotation(3.5F, -4.5F, -1F, 0F, 0F, -0.0698F));
        PartDefinition p_aile_gauche_bout = p_aile_gauche.addOrReplaceChild("aile_gauche_bout", CubeListBuilder.create().texOffs(12, 13).mirror().addBox(-0.5F, -2F, -1F, 1F, 3F, 3F, new CubeDeformation(0F)).mirror(false), PartPose.offsetAndRotation(0.5F, 3F, 3.5F, 0.1396F, 0F, 0F));
        PartDefinition p_queue = p_corps.addOrReplaceChild("queue", CubeListBuilder.create().texOffs(20, 13).addBox(-1.5F, -1F, 0F, 3F, 2F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -1.5F, 3F, -0.1745F, 0F, 0F));
        PartDefinition p_patte_droite = p_racine.addOrReplaceChild("patte_droite", CubeListBuilder.create().texOffs(40, 13).addBox(-1F, 0F, -1.5F, 2F, 1F, 2F, new CubeDeformation(0F)), PartPose.offset(-1.5F, -1F, -1F));
        PartDefinition p_patte_gauche = p_racine.addOrReplaceChild("patte_gauche", CubeListBuilder.create().texOffs(40, 13).mirror().addBox(-1F, 0F, -1.5F, 2F, 1F, 2F, new CubeDeformation(0F)).mirror(false), PartPose.offset(1.5F, -1F, -1F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Dans l'ordre des indices de ChouetteSageAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "corps|p|0,0,0,0,c;1.5,0,0.2,0,c;3,0,0,0,c\ntete|r|0,0,0,0,c;0.6,0,0,0,c;0.9,0,-22,18,c;1.5,0,-22,18,c;1.8,0,0,0,c;2.2,-4,30,-10,c;2.6,-4,30,-10,c;3,0,0,0,c"
        }, 3F, true),
        BlockbenchAnimations.parse(new String[]{
                "paupieres|r|0,0,0,0,l;0.12,-90,0,0,l;0.2,-90,0,0,l;0.35,0,0,0,l"
        }, 0.35F, false),
        BlockbenchAnimations.parse(new String[]{
                "aile_droite|r|0,0,0,6,c;0.2,0,0,0,c;0.4,0,0,0,c;0.6,0,0,0,c;0.8,0,0,6,c\naile_gauche|r|0,0,0,0,c;0.2,0,0,0,c;0.4,0,0,-6,c;0.6,0,0,0,c;0.8,0,0,0,c\ncorps|r|0,0,0,5,c;0.2,4,0,0,c;0.4,0,0,-5,c;0.6,4,0,0,c;0.8,0,0,5,c\npatte_droite|r|0,-25,0,0,c;0.2,0,0,0,c;0.4,15,0,0,c;0.6,0,0,0,c;0.8,-25,0,0,c\npatte_gauche|r|0,15,0,0,c;0.2,0,0,0,c;0.4,-25,0,0,c;0.6,0,0,0,c;0.8,15,0,0,c\nqueue|r|0,0,0,0,c;0.2,-8,0,0,c;0.4,0,0,0,c;0.6,-8,0,0,c;0.8,0,0,0,c\nracine|p|0,0,0,0,c;0.2,0,1,0,c;0.4,0,0,0,c;0.6,0,1,0,c;0.8,0,0,0,c"
        }, 0.8F, true),
        BlockbenchAnimations.parse(new String[]{
                "aile_droite|r|0,0,0,105,c;0.3,0,0,-5,c;0.6,0,0,105,c\naile_droite_bout|r|0,-8,0,20,c;0.3,-8,0,-25,c;0.6,-8,0,20,c\naile_gauche|r|0,0,0,-105,c;0.3,0,0,5,c;0.6,0,0,-105,c\naile_gauche_bout|r|0,-8,0,-20,c;0.3,-8,0,25,c;0.6,-8,0,-20,c\ncorps|r|0,22,0,0,c;0.3,22,0,0,c;0.6,22,0,0,c\npatte_droite|r|0,50,0,0,c;0.3,50,0,0,c;0.6,50,0,0,c\npatte_gauche|r|0,50,0,0,c;0.3,50,0,0,c;0.6,50,0,0,c\nqueue|r|0,12,0,0,c;0.3,12,0,0,c;0.6,12,0,0,c\nracine|p|0,0,5,0,c;0.3,0,5.8,0,c;0.6,0,5,0,c\ntete|r|0,-16,0,0,c;0.3,-16,0,0,c;0.6,-16,0,0,c"
        }, 0.6F, true),
        BlockbenchAnimations.parse(new String[]{
                "aile_droite|r|0,0,0,0,l;0.05,0,0,4.16,l;0.125,0,0,20,l;0.2,0,0,35.84,l;0.25,0,0,40,l;0.29,0,0,46.76,l;0.35,0,0,72.5,l;0.41,0,0,98.24,l;0.45,0,0,105,l;0.65,0,0,-5,l;0.7,0,0,6.44,l;0.775,0,0,50,l;0.85,0,0,93.56,l;0.9,0,0,105,l\naile_droite_bout|r|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,-0.832,0,2.08,l;0.35,-4,0,10,l;0.41,-7.168,0,17.92,l;0.45,-8,0,20,l;0.65,-8,0,-25,l;0.7,-8,0,-20.32,l;0.775,-8,0,-2.5,l;0.85,-8,0,15.32,l;0.9,-8,0,20,l\naile_gauche|r|0,0,0,0,l;0.05,0,0,-4.16,l;0.125,0,0,-20,l;0.2,0,0,-35.84,l;0.25,0,0,-40,l;0.29,0,0,-46.76,l;0.35,0,0,-72.5,l;0.41,0,0,-98.24,l;0.45,0,0,-105,l;0.65,0,0,5,l;0.7,0,0,-6.44,l;0.775,0,0,-50,l;0.85,0,0,-93.56,l;0.9,0,0,-105,l\naile_gauche_bout|r|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,-0.832,0,-2.08,l;0.35,-4,0,-10,l;0.41,-7.168,0,-17.92,l;0.45,-8,0,-20,l;0.65,-8,0,25,l;0.7,-8,0,20.32,l;0.775,-8,0,2.5,l;0.85,-8,0,-15.32,l;0.9,-8,0,-20,l\ncorps|p|0,0,0,0,l;0.05,0,-0.052,0,l;0.125,0,-0.25,0,l;0.2,0,-0.448,0,l;0.25,0,-0.5,0,l;0.29,0,-0.448,0,l;0.35,0,-0.25,0,l;0.41,0,-0.052,0,l;0.45,0,0,0,l;0.65,0,0,0,l;0.7,0,0,0,l;0.775,0,0,0,l;0.85,0,0,0,l;0.9,0,0,0,l\ncorps|r|0,0,0,0,l;0.05,1.04,0,0,l;0.125,5,0,0,l;0.2,8.96,0,0,l;0.25,10,0,0,l;0.29,11.248,0,0,l;0.35,16,0,0,l;0.41,20.752,0,0,l;0.45,22,0,0,l;0.65,22,0,0,l;0.7,22,0,0,l;0.775,22,0,0,l;0.85,22,0,0,l;0.9,22,0,0,l\npatte_droite|r|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,5.2,0,0,l;0.35,25,0,0,l;0.41,44.8,0,0,l;0.45,50,0,0,l;0.65,50,0,0,l;0.7,50,0,0,l;0.775,50,0,0,l;0.85,50,0,0,l;0.9,50,0,0,l\npatte_gauche|r|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,5.2,0,0,l;0.35,25,0,0,l;0.41,44.8,0,0,l;0.45,50,0,0,l;0.65,50,0,0,l;0.7,50,0,0,l;0.775,50,0,0,l;0.85,50,0,0,l;0.9,50,0,0,l\nqueue|r|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,1.248,0,0,l;0.35,6,0,0,l;0.41,10.752,0,0,l;0.45,12,0,0,l;0.65,12,0,0,l;0.7,12,0,0,l;0.775,12,0,0,l;0.85,12,0,0,l;0.9,12,0,0,l\nracine|p|0,0,0,0,l;0.05,0,0,0,l;0.125,0,0,0,l;0.2,0,0,0,l;0.25,0,0,0,l;0.29,0,0.156,0,l;0.35,0,0.75,0,l;0.41,0,1.344,0,l;0.45,0,1.5,0,l;0.65,0,3.5,0,l;0.7,0,3.656,0,l;0.775,0,4.25,0,l;0.85,0,4.844,0,l;0.9,0,5,0,l\ntete|r|0,0,0,0,l;0.05,-0.624,0,0,l;0.125,-3,0,0,l;0.2,-5.376,0,0,l;0.25,-6,0,0,l;0.29,-7.04,0,0,l;0.35,-11,0,0,l;0.41,-14.96,0,0,l;0.45,-16,0,0,l;0.65,-16,0,0,l;0.7,-16,0,0,l;0.775,-16,0,0,l;0.85,-16,0,0,l;0.9,-16,0,0,l"
        }, 0.9F, false),
        BlockbenchAnimations.parse(new String[]{
                "aile_droite|r|0,0,0,105,l;0.06,0,0,102.4,l;0.15,0,0,92.5,l;0.24,0,0,82.6,l;0.3,0,0,80,l;0.35,0,0,73.76,l;0.425,0,0,50,l;0.5,0,0,26.24,l;0.55,0,0,20,l;0.6,0,0,17.92,l;0.675,0,0,10,l;0.75,0,0,2.08,l;0.8,0,0,0,l\naile_droite_bout|r|0,-8,0,20,l;0.06,-5.088,0,20,l;0.15,6,0,20,l;0.24,17.088,0,20,l;0.3,20,0,20,l;0.35,17.92,0,17.92,l;0.425,10,0,10,l;0.5,2.08,0,2.08,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\naile_gauche|r|0,0,0,-105,l;0.06,0,0,-102.4,l;0.15,0,0,-92.5,l;0.24,0,0,-82.6,l;0.3,0,0,-80,l;0.35,0,0,-73.76,l;0.425,0,0,-50,l;0.5,0,0,-26.24,l;0.55,0,0,-20,l;0.6,0,0,-17.92,l;0.675,0,0,-10,l;0.75,0,0,-2.08,l;0.8,0,0,0,l\naile_gauche_bout|r|0,-8,0,-20,l;0.06,-5.088,0,-20,l;0.15,6,0,-20,l;0.24,17.088,0,-20,l;0.3,20,0,-20,l;0.35,17.92,0,-17.92,l;0.425,10,0,-10,l;0.5,2.08,0,-2.08,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\ncorps|p|0,0,0,0,l;0.06,0,0,0,l;0.15,0,0,0,l;0.24,0,0,0,l;0.3,0,0,0,l;0.35,0,-0.0416,0,l;0.425,0,-0.2,0,l;0.5,0,-0.3584,0,l;0.55,0,-0.4,0,l;0.6,0,-0.3584,0,l;0.675,0,-0.2,0,l;0.75,0,-0.0416,0,l;0.8,0,0,0,l\ncorps|r|0,22,0,0,l;0.06,18.672,0,0,l;0.15,6,0,0,l;0.24,-6.672,0,0,l;0.3,-10,0,0,l;0.35,-8.128,0,0,l;0.425,-1,0,0,l;0.5,6.128,0,0,l;0.55,8,0,0,l;0.6,7.168,0,0,l;0.675,4,0,0,l;0.75,0.832,0,0,l;0.8,0,0,0,l\npatte_droite|r|0,50,0,0,l;0.06,41.16,0,0,l;0.15,7.5,0,0,l;0.24,-26.16,0,0,l;0.3,-35,0,0,l;0.35,-31.36,0,0,l;0.425,-17.5,0,0,l;0.5,-3.64,0,0,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\npatte_gauche|r|0,50,0,0,l;0.06,41.16,0,0,l;0.15,7.5,0,0,l;0.24,-26.16,0,0,l;0.3,-35,0,0,l;0.35,-31.36,0,0,l;0.425,-17.5,0,0,l;0.5,-3.64,0,0,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\nqueue|r|0,12,0,0,l;0.06,12.832,0,0,l;0.15,16,0,0,l;0.24,19.168,0,0,l;0.3,20,0,0,l;0.35,17.92,0,0,l;0.425,10,0,0,l;0.5,2.08,0,0,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\nracine|p|0,0,5,0,l;0.06,0,4.688,0,l;0.15,0,3.5,0,l;0.24,0,2.312,0,l;0.3,0,2,0,l;0.35,0,1.792,0,l;0.425,0,1,0,l;0.5,0,0.208,0,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l\ntete|r|0,-16,0,0,l;0.06,-13.712,0,0,l;0.15,-5,0,0,l;0.24,3.712,0,0,l;0.3,6,0,0,l;0.35,5.376,0,0,l;0.425,3,0,0,l;0.5,0.624,0,0,l;0.55,0,0,0,l;0.6,0,0,0,l;0.675,0,0,0,l;0.75,0,0,0,l;0.8,0,0,0,l"
        }, 0.8F, false),
        BlockbenchAnimations.parse(new String[]{
                "aile_droite|r|0,0,0,-3,c;1.5,0,0,-3,c;3,0,0,-3,c\naile_gauche|r|0,0,0,3,c;1.5,0,0,3,c;3,0,0,3,c\ncorps|p|0,0,0,0,c;1.5,0,0.25,0,c;3,0,0,0,c\npaupieres|r|0,-90,0,0,c;1.5,-90,0,0,c;3,-90,0,0,c\nqueue|r|0,-8,0,0,c;1.5,-8,0,0,c;3,-8,0,0,c\ntete|p|0,0,-0.3,0,c;1.5,0,-0.2,0,c;3,0,-0.3,0,c\ntete|r|0,16,0,6,c;1.5,19,0,6,c;3,16,0,6,c"
        }, 3F, true),
        BlockbenchAnimations.parse(new String[]{
                "corps|p|0,0,-0.4,0,c;1.5,0,-0.3,0,c;3,0,-0.4,0,c\ncorps|r|0,-6,0,3,c;0.8,-6,0,0,c;1.5,-6,0,-3,c;2.3,-6,0,0,c;3,-6,0,3,c\npatte_droite|r|0,-18,0,0,c;0.8,-18,0,0,c;1.5,-18,0,0,c;2.3,-18,0,0,c;3,-18,0,0,c\npatte_gauche|r|0,-18,0,0,c;0.8,-18,0,0,c;1.5,-18,0,0,c;2.3,-18,0,0,c;3,-18,0,0,c\nqueue|r|0,-28,0,0,c;0.8,-28,0,0,c;1.5,-28,0,0,c;2.3,-28,0,0,c;3,-28,0,0,c\ntete|r|0,0,0,0,c;0.8,0,-35,0,c;1.5,0,0,0,c;2.3,0,35,0,c;3,0,0,0,c"
        }, 3F, true)
        };
    }
}
