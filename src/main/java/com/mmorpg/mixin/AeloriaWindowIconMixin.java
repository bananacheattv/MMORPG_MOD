package com.mmorpg.mixin;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.InputStream;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Window.class)
public abstract class AeloriaWindowIconMixin {
 @ModifyVariable(method="setIcon(Ljava/util/List;)V",at=@At("HEAD"),argsOnly=true)
 private List<IoSupplier<InputStream>> aeloria$icon(List<IoSupplier<InputStream>> original){return List.of(()->com.mmorpg.MMORPG.class.getResourceAsStream("/assets/mmorpg/textures/gui/logo/emblem_256.png"),()->com.mmorpg.MMORPG.class.getResourceAsStream("/assets/mmorpg/textures/gui/logo/emblem_128.png"),()->com.mmorpg.MMORPG.class.getResourceAsStream("/assets/mmorpg/textures/gui/logo/emblem_64.png"));}
}
