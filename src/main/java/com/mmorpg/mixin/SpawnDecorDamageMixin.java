package com.mmorpg.mixin;
import com.mmorpg.hub.SpawnDecorProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ArmorStand.class,ItemFrame.class})
public abstract class SpawnDecorDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void aeloria$protect(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){if(SpawnDecorProtection.blocked(source.getEntity(),(Entity)(Object)this))ci.setReturnValue(false);}
}
