package com.mmorpg.mixin;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Holding Shift and dragging over stacks performs native QUICK_MOVE once per visited slot. */
@Mixin(AbstractContainerScreen.class)
public abstract class ShiftDragTransferMixin extends Screen {
 protected ShiftDragTransferMixin(Component title){super(title);}
 @Shadow @Final protected AbstractContainerMenu menu;
 @Shadow protected abstract void slotClicked(Slot slot,int index,int button,ContainerInput input);
 @Shadow private Slot getHoveredSlot(double x,double y){throw new AssertionError();}
 @Unique private final java.util.Set<Integer> aeloria$visited=new java.util.HashSet<>();
 @Inject(method="mouseClicked",at=@At("HEAD")) private void aeloria$begin(MouseButtonEvent e,boolean twice,CallbackInfoReturnable<Boolean> ci){aeloria$visited.clear();if(e.hasShiftDown()){var slot=getHoveredSlot(e.x(),e.y());if(slot!=null)aeloria$visited.add(slot.index);}}
 @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true) private void aeloria$transfer(MouseButtonEvent e,double dx,double dy,CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof CreativeModeInventoryScreen||e.button()!=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT||!e.hasShiftDown()||!menu.getCarried().isEmpty())return;var slot=getHoveredSlot(e.x(),e.y());if(slot!=null&&slot.hasItem()&&slot.mayPickup(minecraft.player)&&aeloria$visited.add(slot.index)){slotClicked(slot,slot.index,0,ContainerInput.QUICK_MOVE);ci.setReturnValue(true);}}
 @Inject(method="mouseReleased",at=@At("HEAD")) private void aeloria$end(MouseButtonEvent e,CallbackInfoReturnable<Boolean> ci){aeloria$visited.clear();}
}
