package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.CharacterScreen;
import com.mmorpg.client.screen.MenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mmorpg.config.LootTables;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

/**
 * Vitrine "revue" : reproduit les plantages corriges lors de la revue du code.
 * Bouton Personnage : le relachement du clic arrive sur le nouvel ecran avant sa premiere image ;
 * chemin d'evolution : fenetre etroite et haute (4:3) a l'echelle 3, libelles longs du Guerrier.
 * 3e passe : touches Entree/Echap (codes 26.3), valeur null de l'editeur admin, selecteur d'objets d'/adminjoueurs, menu Echap et
 * fonctionnalites coupees, coffre renomme « Sacs à dos », cinematique sans plan, reponse de l'editeur de butins, jauge de peche
 * perimee, defilement du wiki, nom du joueur dans le HUD, ecran de mort avec butin perdu.
 */
final class DevReviewChecks {
    private static int tick = -1;
    private static int failures;
    private static long deathOpened;
    private static int deathStep;

    private DevReviewChecks() {
    }

    private interface Step {
        void run() throws Exception;
    }

    private static void check(String name, Step action) {
        try {
            action.run();
            MMORPG.LOGGER.info("[MMORPG] REVUE {} OK", name);
        } catch (Throwable t) {
            failures++;
            MMORPG.LOGGER.error("[MMORPG] REVUE {} ECHEC", name, t);
        }
    }

    private static MouseButtonEvent click(double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
    }

    static void tick(Minecraft mc) {
        if (com.mmorpg.client.cinematic.CinematicClient.active()) {
            com.mmorpg.client.cinematic.CinematicClient.skip();
            return;
        }
        if (LoadingClient.active()) return;
        tick++;
        if (tick == 20) {
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
        }
        if (tick == 40 && !ClientData.DATA.playerClass.isPlayable())
            ClientNet.send(new com.mmorpg.network.Payloads.SelectClass(com.mmorpg.rpg.PlayerClass.GUERRIER.ordinal()));
        if (tick == 80) MenuScreen.open(MenuScreen.Tab.QUETES);
        if (tick == 100) check("personnage_clic", () -> {
            // meme tick, sans image entre les deux : clic sur l'onglet, puis relachement recu par l'ecran Personnage
            MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
            var screen = mc.gui.screen();
            if (!(screen instanceof CharacterScreen)) throw new AssertionError("ecran inattendu : " + screen);
            screen.mouseReleased(click(screen.width / 2.0, screen.height / 2.0));
            screen.mouseClicked(click(screen.width / 2.0 + 30, screen.height / 2.0 + 20), false);
            screen.mouseReleased(click(screen.width / 2.0 + 30, screen.height / 2.0 + 20));
        });
        if (tick == 110) mc.gui.setScreen(null);
        if (tick == 120) {
            mc.getWindow().setWindowed(1152, 864);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }
        if (tick == 140) MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
        if (tick == 170) {
            DevShowcase.shot(mc, "revue_personnage_4_3");
            MMORPG.LOGGER.info("[MMORPG] REVUE personnage_4_3 OK (ecran {} rendu en {}x{})", mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName(),
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
        if (tick == 180) {
            mc.gui.setScreen(null);
            mc.getWindow().setWindowed(1280, 720);
            mc.options.guiScale().set(2);
            mc.resizeGui();
        }
        if (tick == 190) check("entree_hdv", () -> {
            var s = new com.mmorpg.client.screen.AuctionScreen(new CompoundTag());
            s.previewTab(0);
            mc.gui.setScreen(s);
            var search = (EditBox) get(s, "search");
            s.setFocused(search);
            search.setFocused(true);
            if (!s.keyPressed(key(InputConstants.KEY_RETURN))) throw new AssertionError("Entree ignoree dans la recherche");
        });
        if (tick == 195) check("entree_echange", () -> {
            var s = new com.mmorpg.client.screen.TradeScreen(new CompoundTag());
            mc.gui.setScreen(s);
            var gold = (EditBox) get(s, "gold");
            s.setFocused(gold);
            gold.setFocused(true);
            if (!s.keyPressed(key(InputConstants.KEY_RETURN))) throw new AssertionError("Entree ignoree dans le champ d'or");
        });
        if (tick == 200) check("admin_null_echap_entree", () -> {
            var s = new com.mmorpg.client.screen.AdminScreen(List.of("essai.json"));
            mc.gui.setScreen(s);
            JsonObject root = JsonParser.parseString("{\"nom\":null,\"texte\":\"abc\",\"nombre\":3}").getAsJsonObject();
            set(s, "file", "essai.json");
            set(s, "root", root);
            set(s, "loading", false);
            var start = s.getClass().getDeclaredMethod("startEdit", Object.class, JsonElement.class, int.class, int.class, int.class);
            start.setAccessible(true);
            start.invoke(s, "nom", root.get("nom"), 20, 40, 120);                 // plantait : getAsString sur JsonNull
            if (!s.keyPressed(key(InputConstants.KEY_ESCAPE)) || mc.gui.screen() != s || get(s, "editKey") != null)
                throw new AssertionError("Echap doit annuler le champ sans fermer l'ecran");
            start.invoke(s, "nom", root.get("nom"), 20, 40, 120);
            s.keyPressed(key(InputConstants.KEY_RETURN));                             // valeur vide laissee : reste null
            if (!root.get("nom").isJsonNull()) throw new AssertionError("null remplace : " + root.get("nom"));
            start.invoke(s, "texte", root.get("texte"), 20, 40, 120);
            ((EditBox) get(s, "edit")).setValue("xyz");
            s.keyPressed(key(InputConstants.KEY_RETURN));
            start.invoke(s, "nombre", root.get("nombre"), 20, 40, 120);
            ((EditBox) get(s, "edit")).setValue("7");
            s.keyPressed(key(InputConstants.KEY_NUMPADENTER));
            if (!"xyz".equals(root.get("texte").getAsString()) || root.get("nombre").getAsLong() != 7 || !(boolean) get(s, "dirty"))
                throw new AssertionError("Entree ne valide pas : " + root);
        });
        if (tick == 205) mc.gui.setScreen(null);
        if (tick == 210) check("adminjoueurs_selecteur", () -> {
            var s = new com.mmorpg.client.screen.AdminPlayersScreen(new CompoundTag());
            mc.gui.setScreen(s);
            ((EditBox) get(s, "value")).setValue("mmorpg:potion_soin");      // ce que fait le selecteur avant de rendre la main
            ((EditBox) get(s, "key")).setValue("5");
            mc.gui.setScreen(new com.mmorpg.client.screen.ItemPickerScreen(s, "Donner un objet", true, 64, List.of(), (id, q) -> {
            }));
            mc.gui.setScreen(s);                                                       // init() repasse
            String v = ((EditBox) get(s, "value")).getValue(), k = ((EditBox) get(s, "key")).getValue();
            if (!v.equals("mmorpg:potion_soin") || !k.equals("5")) throw new AssertionError("objet choisi perdu : '" + v + "' x '" + k + "'");
        });
        if (tick == 215) mc.gui.setScreen(null);
        if (tick == 220) check("menu_echap_fonctionnalites", () -> {
            ClientData.features.putBoolean("quetes", false);
            ClientData.features.putBoolean("mines", false);
            try {
                var s = new com.mmorpg.client.screen.AeloriaDirectoryScreen();
                mc.gui.setScreen(s);
                List<String> titles = new java.util.ArrayList<>();
                for (Object card : (List<?>) get(s, "cards")) {
                    var m = card.getClass().getDeclaredMethod("title");
                    m.setAccessible(true);
                    titles.add((String) m.invoke(card));
                }
                if (titles.contains("Quêtes") || titles.contains("Les mines") || !titles.contains("Personnage"))
                    throw new AssertionError("cartes : " + titles);
            } finally {
                ClientData.features.remove("quetes");
                ClientData.features.remove("mines");
                mc.gui.setScreen(null);
            }
        });
        if (tick == 225) check("coffre_renomme_sacs", () -> {
            var inv = mc.player.getInventory();
            var chest = new ChestMenu(MenuType.GENERIC_9x3, 120, inv, new SimpleContainer(27), 3);
            mc.gui.setScreen(new ContainerScreen(chest, inv, Component.literal(com.mmorpg.server.Backpacks.TITLE.getString())));
            if (!(mc.gui.screen() instanceof ContainerScreen) || mc.gui.screen() instanceof com.mmorpg.client.screen.BackpackScreen)
                throw new AssertionError("coffre renomme ouvert en sac : " + mc.gui.screen());
            var bags = new ChestMenu(MenuType.GENERIC_9x6, 121, inv, new SimpleContainer(54), 6);
            mc.gui.setScreen(new ContainerScreen(bags, inv, com.mmorpg.server.Backpacks.TITLE));
            if (!(mc.gui.screen() instanceof com.mmorpg.client.screen.BackpackScreen)) throw new AssertionError("vrais sacs : " + mc.gui.screen());
            mc.gui.setScreen(null);
        });
        if (tick == 230) check("cinematique_sans_plan", () -> {
            for (String json : new String[]{"{\"plans\":[]}", "{\"plans\":[{\"duree\":2}]}", "{\"plans\":[{\"duree\":0,\"camera\":[[0,70,0]],\"cible\":[[0,70,5]]}]}"}) {
                var t = new CompoundTag();
                t.putString("json", json);
                com.mmorpg.client.cinematic.CinematicClient.start(t);             // plantait hors du try (liste vide)
                if (com.mmorpg.client.cinematic.CinematicClient.active()) throw new AssertionError("lancee : " + json);
            }
        });
        if (tick == 235) check("editeur_butins_reponse", () -> {
            var data = new CompoundTag();
            var tables = new CompoundTag();
            for (String id : LootTables.ALL) tables.putString(id, "[]");
            data.put("tables", tables);
            var s = new com.mmorpg.client.screen.LootEditorScreen(data);
            mc.gui.setScreen(s);
            @SuppressWarnings("unchecked") var all = (java.util.Map<String, List<LootTables.Entry>>) get(s, "tables");
            @SuppressWarnings("unchecked") var modified = (java.util.Map<String, Boolean>) get(s, "modified");
            set(s, "saving", LootTables.CRATE);                                        // caisse envoyee, puis onglet change avant la reponse
            set(s, "current", LootTables.VOTE);
            all.get(LootTables.VOTE).add(new LootTables.Entry("minecraft:diamond", 1, 1, 10));
            modified.put(LootTables.VOTE, true);
            var reply = new CompoundTag();
            var saved = new CompoundTag();
            for (String id : LootTables.ALL) saved.putString(id, id.equals(LootTables.CRATE) ? "[{\"item\":\"minecraft:emerald\",\"min\":1,\"max\":2,\"weight\":5}]" : "[]");
            reply.put("tables", saved);
            reply.putBoolean("ok", true);
            reply.putString("status", "ok");
            reply.putString("table", LootTables.CRATE);
            s.receive(reply);
            if (all.get(LootTables.VOTE).size() != 1 || !modified.get(LootTables.VOTE)) throw new AssertionError("modifications de l'autre table perdues");
            if (all.get(LootTables.CRATE).size() != 1 || modified.get(LootTables.CRATE)) throw new AssertionError("table enregistree non rechargee");
            mc.gui.setScreen(null);
        });
        if (tick == 240) check("jauge_peche_perimee", () -> {
            var t = new CompoundTag();
            t.putLong("seed", 7);
            t.putInt("rounds", 2);
            t.putInt("period", 30);
            t.putFloat("half", 0.12F);
            t.putInt("status", 0);
            FishFightClient.receive(t);
            if (!FishFightClient.active()) throw new AssertionError("jauge inactive");
            var f = FishFightClient.class.getDeclaredField("roundStartNanos");
            f.setAccessible(true);
            f.setLong(null, System.nanoTime() - 20_000_000_000L);                      // plus de nouvelles du serveur depuis 20 s
            if (FishFightClient.active()) throw new AssertionError("jauge bloquee (clic droit des cannes annule)");
            FishFightClient.reset();
        });
        if (tick == 245) MenuScreen.open(MenuScreen.Tab.WIKI);
        if (tick == 250) check("wiki_defilement", () -> {
            var s = mc.gui.screen();
            int left = (int) get(s, "left");
            for (int i = 0; i < 300; i++) s.mouseScrolled(left + 20, s.height / 2.0, 0, -1);
        });
        if (tick == 252) check("wiki_defilement_borne", () -> {
            int scroll = (int) get(mc.gui.screen(), "listScroll");
            if (scroll > 80) throw new AssertionError("liste partie hors de l'ecran : " + scroll);
        });
        if (tick == 262) DevShowcase.shot(mc, "revue_wiki_bas");                    // laisse passer quelques images (rendu logiciel lent)
        if (tick == 266) mc.gui.setScreen(null);
        if (tick == 272) DevShowcase.shot(mc, "revue_hud_nom");
        if (tick == 280) {
            mc.gui.setScreen(new com.mmorpg.client.screen.RpgDeathScreen(new com.mmorpg.network.Payloads.DeathInfo(1250, 12, 300, 2400, "Tué par un Gobelin",
                    "mmorpg:lingot_aelium*3;minecraft:diamond*2;minecraft:emerald*5")));
            deathOpened = System.nanoTime();
            deathStep = 0;
        }
        if (tick > 280 && deathStep < 3) {
            float t = (System.nanoTime() - deathOpened) / 1.0E9F;
            boolean open = mc.gui.screen() instanceof com.mmorpg.client.screen.RpgDeathScreen;
            if (deathStep == 0 && t >= 1.4F) {
                deathStep = 1;
                DevShowcase.shot(mc, "revue_mort_butin");
                check("mort_animations_finies", () -> {
                    if (!open) throw new AssertionError("ferme a " + t + " s");
                });
            } else if (deathStep == 1 && t >= 2.4F) {
                deathStep = 2;
                check("mort_butin_visible", () -> {
                    if (!open) throw new AssertionError("ferme avant 2,4 s (butin perdu pas lisible)");
                });
            } else if (deathStep == 2 && t >= 4.5F) {
                deathStep = 3;
                check("mort_retour_auto", () -> {
                    if (open) throw new AssertionError("toujours ouvert a " + t + " s");
                });
            }
        }
        if (deathStep == 3 && tick < 10_000) {
            mc.gui.setScreen(null);
            MMORPG.LOGGER.info("[MMORPG] REVUE TERMINEE : {} echec(s)", failures);
            tick = 10_000;
        }
        if (tick == 10_010) DevShowcase.stop(mc);
    }

    private static KeyEvent key(int code) {
        return new KeyEvent(code, 0, 0);
    }

    private static java.lang.reflect.Field field(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                var f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object get(Object o, String name) throws ReflectiveOperationException {
        return field(o.getClass(), name).get(o);
    }

    private static void set(Object o, String name, Object v) throws ReflectiveOperationException {
        field(o.getClass(), name).set(o, v);
    }
}
