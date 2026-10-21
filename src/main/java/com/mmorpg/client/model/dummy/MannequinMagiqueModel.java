// Genere automatiquement par art/mannequin_blockbench/generer_mannequin.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.dummy;

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

/** Modele et animations de Mannequin d'entraînement (format Modded Entity de Blockbench, art/mannequin_blockbench/mannequin_entrainement_magique). */
public final class MannequinMagiqueModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("mannequin_entrainement_magique"), "main");

    private MannequinMagiqueModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_socle = p_racine.addOrReplaceChild("socle", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -2F, -8F, 16F, 2F, 16F, new CubeDeformation(0F)).texOffs(0, 49).addBox(-6F, -3F, -6F, 12F, 1F, 12F, new CubeDeformation(0F)).texOffs(42, 70).addBox(4F, -5F, -8F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(0, 78).addBox(-8F, -5F, -8F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(16, 78).addBox(4F, -5F, 4F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(32, 78).addBox(-8F, -5F, 4F, 4F, 3F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_mannequin = p_racine.addOrReplaceChild("mannequin", CubeListBuilder.create().texOffs(0, 18).addBox(-1.5F, -14F, -1.5F, 3F, 14F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -3F, 0F));
        PartDefinition p_corps = p_mannequin.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(12, 18).addBox(-4F, -6F, -2.5F, 8F, 10F, 5F, new CubeDeformation(0F)).texOffs(12, 70).addBox(-4.5F, 0F, -3F, 9F, 1F, 6F, new CubeDeformation(0F)).texOffs(16, 85).addBox(-3.5F, 4F, -2F, 7F, 1F, 4F, new CubeDeformation(0F)).texOffs(48, 78).addBox(-4F, -4F, -2.55F, 8F, 7F, 0F, new CubeDeformation(0F)).texOffs(0, 62).addBox(-4.5F, -6F, -3F, 9F, 2F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -14F, 0F));
        PartDefinition p_bras = p_mannequin.addOrReplaceChild("bras", CubeListBuilder.create().texOffs(0, 91).addBox(-10F, -1F, -1F, 20F, 2F, 2F, new CubeDeformation(0F)).texOffs(30, 62).addBox(10F, -2F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0F)).texOffs(0, 95).addBox(9F, 1F, -1.5F, 2F, 1F, 3F, new CubeDeformation(0F)).texOffs(42, 62).addBox(-13F, -2F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0F)).texOffs(10, 95).addBox(-11F, 1F, -1.5F, 2F, 1F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -18F, 0F));
        PartDefinition p_tete = p_mannequin.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 35).addBox(-3.5F, -7F, -3.5F, 7F, 7F, 7F, new CubeDeformation(0F)).texOffs(0, 85).addBox(-2F, -9F, -2F, 4F, 2F, 4F, new CubeDeformation(0F)).texOffs(38, 85).addBox(-4.5F, -5F, -1F, 1F, 3F, 2F, new CubeDeformation(0F)).texOffs(44, 85).addBox(3.5F, -5F, -1F, 1F, 3F, 2F, new CubeDeformation(0F)).texOffs(44, 91).addBox(-1.5F, 0F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, 0F));
        PartDefinition p_cristal = p_mannequin.addOrReplaceChild("cristal", CubeListBuilder.create(), PartPose.offset(0F, -32F, 0F));
        p_cristal.addOrReplaceChild("cristal_cristal_corps_r", CubeListBuilder.create().texOffs(0, 70).addBox(-1.5F, -3F, -1.5F, 3F, 5F, 3F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, -0.7854F, 0F));
        p_cristal.addOrReplaceChild("cristal_cristal_pointe_r", CubeListBuilder.create().texOffs(20, 95).addBox(-0.5F, -5F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, -0.7854F, 0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de MannequinMagiqueAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "bras|r|0,0,0,0,c;0.75,0,0,0.6,c;1.5,0,0,0,c;2.25,0,0,-0.6,c;3,0,0,0,c\ncristal|p|0,0,0,0,c;0.75,0,0.8,0,c;1.5,0,0,0,c;2.25,0,-0.8,0,c;3,0,0,0,c\ncristal|r|0,0,0,0,l;1,0,-120,0,l;2,0,-240,0,l;3,0,-360,0,l\nmannequin|r|0,0,0,0,c;0.75,-0.5,0,0.7,c;1.5,0,0,0,c;2.25,0.5,0,-0.7,c;3,0,0,0,c\ntete|r|0,0,0,0,c;0.75,-0.8,-1.5,0,c;1.5,0,0,0,c;2.25,0.8,1.5,0,c;3,0,0,0,c"
        }, 3F, true),
        BlockbenchAnimations.parse(new String[]{
                "bras|r|0,0,0,0,c;0.08,0,0,4.5,c;0.2,0,0,-2.75,c;0.32,0,0,1.6,c;0.46,0,0,-0.9,c;0.62,0,0,0.45,c;0.8,0,0,-0.15,c;1,0,0,0,c\nmannequin|r|0,0,0,0,c;0.08,-9,0,0,c;0.2,5.5,0,0,c;0.32,-3.2,0,0,c;0.46,1.8,0,0,c;0.62,-0.9,0,0,c;0.8,0.3,0,0,c;1,0,0,0,c\ntete|r|0,0,0,0,c;0.08,5.4,0,0,c;0.2,-3.3,0,0,c;0.32,1.92,0,0,c;0.46,-1.08,0,0,c;0.62,0.54,0,0,c;0.8,-0.18,0,0,c;1,0,0,0,c"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "corps|s|0,1,1,1,c;0.3,0.96,1.05,0.96,c;0.6,1.03,0.97,1.03,c;0.9,0.99,1.01,0.99,c;1.2,1,1,1,c\nmannequin|r|0,0,0,0,c;0.5,0,-200,0,c;1,0,-360,0,c;1.2,0,-360,0,c"
        }, 1.2F, false)
        };
    }
}
