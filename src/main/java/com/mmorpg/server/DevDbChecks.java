package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.db.Audit;
import com.mmorpg.db.Database;
import com.mmorpg.db.PlayerSync;
import com.mmorpg.economy.BankData;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Verification automatique de la base de donnees (lancer avec -Dmmorpg.checkDb=true et database.json active vers une base de test) :
 * ecriture et lecture du profil et de la banque, version, passage d'un autre serveur, attente de sa sauvegarde, protection contre
 * l'ecrasement, journal. Retire ses lignes de test a la fin et arrete le serveur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevDbChecks {
    private static int step;
    private static int waitTicks;
    private static PlayerData saved;
    private static long v1;
    private static boolean failed;
    private static String invSnapshot;

    private static void check(boolean ok, String what) {
        if (!ok) failed = true;
        MMORPG.LOGGER.info("[CODEX CHECKS] DB {} : {}", ok ? "ok" : "ECHEC", what);
    }

    private static long scalar(String sql, String arg) {
        Long r = Database.await("test", 5000, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, arg);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : -1L;
                }
            }
        });
        return r == null ? -2 : r;
    }

    private static String text(String sql, String arg) {
        return Database.await("test", 5000, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, arg);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    private static void exec(String sql, Object... args) {
        Database.await("test", 5000, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                return ps.executeUpdate();
            }
        });
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean("mmorpg.checkDb") || step >= 99 || !(event.getEntity() instanceof ServerPlayer p) || p.tickCount < 100) return;
        PlayerData d = RpgPlayers.get(p);
        String id = p.getUUID().toString();
        var server = p.level().getServer();
        try {
            if (step == 0) {
                check(Database.enabled() && Database.ready(), "base connectee (" + Database.lastError() + ")");
                saved = new PlayerData();
                saved.load(d.save());
                saved.dbVersion = d.dbVersion;
                invSnapshot = com.mmorpg.db.InventorySync.encode(p);
                exec("DELETE FROM players WHERE uuid = ?", id);
                exec("DELETE FROM bank_accounts WHERE uuid = ?", id);
                d.playerClass = PlayerClass.GUERRIER;
                d.level = 42;
                d.gold = 12345;
                d.dbVersion = 0;
                BankData.Account a = BankData.get(server).account(p.getUUID());
                a.gold = 777;
                a.items.set(0, new ItemStack(Items.DIAMOND, 3));
                PlayerSync.push(p, false);
                Database.await("barriere", 5000, c -> null);
                step = 1;
            } else if (step == 1) {                                   // le tick suivant : la version est revenue
                v1 = d.dbVersion;
                check(v1 >= 1, "version recue apres la premiere ecriture (" + v1 + ")");
                check(scalar("SELECT level FROM players WHERE uuid = ?", id) == 42 && scalar("SELECT gold FROM players WHERE uuid = ?", id) == 12345, "colonnes niveau et or");
                String data = text("SELECT data FROM players WHERE uuid = ?", id);
                check(data != null && data.contains("GUERRIER") && !data.contains("dbVersion"), "profil complet, sans la version");
                check(scalar("SELECT gold FROM bank_accounts WHERE uuid = ?", id) == 777, "or de la banque");
                String bank = text("SELECT data FROM bank_accounts WHERE uuid = ?", id);
                check(bank != null && bank.contains("diamond"), "objets de la banque");
                check(scalar("SELECT online FROM players WHERE uuid = ?", id) == 1, "joueur marque en ligne");
                // 2e ecriture identique : aucune nouvelle version (seulement la presence)
                PlayerSync.push(p, false);
                Database.await("barriere", 5000, c -> null);
                check(scalar("SELECT version FROM players WHERE uuid = ?", id) == v1, "pas de nouvelle version si rien n'a change");
                // un autre serveur (« hub ») sauvegarde une progression plus avancee, puis libere le joueur
                String bank2 = bank.replace("777", "111");
                check(data.contains("level:42") && data.contains("gold:12345L"), "format du profil stocke : " + data.substring(0, Math.min(120, data.length())));
                String data2 = data.replace("level:42", "level:55").replace("gold:12345L", "gold:999L");
                exec("UPDATE players SET level = 55, gold = 999, data = ?, version = ?, server_id = 'hub', online = 0 WHERE uuid = ?", data2, v1 + 5, id);
                exec("UPDATE bank_accounts SET data = ?, gold = 111, version = ? WHERE uuid = ?", bank2, v1 + 5, id);
                PlayerSync.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
                check(d.level == 55 && d.gold == 999, "arrivee depuis un autre serveur : profil de la base applique (niveau " + d.level + ", or " + d.gold + ")");
                check(d.dbVersion == v1 + 5, "version locale alignee (" + d.dbVersion + ")");
                check(BankData.get(server).account(p.getUUID()).gold == 111, "banque appliquee");
                check(scalar("SELECT online FROM players WHERE uuid = ?", id) == 1 && "serveur".equals(text("SELECT server_id FROM players WHERE uuid = ?", id)), "joueur reserve pour ce serveur");
                // la base a un profil plus ancien que le serveur : on garde celui du serveur
                d.level = 60;
                d.dbVersion = v1 + 50;
                PlayerSync.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
                check(d.level == 60, "base plus ancienne : profil du serveur conserve");
                // un autre serveur tient le joueur : notre ecriture est refusee
                exec("UPDATE players SET server_id = 'mines', online = 1, last_seen = NOW() WHERE uuid = ?", id);
                long vBefore = scalar("SELECT version FROM players WHERE uuid = ?", id);
                d.gold = 5;
                PlayerSync.push(p, false);
                Database.await("barriere", 5000, c -> null);
                check(scalar("SELECT version FROM players WHERE uuid = ?", id) == vBefore && scalar("SELECT gold FROM players WHERE uuid = ?", id) == 999, "ecriture refusee quand un autre serveur tient le joueur");
                // attente de la sauvegarde de l'autre serveur a l'arrivee
                d.level = 10;
                d.dbVersion = 0;
                String data3 = text("SELECT data FROM players WHERE uuid = ?", id).replace("level:55", "level:77");
                exec("UPDATE players SET data = ?, level = 77 WHERE uuid = ?", data3, id);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    var cfg = Database.config();                       // connexion separee, comme un autre serveur du reseau
                    var props = new java.util.Properties();
                    props.setProperty("user", cfg.user);
                    props.setProperty("password", cfg.password);
                    try (var other = new org.mariadb.jdbc.Driver().connect(cfg.url(), props);
                         PreparedStatement ps = other.prepareStatement("UPDATE players SET online = 0, version = version + 1 WHERE uuid = ?")) {
                        ps.setString(1, id);
                        ps.executeUpdate();
                    } catch (java.sql.SQLException e) {
                        MMORPG.LOGGER.error("[CODEX CHECKS] DB liberation en echec", e);
                    }
                }, "test-release").start();
                long t0 = System.currentTimeMillis();
                PlayerSync.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
                long waited = System.currentTimeMillis() - t0;
                check(waited >= 800 && waited < 2200, "attente de l'autre serveur : " + waited + " ms");
                check(d.level == 77, "profil lu apres la liberation (niveau " + d.level + ")");
                // journal
                Audit.log(p, Audit.ADMIN, 5, "test base");
                Database.await("barriere", 5000, c -> null);
                check(scalar("SELECT COUNT(*) FROM events WHERE uuid = ? AND kind = 'admin' AND detail = 'test base'", id) >= 1, "evenement enregistre");
                check(scalar("SELECT COUNT(*) FROM events WHERE uuid = ? AND kind = 'login'", id) >= 1, "connexion enregistree");
                // sortie : le joueur n'est plus en ligne
                PlayerSync.push(p, true);
                Database.await("barriere", 5000, c -> null);
                check(scalar("SELECT online FROM players WHERE uuid = ?", id) == 0, "joueur libere a la sortie");
                step = 2;
            } else if (step == 2) {                                   // inventaire et coffre de l'Ender : aller-retour par la base
                p.getInventory().clearContent();
                p.getEnderChestInventory().clearContent();
                p.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 7));
                p.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
                p.getEnderChestInventory().setItem(2, new ItemStack(Items.EMERALD, 4));
                d.dbVersion = 0;
                PlayerSync.push(p, false);
                Database.await("barriere", 5000, c -> null);
                String inv = text("SELECT inventory FROM players WHERE uuid = ?", id);
                check(inv != null && inv.contains("diamond") && inv.contains("iron_helmet") && inv.contains("emerald"), "inventaire, armure et coffre de l'Ender ecrits");
                // un autre serveur change l'inventaire (diamants -> pommes d'or), le joueur arrive ici
                String inv2 = inv.replace("minecraft:diamond", "minecraft:golden_apple");
                long vNow = scalar("SELECT version FROM players WHERE uuid = ?", id);
                exec("UPDATE players SET inventory = ?, version = ?, server_id = 'hub', online = 0 WHERE uuid = ?", inv2, vNow + 3, id);
                p.getInventory().setItem(8, new ItemStack(Items.STICK, 1));            // objet local qui doit disparaitre au profit de la base
                d.dbVersion = vNow;
                PlayerSync.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
                check(p.getInventory().getItem(5).is(Items.GOLDEN_APPLE) && p.getInventory().getItem(5).getCount() == 7, "inventaire de la base applique");
                check(p.getInventory().getItem(8).isEmpty(), "objet local remplace (pas de doublon)");
                check(p.getInventory().getItem(39).is(Items.IRON_HELMET), "armure appliquee");
                check(p.getEnderChestInventory().getItem(2).is(Items.EMERALD) && p.getEnderChestInventory().getItem(2).getCount() == 4, "coffre de l'Ender applique");
                // transfert vers un lieu qui n'est pas ici : profil ecrit, joueur libere, arrivee notee, message au proxy
                var cfg = com.mmorpg.cluster.Cluster.config();
                cfg.enabled = true;
                cfg.hosts = java.util.List.of("hub");
                cfg.servers = new java.util.LinkedHashMap<>(java.util.Map.of("hub", "serveur", "spawn", "serveur", "mines", "mines", "plains", "mines"));
                cfg.giveUpSeconds = 5;
                check(com.mmorpg.cluster.Cluster.active() && !com.mmorpg.cluster.Cluster.hosts(com.mmorpg.cluster.Cluster.Place.MINES), "reseau actif, les mines sont ailleurs");
                check(!com.mmorpg.cluster.Cluster.transfer(p, com.mmorpg.cluster.Cluster.Place.HUB, 0), "lieu local : pas de transfert");
                check(com.mmorpg.cluster.Cluster.transfer(p, com.mmorpg.cluster.Cluster.Place.MINES, 2), "transfert vers les mines lance");
                Database.await("barriere", 5000, c -> null);
                check("mines".equals(text("SELECT dest_server FROM transfers WHERE uuid = ?", id)) && scalar("SELECT arg FROM transfers WHERE uuid = ?", id) == 2, "arrivee notee (mines, numero 2)");
                check(scalar("SELECT online FROM players WHERE uuid = ?", id) == 0, "joueur libere pour le serveur d'arrivee");
                check(scalar("SELECT COUNT(*) FROM events WHERE uuid = ? AND kind = 'transfer'", id) >= 1, "transfert journalise");
                // le serveur d'arrivee lit l'arrivee et l'efface (connexion separee : ici on simule en changeant le serveur local)
                waitTicks = 0;
                step = 3;
            } else if (step == 3) {                                   // le proxy n'a rien fait : au bout de 5 s le joueur est repris
                if (++waitTicks < 140) return;
                check(scalar("SELECT online FROM players WHERE uuid = ?", id) == 1 && "serveur".equals(text("SELECT server_id FROM players WHERE uuid = ?", id)), "transfert sans effet : joueur repris par ce serveur");
                check(scalar("SELECT COUNT(*) FROM transfers WHERE uuid = ?", id) == 0, "arrivee annulee");
                var cfg = com.mmorpg.cluster.Cluster.config();
                cfg.enabled = false;
                step = 4;
            } else if (step == 4) {
                // nettoyage : etat du joueur de test et lignes de test retirees
                d.load(saved.save());
                d.dbVersion = saved.dbVersion;
                BankData.Account a = BankData.get(server).account(p.getUUID());
                a.gold = 0;
                a.items.set(0, ItemStack.EMPTY);
                if (invSnapshot != null) com.mmorpg.db.InventorySync.apply(p, invSnapshot);
                exec("DELETE FROM transfers WHERE uuid = ?", id);
                exec("DELETE FROM events WHERE uuid = ?", id);
                exec("DELETE FROM bank_accounts WHERE uuid = ?", id);
                exec("DELETE FROM players WHERE uuid = ?", id);
                RpgPlayers.recomputeAndSync(p);
                step = 99;
                MMORPG.LOGGER.info("[CODEX CHECKS] DB {}", failed ? "ECHEC" : "PASS");
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                server.halt(false);
            }
        } catch (RuntimeException e) {
            MMORPG.LOGGER.error("[CODEX CHECKS] DB exception", e);
            step = 99;
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            server.halt(false);
        }
    }
}
