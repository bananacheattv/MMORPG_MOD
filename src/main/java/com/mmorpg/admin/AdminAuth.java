package com.mmorpg.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Acces au menu d'administration : mot de passe (config/mmorpg/admin.json, jamais en clair : PBKDF2 sale), sessions de joueurs
 * authentifies (jusqu'a la deconnexion) et blocage temporaire apres des essais rates. Au premier demarrage un mot de passe
 * aleatoire est genere et affiche une seule fois dans la console du serveur.
 */
public final class AdminAuth {
    private AdminAuth() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int ITERATIONS = 150_000;
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 10 * 60 * 1000L;

    private static final class Stored {
        String salt = "";
        String hash = "";
        int iterations = ITERATIONS;
    }

    private static final Set<UUID> SESSIONS = new HashSet<>();
    private static final Map<UUID, int[]> FAILS = new HashMap<>();
    private static final Map<UUID, Long> LOCKED = new HashMap<>();
    private static Stored stored;

    private static Path file() {
        return ConfigManager.dir().resolve("admin.json");
    }

    private static byte[] hash(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    /** Charge le fichier du mot de passe ; s'il n'existe pas, en genere un et l'annonce dans la console. */
    public static synchronized void load() {
        try {
            Files.createDirectories(ConfigManager.dir());
            if (Files.exists(file())) {
                stored = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), Stored.class);
                if (stored != null && !stored.hash.isEmpty()) return;
            }
            String chars = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            SecureRandom rnd = new SecureRandom();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 14; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
            setPassword(sb.toString());
            MMORPG.LOGGER.warn("[MMORPG] ADMIN : mot de passe du menu d'administration genere : {}  (notez-le puis changez-le avec /adminpass <nouveau> ; il ne sera plus affiche)", sb);
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] Impossible de lire config/mmorpg/admin.json : le menu d'administration est desactive", e);
            stored = null;
        }
    }

    public static synchronized void setPassword(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        Stored s = new Stored();
        s.salt = Base64.getEncoder().encodeToString(salt);
        s.iterations = ITERATIONS;
        s.hash = Base64.getEncoder().encodeToString(hash(password, salt, ITERATIONS));
        Files.writeString(file(), GSON.toJson(s), StandardCharsets.UTF_8);
        stored = s;
        SESSIONS.clear();                                   // changer le mot de passe deconnecte tous les administrateurs
    }

    /** Resultat d'une tentative : null si le mot de passe est bon, sinon le message a afficher. */
    public static synchronized String attempt(ServerPlayer p, String password) {
        if (stored == null) return "Le menu d'administration est désactivé (admin.json illisible).";
        UUID id = p.getUUID();
        long now = System.currentTimeMillis();
        Long until = LOCKED.get(id);
        if (until != null && until > now) return "Trop d'essais : réessayez dans " + Math.max(1, (until - now) / 60_000L) + " min.";
        boolean ok;
        try {
            ok = MessageDigest.isEqual(hash(password, Base64.getDecoder().decode(stored.salt), stored.iterations), Base64.getDecoder().decode(stored.hash));
        } catch (Exception e) {
            ok = false;
        }
        if (ok) {
            FAILS.remove(id);
            LOCKED.remove(id);
            SESSIONS.add(id);
            MMORPG.LOGGER.info("[MMORPG] ADMIN : {} s'est authentifié au menu d'administration", p.getGameProfile().name());
            return null;
        }
        int[] f = FAILS.computeIfAbsent(id, k -> new int[1]);
        f[0]++;
        MMORPG.LOGGER.warn("[MMORPG] ADMIN : mot de passe incorrect pour {} (essai {}/{})", p.getGameProfile().name(), f[0], MAX_FAILS);
        if (f[0] >= MAX_FAILS) {
            LOCKED.put(id, now + LOCK_MS);
            FAILS.remove(id);
            return "Mot de passe incorrect : accès bloqué 10 minutes.";
        }
        return "Mot de passe incorrect (" + (MAX_FAILS - f[0]) + " essai(s) restant(s)).";
    }

    public static synchronized boolean authorized(ServerPlayer p) {
        return SESSIONS.contains(p.getUUID());
    }

    public static synchronized void logout(UUID id) {
        SESSIONS.remove(id);
    }

    public static synchronized void clear() {
        SESSIONS.clear();
        FAILS.clear();
        LOCKED.clear();
    }
}
