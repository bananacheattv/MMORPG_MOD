package com.mmorpg.server;

import com.mmorpg.admin.PlayerAdmin;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Controles du menu d'administration des joueurs : edition generique, bornes, actions et refus sans permission. */
public final class DevAdminPlayerChecks {
    private DevAdminPlayerChecks() {
    }

    private static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError("admin joueurs: " + text);
    }

    private static CompoundTag req(String value, String key, String... path) {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (String s : path) l.add(StringTag.valueOf(s));
        t.put("path", l);
        t.putString("value", value);
        t.putString("key", key);
        return t;
    }

    private static CompoundTag act(String what, String arg) {
        CompoundTag t = new CompoundTag();
        t.putString("what", what);
        t.putString("arg", arg);
        return t;
    }

    private static boolean refused(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void run(ServerPlayer p) {
        PlayerData d = RpgPlayers.get(p);
        long oldGold = d.gold;
        int oldLevel = d.level;
        check(!PlayerAdmin.allowed(p) || p.permissions() != null, "permission lisible");
        // champs simples, types conserves
        PlayerAdmin.testEdit(p, p, req("1234", "", "gold"), "set");
        check(d.gold == 1234, "or modifie");
        PlayerAdmin.testEdit(p, p, req("77", "", "attrPoints"), "set");
        check(d.attributePoints == 77, "points d'attributs modifies");
        PlayerAdmin.testEdit(p, p, req("Maitre", "", "pvpTitle"), "set");
        check(d.pvpTitle.equals("Maitre"), "titre modifie");
        // valeurs invalides refusees et sans effet
        check(refused(() -> PlayerAdmin.testEdit(p, p, req("abc", "", "gold"), "set")) && d.gold == 1234, "texte refuse pour un nombre");
        check(refused(() -> PlayerAdmin.testEdit(p, p, req("5", "", "inconnu"), "set")), "champ inconnu refuse");
        check(refused(() -> PlayerAdmin.testEdit(p, p, req("5", "", "skills"), "set")), "groupe non modifiable en bloc");
        // niveau borne par le jeu
        PlayerAdmin.testEdit(p, p, req("40", "", "level"), "set");
        check(d.level == 40, "niveau modifie");
        check(refused(() -> PlayerAdmin.testEdit(p, p, req("101", "", "level"), "set")) && d.level == 40, "niveau > 100 refuse");
        // ajout / suppression dans une liste et un groupe
        PlayerAdmin.testEdit(p, p, req("quete_test", "", "completedQuests"), "add");
        check(d.completedQuests.contains("quete_test"), "ajout dans une liste");
        PlayerAdmin.testEdit(p, p, req("", "", "completedQuests", String.valueOf(indexOf(d, "quete_test"))), "del");
        check(!d.completedQuests.contains("quete_test"), "suppression dans une liste");
        PlayerAdmin.testEdit(p, p, req("1500", "mineur", "jobXp"), "add");
        check(d.jobXp.getOrDefault("mineur", 0L) == 1500L, "ajout dans un groupe (type conserve)");
        PlayerAdmin.testEdit(p, p, req("", "", "jobXp"), "clear");
        check(d.jobXp.isEmpty(), "groupe vide");
        // actions
        d.hp = 1;
        PlayerAdmin.testAct(p, p, act("heal", ""));
        check(d.hp > 1, "soin");
        PlayerAdmin.testAct(p, p, act("give", "minecraft:diamond"));
        check(refused(() -> PlayerAdmin.testAct(p, p, act("give", "inconnu:truc"))), "objet inconnu refuse");
        check(refused(() -> PlayerAdmin.testAct(p, p, act("nimporte", ""))), "action inconnue refusee");
        PlayerAdmin.testAct(p, p, act("mute", ""));
        check(PlayerAdmin.testProfile(p, p).getBooleanOr("muted", false), "sourdine active");
        PlayerAdmin.testAct(p, p, act("unmute", ""));
        check(!PlayerAdmin.testProfile(p, p).getBooleanOr("muted", false), "sourdine levee");
        PlayerAdmin.testAct(p, p, act("resetquests", ""));
        check(d.completedQuests.isEmpty() && d.activeQuests.isEmpty(), "quetes reinitialisees");
        var profile = PlayerAdmin.testProfile(p, p);
        check(profile.getCompoundOrEmpty("data").getIntOr("level", 0) == d.level && profile.getListOrEmpty("inventory") != null, "profil complet renvoye");
        d.gold = oldGold;
        RpgPlayers.setLevel(p, oldLevel);
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] ADMIN_PLAYERS PASS: edition generique, bornes, listes, groupes, actions, sourdine, profil");
    }

    private static int indexOf(PlayerData d, String quest) {
        List<String> l = new java.util.ArrayList<>();
        var tag = d.save().getListOrEmpty("completedQuests");
        for (int i = 0; i < tag.size(); i++) l.add(tag.getStringOr(i, ""));
        return l.indexOf(quest);
    }
}
