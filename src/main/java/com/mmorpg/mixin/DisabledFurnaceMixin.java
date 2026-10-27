package com.mmorpg.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla furnaces, smokers and blast furnaces cannot bypass the forge through hopper automation. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class DisabledFurnaceMixin {
    @Inject(method="serverTick",at=@At("HEAD"),cancellable=true)
    private static void aeloria$disableSmelting(ServerLevel level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo callback){
        if(state.getValue(AbstractFurnaceBlock.LIT))level.setBlock(pos,state.setValue(AbstractFurnaceBlock.LIT,false),2);
        callback.cancel();
    }
}
