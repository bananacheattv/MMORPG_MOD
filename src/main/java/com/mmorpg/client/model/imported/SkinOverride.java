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

    private static final String API = System.getProperty("mmorpg.mojangapi", "https://api.mojang.com");
    private static final String SESSION = System.getProperty("mmorpg.mojangsession", "https://sessionserver.mojang.com");

    private static PlayerSkin resolve(String spec) {
        var s = URL_SKINS.get(spec);
        if (s == null && LOADING.putIfAbsent(spec, Boolean.TRUE) == null) {
            if (spec.startsWith("http://") || spec.startsWith("https://")) loadPng(spec, spec, PlayerModelType.WIDE);
            else lookupPseudo(spec);
        }
        return s;
    }

    private static HttpClient http() {
        if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
        return http;
    }

    private static java.util.concurrent.CompletableFuture<String> getText(String url) {
        return http().sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode() + " " + url);return r.body();});
    }

    /** pseudo -> UUID (api.mojang.com) -> profil (sessionserver) -> textures.SKIN.url -> PNG. */
    private static void lookupPseudo(String name) {
        getText(API + "/users/profiles/minecraft/" + name)
                .thenCompose(b -> getText(SESSION + "/session/minecraft/profile/" + com.google.gson.JsonParser.parseString(b).getAsJsonObject().get("id").getAsString()))
                .thenAccept(b -> {
                    for (var e : com.google.gson.JsonParser.parseString(b).getAsJsonObject().getAsJsonArray("properties")) {
                        var o = e.getAsJsonObject();
                        if (!"textures".equals(o.get("name").getAsString())) continue;
                        var tex = com.google.gson.JsonParser.parseString(new String(java.util.Base64.getDecoder().decode(o.get("value").getAsString()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("textures");
                        if (tex == null || !tex.has("SKIN")) throw new IllegalStateException("pas de skin");
                        var sk = tex.getAsJsonObject("SKIN");
                        boolean slim = sk.has("metadata") && sk.getAsJsonObject("metadata").has("model") && "slim".equals(sk.getAsJsonObject("metadata").get("model").getAsString());
                        loadPng(name, sk.get("url").getAsString(), slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
                        return;
                    }
                    throw new IllegalStateException("pas de textures");
                }).exceptionally(e -> {MMORPG.LOGGER.warn("Skin introuvable pour le pseudo {}: {}", name, e.toString());return null;});
    }

    private static void loadPng(String key, String url, PlayerModelType model) {
        http().sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(r -> {
                    try {
                        byte[] b = r.body();
                        if (r.statusCode() != 200 || b.length > 1_000_000) throw new IllegalStateException("HTTP " + r.statusCode());
                        NativeImage img = NativeImage.read(new ByteArrayInputStream(b));
                        if (img.getWidth() != 64 || img.getHeight() != 64) {img.close();throw new IllegalStateException("image 64x64 requise");}
                        Minecraft.getInstance().execute(() -> {
                            var id = Identifier.fromNamespaceAndPath(MMORPG.MODID, "skin_url/" + Integer.toHexString(key.hashCode()) + Long.toHexString(System.nanoTime()));
                            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> id.toString(), img));
                            URL_SKINS.put(key, new PlayerSkin(new ClientAsset.ResourceTexture(id), null, null, model, true));
                        });
                    } catch (Exception e) {
                        MMORPG.LOGGER.warn("Skin illisible ({}): {}", url, e.toString());
                    }
                }).exceptionally(e -> {MMORPG.LOGGER.warn("Skin inaccessible ({}): {}", url, e.toString());return null;});
    }
}
