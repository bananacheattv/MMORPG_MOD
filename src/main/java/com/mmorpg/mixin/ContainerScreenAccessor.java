package com.mmorpg.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position de l'interface d'un conteneur (recherche et tri d'inventaire). */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int aeloria$left();
    @Accessor("topPos") int aeloria$top();
}
