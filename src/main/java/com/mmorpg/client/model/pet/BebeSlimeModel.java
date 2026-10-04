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

/** Modele et animations de Bebe slime (format Modded Entity de Blockbench, art/familiers_blockbench/bebe_slime). */
public final class BebeSlimeModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("bebe_slime"), "main");

    private BebeSlimeModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_enveloppe = p_racine.addOrReplaceChild("enveloppe", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_noyau = p_racine.addOrReplaceChild("noyau", CubeListBuilder.create().texOffs(32, 0).addBox(-3F, -6F, -3F, 6F, 6F, 6F, new CubeDeformation(0F)), PartPose.offset(0F, -0.5F, 0F));
        PartDefinition p_visage = p_noyau.addOrReplaceChild("visage", CubeListBuilder.create().texOffs(0, 16).addBox(-3F, -3F, 0F, 6F, 6F, 0F, new CubeDeformation(0F)), PartPose.offset(0F, -3F, -3.1F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Dans l'ordre des indices de BebeSlimeAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1,1,1,c;0.5,1.04,0.95,1.04,c;1,0.97,1.04,0.97,c;1.5,1.03,0.97,1.03,c;2,1,1,1,c\nnoyau|s|0,1,1,1,c;0.5,0.99,1.02,0.99,c;1,1.03,0.96,1.03,c;1.5,0.98,1.03,0.98,c;2,1,1,1,c"
        }, 2F, true),
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1,1,1,l;0.06,1.0229,0.9688,1.0229,l;0.15,1.11,0.85,1.11,l;0.24,1.1971,0.7312,1.1971,l;0.3,1.22,0.7,1.22,l;0.32,1.2179,0.7021,1.2179,l;0.35,1.21,0.71,1.21,l;0.38,1.2021,0.7179,1.2021,l;0.4,1.2,0.72,1.2,l\nnoyau|s|0,1,1,1,l;0.06,1.0166,0.975,1.0166,l;0.15,1.08,0.88,1.08,l;0.24,1.1434,0.785,1.1434,l;0.3,1.16,0.76,1.16,l;0.32,1.159,0.761,1.159,l;0.35,1.155,0.765,1.155,l;0.38,1.151,0.769,1.151,l;0.4,1.15,0.77,1.15,l\nvisage|p|0,0,0,0,l;0.06,0,-0.0624,0,l;0.15,0,-0.3,0,l;0.24,0,-0.5376,0,l;0.3,0,-0.6,0,l;0.32,0,-0.6,0,l;0.35,0,-0.6,0,l;0.38,0,-0.6,0,l;0.4,0,-0.6,0,l"
        }, 0.4F, false),
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1.2,0.72,1.2,l;0.1,0.84,1.26,0.84,l;0.144,0.8525,1.2392,0.8525,l;0.21,0.9,1.16,0.9,l;0.276,0.9475,1.0808,0.9475,l;0.32,0.96,1.06,0.96,l;0.376,0.9538,1.0683,0.9538,l;0.46,0.93,1.1,0.93,l;0.544,0.9062,1.1317,0.9062,l;0.6,0.9,1.14,0.9,l;0.62,0.9021,1.1358,0.9021,l;0.65,0.91,1.12,0.91,l;0.68,0.9179,1.1042,0.9179,l;0.7,0.92,1.1,0.92,l\nnoyau|s|0,1.15,0.77,1.15,l;0.1,0.9,1.15,0.9,l;0.144,0.9042,1.1448,0.9042,l;0.21,0.92,1.125,0.92,l;0.276,0.9358,1.1052,0.9358,l;0.32,0.94,1.1,0.94,l;0.376,0.941,1.0979,0.941,l;0.46,0.945,1.09,0.945,l;0.544,0.949,1.0821,0.949,l;0.6,0.95,1.08,0.95,l;0.62,0.95,1.0779,0.95,l;0.65,0.95,1.07,0.95,l;0.68,0.95,1.0621,0.95,l;0.7,0.95,1.06,0.95,l\nracine|p|0,0,0,0,l;0.1,0,1.2,0,l;0.144,0,1.5952,0,l;0.21,0,3.1,0,l;0.276,0,4.6048,0,l;0.32,0,5,0,l;0.376,0,4.584,0,l;0.46,0,3,0,l;0.544,0,1.416,0,l;0.6,0,1,0,l;0.62,0,0.896,0,l;0.65,0,0.5,0,l;0.68,0,0.104,0,l;0.7,0,0,0,l"
        }, 0.7F, false),
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1.3,0.6,1.3,l;0.03,1.2584,0.6572,1.2584,l;0.075,1.1,0.875,1.1,l;0.12,0.9416,1.0928,0.9416,l;0.15,0.9,1.15,0.9,l;0.18,0.9177,1.1271,0.9177,l;0.225,0.985,1.04,0.985,l;0.27,1.0523,0.9529,1.0523,l;0.3,1.07,0.93,1.07,l;0.33,1.0606,0.9394,1.0606,l;0.375,1.025,0.975,1.025,l;0.42,0.9894,1.0106,0.9894,l;0.45,0.98,1.02,0.98,l;0.48,0.9821,1.0179,0.9821,l;0.525,0.99,1.01,0.99,l;0.57,0.9979,1.0021,0.9979,l;0.6,1,1,1,l\nnoyau|s|0,1.2,0.7,1.2,l;0.03,1.1896,0.7156,1.1896,l;0.075,1.15,0.775,1.15,l;0.12,1.1104,0.8344,1.1104,l;0.15,1.1,0.85,1.1,l;0.18,1.0834,0.8739,1.0834,l;0.225,1.02,0.965,1.02,l;0.27,0.9566,1.0561,0.9566,l;0.3,0.94,1.08,0.94,l;0.33,0.9494,1.0686,0.9494,l;0.375,0.985,1.025,0.985,l;0.42,1.0206,0.9814,1.0206,l;0.45,1.03,0.97,1.03,l;0.48,1.0269,0.9731,1.0269,l;0.525,1.015,0.985,1.015,l;0.57,1.0031,0.9969,1.0031,l;0.6,1,1,1,l"
        }, 0.6F, false),
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1,1,1,l;0.024,1.0187,0.974,1.0187,l;0.06,1.09,0.875,1.09,l;0.096,1.1613,0.776,1.1613,l;0.12,1.18,0.75,1.18,l;0.22,0.88,1.18,0.88,l;0.252,0.8904,1.1654,0.8904,l;0.3,0.93,1.11,0.93,l;0.348,0.9696,1.0546,0.9696,l;0.38,0.98,1.04,0.98,l;0.412,1.0081,1.0026,1.0081,l;0.46,1.115,0.86,1.115,l;0.508,1.2219,0.7174,1.2219,l;0.54,1.25,0.68,1.25,l;0.64,0.9,1.15,0.9,l;0.668,0.9083,1.1375,0.9083,l;0.71,0.94,1.09,0.94,l;0.752,0.9717,1.0425,0.9717,l;0.78,0.98,1.03,0.98,l;0.812,1.005,0.9978,1.005,l;0.86,1.1,0.875,1.1,l;0.908,1.195,0.7522,1.195,l;0.94,1.22,0.72,1.22,l;0.972,1.193,0.7543,1.193,l;1.02,1.09,0.885,1.09,l;1.068,0.987,1.0157,0.987,l;1.1,0.96,1.05,0.96,l;1.16,0.9642,1.0448,0.9642,l;1.25,0.98,1.025,0.98,l;1.34,0.9958,1.0052,0.9958,l;1.4,1,1,1,l\nnoyau|s|0,1,1,1,l;0.024,1.0125,0.9792,1.0125,l;0.06,1.06,0.9,1.06,l;0.096,1.1075,0.8208,1.1075,l;0.12,1.12,0.8,1.12,l;0.22,1,1,1,l;0.252,1,1,1,l;0.3,1,1,1,l;0.348,1,1,1,l;0.38,1,1,1,l;0.412,1.0156,0.9771,1.0156,l;0.46,1.075,0.89,1.075,l;0.508,1.1344,0.8029,1.1344,l;0.54,1.15,0.78,1.15,l;0.64,1,1,1,l;0.668,1,1,1,l;0.71,1,1,1,l;0.752,1,1,1,l;0.78,1,1,1,l;0.812,1.0125,0.9792,1.0125,l;0.86,1.06,0.9,1.06,l;0.908,1.1075,0.8208,1.1075,l;0.94,1.12,0.8,1.12,l;0.972,1.1075,0.8208,1.1075,l;1.02,1.06,0.9,1.06,l;1.068,1.0125,0.9792,1.0125,l;1.1,1,1,1,l;1.16,1,1,1,l;1.25,1,1,1,l;1.34,1,1,1,l;1.4,1,1,1,l\nracine|p|0,0,0,0,l;0.22,0,1,0,l;0.252,0,1.2288,0,l;0.3,0,2.1,0,l;0.348,0,2.9712,0,l;0.38,0,3.2,0,l;0.412,0,2.8672,0,l;0.46,0,1.6,0,l;0.508,0,0.3328,0,l;0.54,0,0,0,l;0.64,0,0.8,0,l;0.668,0,0.9456,0,l;0.71,0,1.5,0,l;0.752,0,2.0544,0,l;0.78,0,2.2,0,l;0.812,0,1.9712,0,l;0.86,0,1.1,0,l;0.908,0,0.2288,0,l;0.94,0,0,0,l;1.032,0,0,0,l;1.17,0,0,0,l;1.308,0,0,0,l;1.4,0,0,0,l\nvisage|s|0,1,1,1,c;0.12,1,0.5,1,c;1.1,1,0.5,1,c;1.4,1,1,1,c"
        }, 1.4F, false),
        BlockbenchAnimations.parse(new String[]{
                "enveloppe|s|0,1.18,0.72,1.18,c;1.2,1.15,0.77,1.15,c;2.4,1.18,0.72,1.18,c\nnoyau|s|0,1.12,0.76,1.12,c;1.2,1.1,0.8,1.1,c;2.4,1.12,0.76,1.12,c\nvisage|s|0,1,0.8,1,c;1.2,1,0.85,1,c;2.4,1,0.8,1,c"
        }, 2.4F, true)
        };
    }
}
