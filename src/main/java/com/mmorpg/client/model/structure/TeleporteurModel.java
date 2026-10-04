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

/** Modele et animations de Téléporteur (format Modded Entity de Blockbench, art/structures_blockbench/teleporteur). */
public final class TeleporteurModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("teleporteur"), "main");

    private TeleporteurModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(132, 0).addBox(-40F, -6F, -16F, 80F, 6F, 40F, new CubeDeformation(0F)).texOffs(328, 98).addBox(-36F, -2F, -24F, 72F, 2F, 4F, new CubeDeformation(0F)).texOffs(100, 98).addBox(-38F, -4F, -20F, 76F, 4F, 4F, new CubeDeformation(0F)).texOffs(0, 48).addBox(40F, -4F, -14F, 16F, 4F, 24F, new CubeDeformation(0F)).texOffs(372, 0).addBox(-40F, -8F, -16F, 2F, 2F, 40F, new CubeDeformation(0F)).texOffs(372, 0).addBox(38F, -8F, -16F, 2F, 2F, 40F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_arche = p_racine.addOrReplaceChild("arche", CubeListBuilder.create(), PartPose.offset(0F, -6F, 0F));
        PartDefinition p_montant_gauche = p_arche.addOrReplaceChild("montant_gauche", CubeListBuilder.create().texOffs(80, 48).addBox(-10F, -10F, -8F, 20F, 10F, 18F, new CubeDeformation(0F)).texOffs(72, 0).addBox(-8F, -44F, -6F, 16F, 34F, 14F, new CubeDeformation(0F)).texOffs(260, 76).addBox(-9F, -18F, -7F, 18F, 2F, 16F, new CubeDeformation(0F)).texOffs(260, 76).addBox(-9F, -39F, -7F, 18F, 2F, 16F, new CubeDeformation(0F)).texOffs(76, 98).addBox(-5F, -33F, -8F, 10F, 10F, 2F, new CubeDeformation(0F)).texOffs(306, 98).addBox(-3F, -31F, -9F, 6F, 6F, 1F, new CubeDeformation(0F)).texOffs(384, 48).addBox(-10F, -50F, -8F, 20F, 6F, 18F, new CubeDeformation(0F)), PartPose.offset(-27F, 0F, 0F));
        PartDefinition p_montant_droit = p_arche.addOrReplaceChild("montant_droit", CubeListBuilder.create().texOffs(80, 48).addBox(-10F, -10F, -8F, 20F, 10F, 18F, new CubeDeformation(0F)).texOffs(72, 0).addBox(-8F, -44F, -6F, 16F, 34F, 14F, new CubeDeformation(0F)).texOffs(260, 76).addBox(-9F, -18F, -7F, 18F, 2F, 16F, new CubeDeformation(0F)).texOffs(260, 76).addBox(-9F, -39F, -7F, 18F, 2F, 16F, new CubeDeformation(0F)).texOffs(76, 98).addBox(-5F, -33F, -8F, 10F, 10F, 2F, new CubeDeformation(0F)).texOffs(306, 98).addBox(-3F, -31F, -9F, 6F, 6F, 1F, new CubeDeformation(0F)).texOffs(384, 48).addBox(-10F, -50F, -8F, 20F, 6F, 18F, new CubeDeformation(0F)), PartPose.offset(27F, 0F, 0F));
        PartDefinition p_linteau = p_arche.addOrReplaceChild("linteau", CubeListBuilder.create().texOffs(0, 76).addBox(-37F, 0F, -9F, 74F, 2F, 20F, new CubeDeformation(0F)).texOffs(156, 48).addBox(-36F, -8F, -8F, 72F, 8F, 18F, new CubeDeformation(0F)).texOffs(260, 98).addBox(-28F, -7F, -8.5F, 14F, 6F, 1F, new CubeDeformation(0F)).texOffs(260, 98).addBox(14F, -7F, -8.5F, 14F, 6F, 1F, new CubeDeformation(0F)).texOffs(188, 76).addBox(-36F, -14F, -7F, 14F, 6F, 16F, new CubeDeformation(0F)).texOffs(188, 76).addBox(22F, -14F, -7F, 14F, 6F, 16F, new CubeDeformation(0F)).texOffs(452, 76).addBox(-35F, -18F, -5F, 10F, 4F, 12F, new CubeDeformation(0F)).texOffs(452, 76).addBox(25F, -18F, -5F, 10F, 4F, 12F, new CubeDeformation(0F)).texOffs(328, 76).addBox(-10F, -12F, -6F, 20F, 4F, 14F, new CubeDeformation(0F)), PartPose.offset(0F, -50F, 0F));
        PartDefinition p_cristal = p_racine.addOrReplaceChild("cristal", CubeListBuilder.create().texOffs(0, 114).addBox(-7F, 8F, -2F, 14F, 3F, 3F, new CubeDeformation(0F)).texOffs(248, 76).addBox(-9F, -7F, -2F, 3F, 16F, 3F, new CubeDeformation(0F)).texOffs(248, 76).addBox(6F, -7F, -2F, 3F, 16F, 3F, new CubeDeformation(0F)).texOffs(34, 114).addBox(-6F, -9F, -2F, 12F, 3F, 3F, new CubeDeformation(0F)).texOffs(64, 114).addBox(-2F, -12F, -2F, 4F, 3F, 3F, new CubeDeformation(0F)), PartPose.offset(0F, -66F, -10F));
        PartDefinition p_cristal_arche = p_cristal.addOrReplaceChild("cristal_arche", CubeListBuilder.create().texOffs(0, 98).addBox(-4F, -6F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(78, 114).addBox(-2F, -9F, -1F, 4F, 3F, 2F, new CubeDeformation(0F)).texOffs(78, 114).addBox(-2F, 6F, -1F, 4F, 3F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, 1F, -3F));
        PartDefinition p_portail = p_racine.addOrReplaceChild("portail", CubeListBuilder.create().texOffs(0, 0).addBox(-18F, -24F, 0F, 36F, 48F, 0F, new CubeDeformation(0F)), PartPose.offset(0F, -30F, 1F));
        PartDefinition p_pupitre = p_racine.addOrReplaceChild("pupitre", CubeListBuilder.create().texOffs(336, 48).addBox(-6F, -13F, -6F, 12F, 13F, 12F, new CubeDeformation(0F)).texOffs(396, 76).addBox(-7F, -13F, -7F, 14F, 2F, 14F, new CubeDeformation(0F)), PartPose.offset(48F, -4F, -4F));
        p_pupitre.addOrReplaceChild("pupitre_pupitre_tablette_r", CubeListBuilder.create().texOffs(24, 98).addBox(-7F, -2F, -6F, 14F, 2F, 12F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -13F, -1F, 0.4363F, 0F, 0F));
        PartDefinition p_cristal_pupitre = p_pupitre.addOrReplaceChild("cristal_pupitre", CubeListBuilder.create().texOffs(290, 98).addBox(-2F, 1F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(320, 98).addBox(-1F, -4F, -1F, 2F, 5F, 2F, new CubeDeformation(0F)).texOffs(90, 114).addBox(-0.5F, -6F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -17F, 5F));
        return LayerDefinition.create(mesh, 512, 128);
    }

    /** Dans l'ordre des indices de TeleporteurAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "cristal_arche|p|0,0,0,0,c;1.5,0,0.5,0,c;3,0,0,0,c\ncristal_pupitre|p|0,0,0,0,c;1.5,0,0.3,0,c;3,0,0,0,c\nportail|s|0,1,0.02,1,l;3,1,0.02,1,l"
        }, 3F, true),
        BlockbenchAnimations.parse(new String[]{
                "cristal_arche|p|0,0,0,0,l;0.24,0,0.104,-0.104,l;0.6,0,0.5,-0.5,l;0.96,0,0.896,-0.896,l;1.2,0,1,-1,l\ncristal_pupitre|p|0,0,0,0,l;0.24,0,0.156,0,l;0.6,0,0.75,0,l;0.96,0,1.344,0,l;1.2,0,1.5,0,l\ncristal_pupitre|r|0,0,0,0,l;0.24,0,-18.72,0,l;0.6,0,-90,0,l;0.96,0,-161.28,0,l;1.2,0,-180,0,l\nportail|s|0,1,0.02,1,l;0.06,0.9272,0.0283,1,l;0.15,0.65,0.06,1,l;0.24,0.3728,0.0917,1,l;0.3,0.3,0.1,1,l;0.48,0.3728,0.1936,1,l;0.75,0.65,0.55,1,l;1.02,0.9272,0.9064,1,l;1.2,1,1,1,l"
        }, 1.2F, false),
        BlockbenchAnimations.parse(new String[]{
                "cristal_arche|p|0,0,1,-1,c;1,0,2,-1,c;2,0,1,-1,c\ncristal_pupitre|p|0,0,1.5,0,c;1,0,2.2,0,c;2,0,1.5,0,c\ncristal_pupitre|r|0,0,-180,0,l;2,0,-540,0,l\nportail|s|0,1,1,1,c;0.5,1.02,0.99,1,c;1,1,1,1,c;1.5,0.99,1.01,1,c;2,1,1,1,c"
        }, 2F, true)
        };
    }
}
