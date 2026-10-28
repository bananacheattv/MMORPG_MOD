package com.mmorpg.mixin;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Slot.class)
public interface SlotPositionAccessor {
 @Mutable @Accessor("x") void aeloria$x(int value);
 @Mutable @Accessor("y") void aeloria$y(int value);
}
