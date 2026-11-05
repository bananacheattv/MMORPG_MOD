package com.mmorpg.client.model.imported;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModAttachments;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Applique le skin choisi avec /skin (pseudo Minecraft ou URL d'image PNG 64x64) a un joueur, pour tous les clients. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class SkinOverride {
    private static final Map<String, PlayerSkin> URL_SKINS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static HttpClient http;

    private SkinOverride() {}

    @SubscribeEvent public static void states(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
                var pub = entity.getExistingDataOrNull(ModAttachments.PUBLIC);
                if (pub == null || pub.skin == null || pub.skin.isEmpty()) return;
                var skin = resolve(pub.skin);
                if (skin != null) state.skin = skin;
            }
        });
    }

    private static PlayerSkin resolve(String spec) {
        if (spec.startsWith("http://") || spec.startsWith("https://")) {
            var s = URL_SKINS.get(spec);
            if (s == null && LOADING.putIfAbsent(spec, Boolean.TRUE) == null) download(spec);
            return s;
        }
        try {
            return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(ResolvableProfile.createUnresolved(spec)).playerSkin();
        } catch (Exception e) {
            return null;
        }
    }

    private static void download(String url) {
        if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
        http.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(r -> {
                    try {
                        byte[] b = r.body();
                        if (r.statusCode() != 200 || b.length > 1_000_000) throw new IllegalStateException("HTTP " + r.statusCode());
                        NativeImage img = NativeImage.read(new ByteArrayInputStream(b));
                        if (img.getWidth() != 64 || img.getHeight() != 64) {img.close();throw new IllegalStateException("image 64x64 requise");}
                        Minecraft.getInstance().execute(() -> {
                            var id = Identifier.fromNamespaceAndPath(MMORPG.MODID, "skin_url/" + Integer.toHexString(url.hashCode()) + Long.toHexString(System.nanoTime()));
                            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> id.toString(), img));
                            URL_SKINS.put(url, new PlayerSkin(new ClientAsset.ResourceTexture(id), null, null, PlayerModelType.WIDE, true));
                        });
                    } catch (Exception e) {
                        MMORPG.LOGGER.warn("Skin URL illisible ({}): {}", url, e.toString());
                    }
                }).exceptionally(e -> {MMORPG.LOGGER.warn("Skin URL inaccessible ({}): {}", url, e.toString());return null;});
    }
}
