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

/** Modele et animations de Maître Guerrier (format Modded Entity de Blockbench, art/classes_blockbench/maitre_guerrier). */
public final class MaitreGuerrierModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("maitre_guerrier"), "main");

    private MaitreGuerrierModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 21).addBox(-4F, -12F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(0, 37).addBox(-4.5F, -10F, -2.6F, 9F, 9F, 5F, new CubeDeformation(0F)).texOffs(18, 61).addBox(-4.4F, -2F, -2.7F, 9F, 2F, 5F, new CubeDeformation(0F)).texOffs(0, 75).addBox(-1F, -3F, -3.1F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(26, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)).texOffs(0, 51).addBox(-4F, -8F, -4F, 8F, 2F, 8F, new CubeDeformation(0.45F)).texOffs(48, 37).addBox(-4.6F, -6F, -3F, 1F, 6F, 6F, new CubeDeformation(0F)).texOffs(48, 37).mirror().addBox(3.6F, -6F, -3F, 1F, 6F, 6F, new CubeDeformation(0F)).mirror(false).texOffs(46, 61).addBox(-0.5F, -7F, -4.7F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(24, 21).mirror().addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(0, 70).addBox(-3F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(32, 51).mirror().addBox(-3.2F, -2F, -3F, 5F, 3F, 6F, new CubeDeformation(0F)).mirror(false).texOffs(40, 70).addBox(-1.2F, -5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, -10F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(24, 21).addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).texOffs(14, 70).addBox(-1F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)).texOffs(32, 51).addBox(-1.8F, -2F, -3F, 5F, 3F, 6F, new CubeDeformation(0F)).texOffs(40, 70).addBox(0.2F, -5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, -10F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(32, 37).mirror().addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(0, 61).mirror().addBox(-1.8F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-2F, -12F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(32, 37).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).texOffs(0, 61).addBox(-2.2F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)), PartPose.offset(2F, -12F, 0F));
        PartDefinition p_corne_d = p_tete.addOrReplaceChild("corne_d", CubeListBuilder.create().texOffs(50, 61).addBox(0F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4F, -6F, 0F, 0F, 0F, -0.6109F));
        PartDefinition p_corne_g = p_tete.addOrReplaceChild("corne_g", CubeListBuilder.create().texOffs(50, 61).addBox(-1F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(4F, -6F, 0F, 0F, 0F, 0.6109F));
        PartDefinition p_cape = p_corps.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, 0F, 8F, 20F, 1F, new CubeDeformation(0F)).texOffs(10, 75).addBox(-4F, 19F, 0.05F, 8F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -11F, 2F));
        PartDefinition p_arme = p_corps.addOrReplaceChild("arme", CubeListBuilder.create().texOffs(32, 70).addBox(-1F, 0F, -0.5F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(28, 70).addBox(-0.5F, -4F, 0F, 1F, 4F, 1F, new CubeDeformation(0F)).texOffs(44, 70).addBox(-3F, -5F, -0.5F, 6F, 1F, 2F, new CubeDeformation(0F)).texOffs(18, 0).addBox(-1.5F, -23F, 0F, 3F, 18F, 1F, new CubeDeformation(0F)).texOffs(6, 75).addBox(-0.5F, -25F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)).texOffs(28, 37).addBox(-1F, -21F, 0.45F, 2F, 14F, 0F, new CubeDeformation(0F)), PartPose.offset(-6F, -1F, -3F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de MaitreGuerrierAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "arme|r|0,0,0,0,c;1.8,0,0,1.2,c;3.6,0,0,0,c\nbras_droit|r|0,0,0,0,c;0.9,-2,0,1.5,c;1.8,0,0,0,c;2.7,2,0,1.5,c;3.6,0,0,0,c\nbras_gauche|r|0,0,0,0,c;0.9,2,0,-1.5,c;1.8,0,0,0,c;2.7,-2,0,-1.5,c;3.6,0,0,0,c\ncape|r|0,0,0,0,c;0.9,-3,0,1.5,c;1.8,0,0,0,c;2.7,-3,0,-1.5,c;3.6,0,0,0,c\ncorps|r|0,0,0,0,c;0.9,0.8,0,0.6,c;1.8,0,0,0,c;2.7,0.8,0,-0.6,c;3.6,0,0,0,c\ncorps|s|0,1,1,1,c;0.9,1,1.012,1,c;1.8,1,1,1,c;2.7,1,1.012,1,c;3.6,1,1,1,c\ntete|r|0,0,0,0,c;0.9,-0.8,-8,0,c;1.8,0,0,0,c;2.7,-0.8,8,0,c;3.6,0,0,0,c"
        }, 3.6F, true),
        BlockbenchAnimations.parse(new String[]{
                "arme|r|0,0,0,0,l;0.1,1.872,0,0.416,l;0.25,9,0,2,l;0.4,16.128,0,3.584,l;0.5,18,0,4,l;0.64,18.832,0,4.208,l;0.85,22,0,5,l;1.06,25.168,0,5.792,l;1.2,26,0,6,l;1.44,23.296,0,5.376,l;1.8,13,0,3,l;2.16,2.704,0,0.624,l;2.4,0,0,0,l\narme|s|0,1,1,1,c;0.5,1.06,1.06,1.06,c;1.2,1.1,1.1,1.1,c;2.4,1,1,1,c\nbras_droit|r|0,0,0,0,l;0.1,3.12,0,-1.04,l;0.25,15,0,-5,l;0.4,26.88,0,-8.96,l;0.5,30,0,-10,l;0.64,30,0,-10,l;0.85,30,0,-10,l;1.06,30,0,-10,l;1.2,30,0,-10,l;1.44,26.88,0,-8.96,l;1.8,15,0,-5,l;2.16,3.12,0,-1.04,l;2.4,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.1,-2.6,0,1.248,l;0.25,-12.5,0,6,l;0.4,-22.4,0,10.752,l;0.5,-25,0,12,l;0.64,-25,0,12,l;0.85,-25,0,12,l;1.06,-25,0,12,l;1.2,-25,0,12,l;1.44,-22.4,0,10.752,l;1.8,-12.5,0,6,l;2.16,-2.6,0,1.248,l;2.4,0,0,0,l\ncorps|r|0,0,0,0,l;0.1,0.416,0,0,l;0.25,2,0,0,l;0.4,3.584,0,0,l;0.5,4,0,0,l;0.64,4.208,0,0,l;0.85,5,0,0,l;1.06,5.792,0,0,l;1.2,6,0,0,l;1.44,5.376,0,0,l;1.8,3,0,0,l;2.16,0.624,0,0,l;2.4,0,0,0,l\ntete|r|0,0,0,0,l;0.1,1.04,0,0,l;0.25,5,0,0,l;0.4,8.96,0,0,l;0.5,10,0,0,l;0.64,10.416,0,0,l;0.85,12,0,0,l;1.06,13.584,0,0,l;1.2,14,0,0,l;1.44,12.544,0,0,l;1.8,7,0,0,l;2.16,1.456,0,0,l;2.4,0,0,0,l"
        }, 2.4F, false)
        };
    }
}
