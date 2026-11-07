package com.mmorpg.client.model.imported;

import com.google.gson.JsonParser;
import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** /skin uses the vanilla decoder (including legacy 64x32 skins) and the player's actual skin accessor. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class SkinOverride {
    private static final SkinLoadCache<PlayerSkin> CACHE = new SkinLoadCache<>(System::currentTimeMillis, 30_000, 600_000);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final String API = System.getProperty("mmorpg.mojangapi", "https://api.mojang.com");
    private static final String SESSION = System.getProperty("mmorpg.mojangsession", "https://sessionserver.mojang.com");
    private static String lastFailure = "", observedSpec = "";

    private SkinOverride() { }

    private static String key(String spec) {
        return spec.startsWith("http://") || spec.startsWith("https://") ? spec : spec.toLowerCase(Locale.ROOT);
    }

    /** Applied before renderer/model selection: slim arms, first-person hands and entity previews agree. */
    public static PlayerSkin forPlayer(AbstractClientPlayer player, PlayerSkin original) {
        var pub = player.getExistingDataOrNull(ModAttachments.PUBLIC);
        if (pub == null || pub.skin == null || pub.skin.isBlank()) return original;
        PlayerSkin chosen = resolve(pub.skin);
        return chosen == null ? original : new PlayerSkin(chosen.body(), original.cape(), original.elytra(), chosen.model(), false);
    }

    private static PlayerSkin resolve(String spec) {
        String key = key(spec);
        return CACHE.get(key, () -> location(key).thenCompose(location -> download(key, location)).whenComplete((skin, error) -> {
            if (error == null) return;
            MMORPG.LOGGER.warn("Impossible de charger le skin demandé : {}", error.getMessage());
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                var pub = player == null ? null : player.getExistingDataOrNull(ModAttachments.PUBLIC);
                if (pub != null && pub.skin != null && key(pub.skin).equals(key) && !lastFailure.equals(key)) {
                    lastFailure = key;
                    player.sendSystemMessage(Component.literal("Skin indisponible : vérifie le pseudo ou le lien direct vers un PNG 64×64 ou 64×32. Nouvelle tentative automatique dans 30 s."));
                }
            });
        }));
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        var pub = player == null ? null : player.getExistingDataOrNull(ModAttachments.PUBLIC);
        String spec = pub == null || pub.skin == null ? "" : pub.skin;
        if (!spec.equals(observedSpec)) { observedSpec = spec; lastFailure = ""; }
        if (!spec.isBlank()) resolve(spec);
    }

    private static CompletableFuture<String> getText(String url) {
        return HTTP.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> { if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode()); return r.body(); });
    }

    private record Location(String url, PlayerModelType model) { }

    private static CompletableFuture<Location> location(String spec) {
        if (spec.startsWith("http://") || spec.startsWith("https://"))
            return CompletableFuture.completedFuture(new Location(spec, PlayerModelType.WIDE));
        return getText(API + "/users/profiles/minecraft/" + spec)
                .thenCompose(body -> getText(SESSION + "/session/minecraft/profile/" + JsonParser.parseString(body).getAsJsonObject().get("id").getAsString()))
                .thenApply(body -> {
                    for (var value : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("properties")) {
                        var property = value.getAsJsonObject();
                        if (!"textures".equals(property.get("name").getAsString())) continue;
                        var textures = JsonParser.parseString(new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8))
                                .getAsJsonObject().getAsJsonObject("textures");
                        if (textures == null || !textures.has("SKIN")) break;
                        var skin = textures.getAsJsonObject("SKIN");
                        boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                                && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
                        String url = skin.get("url").getAsString().replace("http://textures.minecraft.net/", "https://textures.minecraft.net/");
                        return new Location(url, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
                    }
                    throw new IllegalStateException("Ce profil n'a pas de skin");
                });
    }

    private static CompletableFuture<PlayerSkin> download(String key, Location location) {
        var mc = Minecraft.getInstance();
        return CompletableFuture.supplyAsync(() -> {
            try {
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
                var file = mc.gameDirectory.toPath().resolve("cache/aeloria-skins/" + hash + ".png");
                // Bound both download size and wait time before handing the PNG to the vanilla decoder.
                // Vanilla's downloader otherwise has no read timeout or size limit for arbitrary URLs.
                URI uri = URI.create(location.url);
                byte[] bytes = null;
                for (int redirect = 0; redirect < 6; redirect++) {
                    if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) throw new IllegalArgumentException("Lien HTTP(S) requis");
                    var connection = (HttpURLConnection) uri.toURL().openConnection(mc.getProxy());
                    connection.setConnectTimeout(8_000);
                    connection.setReadTimeout(15_000);
                    connection.setInstanceFollowRedirects(false);
                    try {
                        int status = connection.getResponseCode();
                        if (status >= 300 && status < 400 && connection.getHeaderField("Location") != null) {
                            uri = uri.resolve(connection.getHeaderField("Location"));
                            continue;
                        }
                        if (status != 200) throw new IllegalStateException("HTTP " + status);
                        try (var input = connection.getInputStream()) { bytes = input.readNBytes(1_000_001); }
                        break;
                    } finally { connection.disconnect(); }
                }
                if (bytes == null || bytes.length > 1_000_000 || bytes.length < 24) throw new IllegalArgumentException("PNG absent ou trop volumineux");
                var header = java.nio.ByteBuffer.wrap(bytes);
                if (header.getLong() != 0x89504E470D0A1A0AL || header.getInt(16) != 64
                        || (header.getInt(20) != 64 && header.getInt(20) != 32)) throw new IllegalArgumentException("PNG 64x64 ou 64x32 requis");
                Files.createDirectories(file.getParent());
                Files.write(file, bytes);
                return file;
            } catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
        }).thenCompose(file -> new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc)
                .downloadAndRegisterSkin(MMORPG.id("skin_url/" + file.getFileName().toString().replace(".png", "")), file, location.url, true))
                .thenApply(texture -> PlayerSkin.insecure(texture, null, null, location.model));
    }
}
