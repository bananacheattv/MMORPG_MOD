package com.mmorpg.mixin;
import com.mmorpg.server.CreativeAccess;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public abstract class CreativeAccessMixin {
 @Shadow @Final protected ServerPlayer player;
 @Inject(method="changeGameModeForPlayer",at=@At("HEAD"),cancellable=true)
 private void aeloria$operatorCreative(GameType type,CallbackInfoReturnable<Boolean> ci){if(type==GameType.CREATIVE&&!CreativeAccess.allowed(player))ci.setReturnValue(false);}
}
