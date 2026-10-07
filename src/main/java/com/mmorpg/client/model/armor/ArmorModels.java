package com.mmorpg.client.model.armor;

import com.mmorpg.MMORPG;
import com.mmorpg.item.RpgArmorItem;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Armures 3D (panoplies Blockbench, voir ArmorLayers) : chaque piece remplace le modele d'armure vanilla par le
 * modele de sa panoplie pour son emplacement ; la texture est celle de la panoplie (textures/entity/armor/).
 * Le modele est un HumanoidModel : il suit les mouvements du porteur (marche, accroupi, bras...).
 */
public final class ArmorModels {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<String, HumanoidModel<HumanoidRenderState>> MODELS = new HashMap<>();
    private static EntityModelSet bakedFrom;
    /**
     * Sets sans entree dans ArmorLayers.SET_PANOPLY : panoplie 3D utilisee et texture (une texture recoloree propre au set,
     * generee par tools/gen_equipment.py, ou la texture d'origine d'une panoplie libre).
     */
    public static final Map<String, String[]> SET_VARIANTS = Map.ofEntries(
            Map.entry("veteran", new String[]{"panoplie_05_niv_020", "veteran"}),
            Map.entry("croise", new String[]{"panoplie_10_niv_045", "panoplie_10_niv_045"}),
            Map.entry("conquerant", new String[]{"panoplie_15_niv_070", "panoplie_15_niv_070"}),
            Map.entry("seigneur_dragon", new String[]{"panoplie_16_niv_075", "panoplie_16_niv_075"}),
            Map.entry("valkyrie", new String[]{"panoplie_20_niv_100", "valkyrie"}),
            Map.entry("acolyte", new String[]{"panoplie_01_niv_001", "acolyte"}),
            Map.entry("enchanteur", new String[]{"panoplie_08_niv_035", "enchanteur"}),
            Map.entry("mage_bataille", new String[]{"panoplie_11_niv_050", "mage_bataille"}),
            Map.entry("oracle_astral", new String[]{"panoplie_14_niv_065", "oracle_astral"}),
            Map.entry("eternel_arcanes", new String[]{"panoplie_20_niv_100", "eternel_arcanes"}),
            Map.entry("eclaireur", new String[]{"panoplie_02_niv_005", "eclaireur"}),
            Map.entry("traqueur", new String[]{"panoplie_03_niv_010", "traqueur"}),
            Map.entry("lame_ombre", new String[]{"panoplie_09_niv_040", "lame_ombre"}),
            Map.entry("chasseur_lunaire", new String[]{"panoplie_17_niv_080", "chasseur_lunaire"}),
            Map.entry("sentinelle_astrale", new String[]{"panoplie_20_niv_100", "sentinelle_astrale"}),
            Map.entry("sentinelle_fer", new String[]{"panoplie_04_niv_015", "sentinelle_fer"}),
            Map.entry("rempart", new String[]{"panoplie_07_niv_030", "rempart"}),
            Map.entry("croise_sacre", new String[]{"panoplie_13_niv_060", "croise_sacre"}),
            Map.entry("bastion_eternel", new String[]{"panoplie_18_niv_085", "bastion_eternel"}),
            Map.entry("egide_divine", new String[]{"panoplie_20_niv_100", "egide_divine"}));

    private ArmorModels() {
    }

    private static ModelLayerLocation layer(String panoply, EquipmentSlot slot) {
        return new ModelLayerLocation(MMORPG.id("armor/" + panoply), slot.getName());
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LinkedHashSet<String> panoplies = new LinkedHashSet<>(ArmorLayers.SET_PANOPLY.values());
        for (String[] v : SET_VARIANTS.values()) panoplies.add(v[0]);
        for (String panoply : panoplies) {
            for (EquipmentSlot slot : SLOTS) {
                event.registerLayerDefinition(layer(panoply, slot), () -> ArmorLayers.create(panoply, slot));
            }
        }
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (DeferredItem<? extends Item> holder : ModItems.ARMORS) {
            if (!(holder.get() instanceof RpgArmorItem armor)) continue;
            String setId = armor.rpgDef().setId();
            String[] variant = SET_VARIANTS.get(setId);
            String panoply = variant != null ? variant[0] : ArmorLayers.SET_PANOPLY.get(setId);
            if (panoply == null) continue;
            Identifier texture = MMORPG.id("textures/entity/armor/" + (variant != null ? variant[1] : panoply) + ".png");
            event.registerItem(new IClientItemExtensions() {
                @Override
                @SuppressWarnings("rawtypes")
                public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original) {
                    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                    if (type == EquipmentClientInfo.LayerType.HUMANOID_BABY || equippable == null) return original;
                    return model(panoply, equippable.slot());
                }

                @Override
                public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier fallback) {
                    return type == EquipmentClientInfo.LayerType.HUMANOID_BABY ? fallback : texture;
                }
            }, armor);
        }
    }

    private static HumanoidModel<HumanoidRenderState> model(String panoply, EquipmentSlot slot) {
        EntityModelSet set = Minecraft.getInstance().getEntityModels();
        if (set != bakedFrom) {                 // rechargement des ressources : modeles a recuire
            MODELS.clear();
            bakedFrom = set;
        }
        return MODELS.computeIfAbsent(panoply + "/" + slot.getName(), k -> new HumanoidModel<>(set.bakeLayer(layer(panoply, slot))));
    }
}
