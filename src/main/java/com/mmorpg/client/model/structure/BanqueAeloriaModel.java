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

/** Modele et animations de Banque d'Aeloria (format Modded Entity de Blockbench, art/structures_blockbench/banque_aeloria). */
public final class BanqueAeloriaModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MMORPG.id("banque_aeloria"), "main");

    private BanqueAeloriaModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_racine = root.addOrReplaceChild("racine", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition p_plateforme = p_racine.addOrReplaceChild("plateforme", CubeListBuilder.create().texOffs(0, 0).addBox(-48F, -4F, -20F, 96F, 4F, 52F, new CubeDeformation(0F)).texOffs(0, 143).addBox(-24F, -2F, -26F, 48F, 2F, 6F, new CubeDeformation(0F)).texOffs(424, 104).addBox(-44F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)).texOffs(424, 104).addBox(36F, -10F, -22F, 8F, 10F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition p_chambre_forte = p_racine.addOrReplaceChild("chambre_forte", CubeListBuilder.create().texOffs(296, 0).addBox(-48F, -44F, 2F, 96F, 44F, 8F, new CubeDeformation(0F)).texOffs(0, 130).addBox(-50F, -47F, 1F, 100F, 3F, 10F, new CubeDeformation(0F)).texOffs(152, 143).addBox(-48F, -44F, -1F, 96F, 4F, 3F, new CubeDeformation(0F)).texOffs(32, 56).addBox(-20F, -40F, -3F, 40F, 40F, 5F, new CubeDeformation(0F)).texOffs(122, 56).addBox(-17F, -37F, -4F, 34F, 34F, 2F, new CubeDeformation(0F)).texOffs(194, 56).addBox(-14F, -34F, -5F, 28F, 28F, 2F, new CubeDeformation(0F)).texOffs(254, 56).addBox(-12F, -32F, -9F, 24F, 24F, 4F, new CubeDeformation(0F)).texOffs(426, 143).addBox(-14F, -9F, -10F, 28F, 3F, 2F, new CubeDeformation(0F)).texOffs(0, 151).addBox(-14F, -34F, -10F, 28F, 3F, 2F, new CubeDeformation(0F)).texOffs(0, 56).addBox(-44F, -40F, -6F, 8F, 40F, 8F, new CubeDeformation(0F)).texOffs(0, 56).addBox(36F, -40F, -6F, 8F, 40F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 18F));
        p_chambre_forte.addOrReplaceChild("chambre_forte_porte_carre_tourne_r", CubeListBuilder.create().texOffs(310, 56).addBox(-12F, -12F, -2F, 24F, 24F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, -20F, -7F, 0F, 0F, 0.7854F));
        PartDefinition p_volant = p_chambre_forte.addOrReplaceChild("volant", CubeListBuilder.create().texOffs(284, 130).addBox(-3F, -3F, -1F, 6F, 6F, 4F, new CubeDeformation(0F)).texOffs(128, 151).addBox(-9F, -1F, -1F, 18F, 2F, 2F, new CubeDeformation(0F)).texOffs(400, 104).addBox(-1F, -9F, -1F, 2F, 18F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, -11F));
        p_volant.addOrReplaceChild("volant_volant_diag_1_r", CubeListBuilder.create().texOffs(168, 151).addBox(-8F, -1F, -1F, 16F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.7854F));
        p_volant.addOrReplaceChild("volant_volant_diag_2_r", CubeListBuilder.create().texOffs(168, 151).addBox(-8F, -1F, -1F, 16F, 2F, 2F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.7854F));
        PartDefinition p_comptoir = p_racine.addOrReplaceChild("comptoir", CubeListBuilder.create().texOffs(0, 104).addBox(-36F, -12F, -8F, 72F, 12F, 14F, new CubeDeformation(0F)).texOffs(188, 104).addBox(-38F, -15F, -10F, 76F, 3F, 18F, new CubeDeformation(0F)).texOffs(210, 151).addBox(-38F, -16F, -10F, 76F, 1F, 2F, new CubeDeformation(0F)).texOffs(496, 104).addBox(-36F, -12F, -10F, 6F, 12F, 2F, new CubeDeformation(0F)).texOffs(496, 104).addBox(30F, -12F, -10F, 6F, 12F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, -4F, 0F));
        PartDefinition p_grilles = p_racine.addOrReplaceChild("grilles", CubeListBuilder.create().texOffs(308, 130).addBox(-38F, -22F, -8F, 76F, 4F, 4F, new CubeDeformation(0F)).texOffs(408, 104).addBox(34F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(27F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(13F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(6F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(-8F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(-15F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(-29F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(408, 104).addBox(-36F, -18F, -7F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(172, 104).addBox(-30F, -18F, -8F, 4F, 18F, 4F, new CubeDeformation(0F)).texOffs(172, 104).addBox(-2F, -18F, -8F, 4F, 18F, 4F, new CubeDeformation(0F)).texOffs(172, 104).addBox(26F, -18F, -8F, 4F, 18F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, 0F));
        PartDefinition p_lingots = p_racine.addOrReplaceChild("lingots", CubeListBuilder.create().texOffs(374, 143).addBox(28F, -3F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(28F, -6F, -5F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(28F, -9F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(28F, -3F, 1F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(28F, -6F, 2F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(22F, -3F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(22F, -6F, -5F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(22F, -9F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-32F, -3F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-32F, -6F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-38F, -3F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-38F, -6F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-38F, -9F, -6F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-35F, -3F, 1F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(374, 143).addBox(-35F, -6F, 1F, 6F, 3F, 4F, new CubeDeformation(0F)).texOffs(456, 104).addBox(-20F, -7F, -6F, 12F, 7F, 8F, new CubeDeformation(0F)).texOffs(244, 130).addBox(-20F, -10F, -6F, 12F, 3F, 8F, new CubeDeformation(0F)).texOffs(204, 151).addBox(-15F, -7F, -7F, 2F, 3F, 1F, new CubeDeformation(0F)).texOffs(60, 151).addBox(-18F, -11F, -4F, 8F, 1F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -20F, 0F));
        p_lingots.addOrReplaceChild("lingots_sac_or_r", CubeListBuilder.create().texOffs(220, 130).addBox(-3F, -7F, -3F, 6F, 7F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(15F, 0F, -1F, 0F, -0.3491F, 0F));
        PartDefinition p_balance = p_racine.addOrReplaceChild("balance", CubeListBuilder.create().texOffs(416, 104).addBox(-1F, -2F, -2F, 2F, 18F, 2F, new CubeDeformation(0F)).texOffs(108, 143).addBox(-4F, 14F, -4F, 8F, 2F, 6F, new CubeDeformation(0F)), PartPose.offset(4F, -36F, 16F));
        PartDefinition p_balance_fleau = p_balance.addOrReplaceChild("balance_fleau", CubeListBuilder.create().texOffs(84, 151).addBox(-10F, -1F, -1F, 20F, 2F, 2F, new CubeDeformation(0F)).texOffs(350, 143).addBox(7F, 9F, -3F, 6F, 1F, 6F, new CubeDeformation(0F)).texOffs(350, 143).addBox(-13F, 9F, -3F, 6F, 1F, 6F, new CubeDeformation(0F)).texOffs(304, 130).addBox(9F, 1F, 0F, 1F, 8F, 1F, new CubeDeformation(0F)).texOffs(304, 130).addBox(-11F, 1F, 0F, 1F, 8F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -2F, -1F));
        PartDefinition p_cristal_ambre = p_racine.addOrReplaceChild("cristal_ambre", CubeListBuilder.create().texOffs(394, 143).addBox(-2F, 0F, -2F, 4F, 3F, 4F, new CubeDeformation(0F)).texOffs(376, 104).addBox(-3F, -14F, -3F, 6F, 14F, 6F, new CubeDeformation(0F)).texOffs(136, 143).addBox(-2F, -18F, -2F, 4F, 4F, 4F, new CubeDeformation(0F)).texOffs(410, 143).addBox(-2F, 0F, -2F, 4F, 2F, 4F, new CubeDeformation(0F)), PartPose.offset(0F, -52F, 14F));
        return LayerDefinition.create(mesh, 512, 256);
    }

    /** Dans l'ordre des indices de BanqueAeloriaAnims. */
    public static AnimationDefinition[] animations() {
        return new AnimationDefinition[]{
        BlockbenchAnimations.parse(new String[]{
                "balance_fleau|r|0,0,0,-3,l;2,0,0,3,l;4,0,0,-3,l\ncristal_ambre|p|0,0,0,0,c;2,0,0.8,0,c;4,0,0,0,c"
        }, 4F, true),
        BlockbenchAnimations.parse(new String[]{
                "balance_fleau|r|0,0,0,-3,l;1,0,0,8,l\ncristal_ambre|p|0,0,0,0,l;0.2,0,0.208,0,l;0.5,0,1,0,l;0.8,0,1.792,0,l;1,0,2,0,l\nvolant|r|0,0,0,0,l;1,0,0,180,l"
        }, 1F, false),
        BlockbenchAnimations.parse(new String[]{
                "balance_fleau|r|0,0,0,8,l;1,0,0,-8,l;2,0,0,8,l\ncristal_ambre|p|0,0,2,0,c;1,0,2.8,0,c;2,0,2,0,c\ncristal_ambre|r|0,0,0,0,l;2,0,-90,0,l\nvolant|r|0,0,0,180,l;2,0,0,540,l"
        }, 2F, true)
        };
    }
}
