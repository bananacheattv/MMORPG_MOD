package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.db.Database;
import com.mmorpg.economy.AuctionData;
import com.mmorpg.guild.GuildStore;
import com.mmorpg.network.SharedLedger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Verification des donnees partagees en base (-PsharedChecks, database.json active vers une base de test) : import du guilds.json existant,
 * ecriture et relecture des guildes, registres de coordination, hotel des ventes (ecriture puis rechargement). Vide shared_docs puis arrete le serveur.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevSharedDocsChecks {
    public static final class Counter {public int n;}
    private static boolean failed;

    private static void check(boolean ok, String what) {
        if (!ok) failed = true;
        MMORPG.LOGGER.info("[CODEX CHECKS] SHARED {} : {}", ok ? "ok" : "ECHEC", what);
    }

    private static String doc(String name) {
        return Database.await("test", 5000, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data FROM shared_docs WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    private static void sql(String s) {
        Database.await("test", 5000, c -> {
            try (var st = c.createStatement()) {
                return st.executeUpdate(s);
            }
        });
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkShared")) return;
        var server = event.getServer();
        try {
            check(Database.enabled() && Database.ready(), "base connectee (" + Database.lastError() + ")");
            sql("DELETE FROM shared_docs");
            // 1) import de l'ancien guilds.json
            var dir = GuildStore.directory(server);
            Files.createDirectories(dir);
            var file = dir.resolve("guilds.json");
            Files.deleteIfExists(dir.resolve("guilds.json.migre"));
            var legacy = new GuildStore.State();
            var g = new GuildStore.Guild();
            g.name = "Anciens";
            UUID chef = UUID.randomUUID();
            g.members.put(chef, new GuildStore.Member(chef, "Chef", "CHEF"));
            legacy.guilds.add(g);
            Files.writeString(file, new com.google.gson.Gson().toJson(legacy));
            int count = GuildStore.access(server, false, s -> s.guilds.size());
            check(count == 1, "guilde existante importee (" + count + ")");
            check(doc("guilds") != null && doc("guilds").contains("Anciens"), "guilde presente dans la base");
            check(!Files.exists(file) && Files.exists(dir.resolve("guilds.json.migre")), "ancien fichier renomme");
            // 2) ecriture puis relecture
            UUID second = UUID.randomUUID();
            GuildStore.access(server, true, s -> {
                var n = new GuildStore.Guild();
                n.name = "Nouvelle";
                n.members.put(second, new GuildStore.Member(second, "Autre", "CHEF"));
                s.guilds.add(n);
                return null;
            });
            check(GuildStore.access(server, false, s -> s.guilds.size()) == 2, "deux guildes apres ecriture");
            check(GuildStore.access(server, false, s -> GuildStore.of(s, second) != null), "membre retrouve");
            try {
                GuildStore.access(server, true, s -> {
                    s.guilds.get(0).level = 99;                        // invalide : rien ne doit etre ecrit
                    return null;
                });
                check(false, "ecriture invalide refusee");
            } catch (IllegalStateException e) {
                check(GuildStore.access(server, false, s -> s.guilds.get(0).level) == 1, "ecriture invalide refusee, base intacte");
            }
            // 3) registre de coordination
            for (int i = 0; i < 2; i++) SharedLedger.access(server, "test-ledger", Counter.class, Counter::new, c -> ++c.n);
            check(SharedLedger.access(server, "test-ledger", Counter.class, Counter::new, c -> c.n) == 2, "registre de coordination (2)");
            check(SharedLedger.read(server, "test-ledger", Counter.class, Counter::new, c -> ++c.n) == 3
                    && SharedLedger.access(server, "test-ledger", Counter.class, Counter::new, c -> c.n) == 2, "lecture seule du registre : rien n'est ecrit");
            // 4) hotel des ventes
            AuctionData a = AuctionData.get(server);
            a.listings().clear();                                      // le monde de test peut garder des annonces d'une execution precedente
            a.takeEarnings(chef);
            a.add(new AuctionData.Listing(a.nextId(), UUID.randomUUID(), "Vendeur", new ItemStack(Items.DIAMOND, 5), 250, System.currentTimeMillis() + 3_600_000L));
            a.addEarnings(chef, 40);
            Database.await("barriere", 5000, c -> null);
            String json = doc("auction");
            check(json != null && json.contains("diamond") && json.contains("Vendeur"), "hotel des ventes ecrit dans la base");
            a.listings().clear();                                      // etat du monde « perdu » : la base doit le restituer
            a.reloadFromDatabase(server);
            check(a.listings().size() == 1 && a.listings().get(0).stack().getCount() == 5 && a.earnings(chef) == 40, "hotel des ventes relu depuis la base");
            // 5) un autre serveur ecrit le document (menu d'administration) : la copie en memoire est relue avant usage
            com.mmorpg.db.SharedDocs.put("auction", doc("auction").replace("\"Vendeur\"", "\"Autre serveur\""));
            Database.await("barriere", 5000, c -> null);
            check(AuctionData.get(server).listings().get(0).sellerName().equals("Autre serveur"), "hotel des ventes relu apres l'ecriture d'un autre serveur");
            a.addEarnings(chef, 2);
            Database.await("barriere", 5000, c -> null);
            check(AuctionData.get(server).earnings(chef) == 42 && doc("auction").contains("Autre serveur"), "notre propre ecriture reste en memoire et en base");
            // 5b) un objet devenu illisible (retire du mod) ne fait pas perdre les autres annonces et reste conserve
            com.mmorpg.db.SharedDocs.put("auction", doc("auction").replaceFirst("\"listings\":\\[", "\"listings\":[{\"id\":99,\"seller\":\"" + UUID.randomUUID() + "\",\"sellerName\":\"Perdu\",\"stack\":{\"id\":\"mmorpg:objet_supprime\",\"count\":1},\"price\":5,\"expires\":9999999999999},"));
            Database.await("barriere", 5000, c -> null);
            a.reloadFromDatabase(server);
            check(a.listings().size() == 1, "annonce illisible : les autres annonces survivent");
            a.addEarnings(chef, 1);
            Database.await("barriere", 5000, c -> null);
            check(doc("auction").contains("objet_supprime"), "annonce illisible conservee en base, pas detruite");
            // 6) interrupteurs : ecrits dans la base, relus par les autres serveurs (et apres un redemarrage), sans redemarrage
            FeatureFlags.set(server, "drones", false);
            FeatureFlags.forget(server);
            check(!FeatureFlags.enabled(server, "drones"), "interrupteur relu depuis la base (autre serveur ou redemarrage)");
            com.mmorpg.db.SharedDocs.put("ledger-features", doc("ledger-features").replace("\"drones\":false", "\"drones\":true"));
            Database.await("barriere", 5000, c -> null);
            boolean flagSeen = false;
            for (long until = System.currentTimeMillis() + 6000; !flagSeen && System.currentTimeMillis() < until; Thread.sleep(100)) flagSeen = FeatureFlags.enabled(server, "drones");
            check(flagSeen, "interrupteur change par un autre serveur pris en compte sans redemarrage");
            // 7) chat de guilde : les messages ecrits dans la base sont distribues
            int got = -1;
            for (long until = System.currentTimeMillis() + 3000; got < 0 && System.currentTimeMillis() < until; Thread.sleep(100)) got = com.mmorpg.guild.GuildChat.poll(server);
            SharedLedger.access(server, "guild-chat", com.mmorpg.guild.GuildChat.State.class, com.mmorpg.guild.GuildChat.State::new, st -> {
                var m = new com.mmorpg.guild.GuildChat.Message();
                m.id = st.next++;
                m.guild = g.id.toString();
                m.name = "Chef";
                m.rank = "CHEF";
                m.text = "bonjour";
                st.messages.add(m);
                return null;
            });
            int delivered = 0;
            for (long until = System.currentTimeMillis() + 3000; delivered == 0 && System.currentTimeMillis() < until; Thread.sleep(100)) delivered = Math.max(0, com.mmorpg.guild.GuildChat.poll(server));
            check(got == 0 && delivered == 1, "chat de guilde relu depuis la base (" + got + ", " + delivered + ")");
            // base lente : la lecture abandonnee (delai depasse) prend le verrou plus tard ; il doit etre rendu, sinon les autres
            // serveurs attendent ce document pour toujours
            Database.fire("lenteur", c -> {
                try (var st = c.createStatement()) {
                    st.executeQuery("SELECT SLEEP(5)").close();
                }
                return null;
            });
            boolean abandoned = false;
            try {
                com.mmorpg.db.SharedDocs.access("test-lent", Counter.class, Counter::new, null, false, c -> c.n);
            } catch (IllegalStateException e) {
                abandoned = true;
            }
            Database.await("barriere", 10000, c -> null);                // file a un seul fil : la lecture abandonnee est passee
            Long holder = Database.await("test", 5000, c -> {
                try (var ps = c.prepareStatement("SELECT IS_USED_LOCK('aeloria:test-lent')"); var rs = ps.executeQuery()) {
                    rs.next();
                    long v = rs.getLong(1);
                    return rs.wasNull() ? 0L : v;
                }
            });
            check(abandoned && holder != null && holder == 0, "verrou rendu apres une lecture abandonnee (" + abandoned + ", " + holder + ")");
            check(com.mmorpg.db.SharedDocs.access("test-lent", Counter.class, Counter::new, null, true, c -> ++c.n) == 1, "document utilisable ensuite");
        } catch (Exception e) {
            failed = true;
            MMORPG.LOGGER.error("[CODEX CHECKS] SHARED exception", e);
        } finally {
            try {
                sql("DELETE FROM shared_docs");
            } catch (RuntimeException ignored) {
            }
            MMORPG.LOGGER.info("[CODEX CHECKS] SHARED {}", failed ? "ECHEC" : "PASS");
            server.halt(false);
        }
    }
}
