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

/** Modele et animations de Maître Mage (format Modded Entity de Blockbench, art/classes_blockbench/maitre_mage). */
public final class MaitreMageModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("maitre_mage"), "main");

    private MaitreMageModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_corps = p_racine.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(32, 37).addBox(-4F, -12F, -2F, 8F, 12F, 4F, new CubeDeformation(0F)).texOffs(32, 100).addBox(-4.5F, -2F, -2.5F, 9F, 2F, 5F, new CubeDeformation(0F)).texOffs(4, 0).addBox(-5F, 0F, -3F, 10F, 10F, 6F, new CubeDeformation(0F)).texOffs(0, 100).addBox(-4.8F, 10F, -3.2F, 10F, 1F, 6F, new CubeDeformation(0F)).texOffs(0, 107).addBox(-4.5F, -12F, -2.5F, 9F, 2F, 5F, new CubeDeformation(0F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_tete = p_corps.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 37).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0F)).texOffs(0, 114).addBox(-3F, -1F, -4.6F, 6F, 5F, 1F, new CubeDeformation(0F)).texOffs(12, 120).addBox(-2F, 4F, -4.6F, 4F, 3F, 1F, new CubeDeformation(0F)).texOffs(44, 107).addBox(-4F, -9F, 3.6F, 8F, 5F, 1F, new CubeDeformation(0F)).texOffs(16, 53).addBox(-6F, -8.5F, -6F, 12F, 1F, 12F, new CubeDeformation(0F)).texOffs(16, 69).addBox(-4F, -11.5F, -4F, 8F, 3F, 8F, new CubeDeformation(0F)).texOffs(0, 82).addBox(-3.8F, -9.2F, -4.2F, 8F, 1F, 8F, new CubeDeformation(0.2F)), PartPose.offset(0F, -12F, 0F));
        PartDefinition p_bras_droit = p_corps.addOrReplaceChild("bras_droit", CubeListBuilder.create().texOffs(0, 53).mirror().addBox(-3F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(22, 114).addBox(-3F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)), PartPose.offset(-5F, -10F, 0F));
        PartDefinition p_bras_gauche = p_corps.addOrReplaceChild("bras_gauche", CubeListBuilder.create().texOffs(0, 53).addBox(-1F, -2F, -2F, 4F, 12F, 4F, new CubeDeformation(0F)).texOffs(36, 114).addBox(-1F, 9F, -1.5F, 4F, 2F, 3F, new CubeDeformation(-0.05F)), PartPose.offset(5F, -10F, 0F));
        PartDefinition p_jambe_droite = p_racine.addOrReplaceChild("jambe_droite", CubeListBuilder.create().texOffs(0, 69).mirror().addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).mirror(false).texOffs(0, 91).mirror().addBox(-1.8F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)).mirror(false), PartPose.offset(-2F, -12F, 0F));
        PartDefinition p_jambe_gauche = p_racine.addOrReplaceChild("jambe_gauche", CubeListBuilder.create().texOffs(0, 69).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0F)).texOffs(0, 91).addBox(-2.2F, 8F, -2.6F, 4F, 4F, 5F, new CubeDeformation(0F)), PartPose.offset(2F, -12F, 0F));
        PartDefinition p_chapeau_haut = p_tete.addOrReplaceChild("chapeau_haut", CubeListBuilder.create().texOffs(32, 82).addBox(-3F, -3.5F, -3F, 6F, 3F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -11F, 0F, -0.1396F, 0F, 0.1047F));
        PartDefinition p_chapeau_pointe = p_chapeau_haut.addOrReplaceChild("chapeau_pointe", CubeListBuilder.create().texOffs(28, 107).addBox(-2F, -3.5F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(50, 114).addBox(-1F, -6.5F, -1F, 2F, 3F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -3F, 0F, -0.2094F, 0F, 0.1396F));
        PartDefinition p_arme = p_bras_droit.addOrReplaceChild("arme", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -26F, -0.5F, 1F, 36F, 1F, new CubeDeformation(0F)).texOffs(0, 120).addBox(-1.5F, -27F, -1.5F, 3F, 1F, 3F, new CubeDeformation(0F)).texOffs(30, 120).addBox(0.5F, -30F, -1.5F, 1F, 3F, 1F, new CubeDeformation(0F)).texOffs(34, 120).addBox(-1.5F, -30F, -1.5F, 1F, 3F, 1F, new CubeDeformation(0F)).texOffs(38, 120).addBox(0.5F, -30F, 0.5F, 1F, 3F, 1F, new CubeDeformation(0F)).texOffs(42, 120).addBox(-1.5F, -30F, 0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(-1F, 10F, -1F));
        PartDefinition p_effet = p_arme.addOrReplaceChild("effet", CubeListBuilder.create().texOffs(32, 91).addBox(-2F, -2.5F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)).texOffs(22, 120).addBox(-1F, -1.5F, -1F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -29F, 0F));
        PartDefinition p_grimoire = p_corps.addOrReplaceChild("grimoire", CubeListBuilder.create().texOffs(18, 91).addBox(-3F, -6F, -1F, 6F, 8F, 1F, new CubeDeformation(0F)).texOffs(48, 91).addBox(-2.5F, -5.5F, 0F, 5F, 7F, 1F, new CubeDeformation(0F)).texOffs(14, 114).addBox(-2F, -5F, 0.05F, 4F, 6F, 0F, new CubeDeformation(0F)), PartPose.offset(8F, -8F, -4F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** Dans l'ordre des indices de MaitreMageAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,c;0.9,-2,0,1.5,c;1.8,0,0,0,c;2.7,2,0,1.5,c;3.6,0,0,0,c\nbras_gauche|r|0,0,0,0,c;0.9,2,0,-1.5,c;1.8,0,0,0,c;2.7,-2,0,-1.5,c;3.6,0,0,0,c\nchapeau_haut|r|0,0,0,0,c;1.8,-3,0,2,c;3.6,0,0,0,c\ncorps|r|0,0,0,0,c;0.9,0.8,0,0.6,c;1.8,0,0,0,c;2.7,0.8,0,-0.6,c;3.6,0,0,0,c\ncorps|s|0,1,1,1,c;0.9,1,1.012,1,c;1.8,1,1,1,c;2.7,1,1.012,1,c;3.6,1,1,1,c\neffet|p|0,0,0,0,c;0.9,0,0.8,0,c;1.8,0,0,0,c;2.7,0,-0.8,0,c;3.6,0,0,0,c\neffet|s|0,1,1,1,c;0.9,1.15,1.15,1.15,c;1.8,1,1,1,c;2.7,1.15,1.15,1.15,c;3.6,1,1,1,c\ngrimoire|p|0,0,0,0,c;0.9,0,1.2,0,c;1.8,0,0,0,c;2.7,0,-1.2,0,c;3.6,0,0,0,c\ngrimoire|r|0,0,0,0,c;1.8,0,-20,0,c;3.6,0,0,0,c\ntete|r|0,0,0,0,c;0.9,-0.8,-8,0,c;1.8,0,0,0,c;2.7,-0.8,8,0,c;3.6,0,0,0,c"
        }, 3.6F, true),
        BlockbenchAnimations.parse(new String[]{
                "bras_droit|r|0,0,0,0,l;0.12,3.64,0,-0.832,l;0.3,17.5,0,-4,l;0.48,31.36,0,-7.168,l;0.6,35,0,-8,l;0.74,36.04,0,-8,l;0.95,40,0,-8,l;1.16,43.96,0,-8,l;1.3,45,0,-8,l;1.52,40.32,0,-7.168,l;1.85,22.5,0,-4,l;2.18,4.68,0,-0.832,l;2.4,0,0,0,l\nbras_gauche|r|0,0,0,0,l;0.12,3.12,0,1.04,l;0.3,15,0,5,l;0.48,26.88,0,8.96,l;0.6,30,0,10,l;0.74,31.04,0,10,l;0.95,35,0,10,l;1.16,38.96,0,10,l;1.3,40,0,10,l;1.52,35.84,0,8.96,l;1.85,20,0,5,l;2.18,4.16,0,1.04,l;2.4,0,0,0,l\ncorps|r|0,0,0,0,l;0.12,0.312,0,0,l;0.3,1.5,0,0,l;0.48,2.688,0,0,l;0.6,3,0,0,l;0.74,3.208,0,0,l;0.95,4,0,0,l;1.16,4.792,0,0,l;1.3,5,0,0,l;1.52,4.48,0,0,l;1.85,2.5,0,0,l;2.18,0.52,0,0,l;2.4,0,0,0,l\neffet|s|0,1,1,1,c;0.6,1.5,1.5,1.5,c;1.3,2,2,2,c;2.4,1,1,1,c\ngrimoire|r|0,0,0,0,c;0.8,0,-160,0,c;1.6,0,-360,0,c;2.4,0,-360,0,c\ntete|r|0,0,0,0,l;0.12,1.04,0,0,l;0.3,5,0,0,l;0.48,8.96,0,0,l;0.6,10,0,0,l;0.74,10.416,0,0,l;0.95,12,0,0,l;1.16,13.584,0,0,l;1.3,14,0,0,l;1.52,12.544,0,0,l;1.85,7,0,0,l;2.18,1.456,0,0,l;2.4,0,0,0,l"
        }, 2.4F, false)
        };
    }
}
