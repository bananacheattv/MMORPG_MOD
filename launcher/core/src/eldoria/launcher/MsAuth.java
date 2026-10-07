package eldoria.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Connexion Microsoft (code d'appareil) -> Xbox Live -> XSTS -> compte Minecraft. */
final class MsAuth {
    static final String SCOPE = "XboxLive.signin offline_access";
    private static final String MS = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";

    /** Compte pret a jouer. */
    record Account(String name, String uuid, String token, String xuid, String type) {
        static Account offline(String name) {
            String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .toString().replace("-", "");
            return new Account(name, uuid, "0", "0", "legacy");
        }
    }

    private final String clientId;
    private final Path store;

    MsAuth(String clientId, Path store) {
        this.clientId = clientId;
        this.store = store;
    }

    /** Reconnexion silencieuse avec le jeton enregistre ; null si impossible. */
    Account refresh() {
        try {
            if (!Files.exists(store)) return null;
            String refresh = Json.str(Json.obj(Json.parse(Files.readString(store))).get("refresh_token"), null);
            if (refresh == null) return null;
            Object[] r = Net.post(MS + "token", "application/x-www-form-urlencoded", Net.form(Map.of(
                    "client_id", clientId, "grant_type", "refresh_token", "refresh_token", refresh, "scope", SCOPE)));
            if ((int) r[0] != 200) return null;
            return finish(Json.obj(Json.parse((String) r[1])));
        } catch (Exception e) {
            return null;
        }
    }

    /** Connexion interactive : {@code showCode} recoit (url, code) a afficher au joueur. */
    Account login(java.util.function.BiConsumer<String, String> showCode, Consumer<String> log) throws Exception {
        Object[] r = Net.post(MS + "devicecode", "application/x-www-form-urlencoded",
                Net.form(Map.of("client_id", clientId, "scope", SCOPE)));
        if ((int) r[0] != 200) throw new IllegalStateException("Connexion Microsoft refusee : " + r[1]);
        Map<String, Object> dc = Json.obj(Json.parse((String) r[1]));
        String device = Json.str(dc.get("device_code"), "");
        showCode.accept(Json.str(dc.get("verification_uri"), "https://microsoft.com/link"), Json.str(dc.get("user_code"), "?"));
        long interval = (long) Double.parseDouble(Json.str(dc.get("interval"), "5"));
        long end = System.currentTimeMillis() + (long) Double.parseDouble(Json.str(dc.get("expires_in"), "900")) * 1000;
        while (System.currentTimeMillis() < end) {
            Thread.sleep(interval * 1000);
            Object[] t = Net.post(MS + "token", "application/x-www-form-urlencoded", Net.form(Map.of(
                    "client_id", clientId, "grant_type", "urn:ietf:params:oauth:grant-type:device_code", "device_code", device)));
            Map<String, Object> body = Json.obj(Json.parse((String) t[1]));
            if ((int) t[0] == 200) return finish(body);
            String err = Json.str(body.get("error"), "");
            if (err.equals("slow_down")) interval += 5;
            else if (!err.equals("authorization_pending")) throw new IllegalStateException("Connexion Microsoft : " + err);
        }
        throw new IllegalStateException("Code expire, recommence la connexion.");
    }

    void logout() throws Exception { Files.deleteIfExists(store); }

    private Account finish(Map<String, Object> ms) throws Exception {
        Map<String, Object> save = new LinkedHashMap<>();
        save.put("refresh_token", ms.get("refresh_token"));
        Files.createDirectories(store.getParent());
        Files.writeString(store, Json.write(save));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("AuthMethod", "RPS");
        props.put("SiteName", "user.auth.xboxlive.com");
        props.put("RpsTicket", "d=" + ms.get("access_token"));
        Map<String, Object> xbl = Net.postJson("https://user.auth.xboxlive.com/user/authenticate",
                Map.of("Properties", props, "RelyingParty", "http://auth.xboxlive.com", "TokenType", "JWT"));
        String uhs = Json.str(Json.obj(Json.arr(Json.obj(xbl.get("DisplayClaims")).get("xui")).get(0)).get("uhs"), "");

        Object[] r = Net.post("https://xsts.auth.xboxlive.com/xsts/authorize", "application/json", Json.write(Map.of(
                "Properties", Map.of("SandboxId", "RETAIL", "UserTokens", List.of(xbl.get("Token"))),
                "RelyingParty", "rp://api.minecraftservices.com/", "TokenType", "JWT")));
        if ((int) r[0] != 200) {
            String x = Json.str(Json.obj(Json.parse((String) r[1])).get("XErr"), "");
            throw new IllegalStateException(switch (x) {
                case "2148916233" -> "Ce compte Microsoft n'a pas de profil Xbox (cree-en un sur xbox.com).";
                case "2148916238" -> "Compte enfant : il doit etre ajoute a une famille Microsoft.";
                default -> "Echec Xbox Live (XSTS " + x + ")";
            });
        }
        Map<String, Object> xsts = Json.obj(Json.parse((String) r[1]));
        Map<String, Object> xui = Json.obj(Json.arr(Json.obj(xsts.get("DisplayClaims")).get("xui")).get(0));

        Map<String, Object> mc = Net.postJson("https://api.minecraftservices.com/authentication/login_with_xbox",
                Map.of("identityToken", "XBL3.0 x=" + uhs + ";" + xsts.get("Token")));
        String token = Json.str(mc.get("access_token"), "");
        Map<String, Object> profile;
        try {
            profile = Net.getJson("https://api.minecraftservices.com/minecraft/profile", token);
        } catch (Exception e) {
            throw new IllegalStateException("Ce compte ne possede pas Minecraft Java Edition.");
        }
        return new Account(Json.str(profile.get("name"), "?"), Json.str(profile.get("id"), ""), token,
                Json.str(xui.get("xid"), "0"), "msa");
    }
}
