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

/** Modele et animations de Boîte aux lettres d'Aeloria (format Modded Entity de Blockbench, art/structures_blockbench/boite_aux_lettres). */
public final class BoiteAuxLettresModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("boite_aux_lettres"), "main");

    private BoiteAuxLettresModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_socle = p_racine.addOrReplaceChild("socle", CubeListBuilder.create().texOffs(0, 28).addBox(-7F, -3F, -7F, 14F, 3F, 14F, new CubeDeformation(0F)).texOffs(0, 60).addBox(-5F, -5F, -5F, 10F, 2F, 10F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_poteau = p_racine.addOrReplaceChild("poteau", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -22F, -3F, 6F, 22F, 6F, new CubeDeformation(0F)).texOffs(80, 60).addBox(-4F, -5F, -4F, 8F, 2F, 8F, new CubeDeformation(0F)).texOffs(80, 60).addBox(-4F, -19F, -4F, 8F, 2F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, -5F, 0F));
        PartDefinition p_boite = p_racine.addOrReplaceChild("boite", CubeListBuilder.create().texOffs(24, 0).addBox(-8F, -10F, -6F, 16F, 10F, 12F, new CubeDeformation(0F)).texOffs(0, 45).addBox(-8F, 0F, -7F, 16F, 1F, 14F, new CubeDeformation(0F)).texOffs(56, 28).addBox(-8F, -12F, -7F, 16F, 2F, 14F, new CubeDeformation(0F)).texOffs(60, 45).addBox(-7F, -14F, -6F, 14F, 2F, 12F, new CubeDeformation(0F)).texOffs(40, 60).addBox(-5F, -16F, -5F, 10F, 2F, 10F, new CubeDeformation(0F)).texOffs(72, 72).addBox(-6F, -8F, -7F, 12F, 2F, 1F, new CubeDeformation(0F)).texOffs(38, 72).addBox(-3F, -5F, -7F, 6F, 3F, 1F, new CubeDeformation(0F)).texOffs(0, 72).addBox(-7F, -8F, -7F, 2F, 7F, 1F, new CubeDeformation(0F)).texOffs(0, 72).addBox(5F, -8F, -7F, 2F, 7F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -27F, 0F));
        PartDefinition p_battant = p_racine.addOrReplaceChild("battant", CubeListBuilder.create().texOffs(104, 72).addBox(-5F, -1F, -1F, 10F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -27F, -7F));
        PartDefinition p_lettres = p_racine.addOrReplaceChild("lettres", CubeListBuilder.create(), PartPose.offset(0F, -34F, -5F));
        p_lettres.addOrReplaceChild("lettres_lettre_1_r", CubeListBuilder.create().texOffs(52, 72).addBox(-2F, -2F, 0F, 5F, 3F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -1F, -1F, 0F, 0F, 0.1396F));
        p_lettres.addOrReplaceChild("lettres_lettre_2_r", CubeListBuilder.create().texOffs(52, 72).addBox(-3F, -2F, 0F, 5F, 3F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2F, -1F, 0F, 0F, 0F, -0.1047F));
        PartDefinition p_drapeau = p_racine.addOrReplaceChild("drapeau", CubeListBuilder.create().texOffs(112, 45).addBox(-1F, -10F, -1F, 1F, 12F, 2F, new CubeDeformation(0F)).texOffs(6, 72).addBox(-8F, -9F, -1F, 7F, 4F, 1F, new CubeDeformation(0F)).texOffs(98, 72).addBox(-10F, -8F, -1F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(-9F, -31F, 0F));
        PartDefinition p_cristal = p_racine.addOrReplaceChild("cristal", CubeListBuilder.create().texOffs(22, 72).addBox(-2F, 0F, -2F, 4F, 1F, 4F, new CubeDeformation(0F)).texOffs(112, 60).addBox(-1.5F, -6F, -1.5F, 3F, 6F, 3F, new CubeDeformation(0F)).texOffs(64, 72).addBox(-1F, -8F, -1F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -44F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Dans l'ordre des indices de BoiteAuxLettresAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "cristal|p|0,0,0,0,c;2,0,0.5,0,c;4,0,0,0,c\ndrapeau|r|0,0,0,-80,l;2,0,0,-78,l;4,0,0,-80,l"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "battant|r|0,0,0,0,l;0.5,60,0,0,l;1,60,0,0,l\ndrapeau|r|0,0,0,-80,l;0.6,0,0,10,l;1,0,0,0,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "cristal|p|0,0,1,0,c;1,0,1.8,0,c;2,0,1,0,c\ncristal|r|0,0,0,0,l;2,0,-360,0,l\ndrapeau|r|0,0,0,0,l;1,0,0,12,l;2,0,0,0,l"
        }, 2F, true)
        };
    }
}
