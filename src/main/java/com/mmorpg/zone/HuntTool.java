package com.mmorpg.zone;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Menu de la baguette de zone (operateurs) : cree et gere les zones de chasse sans commande. Le serveur reverifie tout. */
public final class HuntTool {
    private HuntTool() {
    }

    private static boolean allowed(ServerPlayer p) {
        return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Monstres reglables dans une zone : ceux du mod qui existent vraiment, sans les boss. */
    private static List<String> mobKeys() {
        var out = new TreeSet<String>();
        for (var e : ConfigManager.mobs().entrySet()) if (!e.getValue().boss && ModEntities.SPAWNABLE.containsKey(e.getKey())) out.add(e.getKey());
        return new ArrayList<>(out);
    }

    public static void open(ServerPlayer p) {
        open(p, "", true);
    }

    private static void open(ServerPlayer p, String status, boolean ok) {
        if (!allowed(p)) return;
        var s = ZoneManager.selection(p);
        CompoundTag t = new CompoundTag();
        t.putString("status", status);
        t.putBoolean("ok", ok);
        boolean sel = s.complete() && p.level().dimension().equals(s.dimension);
        t.putBoolean("selection", sel);
        if (sel) {
            t.putInt("w", Math.abs(s.a.getX() - s.b.getX()) + 1);
            t.putInt("d", Math.abs(s.a.getZ() - s.b.getZ()) + 1);
        }
        ListTag mobs = new ListTag();
        for (String k : mobKeys()) mobs.add(StringTag.valueOf(k));
        t.put("mobs", mobs);
        ListTag zones = new ListTag();
        for (var z : HuntZones.all()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", z.id);
            e.putString("nom", z.title());
            e.putInt("min", z.niveauMin);
            e.putInt("max", z.niveauMax);
            e.putBoolean("actif", z.actif);
            e.putInt("mobs", z.monstres.size());
            zones.add(e);
        }
        t.put("zones", zones);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.HUNT_ZONE_TOOL, t));
    }

    public static void action(ServerPlayer p, CompoundTag d) {
        if (!allowed(p)) return;
        String a = d.getStringOr("action", "");
        String msg;
        try {
            msg = switch (a) {
                case "create" -> create(p, d);
                case "delete" -> HuntZones.remove(d.getStringOr("id", "")) ? "Zone supprimée." : "Zone inconnue.";
                case "toggle" -> {
                    var z = HuntZones.get(d.getStringOr("id", ""));
                    if (z.isEmpty()) yield "Zone inconnue.";
                    z.get().actif = !z.get().actif;
                    HuntZones.put(z.get());
                    yield "Zone « " + z.get().id + " » " + (z.get().actif ? "activée." : "désactivée.");
                }
                case "tp" -> {
                    var z = HuntZones.get(d.getStringOr("id", ""));
                    if (z.isEmpty()) yield "Zone inconnue.";
                    p.level().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), "zone chasse tp " + z.get().id);
                    yield "";
                }
                case "clear" -> {
                    ZoneManager.clearSelection(p);
                    yield "Sélection effacée.";
                }
                default -> "";
            };
        } catch (java.io.IOException e) {
            msg = "Écriture impossible : " + e.getMessage();
            open(p, msg, false);
            return;
        }
        if (a.equals("tp")) return;
        open(p, msg, !msg.contains("inconnue") && !msg.startsWith("Choisissez") && !msg.startsWith("Identifiant") && !msg.startsWith("Sélectionnez") && !msg.contains("déjà"));
    }

    private static String create(ServerPlayer p, CompoundTag d) throws java.io.IOException {
        String id = ZoneData.slug(d.getStringOr("id", ""));
        if (id.isEmpty()) return "Identifiant invalide (lettres, chiffres, tirets).";
        if (HuntZones.get(id).isPresent()) return "Identifiant déjà utilisé : " + id;
        var s = ZoneManager.selection(p);
        if (!s.complete() || !p.level().dimension().equals(s.dimension)) return "Sélectionnez deux coins avec la baguette (clic droit sur un bloc, deux fois).";
        var valid = mobKeys();
        List<String> mobs = new ArrayList<>();
        var list = d.getListOrEmpty("mobs");
        for (int i = 0; i < list.size(); i++) {
            String k = list.getStringOr(i, "");
            if (valid.contains(k) && !mobs.contains(k)) mobs.add(k);
        }
        if (mobs.isEmpty()) return "Choisissez au moins un monstre.";
        var z = new HuntZones.Zone();
        z.id = id;
        String nom = d.getStringOr("nom", "").trim();
        z.nom = nom.isEmpty() ? id : nom.substring(0, Math.min(40, nom.length()));
        z.dimension = s.dimension.identifier().toString();
        z.x1 = Math.min(s.a.getX(), s.b.getX());
        z.x2 = Math.max(s.a.getX(), s.b.getX());
        z.z1 = Math.min(s.a.getZ(), s.b.getZ());
        z.z2 = Math.max(s.a.getZ(), s.b.getZ());
        z.y1 = Math.min(s.a.getY(), s.b.getY()) - 8;
        z.y2 = Math.max(s.a.getY(), s.b.getY()) + 24;
        z.niveauMin = Math.max(1, Math.min(200, d.getIntOr("min", 1)));
        z.niveauMax = Math.max(z.niveauMin, Math.min(200, d.getIntOr("max", z.niveauMin)));
        z.monstres = mobs;
        z.max = Math.max(1, Math.min(200, d.getIntOr("maxMobs", 24)));
        z.surRayon = Math.max(0, Math.min(500, d.getIntOr("sure", 0)));
        z.recolte = d.getBooleanOr("recolte", false);
        HuntZones.put(z);
        ZoneManager.clearSelection(p);
        com.mmorpg.MMORPG.LOGGER.info("[MMORPG] ZONE-CHASSE : {} a cree {} (niv. {}-{}, {})", p.getGameProfile().name(), id, z.niveauMin, z.niveauMax, mobs);
        return "Zone « " + id + " » créée : niveaux " + z.niveauMin + "-" + z.niveauMax + ", " + mobs.size() + " monstre(s).";
    }
}
