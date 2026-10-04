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

/** Modele et animations de Feu follet (format Modded Entity de Blockbench, art/familiers_blockbench/feu_follet). */
public final class FeuFolletModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("feu_follet"), "main");

    private FeuFolletModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_noyau = p_racine.addOrReplaceChild("noyau", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -2.5F, 5F, 5F, 5F, new CubeDeformation(0F)), PartPose.offset(0F, -5F, 0F));
        PartDefinition p_visage = p_noyau.addOrReplaceChild("visage", CubeListBuilder.create().texOffs(38, 10).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 0F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, -2.6F));
        PartDefinition p_flamme_1 = p_racine.addOrReplaceChild("flamme_1", CubeListBuilder.create().texOffs(30, 0).addBox(-3F, -0.5F, -3F, 6F, 2F, 6F, new CubeDeformation(0F)).texOffs(0, 17).addBox(-1.5F, 1.5F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -3F, 0F));
        PartDefinition p_flamme_2 = p_racine.addOrReplaceChild("flamme_2", CubeListBuilder.create().texOffs(20, 0).addBox(2.5F, -3F, -2F, 1F, 5F, 4F, new CubeDeformation(0F)).texOffs(12, 10).addBox(-3.5F, -2F, -1.5F, 1F, 4F, 3F, new CubeDeformation(0F)).texOffs(0, 10).addBox(-2.5F, -4F, 2.5F, 5F, 6F, 1F, new CubeDeformation(0F)).texOffs(48, 10).addBox(3.5F, -3F, -1F, 1F, 3F, 2F, new CubeDeformation(0F)).texOffs(24, 17).addBox(-4.5F, -1F, -0.5F, 1F, 2F, 2F, new CubeDeformation(0F)).texOffs(30, 17).addBox(2.5F, -5F, -1F, 1F, 2F, 2F, new CubeDeformation(0F)).texOffs(42, 17).addBox(-3.5F, -4F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)).texOffs(36, 17).addBox(-0.5F, -6F, 2.5F, 2F, 2F, 1F, new CubeDeformation(0F)).texOffs(52, 17).addBox(-1.5F, -5F, 2.5F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -5F, 0F));
        PartDefinition p_flamme_3 = p_racine.addOrReplaceChild("flamme_3", CubeListBuilder.create().texOffs(20, 10).addBox(-2.5F, -1F, -2F, 5F, 1F, 4F, new CubeDeformation(0F)).texOffs(12, 17).addBox(-1F, -2F, -1F, 3F, 1F, 3F, new CubeDeformation(0F)).texOffs(46, 17).addBox(0.5F, -3F, -0.5F, 2F, 1F, 1F, new CubeDeformation(0F)).texOffs(56, 17).addBox(2F, -4F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -7.5F, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Dans l'ordre des indices de FeuFolletAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "flamme_1|s|0,1,1,1,c;0.5,0.95,1.1,0.95,c;1,1,1,1,c;1.5,1.08,0.92,1.08,c;2,1,1,1,c\nflamme_2|r|0,0,0,0,c;0.5,0,-6,0,c;1,0,0,0,c;1.5,0,6,0,c;2,0,0,0,c\nflamme_2|s|0,1,1,1,c;0.5,1.08,0.94,1.08,c;1,1,1,1,c;1.5,0.95,1.06,0.95,c;2,1,1,1,c\nflamme_3|r|0,0,0,-6,c;0.5,-4,0,4,c;1,0,0,-3,c;1.5,3,0,6,c;2,0,0,-6,c\nracine|p|0,0,0,0,c;1,0,0.7,0,c;2,0,0,0,c\nvisage|s|0,1,1,1,l;1.5,1,1,1,l;1.57,1,0.15,1,l;1.66,1,1,1,l;2,1,1,1,l"
        }, 2F, true),
        BlockbenchAnimations.parse(new String[]{
                "flamme_1|r|0,6,0,0,c;0.5,6,0,0,c;1,6,0,0,c\nflamme_2|r|0,-10,0,0,c;0.5,-13,0,0,c;1,-10,0,0,c\nflamme_3|r|0,-26,0,0,c;0.5,-34,0,4,c;1,-26,0,0,c\nracine|p|0,0,0,0,c;0.5,0,0.5,0,c;1,0,0,0,c\nracine|r|0,14,0,0,c;0.5,14,0,0,c;1,14,0,0,c"
        }, 1F, true),
        BlockbenchAnimations.parse(new String[]{
                "flamme_2|s|0,1,1,1,l;0.024,1,1,1,l;0.06,1,1,1,l;0.096,1,1,1,l;0.12,1,1,1,l;0.156,1.0208,1.0208,1.0208,l;0.21,1.1,1.1,1.1,l;0.264,1.1792,1.1792,1.1792,l;0.3,1.2,1.2,1.2,l;0.35,1.1792,1.1792,1.1792,l;0.425,1.1,1.1,1.1,l;0.5,1.0208,1.0208,1.0208,l;0.55,1,1,1,l;0.6,1,1,1,l;0.675,1,1,1,l;0.75,1,1,1,l;0.8,1,1,1,l;0.84,1,1,1,l;0.9,1,1,1,l;0.96,1,1,1,l;1,1,1,1,l;1.04,1,1,1,l;1.1,1,1,1,l;1.16,1,1,1,l;1.2,1,1,1,l\nflamme_3|r|0,0,0,0,l;0.02,0,0,0,l;0.05,0,0,0,l;0.08,0,0,0,l;0.1,0,0,0,l;0.55,0,0,0,l;0.6,0,0,1.248,l;0.675,0,0,6,l;0.75,0,0,10.752,l;0.8,0,0,12,l;0.84,0,0,9.92,l;0.9,0,0,2,l;0.96,0,0,-5.92,l;1,0,0,-8,l;1.04,0,0,-7.168,l;1.1,0,0,-4,l;1.16,0,0,-0.832,l;1.2,0,0,0,l\nflamme_3|s|0,1,1,1,l;0.024,1,1,1,l;0.06,1,1,1,l;0.096,1,1,1,l;0.12,1,1,1,l;0.156,1.0312,1.0312,1.0312,l;0.21,1.15,1.15,1.15,l;0.264,1.2688,1.2688,1.2688,l;0.3,1.3,1.3,1.3,l;0.35,1.2688,1.2688,1.2688,l;0.425,1.15,1.15,1.15,l;0.5,1.0312,1.0312,1.0312,l;0.55,1,1,1,l;0.6,1.0208,1.0208,1.0208,l;0.675,1.1,1.1,1.1,l;0.75,1.1792,1.1792,1.1792,l;0.8,1.2,1.2,1.2,l;0.84,1.1792,1.1792,1.1792,l;0.9,1.1,1.1,1.1,l;0.96,1.0208,1.0208,1.0208,l;1,1,1,1,l;1.04,1,1,1,l;1.1,1,1,1,l;1.16,1,1,1,l;1.2,1,1,1,l\nnoyau|s|0,1,1,1,l;0.024,1.0156,0.9844,1.0156,l;0.06,1.075,0.925,1.075,l;0.096,1.1344,0.8656,1.1344,l;0.12,1.15,0.85,1.15,l;0.156,1.124,0.8781,1.124,l;0.21,1.025,0.985,1.025,l;0.264,0.926,1.0919,0.926,l;0.3,0.9,1.12,0.9,l;0.35,0.9229,1.095,0.9229,l;0.425,1.01,1,1.01,l;0.5,1.0971,0.905,1.0971,l;0.55,1.12,0.88,1.12,l;0.6,1.1075,0.8925,1.1075,l;0.675,1.06,0.94,1.06,l;0.75,1.0125,0.9875,1.0125,l;0.8,1,1,1,l;0.84,1.0083,0.9917,1.0083,l;0.9,1.04,0.96,1.04,l;0.96,1.0717,0.9283,1.0717,l;1,1.08,0.92,1.08,l;1.04,1.0717,0.9283,1.0717,l;1.1,1.04,0.96,1.04,l;1.16,1.0083,0.9917,1.0083,l;1.2,1,1,1,l\nracine|p|0,0,0,0,l;0.06,0,0.312,0,l;0.15,0,1.5,0,l;0.24,0,2.688,0,l;0.3,0,3,0,l;0.35,0,2.688,0,l;0.425,0,1.5,0,l;0.5,0,0.312,0,l;0.55,0,0,0,l;0.6,0,0.156,0,l;0.675,0,0.75,0,l;0.75,0,1.344,0,l;0.8,0,1.5,0,l;0.84,0,1.344,0,l;0.9,0,0.75,0,l;0.96,0,0.156,0,l;1,0,0,0,l;1.04,0,0,0,l;1.1,0,0,0,l;1.16,0,0,0,l;1.2,0,0,0,l\nracine|r|0,0,0,0,l;0.02,0,0,0,l;0.05,0,0,0,l;0.08,0,0,0,l;0.1,0,0,0,l;0.55,0,-360,0,l;0.6,0,-360,0,l;0.675,0,-360,0,l;0.75,0,-360,0,l;0.8,0,-360,0,l;0.84,0,-360,0,l;0.9,0,-360,0,l;0.96,0,-360,0,l;1,0,-360,0,l;1.04,0,-360,0,l;1.1,0,-360,0,l;1.16,0,-360,0,l;1.2,0,-360,0,l\nvisage|s|0,1,1,1,l;0.1,1,0.4,1,l;1,1,0.4,1,l;1.2,1,1,1,l"
        }, 1.2F, false),
        BlockbenchAnimations.parse(new String[]{
                "flamme_1|p|0,0,0.8,0,c;0.8,0,0.8,0,c;1.6,0,0.8,0,c\nflamme_1|r|0,0,-15,0,c;0.8,0,15,0,c;1.6,0,-15,0,c\nflamme_1|s|0,0.92,1,0.92,c;0.4,0.92,1,0.92,c;0.8,0.92,1,0.92,c;1.2,0.92,1,0.92,c;1.6,0.92,1,0.92,c\nflamme_2|r|0,0,20,0,c;0.8,0,-20,0,c;1.6,0,20,0,c\nflamme_2|s|0,0.9,1.05,0.9,c;0.4,0.9,1.05,0.9,c;0.8,0.9,1.05,0.9,c;1.2,0.9,1.05,0.9,c;1.6,0.9,1.05,0.9,c\nflamme_3|p|0,0,-1,0,c;0.8,0,-1.2,0,c;1.6,0,-1,0,c\nflamme_3|r|0,0,0,-4,c;0.4,0,0,4,c;0.8,0,0,-4,c;1.2,0,0,4,c;1.6,0,0,-4,c\nflamme_3|s|0,0.9,0.9,0.9,c;0.4,0.9,0.9,0.9,c;0.8,0.9,0.9,0.9,c;1.2,0.9,0.9,0.9,c;1.6,0.9,0.9,0.9,c\nnoyau|s|0,1,1,1,c;0.4,1.07,1.07,1.07,c;0.8,1,1,1,c;1.2,1.07,1.07,1.07,c;1.6,1,1,1,c\nracine|p|0,0,0.5,0,c;0.8,0,0.8,0,c;1.6,0,0.5,0,c"
        }, 1.6F, true)
        };
    }
}
