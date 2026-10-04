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

    private ArmorModels() {
    }

    private static ModelLayerLocation layer(String panoply, EquipmentSlot slot) {
        return new ModelLayerLocation(MMORPG.id("armor/" + panoply), slot.getName());
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String panoply : new LinkedHashSet<>(ArmorLayers.SET_PANOPLY.values())) {
            for (EquipmentSlot slot : SLOTS) {
                event.registerLayerDefinition(layer(panoply, slot), () -> ArmorLayers.create(panoply, slot));
            }
        }
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (DeferredItem<? extends Item> holder : ModItems.ARMORS) {
            if (!(holder.get() instanceof RpgArmorItem armor)) continue;
            String panoply = ArmorLayers.SET_PANOPLY.get(armor.rpgDef().setId());
            if (panoply == null) continue;
            Identifier texture = MMORPG.id("textures/entity/armor/" + panoply + ".png");
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
