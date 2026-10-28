package com.mmorpg.client.ui;
import com.mmorpg.MMORPG;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.platform.NativeImage;
public final class LoadingBrand {
 public static final Identifier LOGO=MMORPG.id("textures/gui/brand/early_logo.png");
 public static void register(TextureManager manager){manager.registerAndLoad(LOGO,new ReloadableTexture(LOGO){@Override public TextureContents loadContents(ResourceManager resources)throws java.io.IOException{try(var in=MMORPG.class.getResourceAsStream("/assets/mmorpg/textures/gui/logo/logo_512.png")){if(in==null)throw new java.io.IOException("Aeloria logo missing");return new TextureContents(NativeImage.read(in),null);}}});}
}
