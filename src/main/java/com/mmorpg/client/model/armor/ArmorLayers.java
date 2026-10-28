// Genere automatiquement par art/armures_blockbench/exporter_java.py - ne pas modifier a la main (relancer le script).
package com.mmorpg.client.model.armor;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.Map;

/** Modeles 3D des panoplies (art/armures_blockbench), un LayerDefinition HumanoidModel par panoplie et par emplacement. */
public final class ArmorLayers {
    /** Set d'armure du mod -> panoplie utilisee. */
    public static final Map<String, String> SET_PANOPLY = Map.ofEntries(
            Map.entry("acier_soldat", "panoplie_05_niv_020"),
            Map.entry("berserker", "panoplie_06_niv_025"),
            Map.entry("seigneur_guerre", "panoplie_12_niv_055"),
            Map.entry("dieu_guerre", "panoplie_19_niv_090"),
            Map.entry("apprenti", "panoplie_01_niv_001"),
            Map.entry("sorcier", "panoplie_08_niv_035"),
            Map.entry("archimage", "panoplie_11_niv_050"),
            Map.entry("avatar_arcanique", "panoplie_14_niv_065"),
            Map.entry("chasseur", "panoplie_02_niv_005"),
            Map.entry("rodeur", "panoplie_03_niv_010"),
            Map.entry("tireur_elite", "panoplie_09_niv_040"),
            Map.entry("sylvestre", "panoplie_17_niv_080"),
            Map.entry("garde", "panoplie_04_niv_015"),
            Map.entry("gardien", "panoplie_07_niv_030"),
            Map.entry("paladin", "panoplie_13_niv_060"),
            Map.entry("titan", "panoplie_18_niv_085"));
    public static final List<String> PANOPLIES = List.of("panoplie_01_niv_001", "panoplie_02_niv_005", "panoplie_03_niv_010", "panoplie_04_niv_015", "panoplie_05_niv_020", "panoplie_06_niv_025", "panoplie_07_niv_030", "panoplie_08_niv_035", "panoplie_09_niv_040", "panoplie_10_niv_045", "panoplie_11_niv_050", "panoplie_12_niv_055", "panoplie_13_niv_060", "panoplie_14_niv_065", "panoplie_15_niv_070", "panoplie_16_niv_075", "panoplie_17_niv_080", "panoplie_18_niv_085", "panoplie_19_niv_090", "panoplie_20_niv_100");

    private ArmorLayers() {
    }

    public static LayerDefinition create(String panoply, EquipmentSlot slot) {
        return switch (panoply + "/" + slot.getName()) {
            case "panoplie_01_niv_001/head" -> p01Head();
            case "panoplie_01_niv_001/chest" -> p01Chest();
            case "panoplie_01_niv_001/legs" -> p01Legs();
            case "panoplie_01_niv_001/feet" -> p01Feet();
            case "panoplie_02_niv_005/head" -> p02Head();
            case "panoplie_02_niv_005/chest" -> p02Chest();
            case "panoplie_02_niv_005/legs" -> p02Legs();
            case "panoplie_02_niv_005/feet" -> p02Feet();
            case "panoplie_03_niv_010/head" -> p03Head();
            case "panoplie_03_niv_010/chest" -> p03Chest();
            case "panoplie_03_niv_010/legs" -> p03Legs();
            case "panoplie_03_niv_010/feet" -> p03Feet();
            case "panoplie_04_niv_015/head" -> p04Head();
            case "panoplie_04_niv_015/chest" -> p04Chest();
            case "panoplie_04_niv_015/legs" -> p04Legs();
            case "panoplie_04_niv_015/feet" -> p04Feet();
            case "panoplie_05_niv_020/head" -> p05Head();
            case "panoplie_05_niv_020/chest" -> p05Chest();
            case "panoplie_05_niv_020/legs" -> p05Legs();
            case "panoplie_05_niv_020/feet" -> p05Feet();
            case "panoplie_06_niv_025/head" -> p06Head();
            case "panoplie_06_niv_025/chest" -> p06Chest();
            case "panoplie_06_niv_025/legs" -> p06Legs();
            case "panoplie_06_niv_025/feet" -> p06Feet();
            case "panoplie_07_niv_030/head" -> p07Head();
            case "panoplie_07_niv_030/chest" -> p07Chest();
            case "panoplie_07_niv_030/legs" -> p07Legs();
            case "panoplie_07_niv_030/feet" -> p07Feet();
            case "panoplie_08_niv_035/head" -> p08Head();
            case "panoplie_08_niv_035/chest" -> p08Chest();
            case "panoplie_08_niv_035/legs" -> p08Legs();
            case "panoplie_08_niv_035/feet" -> p08Feet();
            case "panoplie_09_niv_040/head" -> p09Head();
            case "panoplie_09_niv_040/chest" -> p09Chest();
            case "panoplie_09_niv_040/legs" -> p09Legs();
            case "panoplie_09_niv_040/feet" -> p09Feet();
            case "panoplie_10_niv_045/head" -> p10Head();
            case "panoplie_10_niv_045/chest" -> p10Chest();
            case "panoplie_10_niv_045/legs" -> p10Legs();
            case "panoplie_10_niv_045/feet" -> p10Feet();
            case "panoplie_11_niv_050/head" -> p11Head();
            case "panoplie_11_niv_050/chest" -> p11Chest();
            case "panoplie_11_niv_050/legs" -> p11Legs();
            case "panoplie_11_niv_050/feet" -> p11Feet();
            case "panoplie_12_niv_055/head" -> p12Head();
            case "panoplie_12_niv_055/chest" -> p12Chest();
            case "panoplie_12_niv_055/legs" -> p12Legs();
            case "panoplie_12_niv_055/feet" -> p12Feet();
            case "panoplie_13_niv_060/head" -> p13Head();
            case "panoplie_13_niv_060/chest" -> p13Chest();
            case "panoplie_13_niv_060/legs" -> p13Legs();
            case "panoplie_13_niv_060/feet" -> p13Feet();
            case "panoplie_14_niv_065/head" -> p14Head();
            case "panoplie_14_niv_065/chest" -> p14Chest();
            case "panoplie_14_niv_065/legs" -> p14Legs();
            case "panoplie_14_niv_065/feet" -> p14Feet();
            case "panoplie_15_niv_070/head" -> p15Head();
            case "panoplie_15_niv_070/chest" -> p15Chest();
            case "panoplie_15_niv_070/legs" -> p15Legs();
            case "panoplie_15_niv_070/feet" -> p15Feet();
            case "panoplie_16_niv_075/head" -> p16Head();
            case "panoplie_16_niv_075/chest" -> p16Chest();
            case "panoplie_16_niv_075/legs" -> p16Legs();
            case "panoplie_16_niv_075/feet" -> p16Feet();
            case "panoplie_17_niv_080/head" -> p17Head();
            case "panoplie_17_niv_080/chest" -> p17Chest();
            case "panoplie_17_niv_080/legs" -> p17Legs();
            case "panoplie_17_niv_080/feet" -> p17Feet();
            case "panoplie_18_niv_085/head" -> p18Head();
            case "panoplie_18_niv_085/chest" -> p18Chest();
            case "panoplie_18_niv_085/legs" -> p18Legs();
            case "panoplie_18_niv_085/feet" -> p18Feet();
            case "panoplie_19_niv_090/head" -> p19Head();
            case "panoplie_19_niv_090/chest" -> p19Chest();
            case "panoplie_19_niv_090/legs" -> p19Legs();
            case "panoplie_19_niv_090/feet" -> p19Feet();
            case "panoplie_20_niv_100/head" -> p20Head();
            case "panoplie_20_niv_100/chest" -> p20Chest();
            case "panoplie_20_niv_100/legs" -> p20Legs();
            case "panoplie_20_niv_100/feet" -> p20Feet();
            default -> throw new IllegalArgumentException("Panoplie inconnue : " + panoply + "/" + slot.getName());
        };
    }

    private static LayerDefinition p01Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-4F, -8F, -4F, 8F, 4F, 8F, new CubeDeformation(0.6F)).texOffs(22, 16).addBox(-4F, -4F, -4F, 8F, 1F, 8F, new CubeDeformation(0.85F)).texOffs(48, 36).addBox(3F, -3.5F, -1F, 1F, 3F, 3F, new CubeDeformation(0.65F)).texOffs(48, 36).addBox(-4F, -3.5F, -1F, 1F, 3F, 3F, new CubeDeformation(0.65F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p01Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.6F)).texOffs(16, 16).addBox(1F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(26, 43).addBox(1F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)).texOffs(16, 16).addBox(-3F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(26, 43).addBox(-3F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 36).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 36).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 27).addBox(-3F, -2F, -2F, 4F, 5F, 4F, new CubeDeformation(0.5F)).texOffs(0, 36).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(32, 43).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(32, 43).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 27).addBox(-1F, -2F, -2F, 4F, 5F, 4F, new CubeDeformation(0.5F)).texOffs(0, 36).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(32, 43).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(32, 43).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p01Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 27).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.45F)).texOffs(24, 36).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.8F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(56, 36).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(56, 36).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p01Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 27).addBox(-2F, 8F, -2F, 4F, 4F, 4F, new CubeDeformation(0.75F)).texOffs(0, 43).addBox(-2F, 8F, -2F, 4F, 1F, 4F, new CubeDeformation(1.05F)).texOffs(16, 43).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 27).addBox(-2F, 8F, -2F, 4F, 4F, 4F, new CubeDeformation(0.75F)).texOffs(0, 43).addBox(-2F, 8F, -2F, 4F, 1F, 4F, new CubeDeformation(1.05F)).texOffs(16, 43).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p02Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.8F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p02Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.6F)).texOffs(48, 16).addBox(-3F, 2F, -3F, 6F, 7F, 1F, new CubeDeformation(0.5F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 27).addBox(-4.1F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.3F)).texOffs(16, 16).addBox(-3F, 3F, -2F, 4F, 5F, 4F, new CubeDeformation(0.5F)).texOffs(44, 27).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(8, 41).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(8, 41).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 27).addBox(-0.9F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.3F)).texOffs(16, 16).addBox(-1F, 3F, -2F, 4F, 5F, 4F, new CubeDeformation(0.5F)).texOffs(44, 27).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(8, 41).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(8, 41).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p02Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 27).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.45F)).texOffs(4, 34).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.8F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(0, 41).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.55F)).texOffs(28, 34).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(0, 41).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.55F)).texOffs(28, 34).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p02Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(32, 34).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(48, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(32, 34).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(48, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p03Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.8F)).texOffs(0, 27).addBox(-4F, -6F, -4F, 8F, 1F, 8F, new CubeDeformation(0.95F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p03Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.6F)).texOffs(0, 50).addBox(-2F, 2F, -3F, 4F, 4F, 1F, new CubeDeformation(0.4F)).texOffs(16, 16).addBox(1F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(54, 50).addBox(1F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)).texOffs(16, 16).addBox(-3F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(54, 50).addBox(-3F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(0, 43).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(0, 43).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 36).addBox(-4.1F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.35F)).texOffs(32, 43).addBox(-3.6F, -3.5F, -2F, 4F, 1F, 4F, new CubeDeformation(0.3F)).texOffs(22, 16).addBox(-3F, 2F, -2F, 4F, 6F, 4F, new CubeDeformation(0.5F)).texOffs(10, 50).addBox(-3F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.35F)).texOffs(44, 36).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(60, 50).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(60, 50).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 36).addBox(-0.9F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.35F)).texOffs(32, 43).addBox(-0.4F, -3.5F, -2F, 4F, 1F, 4F, new CubeDeformation(0.3F)).texOffs(22, 16).addBox(-1F, 2F, -2F, 4F, 6F, 4F, new CubeDeformation(0.5F)).texOffs(10, 50).addBox(-1F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.35F)).texOffs(44, 36).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(60, 50).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(60, 50).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p03Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 36).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.45F)).texOffs(4, 43).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.8F)).texOffs(48, 50).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.85F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(30, 50).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.55F)).texOffs(28, 43).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 7F, 4F, new CubeDeformation(0.4F)).texOffs(30, 50).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.55F)).texOffs(28, 43).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p03Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 27).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(38, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(48, 43).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(20, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 27).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(38, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(48, 43).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(20, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p04Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.9F)).texOffs(54, 54).addBox(-0.5F, -5F, -5F, 1F, 3F, 1F, new CubeDeformation(0.2F)).texOffs(0, 29).addBox(-4F, -2F, -4F, 8F, 2F, 8F, new CubeDeformation(1.2F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p04Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.65F)).texOffs(0, 16).addBox(-4F, 0F, -2F, 8F, 9F, 4F, new CubeDeformation(0.8F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(40, 47).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(40, 47).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16).addBox(-3F, -2F, -2F, 4F, 9F, 4F, new CubeDeformation(0.55F)).texOffs(0, 47).addBox(-4.1F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(20, 47).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(16, 60).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(16, 60).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).addBox(-1F, -2F, -2F, 4F, 9F, 4F, new CubeDeformation(0.55F)).texOffs(0, 47).addBox(-0.9F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(20, 47).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(16, 60).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(16, 60).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p04Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 39).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 54).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(10, 60).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(0, 39).addBox(-4F, 11F, -2F, 8F, 4F, 4F, new CubeDeformation(0.75F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.4F)).texOffs(24, 54).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.4F)).texOffs(24, 54).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p04Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(32, 29).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(0, 60).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(28, 54).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(44, 54).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 29).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.8F)).texOffs(0, 60).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(28, 54).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.1F)).texOffs(44, 54).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p05Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(0, 28).addBox(-0.5F, -9F, -4F, 1F, 1F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p05Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(40, 37).addBox(-3F, 2F, -3F, 6F, 6F, 1F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(54, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(54, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(28, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(18, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-4.6F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(0, 37).addBox(-4.1F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.3F)).texOffs(16, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(20, 37).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(30, 50).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(30, 50).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.4F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(0, 37).addBox(-0.9F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.3F)).texOffs(16, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(20, 37).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(30, 50).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(30, 50).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p05Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 28).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 44).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(24, 50).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(10, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(10, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(48, 44).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.6F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(48, 44).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.6F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p05Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(0, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(38, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(0, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(38, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p06Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(26, 52).addBox(2F, -4F, -5F, 2F, 4F, 1F, new CubeDeformation(0.3F)).texOffs(26, 52).addBox(-4F, -4F, -5F, 2F, 4F, 1F, new CubeDeformation(0.3F)).texOffs(0, 28).addBox(-0.5F, -10F, -4F, 1F, 2F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p06Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(34, 28).addBox(-3F, 2F, -3F, 6F, 7F, 1F, new CubeDeformation(0.5F)).texOffs(24, 45).addBox(-4F, -1F, -2F, 8F, 2F, 4F, new CubeDeformation(1.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 45).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 45).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(16, 52).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(8, 57).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-5.1F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(0, 38).addBox(-4.6F, -4F, -3F, 6F, 1F, 6F, new CubeDeformation(0.2F)).texOffs(16, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 45).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(20, 57).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(20, 57).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.9F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(0, 38).addBox(-1.4F, -4F, -3F, 6F, 1F, 6F, new CubeDeformation(0.2F)).texOffs(16, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 45).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(20, 57).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(20, 57).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p06Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 38).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.95F)).texOffs(14, 57).addBox(-1F, 9.5F, -3F, 2F, 2F, 1F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(0, 57).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 57).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 57).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(0, 57).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 57).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 57).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(32, 52).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(48, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(32, 52).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(48, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p06Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(52, 52).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 52).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(42, 52).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(52, 52).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 52).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(42, 52).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p07Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(0, 42).addBox(-4F, -6F, -5F, 8F, 5F, 1F, new CubeDeformation(0.5F)).texOffs(16, 16).addBox(-0.5F, -10F, -4F, 1F, 2F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p07Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(36, 48).addBox(-3.5F, 2F, -3F, 7F, 2F, 1F, new CubeDeformation(0.4F)).texOffs(36, 48).addBox(-3.5F, 4.5F, -3F, 7F, 2F, 1F, new CubeDeformation(0.45F)).texOffs(36, 48).addBox(-3.5F, 7F, -3F, 7F, 2F, 1F, new CubeDeformation(0.5F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(36, 35).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(36, 35).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(16, 48).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(8, 53).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(34, 16).addBox(-4.6F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(40, 27).addBox(-4.1F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(38, 42).addBox(-3.6F, -4.5F, -2F, 4F, 1F, 4F, new CubeDeformation(0.2F)).texOffs(0, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 35).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(20, 53).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(20, 53).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(34, 16).addBox(-1.4F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(40, 27).addBox(-0.9F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(38, 42).addBox(-0.4F, -4.5F, -2F, 4F, 1F, 4F, new CubeDeformation(0.2F)).texOffs(0, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 35).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(20, 53).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(20, 53).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p07Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 27).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(40, 35).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(14, 53).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(0, 53).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 53).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 53).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(0, 53).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 53).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 53).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(20, 35).addBox(-2F, 0F, -2F, 4F, 3F, 4F, new CubeDeformation(0.45F)).texOffs(18, 42).addBox(-2F, 4F, -2F, 4F, 2F, 4F, new CubeDeformation(0.55F)).texOffs(18, 42).addBox(-2F, 6F, -2F, 4F, 2F, 4F, new CubeDeformation(0.45F)).texOffs(52, 48).addBox(-2F, 4F, -3F, 4F, 2F, 1F, new CubeDeformation(0.7F)).texOffs(34, 42).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 35).addBox(-2F, 0F, -2F, 4F, 3F, 4F, new CubeDeformation(0.45F)).texOffs(18, 42).addBox(-2F, 4F, -2F, 4F, 2F, 4F, new CubeDeformation(0.55F)).texOffs(18, 42).addBox(-2F, 6F, -2F, 4F, 2F, 4F, new CubeDeformation(0.45F)).texOffs(52, 48).addBox(-2F, 4F, -3F, 4F, 2F, 1F, new CubeDeformation(0.7F)).texOffs(34, 42).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p07Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 27).addBox(-2F, 8F, -2F, 4F, 4F, 4F, new CubeDeformation(0.9F)).texOffs(52, 48).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 48).addBox(-2F, 8F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(26, 48).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 27).addBox(-2F, 8F, -2F, 4F, 4F, 4F, new CubeDeformation(0.9F)).texOffs(52, 48).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 48).addBox(-2F, 8F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(26, 48).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p08Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(38, 16).addBox(-0.5F, -10F, -4F, 1F, 2F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p08Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.6F)).texOffs(24, 30).addBox(-3.5F, 1F, -3F, 7F, 8F, 1F, new CubeDeformation(0.5F)).texOffs(0, 54).addBox(-3F, 12F, -3F, 6F, 4F, 1F, new CubeDeformation(0.2F)).texOffs(32, 16).addBox(1F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(12, 59).addBox(1F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)).texOffs(32, 16).addBox(-3F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(12, 59).addBox(-3F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("petit_cristal_cadre_rot", CubeListBuilder.create().texOffs(14, 54).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 3.5F, -3.4F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("petit_cristal_rot", CubeListBuilder.create().texOffs(0, 59).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 3.5F, -3.9F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 47).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 47).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(24, 54).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(0, 59).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 30).addBox(-4.6F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(40, 39).addBox(-4.1F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-3F, -2F, -2F, 4F, 10F, 4F, new CubeDeformation(0.5F)).texOffs(0, 39).addBox(-3F, 4F, -2F, 4F, 4F, 4F, new CubeDeformation(0.75F)).texOffs(0, 47).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(18, 59).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(18, 59).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 30).addBox(-1.4F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(40, 39).addBox(-0.9F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-1F, -2F, -2F, 4F, 10F, 4F, new CubeDeformation(0.5F)).texOffs(0, 39).addBox(-1F, 4F, -2F, 4F, 4F, 4F, new CubeDeformation(0.75F)).texOffs(0, 47).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(18, 59).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(18, 59).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p08Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 39).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(24, 47).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(6, 59).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.4F)).texOffs(34, 54).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(48, 47).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.4F)).texOffs(34, 54).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(48, 47).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p08Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 30).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(54, 54).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(44, 54).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 30).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(54, 54).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(44, 54).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p09Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(0, 28).addBox(-0.5F, -10F, -4F, 1F, 2F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p09Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("embleme_argent_rot", CubeListBuilder.create().texOffs(52, 45).addBox(-1.5F, -1.5F, -0.5F, 3F, 3F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 4F, -3.5F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(0, 45).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(0, 45).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(32, 45).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(18, 52).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 38).addBox(-4.1F, -2.5F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.3F)).texOffs(16, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(24, 38).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(34, 52).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(34, 52).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.6F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(34, 28).addBox(-1.1F, -5F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(16, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(24, 38).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(34, 52).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(34, 52).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c6 = left_arm.addOrReplaceChild("pointe_epaule_1_rot", CubeListBuilder.create().texOffs(30, 52).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(3F, -4.5F, 0F, 0F, 0F, 0.3927F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p09Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 38).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(4, 45).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(24, 52).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(10, 52).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 52).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 52).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(10, 52).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 52).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(10, 52).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(52, 45).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(28, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(52, 45).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(28, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p09Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(0, 52).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(42, 45).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(18, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(0, 52).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(42, 45).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p10Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(16, 16).addBox(-0.5F, -11F, -4F, 1F, 3F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("cristal_front_rot", CubeListBuilder.create().texOffs(40, 59).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0F, -6.5F, -4.9F, 0F, 0F, 0.7854F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p10Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.8F)).texOffs(28, 28).addBox(-4F, 1F, -3F, 8F, 9F, 1F, new CubeDeformation(0.6F)).texOffs(50, 53).addBox(-2F, -1F, -3.2F, 4F, 3F, 1F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_central_cadre_rot", CubeListBuilder.create().texOffs(40, 53).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.5F, -3.6F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_central_rot", CubeListBuilder.create().texOffs(28, 59).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5.5F, -4.1F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 46).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 46).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(40, 53).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(28, 59).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 28).addBox(-5.1F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(0, 38).addBox(-4.6F, -4.5F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(0, 53).addBox(-4.1F, -5F, -2.5F, 5F, 1F, 5F, new CubeDeformation(0.2F)).texOffs(34, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.65F)).texOffs(0, 46).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(44, 59).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(44, 59).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c7 = right_arm.addOrReplaceChild("cristal_epaule_gauche_rot", CubeListBuilder.create().texOffs(40, 59).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(-1.6F, -2F, -3.9F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 28).addBox(-1.9F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(0, 38).addBox(-1.4F, -4.5F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(0, 53).addBox(-0.9F, -5F, -2.5F, 5F, 1F, 5F, new CubeDeformation(0.2F)).texOffs(34, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.65F)).texOffs(0, 46).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(44, 59).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(44, 59).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c8 = left_arm.addOrReplaceChild("cristal_epaule_droit_rot", CubeListBuilder.create().texOffs(40, 59).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(1.6F, -2F, -3.9F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p10Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 38).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.55F)).texOffs(24, 46).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.9F)).texOffs(34, 59).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.95F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(20, 59).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(20, 59).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(20, 59).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(20, 59).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(20, 59).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(20, 59).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(50, 53).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(20, 53).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c3 = right_leg.addOrReplaceChild("cristal_genou_gauche_rot", CubeListBuilder.create().texOffs(40, 59).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0F, 5.5F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(50, 53).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(20, 53).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c4 = left_leg.addOrReplaceChild("cristal_genou_droit_rot", CubeListBuilder.create().texOffs(40, 59).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0F, 5.5F, -3.7F, 0F, 0F, 0.7854F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p10Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(46, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(10, 59).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(24, 53).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 59).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(46, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(10, 59).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(24, 53).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 59).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p11Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(22, 16).addBox(-0.5F, -10F, -4F, 1F, 2F, 8F, new CubeDeformation(0.4F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p11Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(16, 28).addBox(-3F, 1.5F, -3F, 6F, 8F, 1F, new CubeDeformation(0.5F)).texOffs(16, 16).addBox(1F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(30, 50).addBox(1F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)).texOffs(16, 16).addBox(-3F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(30, 50).addBox(-3F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("gemme_runique_cadre_rot", CubeListBuilder.create().texOffs(28, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 3.5F, -3.4F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("gemme_runique_rot", CubeListBuilder.create().texOffs(18, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 3.5F, -3.9F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(40, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(40, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(38, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(18, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-4.6F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(0, 37).addBox(-4.1F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.6F)).texOffs(20, 37).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(36, 50).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(36, 50).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.4F, -3F, -3F, 6F, 3F, 6F, new CubeDeformation(0.3F)).texOffs(0, 37).addBox(-0.9F, -4F, -2.5F, 5F, 2F, 5F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.6F)).texOffs(20, 37).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(36, 50).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(36, 50).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p11Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(30, 28).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 44).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(24, 50).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(0, 50).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(0, 50).addBox(-1.5F, 4F, -3F, 3F, 3F, 1F, new CubeDeformation(0.65F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p11Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(8, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(48, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(8, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(48, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p12Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("aileron_droit_rot", CubeListBuilder.create().texOffs(0, 28).addBox(-0.5F, -4F, -3F, 1F, 4F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(4.6F, -5F, 0F, 0F, 0F, 0.3491F));
        PartDefinition c2 = head.addOrReplaceChild("aileron_gauche_rot", CubeListBuilder.create().texOffs(0, 28).addBox(-0.5F, -4F, -3F, 1F, 4F, 6F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4.6F, -5F, 0F, 0F, 0F, -0.3491F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p12Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(46, 53).addBox(-3.5F, 1F, -3F, 7F, 2F, 1F, new CubeDeformation(0.45F)).texOffs(0, 58).addBox(-3F, 3.5F, -3F, 6F, 2F, 1F, new CubeDeformation(0.45F)).texOffs(46, 53).addBox(-3.5F, 6F, -3F, 7F, 2F, 1F, new CubeDeformation(0.45F)).texOffs(0, 58).addBox(-3F, 8.5F, -3F, 6F, 2F, 1F, new CubeDeformation(0.45F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 46).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 46).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(16, 53).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(32, 58).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(46, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 46).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(44, 58).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(44, 58).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c5 = right_arm.addOrReplaceChild("epauliere_gauche_rot", CubeListBuilder.create().texOffs(16, 16).addBox(-3.5F, -1F, -4F, 7F, 3F, 8F, new CubeDeformation(0.2F)).texOffs(0, 38).addBox(-3F, -2F, -3.5F, 6F, 1F, 7F, new CubeDeformation(0.2F)).texOffs(14, 28).addBox(-4.5F, 2F, -4F, 2F, 1F, 8F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(-1F, -2F, 0F, 0F, 0F, -0.3927F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(46, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(0, 46).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(44, 58).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(44, 58).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c6 = left_arm.addOrReplaceChild("epauliere_droit_rot", CubeListBuilder.create().texOffs(16, 16).addBox(-3.5F, -1F, -4F, 7F, 3F, 8F, new CubeDeformation(0.2F)).texOffs(0, 38).addBox(-3F, -2F, -3.5F, 6F, 1F, 7F, new CubeDeformation(0.2F)).texOffs(14, 28).addBox(2.5F, 2F, -4F, 2F, 1F, 8F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(1F, -2F, 0F, 0F, 0F, 0.3927F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p12Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(26, 38).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(24, 46).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(38, 58).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(24, 58).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(24, 58).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(24, 58).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(24, 58).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(24, 58).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(24, 58).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(26, 53).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(48, 46).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(26, 53).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(48, 46).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p12Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(34, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(14, 58).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 53).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(36, 53).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(34, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(14, 58).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(0, 53).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(36, 53).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p13Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("cristal_crete_1_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0F, -8.5F, -2.5F, 0F, -0.7854F, 0F));
        PartDefinition c2 = head.addOrReplaceChild("cristal_crete_2_rot", CubeListBuilder.create().texOffs(0, 50).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0F, -8.5F, 0F, 0F, -0.7854F, 0F));
        PartDefinition c3 = head.addOrReplaceChild("cristal_crete_3_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0F, -8.5F, 2.5F, 0F, -0.7854F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p13Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.75F)).texOffs(0, 28).addBox(-3.5F, 1.5F, -3F, 7F, 8F, 1F, new CubeDeformation(0.5F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_central_cadre_rot", CubeListBuilder.create().texOffs(44, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 4.5F, -3.5F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_central_rot", CubeListBuilder.create().texOffs(22, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 4.5F, -4F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(44, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(22, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-5.1F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(32, 28).addBox(-4.6F, -4F, -3F, 6F, 1F, 6F, new CubeDeformation(0.25F)).texOffs(16, 16).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(24, 37).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(38, 50).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(38, 50).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c7 = right_arm.addOrReplaceChild("cristal_epaule_gauche_1_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0F, -3.5F, 0F, 0F, 0F, 0.4363F));
        PartDefinition c8 = right_arm.addOrReplaceChild("cristal_epaule_gauche_2_rot", CubeListBuilder.create().texOffs(0, 50).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.2F)), PartPose.offset(-1.6F, -3.5F, 0F));
        PartDefinition c9 = right_arm.addOrReplaceChild("cristal_epaule_gauche_3_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(-3.2F, -3.5F, 0F, 0F, 0F, -0.4363F));
        PartDefinition c10 = right_arm.addOrReplaceChild("cristal_avant_bras_gauche_rot", CubeListBuilder.create().texOffs(42, 50).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(-1F, 4.5F, -3.4F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.9F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(32, 28).addBox(-1.4F, -4F, -3F, 6F, 1F, 6F, new CubeDeformation(0.25F)).texOffs(16, 16).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.6F)).texOffs(24, 37).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(38, 50).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(38, 50).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c11 = left_arm.addOrReplaceChild("cristal_epaule_droit_1_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(3.2F, -3.5F, 0F, 0F, 0F, 0.4363F));
        PartDefinition c12 = left_arm.addOrReplaceChild("cristal_epaule_droit_2_rot", CubeListBuilder.create().texOffs(0, 50).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.2F)), PartPose.offset(1.6F, -3.5F, 0F));
        PartDefinition c13 = left_arm.addOrReplaceChild("cristal_epaule_droit_3_rot", CubeListBuilder.create().texOffs(34, 50).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0F, -3.5F, 0F, 0F, 0F, -0.4363F));
        PartDefinition c14 = left_arm.addOrReplaceChild("cristal_avant_bras_droit_rot", CubeListBuilder.create().texOffs(42, 50).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(1F, 4.5F, -3.4F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p13Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 37).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 44).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(28, 50).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(14, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(14, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(14, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(14, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(14, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(14, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c3 = right_leg.addOrReplaceChild("cristal_genou_gauche_rot", CubeListBuilder.create().texOffs(42, 50).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5.5F, -2.9F, 0F, 0F, 0.7854F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(24, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c4 = left_leg.addOrReplaceChild("cristal_genou_droit_rot", CubeListBuilder.create().texOffs(42, 50).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5.5F, -2.9F, 0F, 0F, 0.7854F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p13Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(4, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(28, 44).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(54, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(4, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(28, 44).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(54, 44).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p14Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(26, 50).addBox(-0.5F, -11F, -4F, 1F, 2F, 2F, new CubeDeformation(0F)).texOffs(0, 50).addBox(-0.5F, -12F, -2F, 1F, 3F, 2F, new CubeDeformation(0F)).texOffs(24, 44).addBox(-0.5F, -13F, 0F, 1F, 4F, 2F, new CubeDeformation(0F)).texOffs(0, 50).addBox(-0.5F, -12F, 2F, 1F, 3F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p14Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.8F)).texOffs(16, 28).addBox(-3F, 1.5F, -3F, 6F, 8F, 1F, new CubeDeformation(0.55F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("gemme_runique_cadre_rot", CubeListBuilder.create().texOffs(50, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 3.5F, -3.6F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("gemme_runique_rot", CubeListBuilder.create().texOffs(50, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 3.5F, -4.1F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(50, 44).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(50, 50).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-5.1F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(30, 28).addBox(-4.6F, -4.5F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.6F)).texOffs(24, 37).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(0, 55).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(0, 55).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.9F, -3F, -3.5F, 7F, 3F, 7F, new CubeDeformation(0.3F)).texOffs(30, 28).addBox(-1.4F, -4.5F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.6F)).texOffs(24, 37).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(0, 55).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(0, 55).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p14Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 37).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 44).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(56, 50).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(42, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(42, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(42, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(42, 50).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(42, 50).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(42, 50).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(6, 50).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(30, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.45F)).texOffs(6, 50).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.7F)).texOffs(30, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p14Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(32, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(34, 44).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(16, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.9F)).texOffs(32, 50).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1F)).texOffs(34, 44).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.2F)).texOffs(16, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p15Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(46, 44).addBox(2F, -5F, -5.2F, 2F, 5F, 1F, new CubeDeformation(0.35F)).texOffs(46, 44).addBox(-4F, -5F, -5.2F, 2F, 5F, 1F, new CubeDeformation(0.35F)).texOffs(0, 55).addBox(-1F, -11F, -3F, 2F, 2F, 2F, new CubeDeformation(0F)).texOffs(46, 50).addBox(-1F, -12F, -1F, 2F, 3F, 2F, new CubeDeformation(0F)).texOffs(0, 55).addBox(-1F, -11F, 1F, 2F, 2F, 2F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p15Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.85F)).texOffs(46, 16).addBox(-4F, 1F, -3F, 8F, 9F, 1F, new CubeDeformation(0.6F)).texOffs(0, 44).addBox(-4F, -1F, -2F, 8F, 2F, 4F, new CubeDeformation(1.15F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_central_cadre_rot", CubeListBuilder.create().texOffs(16, 50).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.5F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_central_rot", CubeListBuilder.create().texOffs(26, 55).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5.5F, -4.2F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(26, 50).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(26, 55).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 16).addBox(-5.1F, -3F, -4F, 7F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(0, 28).addBox(-4.6F, -5F, -3.5F, 6F, 2F, 7F, new CubeDeformation(0.25F)).texOffs(24, 44).addBox(-4.6F, -6F, -2.5F, 6F, 1F, 5F, new CubeDeformation(0.2F)).texOffs(0, 16).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 37).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(32, 55).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(32, 55).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c7 = right_arm.addOrReplaceChild("cristal_epaule_gauche_rot", CubeListBuilder.create().texOffs(36, 55).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-2F, -2F, -4.4F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 16).addBox(-1.9F, -3F, -4F, 7F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(0, 28).addBox(-1.4F, -5F, -3.5F, 6F, 2F, 7F, new CubeDeformation(0.25F)).texOffs(24, 44).addBox(-1.4F, -6F, -2.5F, 6F, 1F, 5F, new CubeDeformation(0.2F)).texOffs(0, 16).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 37).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(32, 55).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(32, 55).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c8 = left_arm.addOrReplaceChild("cristal_epaule_droit_rot", CubeListBuilder.create().texOffs(36, 55).addBox(-0.5F, -0.5F, -0.5F, 1F, 1F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(2F, -2F, -4.4F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p15Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 37).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(1F)).texOffs(26, 55).addBox(-1F, 9.5F, -3F, 2F, 2F, 1F, new CubeDeformation(1.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(18, 55).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(18, 55).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(18, 55).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(18, 55).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(18, 55).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(18, 55).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(36, 50).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.75F)).texOffs(52, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(36, 50).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.75F)).texOffs(52, 44).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p15Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(26, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(8, 55).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(0, 50).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(54, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(26, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(8, 55).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(0, 50).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(54, 50).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p16Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(100, 0).addBox(-1F, -11F, -4F, 2F, 3F, 8F, new CubeDeformation(0F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("cristal_front_rot", CubeListBuilder.create().texOffs(28, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, -6F, -5.1F, 0F, 0F, 0.7854F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p16Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.8F)).texOffs(0, 16).addBox(-4F, 1F, -3F, 8F, 10F, 1F, new CubeDeformation(0.4F)).texOffs(68, 16).addBox(-3F, 2F, -3.5F, 6F, 8F, 1F, new CubeDeformation(0.4F)).texOffs(44, 27).addBox(-2F, 3F, -4F, 4F, 6F, 1F, new CubeDeformation(0.4F)).texOffs(18, 16).addBox(1F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(46, 34).addBox(1F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)).texOffs(18, 16).addBox(-3F, 12F, -3F, 2F, 10F, 1F, new CubeDeformation(0.1F)).texOffs(46, 34).addBox(-3F, 21F, -3.2F, 2F, 1F, 1F, new CubeDeformation(0.05F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_lumineux_cadre_rot", CubeListBuilder.create().texOffs(82, 27).addBox(-2.5F, -2.5F, -0.5F, 5F, 5F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 6F, -4.6F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_lumineux_rot", CubeListBuilder.create().texOffs(10, 34).addBox(-1.5F, -1.5F, -0.5F, 3F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 6F, -5.1F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(54, 27).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(54, 27).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(114, 27).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(34, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-4.6F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(24, 16).addBox(-5.1F, -4.5F, -3.5F, 7F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(82, 16).addBox(-4.6F, -6F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 27).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(52, 34).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(52, 34).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c7 = right_arm.addOrReplaceChild("cristal_epaule_gauche_rot", CubeListBuilder.create().texOffs(28, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-1.8F, -2.5F, -4.8F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-1.4F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(24, 16).addBox(-1.9F, -4.5F, -3.5F, 7F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(82, 16).addBox(-1.4F, -6F, -3F, 6F, 2F, 6F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 27).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(52, 34).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(52, 34).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c8 = left_arm.addOrReplaceChild("cristal_epaule_droit_rot", CubeListBuilder.create().texOffs(28, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(1.8F, -2.5F, -4.8F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p16Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 27).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(58, 27).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(40, 34).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(94, 27).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c1 = right_leg.addOrReplaceChild("cristal_genou_gauche_rot", CubeListBuilder.create().texOffs(28, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -3.5F, 0F, 0F, 0.7854F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(94, 27).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c2 = left_leg.addOrReplaceChild("cristal_genou_droit_rot", CubeListBuilder.create().texOffs(28, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -3.5F, 0F, 0F, 0.7854F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p16Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(52, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(18, 34).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(98, 27).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(52, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(18, 34).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(98, 27).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p17Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("pointe_casque_1_rot", CubeListBuilder.create().texOffs(46, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -8.5F, -2.5F));
        PartDefinition c2 = head.addOrReplaceChild("pointe_casque_2_rot", CubeListBuilder.create().texOffs(46, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -8.5F, 0F));
        PartDefinition c3 = head.addOrReplaceChild("pointe_casque_3_rot", CubeListBuilder.create().texOffs(46, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, -8.5F, 2.5F));
        PartDefinition c4 = head.addOrReplaceChild("corne_droit_rot", CubeListBuilder.create().texOffs(10, 51).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.5F, -6F, 0F, 0F, 0F, 0.6109F));
        PartDefinition c5 = head.addOrReplaceChild("corne_gauche_rot", CubeListBuilder.create().texOffs(10, 51).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.5F, -6F, 0F, 0F, 0F, -0.6109F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p17Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.8F)).texOffs(0, 28).addBox(-3.5F, 1F, -3F, 7F, 9F, 1F, new CubeDeformation(0.55F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_central_cadre_rot", CubeListBuilder.create().texOffs(24, 45).addBox(-2.5F, -2.5F, -0.5F, 5F, 5F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5F, -4.4F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_central_rot", CubeListBuilder.create().texOffs(38, 51).addBox(-1.5F, -1.5F, -0.5F, 3F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5F, -4.9F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(44, 38).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(44, 38).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c5 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(0, 51).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(8, 56).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-4.6F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(32, 28).addBox(-5.1F, -4F, -3.5F, 7F, 1F, 7F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 38).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(24, 56).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(24, 56).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c7 = right_arm.addOrReplaceChild("cristal_epaule_gauche_1_rot", CubeListBuilder.create().texOffs(50, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.6F, -3.5F, 0F, 0F, 0F, 0.5236F));
        PartDefinition c8 = right_arm.addOrReplaceChild("cristal_epaule_gauche_2_rot", CubeListBuilder.create().texOffs(36, 45).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-1F, -3.5F, 0F, 0F, 0F, 0.2094F));
        PartDefinition c9 = right_arm.addOrReplaceChild("cristal_epaule_gauche_3_rot", CubeListBuilder.create().texOffs(14, 51).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-2.6F, -3.5F, 0F, 0F, 0F, -0.2094F));
        PartDefinition c10 = right_arm.addOrReplaceChild("cristal_epaule_gauche_4_rot", CubeListBuilder.create().texOffs(50, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-4.2F, -3.5F, 0F, 0F, 0F, -0.5236F));
        PartDefinition c11 = right_arm.addOrReplaceChild("lame_bras_gauche_rot", CubeListBuilder.create().texOffs(40, 45).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-3.5F, 6F, 0F, 0F, 0F, 0.3491F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-1.4F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(32, 28).addBox(-1.9F, -4F, -3.5F, 7F, 1F, 7F, new CubeDeformation(0.25F)).texOffs(0, 16).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(24, 38).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(24, 56).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(24, 56).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c12 = left_arm.addOrReplaceChild("cristal_epaule_droit_1_rot", CubeListBuilder.create().texOffs(50, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(4.2F, -3.5F, 0F, 0F, 0F, 0.5236F));
        PartDefinition c13 = left_arm.addOrReplaceChild("cristal_epaule_droit_2_rot", CubeListBuilder.create().texOffs(36, 45).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(2.6F, -3.5F, 0F, 0F, 0F, 0.2094F));
        PartDefinition c14 = left_arm.addOrReplaceChild("cristal_epaule_droit_3_rot", CubeListBuilder.create().texOffs(14, 51).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(1F, -3.5F, 0F, 0F, 0F, -0.2094F));
        PartDefinition c15 = left_arm.addOrReplaceChild("cristal_epaule_droit_4_rot", CubeListBuilder.create().texOffs(50, 51).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-0.6F, -3.5F, 0F, 0F, 0F, -0.5236F));
        PartDefinition c16 = left_arm.addOrReplaceChild("lame_bras_droit_rot", CubeListBuilder.create().texOffs(40, 45).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(3.5F, 6F, 0F, 0F, 0F, -0.3491F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p17Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 38).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(0, 45).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(14, 56).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(0, 56).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 56).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 56).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(0, 56).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 56).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(0, 56).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(18, 51).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.75F)).texOffs(40, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c3 = right_leg.addOrReplaceChild("pointe_genou_gauche_1_rot", CubeListBuilder.create().texOffs(20, 56).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 5.5F, -3.5F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(18, 51).addBox(-2F, 4F, -3F, 4F, 3F, 1F, new CubeDeformation(0.75F)).texOffs(40, 45).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c4 = left_leg.addOrReplaceChild("pointe_genou_droit_1_rot", CubeListBuilder.create().texOffs(20, 56).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0F)), PartPose.offset(0F, 5.5F, -3.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p17Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(54, 51).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(44, 45).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(28, 51).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 28).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(54, 51).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(44, 45).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(28, 51).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition p18Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1.1F)).texOffs(70, 26).addBox(-4F, -7F, -5.5F, 8F, 2F, 2F, new CubeDeformation(0.3F)).texOffs(0, 16).addBox(-1.5F, -10.5F, -4F, 3F, 2F, 8F, new CubeDeformation(0.2F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p18Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.9F)).texOffs(66, 16).addBox(-3F, 1F, -4F, 6F, 7F, 1F, new CubeDeformation(0.3F)).texOffs(20, 26).addBox(-2F, 2F, -5F, 4F, 5F, 1F, new CubeDeformation(0.1F)).texOffs(12, 33).addBox(-3.5F, 0F, -5.2F, 7F, 1F, 1F, new CubeDeformation(0.2F)).texOffs(12, 33).addBox(-3.5F, 8F, -5.2F, 7F, 1F, 1F, new CubeDeformation(0.2F)).texOffs(80, 16).addBox(2.5F, 1F, -5.2F, 1F, 7F, 1F, new CubeDeformation(0.2F)).texOffs(80, 16).addBox(-3.5F, 1F, -5.2F, 1F, 7F, 1F, new CubeDeformation(0.2F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(16, 26).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(16, 26).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c3 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(50, 26).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(0, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 26).addBox(-3F, -2F, -2F, 4F, 3F, 4F, new CubeDeformation(1.1F)).texOffs(72, 0).addBox(-6.3F, -6F, -4.5F, 9F, 2F, 9F, new CubeDeformation(0F)).texOffs(38, 16).addBox(-5.3F, -7.5F, -3.5F, 7F, 1F, 7F, new CubeDeformation(0F)).texOffs(108, 0).addBox(-3F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.7F)).texOffs(108, 16).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(28, 33).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c5 = right_arm.addOrReplaceChild("cristal_suspendu_gauche_rot", CubeListBuilder.create().texOffs(6, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-1.8F, -5F, -4.6F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 26).addBox(-1F, -2F, -2F, 4F, 3F, 4F, new CubeDeformation(1.1F)).texOffs(72, 0).addBox(-2.7F, -6F, -4.5F, 9F, 2F, 9F, new CubeDeformation(0F)).texOffs(38, 16).addBox(-1.7F, -7.5F, -3.5F, 7F, 1F, 7F, new CubeDeformation(0F)).texOffs(108, 0).addBox(-1F, 1F, -2F, 4F, 7F, 4F, new CubeDeformation(0.7F)).texOffs(108, 16).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(28, 33).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c6 = left_arm.addOrReplaceChild("cristal_suspendu_droit_rot", CubeListBuilder.create().texOffs(6, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(1.8F, -5F, -4.6F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p18Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(84, 16).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(1.05F)).texOffs(100, 26).addBox(-1.5F, 9F, -3.3F, 3F, 3F, 1F, new CubeDeformation(1F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(118, 26).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(118, 26).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(118, 26).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(118, 26).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(118, 26).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(118, 26).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.55F)).texOffs(60, 26).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.8F)).texOffs(30, 26).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.55F)).texOffs(60, 26).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.8F)).texOffs(30, 26).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p18Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(22, 16).addBox(-2F, 6F, -2F, 4F, 6F, 4F, new CubeDeformation(1F)).texOffs(108, 26).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.1F)).texOffs(34, 26).addBox(-2F, 6F, -2F, 4F, 1F, 4F, new CubeDeformation(1.3F)).texOffs(90, 26).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(22, 16).addBox(-2F, 6F, -2F, 4F, 6F, 4F, new CubeDeformation(1F)).texOffs(108, 26).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.1F)).texOffs(34, 26).addBox(-2F, 6F, -2F, 4F, 1F, 4F, new CubeDeformation(1.3F)).texOffs(90, 26).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p19Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1F)).texOffs(0, 16).addBox(-4F, -10F, -4F, 8F, 2F, 8F, new CubeDeformation(1.15F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("couronne_pointe_1_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.6F, -9.5F, -4.6F, 0F, 0F, 0.2094F));
        PartDefinition c2 = head.addOrReplaceChild("couronne_pointe_2_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offset(0F, -9.5F, -4.9F));
        PartDefinition c3 = head.addOrReplaceChild("couronne_pointe_3_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.6F, -9.5F, -4.6F, 0F, 0F, -0.2094F));
        PartDefinition c4 = head.addOrReplaceChild("couronne_pointe_4_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.9F, -9.5F, 0F, 0F, 0F, -0.2094F));
        PartDefinition c5 = head.addOrReplaceChild("couronne_pointe_5_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.6F, -9.5F, 4.6F, 0F, 0F, -0.2094F));
        PartDefinition c6 = head.addOrReplaceChild("couronne_pointe_6_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offset(0F, -9.5F, 4.9F));
        PartDefinition c7 = head.addOrReplaceChild("couronne_pointe_7_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.6F, -9.5F, 4.6F, 0F, 0F, 0.2094F));
        PartDefinition c8 = head.addOrReplaceChild("couronne_pointe_8_rot", CubeListBuilder.create().texOffs(54, 33).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.9F, -9.5F, 0F, 0F, 0F, 0.2094F));
        PartDefinition c9 = head.addOrReplaceChild("couronne_joyau_rot", CubeListBuilder.create().texOffs(36, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, -10F, -5.1F, 0F, 0F, 0.7854F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p19Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.85F)).texOffs(32, 16).addBox(-4F, 1F, -3F, 8F, 9F, 1F, new CubeDeformation(0.6F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("cristal_royal_cadre_rot", CubeListBuilder.create().texOffs(68, 26).addBox(-2.5F, -2.5F, -0.5F, 5F, 5F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5F, -4.6F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("cristal_royal_rot", CubeListBuilder.create().texOffs(10, 33).addBox(-1.5F, -1.5F, -0.5F, 3F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5F, -5.1F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("fragment_flottant_1_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(11F, -3F, 2F, -0.3491F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("fragment_flottant_2_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(-11F, -3F, 2F, 0.3491F, 0F, 0.7854F));
        PartDefinition c5 = body.addOrReplaceChild("fragment_flottant_3_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(10F, 6F, 4F, 0F, 0F, 0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("fragment_flottant_4_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(-10F, 6F, 4F, 0F, 0F, 0.7854F));
        PartDefinition c7 = body.addOrReplaceChild("fragment_flottant_5_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -5F, 6F, 0F, 0F, 0.7854F));
        PartDefinition c8 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(20, 26).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c9 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(20, 26).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c10 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(110, 26).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c11 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(42, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-4.6F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(50, 16).addBox(-5.1F, -4.5F, -3.5F, 7F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(48, 26).addBox(-4.1F, -5.5F, -2.5F, 5F, 1F, 5F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(0, 26).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(58, 33).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(58, 33).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c12 = right_arm.addOrReplaceChild("cristal_epaule_gauche_rot", CubeListBuilder.create().texOffs(36, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-1.8F, -2.5F, -4.8F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-1.4F, -3F, -4F, 6F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(50, 16).addBox(-1.9F, -4.5F, -3.5F, 7F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(48, 26).addBox(-0.9F, -5.5F, -2.5F, 5F, 1F, 5F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.65F)).texOffs(0, 26).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(58, 33).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(58, 33).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c13 = left_arm.addOrReplaceChild("cristal_epaule_droit_rot", CubeListBuilder.create().texOffs(36, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(1.8F, -2.5F, -4.8F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p19Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(94, 16).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(0.5F)).texOffs(24, 26).addBox(-4F, 9F, -2F, 8F, 2F, 4F, new CubeDeformation(0.85F)).texOffs(48, 33).addBox(-1F, 9F, -3F, 2F, 2F, 1F, new CubeDeformation(0.9F)).texOffs(80, 26).addBox(-2F, 11F, -3F, 4F, 5F, 1F, new CubeDeformation(0.2F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(28, 33).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(28, 33).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(28, 33).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(90, 26).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c3 = right_leg.addOrReplaceChild("cristal_genou_gauche_rot", CubeListBuilder.create().texOffs(36, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -3.5F, 0F, 0F, 0.7854F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.5F)).texOffs(90, 26).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c4 = left_leg.addOrReplaceChild("cristal_genou_droit_rot", CubeListBuilder.create().texOffs(36, 33).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -3.5F, 0F, 0F, 0.7854F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p19Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(78, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(18, 33).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(94, 26).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 33).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(78, 16).addBox(-2F, 7F, -2F, 4F, 5F, 4F, new CubeDeformation(0.95F)).texOffs(18, 33).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.05F)).texOffs(94, 26).addBox(-2F, 7F, -2F, 4F, 1F, 4F, new CubeDeformation(1.25F)).texOffs(0, 33).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p20Head() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(1.05F)).texOffs(18, 16).addBox(-4F, -10F, -4F, 8F, 2F, 8F, new CubeDeformation(1.15F)), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c1 = head.addOrReplaceChild("couronne_pointe_1_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.6F, -9.5F, -4.6F, 0F, 0F, 0.2094F));
        PartDefinition c2 = head.addOrReplaceChild("couronne_pointe_2_rot", CubeListBuilder.create().texOffs(96, 27).addBox(-0.5F, -5F, -0.5F, 1F, 5F, 1F, new CubeDeformation(0.1F)), PartPose.offset(0F, -9.5F, -4.9F));
        PartDefinition c3 = head.addOrReplaceChild("couronne_pointe_3_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.6F, -9.5F, -4.6F, 0F, 0F, -0.2094F));
        PartDefinition c4 = head.addOrReplaceChild("couronne_pointe_4_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.9F, -9.5F, 0F, 0F, 0F, -0.2094F));
        PartDefinition c5 = head.addOrReplaceChild("couronne_pointe_5_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-4.6F, -9.5F, 4.6F, 0F, 0F, -0.2094F));
        PartDefinition c6 = head.addOrReplaceChild("couronne_pointe_6_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offset(0F, -9.5F, 4.9F));
        PartDefinition c7 = head.addOrReplaceChild("couronne_pointe_7_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.6F, -9.5F, 4.6F, 0F, 0F, 0.2094F));
        PartDefinition c8 = head.addOrReplaceChild("couronne_pointe_8_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(4.9F, -9.5F, 0F, 0F, 0F, 0.2094F));
        PartDefinition c9 = head.addOrReplaceChild("couronne_joyau_rot", CubeListBuilder.create().texOffs(72, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, -10F, -5.1F, 0F, 0F, 0.7854F));
        PartDefinition c10 = head.addOrReplaceChild("aile_couronne_droit_rot", CubeListBuilder.create().texOffs(106, 16).addBox(-0.5F, -5F, -2F, 1F, 5F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(4.8F, -7F, 0F, 0F, 0F, 0.5236F));
        PartDefinition c11 = head.addOrReplaceChild("aile_couronne_gauche_rot", CubeListBuilder.create().texOffs(106, 16).addBox(-0.5F, -5F, -2F, 1F, 5F, 4F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-4.8F, -7F, 0F, 0F, 0F, -0.5236F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p20Chest() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2F, 8F, 12F, 4F, new CubeDeformation(0.9F)).texOffs(0, 16).addBox(-4F, 1F, -3F, 8F, 10F, 1F, new CubeDeformation(0.45F)).texOffs(92, 16).addBox(-3F, 2F, -3.5F, 6F, 8F, 1F, new CubeDeformation(0.45F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("noyau_cadre_rot", CubeListBuilder.create().texOffs(68, 27).addBox(-3F, -3F, -0.5F, 6F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 5.5F, -5.3F, 0F, 0F, 0.7854F));
        PartDefinition c2 = body.addOrReplaceChild("noyau_rot", CubeListBuilder.create().texOffs(116, 27).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 5.5F, -5.8F, 0F, 0F, 0.7854F));
        PartDefinition c3 = body.addOrReplaceChild("griffe_noyau_1_rot", CubeListBuilder.create().texOffs(46, 34).addBox(-0.5F, -1.5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(3.8F, 1.8F, -5.2F, 0F, 0F, 0.7854F));
        PartDefinition c4 = body.addOrReplaceChild("griffe_noyau_2_rot", CubeListBuilder.create().texOffs(46, 34).addBox(-0.5F, -1.5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-3.8F, 1.8F, -5.2F, 0F, 0F, -0.7854F));
        PartDefinition c5 = body.addOrReplaceChild("griffe_noyau_3_rot", CubeListBuilder.create().texOffs(46, 34).addBox(-0.5F, -1.5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(3.8F, 9.2F, -5.2F, 0F, 0F, -0.7854F));
        PartDefinition c6 = body.addOrReplaceChild("griffe_noyau_4_rot", CubeListBuilder.create().texOffs(46, 34).addBox(-0.5F, -1.5F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-3.8F, 9.2F, -5.2F, 0F, 0F, 0.7854F));
        PartDefinition c7 = body.addOrReplaceChild("fragment_flottant_1_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(12F, -4F, 2F, -0.3491F, 0F, 0.7854F));
        PartDefinition c8 = body.addOrReplaceChild("fragment_flottant_2_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(-12F, -4F, 2F, 0.3491F, 0F, 0.7854F));
        PartDefinition c9 = body.addOrReplaceChild("fragment_flottant_3_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(11F, 5F, 4F, 0F, 0F, 0.7854F));
        PartDefinition c10 = body.addOrReplaceChild("fragment_flottant_4_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(-11F, 5F, 4F, 0F, 0F, 0.7854F));
        PartDefinition c11 = body.addOrReplaceChild("fragment_flottant_5_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(6F, -7F, 5F, 0F, 0F, 0.7854F));
        PartDefinition c12 = body.addOrReplaceChild("fragment_flottant_6_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(-6F, -7F, 5F, 0F, 0F, 0.7854F));
        PartDefinition c13 = body.addOrReplaceChild("fragment_flottant_7_rot", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 14F, 6F, 0F, 0F, 0.7854F));
        PartDefinition c14 = body.addOrReplaceChild("aeloria_gorget_droit", CubeListBuilder.create().texOffs(92, 27).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.4F, 2F, -3.1F, 0F, 0F, -0.3927F));
        PartDefinition c15 = body.addOrReplaceChild("aeloria_gorget_gauche", CubeListBuilder.create().texOffs(92, 27).addBox(-0.5F, -3F, -0.6F, 1F, 6F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.4F, 2F, -3.1F, 0F, 0F, 0.3927F));
        PartDefinition c16 = body.addOrReplaceChild("aeloria_clasp_cadre_rot", CubeListBuilder.create().texOffs(0, 34).addBox(-2F, -2F, -0.5F, 4F, 4F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(0F, 2F, -3.2F, 0F, 0F, 0.7854F));
        PartDefinition c17 = body.addOrReplaceChild("aeloria_clasp_rot", CubeListBuilder.create().texOffs(78, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, 2F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(88, 0).addBox(-5.1F, -3F, -4F, 7F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(66, 16).addBox(-4.6F, -4.5F, -3.5F, 6F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(0, 27).addBox(-4.6F, -5.5F, -3F, 6F, 1F, 6F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-3F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.7F)).texOffs(48, 27).addBox(-3F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(84, 34).addBox(-1.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(84, 34).addBox(-1.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(-5F, 2F, 0F));
        PartDefinition c18 = right_arm.addOrReplaceChild("aile_epaule_gauche_rot", CubeListBuilder.create().texOffs(72, 0).addBox(-0.5F, -5F, -3.5F, 1F, 5F, 7F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-3.5F, -3F, 0F, 0F, 0F, -0.5236F));
        PartDefinition c19 = right_arm.addOrReplaceChild("cristal_epaule_gauche_1_rot", CubeListBuilder.create().texOffs(50, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, -4.5F, 0F, 0F, 0F, 0.4363F));
        PartDefinition c20 = right_arm.addOrReplaceChild("cristal_epaule_gauche_2_rot", CubeListBuilder.create().texOffs(20, 34).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.25F)), PartPose.offset(-1.8F, -4.5F, 0F));
        PartDefinition c21 = right_arm.addOrReplaceChild("cristal_epaule_gauche_3_rot", CubeListBuilder.create().texOffs(50, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-3.6F, -4.5F, 0F, 0F, 0F, -0.4363F));
        PartDefinition c22 = right_arm.addOrReplaceChild("cristal_avant_bras_gauche_rot", CubeListBuilder.create().texOffs(72, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-1F, 4.5F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(88, 0).addBox(-1.9F, -3F, -4F, 7F, 3F, 8F, new CubeDeformation(0.3F)).texOffs(66, 16).addBox(-1.4F, -4.5F, -3.5F, 6F, 2F, 7F, new CubeDeformation(0.3F)).texOffs(0, 27).addBox(-1.4F, -5.5F, -3F, 6F, 1F, 6F, new CubeDeformation(0.25F)).texOffs(56, 0).addBox(-1F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.7F)).texOffs(48, 27).addBox(-1F, 8F, -3F, 4F, 1F, 6F, new CubeDeformation(0.12F)).texOffs(84, 34).addBox(0.5F, 6F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)).texOffs(84, 34).addBox(0.5F, 3F, -3.4F, 1F, 1F, 1F, new CubeDeformation(0F)), PartPose.offset(5F, 2F, 0F));
        PartDefinition c23 = left_arm.addOrReplaceChild("aile_epaule_droit_rot", CubeListBuilder.create().texOffs(72, 0).addBox(-0.5F, -5F, -3.5F, 1F, 5F, 7F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(3.5F, -3F, 0F, 0F, 0F, 0.5236F));
        PartDefinition c24 = left_arm.addOrReplaceChild("cristal_epaule_droit_1_rot", CubeListBuilder.create().texOffs(50, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(3.6F, -4.5F, 0F, 0F, 0F, 0.4363F));
        PartDefinition c25 = left_arm.addOrReplaceChild("cristal_epaule_droit_2_rot", CubeListBuilder.create().texOffs(20, 34).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F, new CubeDeformation(0.25F)), PartPose.offset(1.8F, -4.5F, 0F));
        PartDefinition c26 = left_arm.addOrReplaceChild("cristal_epaule_droit_3_rot", CubeListBuilder.create().texOffs(50, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0F, -4.5F, 0F, 0F, 0F, -0.4363F));
        PartDefinition c27 = left_arm.addOrReplaceChild("cristal_avant_bras_droit_rot", CubeListBuilder.create().texOffs(72, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(1F, 4.5F, -3.7F, 0F, 0F, 0.7854F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p20Legs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 27).addBox(-4F, 9F, -2F, 8F, 3F, 4F, new CubeDeformation(1.05F)).texOffs(34, 34).addBox(-1.5F, 9F, -3.3F, 3F, 3F, 1F, new CubeDeformation(1.05F)).texOffs(82, 27).addBox(-2F, 11F, -3F, 4F, 6F, 1F, new CubeDeformation(0.2F)), PartPose.offset(0F, 0F, 0F));
        PartDefinition c1 = body.addOrReplaceChild("aeloria_faulds_droit", CubeListBuilder.create().texOffs(64, 34).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(64, 34).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(64, 34).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(3F, 12F, -3F, 0F, 0F, -0.2094F));
        PartDefinition c2 = body.addOrReplaceChild("aeloria_faulds_gauche", CubeListBuilder.create().texOffs(64, 34).addBox(-1.5F, 0F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(64, 34).addBox(-1.5F, 1.4F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)).texOffs(64, 34).addBox(-1.5F, 2.8F, -0.2F, 3F, 2F, 1F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-3F, 12F, -3F, 0F, 0F, 0.2094F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.55F)).texOffs(10, 34).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.8F)).texOffs(96, 27).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c3 = right_leg.addOrReplaceChild("cristal_genou_gauche_rot", CubeListBuilder.create().texOffs(72, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -4.1F, 0F, 0F, 0.7854F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(56, 0).addBox(-2F, 0F, -2F, 4F, 8F, 4F, new CubeDeformation(0.55F)).texOffs(10, 34).addBox(-2F, 3F, -3F, 4F, 4F, 1F, new CubeDeformation(0.8F)).texOffs(96, 27).addBox(-0.5F, 4F, -3.1F, 1F, 5F, 1F, new CubeDeformation(0F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c4 = left_leg.addOrReplaceChild("cristal_genou_droit_rot", CubeListBuilder.create().texOffs(72, 34).addBox(-1F, -1F, -0.5F, 2F, 2F, 1F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0F, 5.5F, -4.1F, 0F, 0F, 0.7854F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static LayerDefinition p20Feet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5F, 2F, 0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(50, 16).addBox(-2F, 6F, -2F, 4F, 6F, 4F, new CubeDeformation(1F)).texOffs(54, 34).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.1F)).texOffs(100, 27).addBox(-2F, 6F, -2F, 4F, 1F, 4F, new CubeDeformation(1.3F)).texOffs(24, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition c1 = right_leg.addOrReplaceChild("pointe_botte_gauche_1_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(-2.2F, 9F, 0F, 0F, 0F, -0.5236F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(50, 16).addBox(-2F, 6F, -2F, 4F, 6F, 4F, new CubeDeformation(1F)).texOffs(54, 34).addBox(-2F, 10F, -3F, 4F, 2F, 1F, new CubeDeformation(1.1F)).texOffs(100, 27).addBox(-2F, 6F, -2F, 4F, 1F, 4F, new CubeDeformation(1.3F)).texOffs(24, 34).addBox(-1.5F, 9.8F, -3.8F, 3F, 2F, 2F, new CubeDeformation(0.15F)), PartPose.offset(1.9F, 12F, 0F));
        PartDefinition c2 = left_leg.addOrReplaceChild("pointe_botte_droit_1_rot", CubeListBuilder.create().texOffs(42, 34).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)), PartPose.offsetAndRotation(2.2F, 9F, 0F, 0F, 0F, 0.5236F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
