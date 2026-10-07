package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MMORPG.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT = TABS.register("equipement", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mmorpg.equipement"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> new ItemStack(ModItems.get("excalibur_celeste")))
            .displayItems((params, out) -> {
                ModItems.WEAPONS.forEach(i -> out.accept(i.get()));
                ModItems.ARMORS.forEach(i -> out.accept(i.get()));
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OBJECTS = TABS.register("objets", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mmorpg.objets"))
            .withTabsBefore(EQUIPMENT.getKey())
            .icon(() -> new ItemStack(ModItems.FRAGMENT_DIVIN.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.FORGE_ARCANIQUE.get());
                out.accept(ModItems.LUCKY_BLOCK.get());
                ModItems.CRATE_BLOCKS.forEach(i -> out.accept(i.get()));
                out.accept(ModItems.TELEPORTEUR.get());
                out.accept(ModItems.AUTEL_INVOCATION.get());
                ModItems.MATERIALS.forEach(i -> out.accept(i.get()));
                out.accept(ModItems.PIECE_OR.get());
                ModItems.CONSUMABLES.forEach(i -> out.accept(i.get()));
                ModItems.SUMMONS.forEach(i -> out.accept(i.get()));
                ModItems.PET_EGGS.values().forEach(i -> out.accept(i.get()));
                ModItems.SPAWN_EGGS.forEach(i -> out.accept(i.get()));
            })
            .build());

    private ModCreativeTabs() {
    }
}
