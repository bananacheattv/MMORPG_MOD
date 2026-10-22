package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.ClassSelectScreen;
import com.mmorpg.client.screen.ForgeScreen;
import com.mmorpg.client.screen.MenuScreen;
import com.mmorpg.client.screen.TeleporterScreen;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Vitrine automatique reservee au developpement : ouvre chaque interface, lance quelques commandes de test
 * et enregistre des captures dans run/screenshots. Active uniquement avec -Dmmorpg.showcase=true.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class DevShowcase {
    private static final String MODE = System.getProperty("mmorpg.showcase", "");
    private static final boolean ENABLED = !MODE.isEmpty() && !MODE.equals("false");
    private static final boolean QUEST_ONLY = MODE.equals("quetes");
    private static int tick = -1;
    private static net.minecraft.world.phys.Vec3 mountStart;

    private DevShowcase() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
        if (!ClientData.received) return;
        tick++;
        // la vitrine doit continuer meme si la fenetre perd le focus
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
        String me = mc.player.getName().getString();
        if (MODE.equals("editeur_workflow")) {
            if(tick==0) mc.getSingleplayerServer().execute(()->com.mmorpg.server.DevQuestEditorChecks.beginWorkflow(mc.getSingleplayerServer()));
            if(tick==5) {mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(3);mc.resizeGui();cmd(mc,"mmorpg editeurquetes");}
            var screen=mc.gui.screen();
            if(screen!=null) {
                int pw=screen.width-12,ph=screen.height-12,lw=Math.min(180,Math.max(120,pw/3)),rx=6+lw+20,rw=pw-lw-30,fy=66;
                if(tick==15) editorClick(screen,25,36);
                if(tick==20) editorClick(screen,40,6+ph-27);
                if(tick==25) {editorText(screen,"Nom du PNJ","Maëlys (validation)");editorText(screen,"Zone","Forêt ancienne");}
                if(tick==30) editorClick(screen,rx+30,6+ph-16);
                if(tick==40) shot(mc,"editeur_pnj_cree");
                if(tick==45) editorClick(screen,14+2*((lw-4)/3)+12,36);
                if(tick==50) editorClick(screen,40,111);
                if(tick==55) {
                    editorText(screen,"Titre de la quête","La mémoire des racines");
                    for(var child:screen.children()) if(child instanceof net.minecraft.client.gui.components.MultiLineEditBox box)
                        box.setValue("Les anciens arbres murmurent un nom oublié.\nMaëlys attend votre aide au cœur de la forêt.\nRapportez les ressources demandées pour retrouver la mémoire des racines.");
                }
                if(tick==60) shot(mc,"editeur_lore_redige");
                if(tick==65) editorClick(screen,rx+3*(rw/4)+15,38);
                if(tick==70) editorClick(screen,rx+30,fy+10);
                if(tick==75) editorText(screen,"Rechercher","Maëlys");
                if(tick==80) editorClick(screen,40,68);
                if(tick==85) editorClick(screen,rx+2*(rw/4)+15,38);
                if(tick==90) editorClick(screen,rx+3*(rw/4)+15,fy);
                if(tick==95) editorClick(screen,rx+30,fy+45);
                if(tick==100) editorText(screen,"Rechercher","Ailes Célestes");
                if(tick==105) shot(mc,"editeur_choix_cosmetique");
                if(tick==110) editorClick(screen,40,68);
                if(tick==115) editorClick(screen,rx+30,6+ph-16);
                if(tick==125) shot(mc,"editeur_quete_enregistree");
                if(tick==130) mc.getSingleplayerServer().execute(()->com.mmorpg.server.DevQuestEditorChecks.endWorkflow(mc.getSingleplayerServer()));
                if(tick==140) stop(mc);
            }
            return;
        }
        if (MODE.equals("editeur_quetes")) {
            if(tick==5) {mc.getWindow().setWindowed(1600,900);cmd(mc,"mmorpg editeurquetes");}
            if(tick==20) shot(mc,"editeur_quetes_lore");
            if(tick==25 && mc.gui.screen() instanceof com.mmorpg.client.screen.QuestEditorScreen s) s.showPage(1);
            if(tick==35) shot(mc,"editeur_quetes_objectifs");
            if(tick==40 && mc.gui.screen() instanceof com.mmorpg.client.screen.QuestEditorScreen s) s.showPage(2);
            if(tick==50) shot(mc,"editeur_quetes_recompenses");
            if(tick==55 && mc.gui.screen() instanceof com.mmorpg.client.screen.QuestEditorScreen s) s.showPage(3);
            if(tick==65) shot(mc,"editeur_quetes_conditions");
            if(tick==70) mc.getWindow().setWindowed(1280,720);
            if(tick==80) shot(mc,"editeur_quetes_petit_ecran");
            if(tick==90) stop(mc);
            return;
        }
        if (MODE.equals("cosmetiques_importes")) {
            if(tick==5) mc.getWindow().setWindowed(1600,900);
            var list=java.util.Arrays.stream(Cosmetic.values()).filter(Cosmetic::hasModel).toList();
            for(int i=0;i<list.size();i++) {
                if(tick==15+i*20) mc.gui.setScreen(new com.mmorpg.client.screen.CosmeticsScreen(list.get(i)));
                if(tick==25+i*20) shot(mc,"cosmetique_3d_"+list.get(i).id);
            }
            if(tick==225) stop(mc);
            return;
        }
        if (MODE.equals("modeles_importes")) {
            if(tick==5) mc.getWindow().setWindowed(1600,900);
            for(int zone=0;zone<6;zone++) {
                if(tick==15+zone*25) mc.gui.setScreen(new com.mmorpg.client.screen.ImportedGalleryScreen(zone));
                if(tick==30+zone*25) shot(mc,"modeles_zone_"+zone);
            }
            if(tick==175) stop(mc);
            return;
        }
        if (MODE.equals("lucky_interface")) {
            var pos = mc.player.blockPosition().offset(2, 0, 0);
            if (tick == 5) {
                mc.getWindow().setWindowed(1280, 720);
                cmd(mc, "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " mmorpg:lucky_block");
            }
            if (tick == 15) {
                var data = new CompoundTag(); data.putLong("pos", pos.asLong());
                mc.gui.setScreen(new com.mmorpg.client.screen.LuckyBlockScreen(data));
            }
            if (tick == 25) shot(mc, "lucky_interface_avant");
            if (tick == 30 && mc.gui.screen() instanceof com.mmorpg.client.screen.LuckyBlockScreen s) s.start();
            if (tick == 55) shot(mc, "lucky_interface_animation");
            if (tick == 130) shot(mc, "lucky_interface_resultat");
            if (tick == 140) stop(mc);
            return;
        }
        if (MODE.equals("bestiaire_dropdown")) {
            if (tick == 5) { mc.getWindow().setWindowed(1280, 720); MenuScreen.open(MenuScreen.Tab.BESTIAIRE); }
            if (tick == 15 || tick == 35) {
                var screen = mc.gui.screen();
                int x = (screen.width - Math.min(screen.width - 12, 440)) / 2 + 30;
                int y = (screen.height - Math.min(screen.height - 12, 272)) / 2 + (tick == 15 ? 48 : 66);
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y,
                        new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            }
            if (tick == 25) shot(mc, "bestiaire_types_deroulants");
            if (tick == 30) MenuScreen.open(MenuScreen.Tab.BESTIAIRE);
            if (tick == 45) shot(mc, "bestiaire_familles_deroulantes");
            if (tick == 55) stop(mc);
            return;
        }
        if (QUEST_ONLY) {
            questPhase(mc, me);
            return;
        }
        if (MODE.equals("tpA") || MODE.equals("tpB")) {
            if (MODE.equals("tpA")) teleporterAdminPhase(mc);
            else teleporterPlayerPhase(mc);
            return;
        }
        if (MODE.equals("competences")) {
            skillPhase(mc, me);
            return;
        }
        if (MODE.equals("armes")) {
            weaponPhase(mc, me);
            return;
        }
        if (MODE.equals("batons")) {
            staffPhase(mc, me);
            return;
        }
        if (MODE.equals("integration")) {
            integrationPhase(mc, me);
            return;
        }
        if (MODE.equals("structures")) {
            structuresPhase(mc, me);
            return;
        }
        if (MODE.equals("reprise_montures")) {
            if (tick == 5) {
                mc.getWindow().setWindowed(1280, 720); mc.gui.setScreen(null);
                cmd(mc, "gamemode creative " + me);
                cmd(mc, "tp " + me + " 30 220 0 0 10");
                cmd(mc, "forceload add 25 -5 35 5");

                cmd(mc, "time set day");
            }
            if (tick == 25) { cmd(mc, "fill 25 219 -5 35 219 5 minecraft:sea_lantern"); cmd(mc, "fill 25 220 -5 35 224 5 minecraft:air"); cmd(mc, "gamemode survival " + me); cmd(mc, "tp " + me + " 30 220 0 0 10"); }
            if (tick == 110) { DevContentChecks.verify(); mc.gui.setScreen(new com.mmorpg.client.screen.MountsScreen()); }
            if (tick == 125) shot(mc, "codex_collection_montures");
            if (tick == 130) ClientNet.send(new Payloads.MountAction(Payloads.MountAction.UNLOCK, "voyageur"));
            if (tick == 140) { mc.gui.setScreen(null); ClientNet.send(new Payloads.MountAction(Payloads.MountAction.SUMMON, "voyageur")); }
            if (tick == 170) {
                var mount = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).filter(e -> e instanceof com.mmorpg.entity.MountEntity).findFirst().orElseThrow();
                mc.gameMode.interact(mc.player, mount, new net.minecraft.world.phys.EntityHitResult(mount), net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            if (tick == 175) { mountStart = mc.player.position(); mc.options.keyUp.setDown(true); }
            if (tick == 185) {
                mc.options.keyUp.setDown(false);
                mc.gui.hud.getChat().clearMessages(false);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                if (mountStart.distanceTo(mc.player.position()) < .2) throw new IllegalStateException("Mount movement missing");
                MMORPG.LOGGER.info("[CODEX CHECKS] MOUNT MOVEMENT PASS");
            }
            if (tick == 190) {
                if (!(mc.player.getVehicle() instanceof com.mmorpg.entity.MountEntity)) throw new IllegalStateException("Client mount riding missing");
                shot(mc, "codex_monture_en_jeu");
            }
            if (tick == 200) { cmd(mc, "forceload remove 25 -5 35 5"); ClientNet.send(new Payloads.MountAction(Payloads.MountAction.DISMISS, "")); mc.options.setCameraType(CameraType.FIRST_PERSON); stop(mc); }
            return;
        }
        if (MODE.equals("reprise_tab")) {
            if (tick == 5) { mc.getWindow().setWindowed(1280, 720); mc.gui.setScreen(null); }
            if (tick == 110) shot(mc, "codex_tableau_eldoria");
            if (tick == 115) mc.options.keyPlayerList.setDown(true);
            if (tick == 130) {
                var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info == null || info.getTabListDisplayName() == null || !info.getTabListDisplayName().getString().contains(ClientData.DATA.playerClass.label)) throw new IllegalStateException("Missing RPG tab name");
                MMORPG.LOGGER.info("[CODEX CHECKS] TAB PASS: synchronized class and level name");
                shot(mc, "codex_tab_eldoria");
            }
            if (tick == 140) { mc.options.keyPlayerList.setDown(false); stop(mc); }
            return;
        }
        if (MODE.equals("reprise_quetes")) {
            if (tick == 5) { mc.getWindow().setWindowed(1280, 720); mc.gui.setScreen(null); cmd(mc, "mmorpg pnj quetes_groupe cryptes 2 Gardien des Cryptes"); }
            if (tick == 15) MenuScreen.open(MenuScreen.Tab.QUETES);
            if (tick == 30) shot(mc, "codex_journal_quetes");
            if (tick == 40) stop(mc);
            return;
        }
        if (MODE.equals("reprise_equipement")) {
            if (tick == 5) { DevContentChecks.verify(); mc.getWindow().setWindowed(1280, 720); }
            String[] sets = {"valkyrie", "eternel_arcanes", "sentinelle_astrale", "egide_divine"};
            String[] classes = {"guerrier", "mage", "archer", "tank"};
            int index = (tick - 10) / 40, stage = (tick - 10) % 40;
            if (tick >= 10 && index < 4) {
                if (stage == 0) {
                    mc.gui.setScreen(null);
                    cmd(mc, "gamemode survival " + me);
                    cmd(mc, "mmorpg classe " + me + " " + classes[index]);
                    cmd(mc, "mmorpg niveau " + me + " 100");
                    String[] slots = {"head", "chest", "legs", "feet"};
                    String[] pieces = {"casque", "plastron", "jambieres", "bottes"};
                    for (int i = 0; i < 4; i++) cmd(mc, "item replace entity " + me + " armor." + slots[i] + " with mmorpg:" + sets[index] + "_" + pieces[i]);
                }
                if (stage == 15) MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
                if (stage == 25) shot(mc, "codex_armure_" + sets[index]);
            }
            if (tick == 175) stop(mc);
            return;
        }
        if (MODE.equals("reprise_mobs")) {
            if (tick == 5) {
                cmd(mc, "gamemode creative " + me);
                cmd(mc, "tp " + me + " 0 220 0 0 10");
                cmd(mc, "time set midnight");
            }
            if (tick == 15) cmd(mc, "fill -12 219 -12 12 219 12 minecraft:sea_lantern");
            if (tick == 20) {
                cmd(mc, "execute positioned -3 220 5 run mmorpg mob zombie_des_cryptes 15");
                cmd(mc, "execute positioned 0 220 5 run mmorpg mob araignee_venimeuse 25");
                cmd(mc, "execute positioned 3 220 5 run mmorpg mob bandit_arbaletrier 40");
            }
            if (tick == 100) {
                java.util.Set<String> found = new java.util.HashSet<>();
                for (var entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof com.mmorpg.entity.RpgMob mob) found.add(mob.mobKey());
                }
                if (!found.containsAll(java.util.List.of("zombie_des_cryptes", "araignee_venimeuse", "bandit_arbaletrier")))
                    throw new IllegalStateException("Missing monster: " + found);
                MMORPG.LOGGER.info("[CODEX CHECKS] MOBS PASS: three new entities loaded");
                shot(mc, "codex_nouveaux_monstres");
            }
            if (tick == 110) MenuScreen.open(MenuScreen.Tab.BESTIAIRE);
            if (tick == 120) shot(mc, "codex_bestiaire_etendu");
            if (tick == 130) stop(mc);
            return;
        }
        if (MODE.equals("reprise")) {
            if (tick == 5) {
                for (PlayerClass cls : PlayerClass.values()) {
                    if (!cls.isPlayable()) continue;
                    var test = new com.mmorpg.rpg.PlayerData();
                    test.playerClass = cls;
                    for (int lv = 1; lv <= 100; lv++) {
                        test.level = lv;
                        test.unlockSkills();
                        for (var skill : com.mmorpg.skill.Skills.forClass(cls)) {
                            if (test.isSkillUnlocked(skill) != (lv >= skill.unlockLevel)) throw new IllegalStateException("Skill unlock " + skill.id + " level " + lv);
                        }
                        if (test.skillBar.length != 6) throw new IllegalStateException("Skill bar size");
                    }
                    if (test.skillRanks.size() != 11) throw new IllegalStateException("Skill count " + cls);
                }
                MMORPG.LOGGER.info("[CODEX CHECKS] SKILLS PASS: 44 skills, levels 1-100, six slots");
                mc.getWindow().setWindowed(1280, 720);
                cmd(mc, "mmorpg classe " + me + " guerrier");
                cmd(mc, "mmorpg niveau " + me + " 100");
            }
            if (tick == 30) MenuScreen.open(MenuScreen.Tab.COMPETENCES);
            if (tick == 40) shot(mc, "codex_competences");
            if (tick == 45 && mc.gui.screen() != null) mc.gui.screen().mouseScrolled(100, 100, 0, -10);
            if (tick == 55) shot(mc, "codex_competences_suite");
            if (tick == 60) MenuScreen.open(MenuScreen.Tab.BESTIAIRE);
            if (tick == 70) shot(mc, "codex_bestiaire");
            if (tick == 80) MenuScreen.open(MenuScreen.Tab.FAMILIERS);
            if (tick == 90) shot(mc, "codex_familiers");
            if (tick == 95) {
                ClientNet.send(new Payloads.CosmeticAction(Cosmetic.Category.AURA.ordinal(), "aura_flammes"));
                ClientNet.send(new Payloads.CosmeticAction(Cosmetic.Category.AILES.ordinal(), "ailes_angeliques"));
            }
            if (tick == 105) {
                if (!"aura_flammes".equals(ClientData.DATA.equippedCosmetics.get(Cosmetic.Category.AURA))
                        || !"ailes_angeliques".equals(ClientData.DATA.equippedCosmetics.get(Cosmetic.Category.AILES)))
                    throw new IllegalStateException("Cosmetic equipment sync failed");
                MMORPG.LOGGER.info("[CODEX CHECKS] COSMETICS PASS: equipped and synchronized");
                MenuScreen.open(MenuScreen.Tab.COSMETIQUES);
            }
            if (tick == 115) shot(mc, "codex_cosmetiques");
            if (tick == 120) { mc.gui.setScreen(null); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
            if (tick == 160) shot(mc, "codex_cosmetiques_jeu");
            if (tick == 170) { mc.options.setCameraType(CameraType.FIRST_PERSON); stop(mc); }
            return;
        }
        if (MODE.equals("ecrans")) {          // inventaire du mod et ecran Personnage (8 attributs)
            if (tick == 5) mc.getWindow().setWindowed(1280, 720);
            if (tick == 20) cmd(mc, "gamemode survival " + me);
            if (tick == 40) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            if (tick == 60) shot(mc, "77_inventaire");
            if (tick == 62) MMORPG.LOGGER.info("[MMORPG] TOUCHE E : ecran = {} (attendu CharacterScreen)", mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName());
            if (tick == 65) {
                mc.gui.setScreen(null);
                mc.options.guiScale().set(2);
                mc.resizeGui();
                MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
            }
            if (tick == 85) shot(mc, "78_personnage_attributs");
            if (tick == 88) {
                mc.options.guiScale().set(0);
                mc.resizeGui();
            }
            if (tick == 90) {
                mc.gui.setScreen(null);
                cmd(mc, "gamemode creative " + me);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (ecrans)");
            }
            if (tick == 100) stop(mc);
            return;
        }
        if (MODE.equals("clic")) {
            clickPhase(mc, me);
            return;
        }
        if (MODE.equals("hud")) {
            hudPhase(mc, me);
            return;
        }
        if (MODE.equals("clicreel")) {                  // clics par le vrai chemin d'entree (MouseHandler), comme une vraie souris
            realClickPhase(mc, me);
            return;
        }
        if (MODE.equals("casser")) {
            breakPhase(mc, me);
            return;
        }
        if (MODE.equals("hub")) {
            hubPhase(mc, me);
            return;
        }
        if (MODE.equals("cinematique") || MODE.equals("cineverif")) {
            cinematicPhase(mc, me);
            return;
        }
        if (MODE.equals("duelliste")) {
            var srv = mc.getSingleplayerServer();
            switch (tick) {
                case 5 -> {
                    cmd(mc, "kill @e[type=mmorpg:mannequin_entrainement]");
                    cmd(mc, "tp " + me + " ~ ~ ~ -90 10");
                }
                case 15 -> srv.execute(() -> spawnDuelist(srv, me));
                case 25 -> srv.execute(() -> duelState(srv, me, "avant", 1.0));
                case 60 -> shot(mc, "duelliste_avertissement");
                case 120 -> srv.execute(() -> duelState(srv, me, "apres un coup", -1));
                case 125 -> srv.execute(() -> duelState(srv, me, "reglage vie 20 %", 0.2));
                case 135 -> srv.execute(() -> duelState(srv, me, "avant coups sous le seuil", -1));
                case 260 -> srv.execute(() -> duelState(srv, me, "apres 4 s sous le seuil (inchange attendu)", -1));
                case 270 -> {
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                    stop(mc);
                }
                default -> {
                }
            }
            return;
        }
        if (MODE.equals("hubambiance")) {
            var srv = mc.getSingleplayerServer();
            switch (tick) {
                case 2 -> {
                    com.mmorpg.config.ConfigManager.general().hubCinematic = false;
                    cmd(mc, "gamemode creative");
                }
                case 5 -> cmd(mc, "hub set");
                case 900 -> cmd(mc, "hub");
                case 960 -> shot(mc, "ambiance_citadelle");
                case 1100 -> srv.execute(() -> hubSpot(srv, me, com.mmorpg.rpg.PlayerClass.GUERRIER));
                case 1180 -> shot(mc, "ambiance_pavillon");
                case 1250 -> {
                    MMORPG.LOGGER.info("[MMORPG] Musique du hub active = {}", "voir journal ci-dessus");
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                    stop(mc);
                }
                default -> {
                }
            }
            return;
        }
        if (MODE.equals("instances")) {
            instancesPhase(mc, me);
            return;
        }
        if (MODE.startsWith("cine_")) {
            cinePhase(mc, me, MODE.substring(5));
            return;
        }
        if (MODE.startsWith("epreuves_")) {
            trialsPhase(mc, me, MODE.substring(9));
            return;
        }
        if (MODE.startsWith("donjon_")) {
            dungeonPhase(mc, me, MODE.substring(7));
            return;
        }
        if (MODE.equals("zone")) {
            var srv = mc.getSingleplayerServer();
            switch (tick) {
                case 5 -> {
                    mc.getWindow().setWindowed(1280, 720);
                    cmd(mc, "item replace entity " + me + " weapon.mainhand with mmorpg:baguette_zone");
                    cmd(mc, "execute in minecraft:overworld run tp " + me + " 3000 150 3000 45 20");
                }
                case 12 -> srv.execute(() -> zoneStep(srv, me, 5));
                case 15 -> srv.execute(() -> zoneStep(srv, me, 0));
                case 90 -> shot(mc, "zone_selection");
                case 95 -> cmd(mc, "zone creer Jardin de test");
                case 112 -> shot(mc, "zone_titre");
                case 118 -> {
                    cmd(mc, "zone ici");
                    cmd(mc, "gamemode survival");
                }
                case 130 -> srv.execute(() -> zoneStep(srv, me, 1));
                case 135 -> srv.execute(() -> zoneStep(srv, me, 2));
                case 140 -> cmd(mc, "zone regle jardin_de_test construction autorise");
                case 150 -> srv.execute(() -> zoneStep(srv, me, 3));
                case 155 -> srv.execute(() -> zoneStep(srv, me, 4));
                case 160 -> {
                    cmd(mc, "zone supprimer jardin_de_test");
                    cmd(mc, "gamemode creative");
                }
                case 175 -> {
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                    stop(mc);
                }
                default -> {
                }
            }
            return;
        }
        if (MODE.equals("sceptre")) {
            switch (tick) {
                case 5 -> {
                    mc.getWindow().setWindowed(1280, 720);
                    cmd(mc, "item replace entity " + me + " weapon.mainhand with mmorpg:sceptre_quetes");
                }
                case 20 -> mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                case 40 -> {
                    MMORPG.LOGGER.info("[MMORPG] SCEPTRE clic droit dans le vide : ecran = {} (attendu QuestEditorScreen)", screenName(mc));
                    shot(mc, "sceptre_atelier");
                    mc.gui.setScreen(null);
                    cmd(mc, "kill @e[type=mmorpg:pnj]");
                    cmd(mc, "mmorpg ville Eldoria");
                }
                case 70 -> {
                    cmd(mc, "item replace entity " + me + " weapon.mainhand with mmorpg:sceptre_quetes");
                    cmd(mc, "tp " + me + " ~-2.5 ~ ~ 90 15");
                }
                case 90 -> talkTo(mc, nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES));
                case 120 -> {
                    MMORPG.LOGGER.info("[MMORPG] SCEPTRE clic sur un PNJ : ecran = {} (attendu QuestEditorScreen, fiche PNJ)", screenName(mc));
                    shot(mc, "sceptre_fiche_pnj");
                }
                case 140 -> {
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                    stop(mc);
                }
                default -> {
                }
            }
            return;
        }
        if (MODE.equals("hubdesign")) {
            hubDesignPhase(mc, me);
            return;
        }
        if (MODE.equals("mannequin2")) {
            dummyTiltPhase(mc, me);
            return;
        }
        if (MODE.equals("mannequin")) {
            dummyPhase(mc, me);
            return;
        }
        if (MODE.equals("familiers")) {
            petsPhase(mc, me);
            return;
        }
        if (MODE.equals("mpA") || MODE.equals("mpB")) {
            multiplayerPhase(mc, me, MODE.equals("mpA"));
            return;
        }
        switch (tick) {
            case 40 -> mc.gui.setScreen(new ClassSelectScreen());
            case 70 -> shot(mc, "01_choix_classe");
            case 75 -> {
                ClientNet.send(new Payloads.SelectClass(PlayerClass.GUERRIER.ordinal()));
                mc.gui.setScreen(null);
            }
            case 90 -> {
                cmd(mc, "time set day");
                cmd(mc, "weather clear");
                cmd(mc, "mmorpg niveau " + me + " 60");
                cmd(mc, "mmorpg familier " + me + " tous");
                cmd(mc, "mmorpg cosmetique " + me + " tous");
                cmd(mc, "give " + me + " mmorpg:epee_seigneur_guerre");
                cmd(mc, "item replace entity " + me + " armor.head with mmorpg:seigneur_guerre_casque");
                cmd(mc, "item replace entity " + me + " armor.chest with mmorpg:seigneur_guerre_plastron");
                cmd(mc, "item replace entity " + me + " armor.legs with mmorpg:seigneur_guerre_jambieres");
                cmd(mc, "item replace entity " + me + " armor.feet with mmorpg:seigneur_guerre_bottes");
                cmd(mc, "give " + me + " mmorpg:acier_orc 16");
                cmd(mc, "give " + me + " mmorpg:defense_orc 8");
                cmd(mc, "give " + me + " mmorpg:couronne_roi_gobelin 2");
                cmd(mc, "give " + me + " mmorpg:piece_or 99");
                cmd(mc, "give " + me + " mmorpg:pierre_amelioration 5");
            }
            case 110 -> mc.player.getInventory().setSelectedSlot(firstSlotWithSword(mc));
            case 130 -> MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
            case 160 -> shot(mc, "02_personnage");
            case 165 -> MenuScreen.open(MenuScreen.Tab.COMPETENCES);
            case 195 -> shot(mc, "03_competences");
            case 200 -> MenuScreen.open(MenuScreen.Tab.FAMILIERS);
            case 230 -> shot(mc, "04_familiers");
            case 235 -> MenuScreen.open(MenuScreen.Tab.COSMETIQUES);
            case 265 -> shot(mc, "05_cosmetiques");
            case 270 -> MenuScreen.open(MenuScreen.Tab.BESTIAIRE);
            case 300 -> shot(mc, "06_bestiaire");
            case 305 -> mc.gui.setScreen(new ForgeScreen(mc.player.blockPosition()));
            case 335 -> shot(mc, "07_forge");
            case 340 -> mc.gui.setScreen(new TeleporterScreen(fakeTeleporters()));
            case 370 -> shot(mc, "08_teleporteur");
            case 375 -> {
                mc.gui.setScreen(null);
                ClientNet.send(new Payloads.CosmeticAction(Cosmetic.Category.AILES.ordinal(), Cosmetic.AILES_ANGELIQUES.id));
                ClientNet.send(new Payloads.CosmeticAction(Cosmetic.Category.AURA.ordinal(), Cosmetic.AURA_FLAMMES.id));
                ClientNet.send(new Payloads.CosmeticAction(Cosmetic.Category.HALO.ordinal(), Cosmetic.HALO_DORE.id));
                ClientNet.send(new Payloads.PetAction("phenix"));
                cmd(mc, "execute as " + me + " at @s positioned ^ ^ ^5 run mmorpg mob gobelin 58");
                cmd(mc, "execute as " + me + " at @s positioned ^1 ^ ^6 run mmorpg mob orc_guerrier 62");
                cmd(mc, "execute as " + me + " at @s positioned ^-2 ^ ^7 run mmorpg mob squelette_maudit 55");
            }
            case 400 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 430 -> ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 1, ""));
            case 440 -> ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 0, ""));
            case 446 -> shot(mc, "09_combat_3e_personne");
            case 450 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 460 -> ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 2, ""));
            case 468 -> shot(mc, "10_hud_combat");
            case 480 -> cmd(mc, "execute as " + me + " at @s positioned ^ ^ ^9 run mmorpg boss roi_gobelin");
            case 530 -> shot(mc, "11_boss");
            case 540 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 560 -> shot(mc, "12_cosmetiques_face");
            case 570 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            }
            // ---------------- phase 2 : ville, donjon, forge
            case 580 -> {
                cmd(mc, "kill @e[type=mmorpg:roi_gobelin]");
                cmd(mc, "mmorpg ville Capitale d'Eldoria");
                cmd(mc, "give " + me + " minecraft:iron_ingot 10");
                cmd(mc, "give " + me + " minecraft:stick 10");
                cmd(mc, "give " + me + " minecraft:cobblestone 10");
                cmd(mc, "setblock ~2 ~ ~2 mmorpg:forge_arcanique");
            }
            case 600 -> {
                net.minecraft.core.BlockPos forge = mc.player.blockPosition().offset(2, 0, 2);
                ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.CRAFT, "masse_garde", 1, forge));
                ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.CRAFT, "pierre_amelioration", 1, forge));
            }
            case 615 -> {
                net.minecraft.core.BlockPos forge = mc.player.blockPosition().offset(2, 0, 2);
                int slot = -1;
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    if (mc.player.getInventory().getItem(i).getItem() instanceof com.mmorpg.item.RpgEquipment) {
                        slot = i;
                        break;
                    }
                }
                if (slot >= 0) ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.UPGRADE, "", slot, forge));
            }
            case 630 -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    var st = mc.player.getInventory().getItem(i);
                    if (!st.isEmpty()) sb.append(st.getHoverName().getString()).append(" x").append(st.getCount()).append(" | ");
                }
                MMORPG.LOGGER.info("[MMORPG] INVENTAIRE : {}", sb);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 640 -> shot(mc, "13_ville");
            case 645 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                cmd(mc, "execute as " + me + " at @s positioned ~40 ~ ~ run mmorpg donjon crypte_gobeline");
            }
            case 665 -> cmd(mc, "tp " + me + " ~40 ~ ~6 0 10");
            case 700 -> shot(mc, "14_donjon_entree");
            case 705 -> cmd(mc, "tp " + me + " ~ ~ ~60 0 10");
            case 740 -> shot(mc, "15_donjon_boss");
            case 745 -> cmd(mc, "mmorpg teleporteurs");
            case 770 -> stop(mc);
            default -> {
            }
        }
    }

    private static com.mmorpg.entity.NpcEntity nearestNpc(Minecraft mc, com.mmorpg.entity.NpcEntity.Role role) {
        com.mmorpg.entity.NpcEntity best = null;
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.mmorpg.entity.NpcEntity n && n.role() == role
                    && (best == null || n.distanceToSqr(mc.player) < best.distanceToSqr(mc.player))) best = n;
        }
        return best;
    }

    private static void talkTo(Minecraft mc, com.mmorpg.entity.NpcEntity npc) {
        if (npc == null) {
            MMORPG.LOGGER.warn("[MMORPG] PNJ introuvable pour la vitrine");
            return;
        }
        mc.gameMode.interact(mc.player, npc, new net.minecraft.world.phys.EntityHitResult(npc), net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    /** Phase de test des quetes, de la boutique et du groupe (-Pshowcase=quetes). */
    private static void questPhase(Minecraft mc, String me) {
        switch (tick) {
            case 20 -> {
                cmd(mc, "time set day");
                cmd(mc, "weather clear");
                cmd(mc, "mmorpg quetes " + me + " reset");
                cmd(mc, "kill @e[type=mmorpg:pnj]");
                cmd(mc, "mmorpg ville Eldoria");
                cmd(mc, "mmorpg or " + me + " 500");
                cmd(mc, "give " + me + " mmorpg:oreille_gobelin 12");
                cmd(mc, "give " + me + " mmorpg:croc_loup 7");
            }
            case 50 -> cmd(mc, "tp " + me + " ~-2.5 ~ ~ 90 15");
            case 70 -> talkTo(mc, nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES));
            case 100 -> shot(mc, "16_maitre_des_quetes");
            case 105 -> {
                var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES);
                int id = npc == null ? -1 : npc.getId();
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, "premiers_pas", id));
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, "apprentissage", id));
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.ACCEPT, "contrat_chasse", id));
            }
            case 130 -> shot(mc, "17_quetes_acceptees");
            case 135 -> {
                mc.gui.setScreen(null);
                for (int i = 0; i < 6; i++) {
                    cmd(mc, "execute as " + me + " at @s positioned ~" + (i % 3 - 1) + " ~ ~" + (i < 3 ? 2 : -2) + " run mmorpg mob gobelin 5");
                }
            }
            case 155, 190 -> {
                int n = 0;
                for (var e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.mmorpg.entity.mob.GoblinEntity g && g.distanceToSqr(mc.player) < 100) n++;
                }
                MMORPG.LOGGER.info("[MMORPG] Gobelins proches (tick {}) : {} — mana {}", tick, n, ClientData.mana);
            }
            case 160 -> {
                ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 1, ""));
                MMORPG.LOGGER.info("[MMORPG] Barre : {}", String.join(",", ClientData.DATA.skillBar));
            }
            case 200 -> shot(mc, "18_suivi_des_quetes");
            case 205 -> MenuScreen.open(MenuScreen.Tab.QUETES);
            case 230 -> shot(mc, "19_journal_quetes");
            case 235 -> {
                mc.gui.setScreen(null);
                talkTo(mc, nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES));
            }
            case 255 -> {
                var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES);
                int id = npc == null ? -1 : npc.getId();
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.COMPLETE, "premiers_pas", id));
                ClientNet.send(new Payloads.QuestAction(Payloads.QuestAction.COMPLETE, "apprentissage", id));
            }
            case 285 -> shot(mc, "20_quetes_rendues");
            case 290 -> {
                mc.gui.setScreen(null);
                cmd(mc, "tp " + me + " ~5 ~ ~ -90 15");
            }
            case 310 -> talkTo(mc, nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.MARCHAND));
            case 330 -> {
                var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.MARCHAND);
                ClientNet.send(new Payloads.ShopAction(Payloads.ShopAction.BUY, 0, 5, npc == null ? -1 : npc.getId()));
            }
            case 350 -> shot(mc, "21_marchand_achat");
            case 355 -> {
                if (mc.gui.screen() instanceof com.mmorpg.client.screen.ShopScreen shop) shop.showSellTab();
            }
            case 380 -> shot(mc, "22_marchand_vente");
            case 385 -> MenuScreen.open(MenuScreen.Tab.GROUPE);
            case 410 -> shot(mc, "23_groupe");
            case 415 -> {
                mc.gui.setScreen(null);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                cmd(mc, "tp " + me + " ~-5 ~ ~4 180 10");
            }
            case 440 -> shot(mc, "24_place_pnj");
            case 445 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            }
            case 455 -> stop(mc);
            default -> {
            }
        }
    }

    private static int baseX, baseY, baseZ;

    /** Lance les 24 competences actives (4 classes) devant des gobelins et capture chaque effet. */
    private static void skillPhase(Minecraft mc, String me) {
        String[] classes = {"guerrier", "mage", "archer", "tank"};
        String[][] skills = {
                {"frappe_puissante", "cri_de_guerre", "tourbillon", "charge_brutale", "rage_sanguinaire", "fureur_divine"},
                {"boule_de_feu", "nova_de_givre", "eclair_en_chaine", "teleportation_arcanique", "bouclier_de_mana", "meteore_celeste"},
                {"tir_percant", "pluie_de_fleches", "saut_arriere", "fleche_explosive", "volee_de_fleches", "tempete_divine"},
                {"coup_de_bouclier", "provocation", "forteresse", "onde_de_choc", "aura_sacree", "rempart_du_titan"}};
        int[][] delay = {{6, 7, 6, 6, 9, 25}, {9, 9, 4, 5, 9, 19}, {3, 20, 5, 9, 5, 24}, {4, 7, 9, 9, 10, 10}};
        if (tick == 20) {
            baseX = mc.player.getBlockX();
            baseZ = mc.player.getBlockZ();
            baseY = 150;
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "fill " + (baseX - 12) + " " + (baseY - 1) + " " + (baseZ - 12) + " " + (baseX + 12) + " " + (baseY - 1) + " " + (baseZ + 12) + " minecraft:polished_andesite");
            cmd(mc, "tp " + me + " " + (baseX + 0.5) + " " + baseY + " " + (baseZ + 0.5) + " 0 20");
            cmd(mc, "clear " + me);
        }
        int t = tick - 40;
        if (t < 0) return;
        int perClass = 50 + 6 * 60;
        int ci = t / perClass, k = t % perClass;
        if (ci >= 4) {
            if (t == 4 * perClass + 5) {
                if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (competences)");
            }
            if (t == 4 * perClass + 15) stop(mc);
            return;
        }
        if (k == 0) {
            cmd(mc, "mmorpg classe " + me + " " + classes[ci]);
            cmd(mc, "mmorpg niveau " + me + " 100");
        }
        if (k == 12) {
            for (int i = 0; i < 6; i++) ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.ASSIGN, i, skills[ci][i]));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        int si = (k - 50) / 60, ks = (k - 50) % 60;
        if (k >= 50 && si < 6) {
            if (ks == 0) {
                cmd(mc, "kill @e[type=mmorpg:gobelin]");
                cmd(mc, "mmorpg soin " + me);
                cmd(mc, "tp " + me + " " + (baseX + 0.5) + " " + baseY + " " + (baseZ + 0.5) + " 0 20");
                for (int g = -1; g <= 1; g++) {
                    cmd(mc, "execute positioned " + (baseX + 0.5 + g * 2) + " " + baseY + " " + (baseZ + 5.5 + Math.abs(g)) + " run mmorpg mob gobelin 60");
                }
                mc.gui.hud.getChat().clearMessages(false);
            }
            if (ks == 8) ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, si, ""));
            if (ks == 8 + delay[ci][si]) shot(mc, "60_" + ci + si + "_" + skills[ci][si]);
        }
    }

    /** Controle des arcs, marteaux et epees 3D : 3e personne, 1re personne, traction de l'arc et inventaire. */
    private static void weaponPhase(Minecraft mc, String me) {
        String[] hot = {"arc_chasseur", "arc_aube_divine", "masse_garde", "marteau_egide", "epee_recrue", "excalibur_celeste"};
        String[] inv = {"arc_elfique", "arc_tempete", "arc_faucon", "arc_spectral", "marteau_bastion", "marteau_colosse", "masse_gardien",
                "marteau_titan", "lame_runique", "epee_berserker", "epee_seigneur_guerre", "lame_demoniaque"};
        if (tick == 20) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "clear " + me);
            for (int i = 0; i < hot.length; i++) cmd(mc, "item replace entity " + me + " hotbar." + i + " with mmorpg:" + hot[i]);
            for (int i = 0; i < inv.length; i++) cmd(mc, "item replace entity " + me + " inventory." + i + " with mmorpg:" + inv[i]);
            cmd(mc, "tp " + me + " ~ ~ ~ 0 0");
        }
        int t = tick - 50;
        if (t >= 0 && t < 6 * 50) {
            int slot = t / 50, k = t % 50;
            if (k == 0) {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                mc.gui.hud.getChat().clearMessages(false);
            }
            if (k == 15) shot(mc, "50_" + slot + "_" + hot[slot] + "_3e");
            if (k == 20) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            }
            if (k == 35) shot(mc, "51_" + slot + "_" + hot[slot] + "_1re");
        }
        // traction de l'arc niveau 100 (3e personne, de face) : etapes pulling_0, pulling_1, pulling_2
        int u = tick - 360;
        if (u == 0) {
            mc.player.getInventory().setSelectedSlot(1);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (u == 10) {
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        if (u == 14) shot(mc, "52_arc_traction_0");
        if (u == 25) shot(mc, "52_arc_traction_1");
        if (u == 40) shot(mc, "52_arc_traction_2");
        if (u == 45) {
            MMORPG.LOGGER.info("[MMORPG] ARC utilise={} duree={}", mc.player.isUsingItem(), mc.player.getTicksUsingItem());
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (u == 60) shot(mc, "53_arc_traction_1re");
        if (u == 62) mc.options.keyUse.setDown(false);
        if (u == 80) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if (u == 100) shot(mc, "54_inventaire");
        if (u == 105) {
            mc.gui.setScreen(null);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (armes)");
        }
        if (u == 115) stop(mc);
    }

    /**
     * Controle de l'integration des modeles Blockbench : armures 3D portees (face et dos) avec une arme 3D en main,
     * puis chaque boss anime (apparition, repos, combat, mort).
     */
    private static void integrationPhase(Minecraft mc, String me) {
        String[][] sets = {{"acier_soldat", "epee_recrue"}, {"sorcier", "baton_flammes"}, {"tireur_elite", "arc_faucon"},
                {"paladin", "masse_gardien"}, {"dieu_guerre", "excalibur_celeste"}, {"egide_divine", "marteau_egide"}};
        String[] bosses = {"roi_gobelin", "liche_ancienne", "seigneur_ignis", "titan_glace", "avatar_neant"};
        int[] dist = {9, 9, 11, 15, 11};
        int[] pitch = {-8, -8, -12, -16, -12};
        String cleanup = "gobelin,squelette_maudit,chevalier_neant,projectile_magique,projectile_sort,roi_gobelin,liche_ancienne,"
                + "seigneur_ignis,titan_glace,avatar_neant";
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 20) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "clear " + me);
            // sans cosmetiques ni familier, sur une plateforme degagee (le Titan mesure 6 blocs)
            for (Cosmetic.Category cat : Cosmetic.Category.values()) ClientNet.send(new Payloads.CosmeticAction(cat.ordinal(), ""));
            ClientNet.send(new Payloads.PetAction(""));
            cmd(mc, "fill ~-24 180 ~-32 ~24 180 ~12 minecraft:smooth_stone");
            for (int i = 0; i < sets.length; i++) cmd(mc, "item replace entity " + me + " hotbar." + i + " with mmorpg:" + sets[i][1]);
            cmd(mc, "tp " + me + " ~ 181 ~ 180 0");
        }
        int t = tick - 40;
        if (t >= 0 && t < sets.length * 40) {
            int i = t / 40, k = t % 40;
            String set = sets[i][0];
            if (k == 0) {
                for (String[] piece : new String[][]{{"head", "casque"}, {"chest", "plastron"}, {"legs", "jambieres"}, {"feet", "bottes"}}) {
                    cmd(mc, "item replace entity " + me + " armor." + piece[0] + " with mmorpg:" + set + "_" + piece[1]);
                }
                mc.player.getInventory().setSelectedSlot(i);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            }
            if (k == 18) shot(mc, "60_" + i + "_" + set + "_face");
            if (k == 20) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (k == 36) shot(mc, "61_" + i + "_" + set + "_dos");
        }
        int u = tick - (50 + sets.length * 40);
        if (u >= 0 && u < bosses.length * 260) {
            int i = u / 260, k = u % 260;
            String key = bosses[i];
            if (k == 0) {
                cmd(mc, "gamemode creative " + me);
                for (String type : cleanup.split(",")) cmd(mc, "kill @e[type=mmorpg:" + type + "]");
                cmd(mc, "kill @e[type=minecraft:item]");
                cmd(mc, "tp " + me + " ~ ~ ~ 180 " + pitch[i]);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            }
            if (k == 5) cmd(mc, "execute at " + me + " positioned ~ ~ ~-" + dist[i] + " run mmorpg boss " + key);
            if (k == 45) shot(mc, "62_" + i + "_" + key + "_apparition");
            if (k == 100) shot(mc, "63_" + i + "_" + key + "_repos");
            if (k == 105) cmd(mc, "gamemode survival " + me);
            if (k == 135 || k == 155 || k == 175) shot(mc, "64_" + i + "_" + key + "_combat_" + (k - 135) / 20);
            if (k == 185) {
                cmd(mc, "gamemode creative " + me);
                cmd(mc, "kill @e[type=mmorpg:" + key + "]");
            }
            if (k == 205) shot(mc, "65_" + i + "_" + key + "_mort_0");
            if (k == 235) shot(mc, "65_" + i + "_" + key + "_mort_1");
        }
        if (u == bosses.length * 260 + 5) {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (integration)");
        }
        if (u == bosses.length * 260 + 15) stop(mc);
    }

    private static net.minecraft.core.BlockPos structOrigin;

    /**
     * Controle des structures multiblocs (autel, teleporteur, forge) : vue d'ensemble eteinte puis active, gros plans,
     * collision de la plateforme de l'autel, interface d'amelioration avec un trefle et amelioration reelle.
     */
    private static void structuresPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 20) {
            cmd(mc, "difficulty peaceful");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "clear " + me);
            for (Cosmetic.Category cat : Cosmetic.Category.values()) ClientNet.send(new Payloads.CosmeticAction(cat.ordinal(), ""));
            ClientNet.send(new Payloads.PetAction(""));
            cmd(mc, "fill ~-30 180 ~-30 ~30 180 ~30 minecraft:smooth_stone");
            cmd(mc, "fill ~-30 181 ~-30 ~30 190 ~30 minecraft:air");
            cmd(mc, "tp " + me + " ~ 181 ~ 180 0");
        }
        if (tick == 28) cmd(mc, "kill @e[type=!minecraft:player,distance=..80]");
        if (tick == 30) {
            structOrigin = mc.player.blockPosition();
            cmd(mc, "setblock ~-15 ~ ~-12 mmorpg:forge_arcanique[facing=south]");
            cmd(mc, "setblock ~ ~ ~-14 mmorpg:autel_invocation[facing=south]");
            cmd(mc, "setblock ~16 ~ ~-12 mmorpg:teleporteur[facing=south]");
        }
        if (structOrigin == null) return;
        int oy = structOrigin.getY();
        if (tick == 45) {
            look(mc, me, 0, 8, 12, 0, -14, 18);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick == 75) shot(mc, "70_structures_eteintes");
        if (tick == 80) cmd(mc, "setblock " + structOrigin.getX() + " " + oy + " " + (structOrigin.getZ() - 14) + " mmorpg:autel_invocation[facing=south,lit=true]");
        // gros plans (a moins de 7 blocs, la forge s'allume et le portail s'ouvre)
        if (tick == 100) look(mc, me, -12, 2, -5.5, -14.5, -11.5, 14);
        if (tick == 140) shot(mc, "71_forge_active");
        if (tick == 145) look(mc, me, 14, 0.5, -5.5, 16.5, -11.5, -6);
        if (tick == 185) shot(mc, "72_teleporteur_actif");
        if (tick == 190) look(mc, me, 6.5, 3, -6.5, 0.5, -13.5, 20);
        if (tick == 230) shot(mc, "73_autel_actif");
        if (tick == 235) look(mc, me, -11, 4, -20, -14.5, -11.5, 18);
        if (tick == 270) shot(mc, "74_forge_dos");
        // collisions : le joueur (mode survie) est lache au centre de cellules choisies
        if (tick == 275) {
            cmd(mc, "gamemode survival " + me);
            drop(mc, me, 0, -12);          // marches de l'autel (attendu 0.5)
        }
        if (tick == 305) {
            MMORPG.LOGGER.info("[MMORPG] COLLISION marches autel : y relatif = {} (attendu 0.5)", mc.player.getY() - oy);
            drop(mc, me, 2, -14);          // plateforme de l'autel, sur le rebord (attendu 0.625)
        }
        if (tick == 335) {
            MMORPG.LOGGER.info("[MMORPG] COLLISION plateforme autel : y relatif = {} (attendu 0.625)", mc.player.getY() - oy);
            drop(mc, me, 16, -12);         // bloc fonctionnel du teleporteur = sol du passage
        }
        if (tick == 365) {
            MMORPG.LOGGER.info("[MMORPG] COLLISION passage teleporteur : y relatif = {} (attendu 0.375)", mc.player.getY() - oy);
            drop(mc, me, -13, -13);        // etabli de la forge (attendu 1.3125)
        }
        if (tick == 395) {
            MMORPG.LOGGER.info("[MMORPG] COLLISION etabli forge : y relatif = {} (attendu 1.3125)", mc.player.getY() - oy);
            cmd(mc, "gamemode creative " + me);
        }
        // amelioration +12 -> +13 avec le trefle du palier +13 a +16, puis trefle d'un autre palier (refuse)
        if (tick == 400) {
            look(mc, me, -15, 0, -9, -14.5, -11.5, 20);
            cmd(mc, "item replace entity " + me + " hotbar.0 with mmorpg:epee_seigneur_guerre[mmorpg:upgrade=12]");
            cmd(mc, "give " + me + " mmorpg:pierre_amelioration_sup 5");
            cmd(mc, "give " + me + " mmorpg:trefle_celeste 1");
            cmd(mc, "give " + me + " mmorpg:trefle_chance 2");
            cmd(mc, "give " + me + " mmorpg:piece_or 640");
        }
        net.minecraft.core.BlockPos forge = structOrigin.offset(-15, 0, -12);
        if (tick == 420) {
            try {
                var f = ForgeScreen.class.getDeclaredField("tab");
                f.setAccessible(true);
                f.setInt(null, com.mmorpg.crafting.ForgeRecipes.Category.values().length);
            } catch (ReflectiveOperationException e) {
                MMORPG.LOGGER.warn("[MMORPG] vitrine : onglet amelioration introuvable", e);
            }
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.gui.setScreen(new ForgeScreen(forge));
        }
        if (tick == 445) shot(mc, "75_forge_trefle");
        if (tick == 450) {
            mc.gui.setScreen(null);
            ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.UPGRADE, "trefle_celeste", 0, forge));
        }
        if (tick == 470) {
            MMORPG.LOGGER.info("[MMORPG] TREFLE CELESTE : epee = +{} (attendu 13), trefles celestes restants = {} (attendu 0)",
                    com.mmorpg.item.RpgEquipment.upgrade(mc.player.getInventory().getItem(0)), countItem(mc, com.mmorpg.registry.ModItems.TREFLE_CELESTE.get()));
            ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.UPGRADE, "trefle_chance", 0, forge));
        }
        if (tick == 490) {
            MMORPG.LOGGER.info("[MMORPG] TREFLE HORS PALIER : epee = +{} (attendu 13), trefles de chance restants = {} (attendu 2)",
                    com.mmorpg.item.RpgEquipment.upgrade(mc.player.getInventory().getItem(0)), countItem(mc, com.mmorpg.registry.ModItems.TREFLE_CHANCE.get()));
            look(mc, me, 16, 0, -5, 16.5, -11.5, 0);            // devant le teleporteur
        }
        // teleporteur : passer sous l'arche ouvre la liste des destinations
        if (tick == 510) MMORPG.LOGGER.info("[MMORPG] TELEPORTEUR devant l'arche : ecran = {} (attendu aucun)", screenName(mc));
        if (tick == 512) look(mc, me, 16, 0.4, -12, 16.5, -20, 0);   // dans le passage
        if (tick == 540) {
            MMORPG.LOGGER.info("[MMORPG] TELEPORTEUR dans le passage : ecran = {} (attendu TeleporterScreen)", screenName(mc));
            shot(mc, "76_teleporteur_passage");
        }
        // inventaire unique : la touche E (ecran d'inventaire de Minecraft) ouvre l'inventaire du mod
        if (tick == 545) {
            mc.gui.setScreen(null);
            cmd(mc, "gamemode survival " + me);
            look(mc, me, 0, 0, 6, 0, 0, 0);
        }
        if (tick == 555) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if (tick == 575) {
            MMORPG.LOGGER.info("[MMORPG] INVENTAIRE : ecran = {} (attendu CharacterScreen)", screenName(mc));
            shot(mc, "77_inventaire");
        }
        if (tick == 580) MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
        if (tick == 600) shot(mc, "78_personnage_attributs");
        // base d'invocation : le boss ne peut pas en sortir
        if (tick == 605) {
            mc.gui.setScreen(null);
            cmd(mc, "gamemode creative " + me);
            cmd(mc, String.format(java.util.Locale.ROOT, "execute positioned %d %d %d run mmorpg boss roi_gobelin",
                    structOrigin.getX(), structOrigin.getY(), structOrigin.getZ() + 12));
        }
        if (tick == 650) cmd(mc, String.format(java.util.Locale.ROOT, "tp @e[type=mmorpg:roi_gobelin] %d %d %d",
                structOrigin.getX() + 25, structOrigin.getY(), structOrigin.getZ() + 12));
        if (tick == 665) MMORPG.LOGGER.info("[MMORPG] BASE BOSS : apres avoir ete pousse a 25 blocs, distance = {} (attendu < 3)", bossDistance(mc));
        if (tick == 670) {
            cmd(mc, "gamemode survival " + me);
            look(mc, me, 22, 0, 12, 0, 12, 0);              // joueur a 22 blocs de la base
        }
        if (tick == 790) MMORPG.LOGGER.info("[MMORPG] BASE BOSS : joueur a 22 blocs, distance du boss a sa base = {} (attendu <= 13)", bossDistance(mc));
        if (tick == 795) {
            cmd(mc, "gamemode creative " + me);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (structures)");
        }
        if (tick == 805) stop(mc);
    }

    private static int countItem(Minecraft mc, net.minecraft.world.item.Item item) {
        int n = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        }
        return n;
    }

    /** Distance horizontale entre le Roi gobelin et sa base (point d'apparition), vue par le serveur integre. */
    private static String bossDistance(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return "?";
        var level = server.getLevel(mc.player.level().dimension());
        for (var e : level.getEntitiesOfClass(com.mmorpg.entity.boss.GoblinKingBoss.class, mc.player.getBoundingBox().inflate(80))) {
            var h = e.getHomePosition();
            return String.format(java.util.Locale.ROOT, "%.1f (rayon %d)", Math.sqrt(Math.pow(e.getX() - h.getX() - 0.5, 2) + Math.pow(e.getZ() - h.getZ() - 0.5, 2)),
                    e.getHomeRadius());
        }
        return "boss introuvable";
    }

    private static String screenName(Minecraft mc) {
        return mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName();
    }

    /** Place le joueur en (dx, dy, dz) par rapport a l'origine de la vitrine, tourne vers le point (tx, tz). */
    // ------------------------------------------------------------------ familiers 3D (-Pshowcase=familiers)

    private static final java.util.List<com.mmorpg.entity.PetEntity> GALLERY = new java.util.ArrayList<>();
    private static net.minecraft.core.BlockPos petOrigin;

    private static void camera(Minecraft mc, String me, double dx, double dy, double dz, float yaw, float pitch) {
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f %.1f", me, petOrigin.getX() + 0.5 + dx, petOrigin.getY() + dy,
                petOrigin.getZ() + 0.5 + dz, yaw, pitch));
    }

    private static void walk(Minecraft mc, String me, double step) {
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s ~%.2f ~ ~", me, step));
    }

    private static String petInfo(Minecraft mc) {
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.mmorpg.entity.PetEntity p && !GALLERY.contains(p) && p.owner() == mc.player) {
                return String.format(java.util.Locale.ROOT, "%s vol=%s dy=%.2f dist=%.2f vitesse=%.3f immobile=%d", p.petType(), p.isFlying(),
                        p.getY() - mc.player.getY(), p.distanceTo(mc.player), p.animSpeed, p.stillTicks);
            }
        }
        return "aucun familier";
    }

    private static String serverPetInfo(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return "?";
        var level = server.getLevel(mc.player.level().dimension());
        var sp = level.getPlayerByUUID(mc.player.getUUID());
        for (var e : level.getEntitiesOfClass(com.mmorpg.entity.PetEntity.class, sp.getBoundingBox().inflate(32))) {
            return String.format(java.util.Locale.ROOT, "%s dy=%.2f dist=%.2f", e.petType(), e.getY() - sp.getY(), e.distanceTo(sp));
        }
        return "aucun";
    }

    private static void forceFocus(Minecraft mc) {
        try {
            var f = com.mojang.blaze3d.platform.Window.class.getDeclaredField("focused");
            f.setAccessible(true);
            f.setBoolean(mc.getWindow(), true);
        } catch (ReflectiveOperationException e) {
            MMORPG.LOGGER.warn("[MMORPG] CLICREEL focus impossible : {}", e.toString());
        }
    }

    private static void realMove(Minecraft mc, double gx, double gy) {
        var w = mc.getWindow();
        mc.mouseHandler.onMove(w.handle(), gx * w.getGuiScale(), gy * w.getGuiScale(), 0, 0);
    }

    private static void realButton(Minecraft mc, boolean down) {
        mc.mouseHandler.onButton(mc.getWindow().handle(), new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0),
                down ? com.mojang.blaze3d.platform.InputConstants.PRESS : com.mojang.blaze3d.platform.InputConstants.RELEASE);
    }

    private static int[] dragFrom;
    private static Object[] itemTest;

    private static void realClickPhase(Minecraft mc, String me) {
        forceFocus(mc);                                          // comme une fenetre active : mouvements et glisser transmis
        if (tick == 5) {
            int[] size = java.util.Arrays.stream(System.getProperty("mmorpg.clicsize", "1280x720:2").split("[x:]")).mapToInt(Integer::parseInt).toArray();
            mc.getWindow().setWindowed(size[0], size[1]);
            mc.options.guiScale().set(size[2]);
            mc.resizeGui();
        }
        if (tick == 20) MMORPG.LOGGER.info("[MMORPG] CLICREEL fenetre {}x{} echelle {} -> interface {}x{}", mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                mc.getWindow().getGuiScale(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        if (tick == 30) MenuScreen.open(MenuScreen.Tab.QUETES);
        int[] tab = null;
        if (mc.gui.screen() != null) {
            var s = mc.gui.screen();
            int pw = Math.min(s.width - 12, 440), ph = Math.min(s.height - 12, 272);
            int left = (s.width - pw) / 2, top = (s.height - ph) / 2;
            int tw = (pw - 16 - 20) / MenuScreen.Tab.values().length;
            tab = new int[]{left + 8 + tw + tw / 2, top + 15, left + 8 + tw / 2};
        }
        if (tick == 45) realMove(mc, tab[0] - 10, tab[1] + 3);
        if (tick == 46) realMove(mc, tab[0], tab[1]);
        if (tick == 48) realButton(mc, true);
        if (tick == 49) realButton(mc, false);
        if (tick == 52) MMORPG.LOGGER.info("[MMORPG] CLICREEL M : onglet Competences -> {} (attendu SkillsScreen)", screenName(mc));
        if (tick == 55) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));   // touche E
        if (tick == 60) MMORPG.LOGGER.info("[MMORPG] CLICREEL E : ecran = {}", screenName(mc));
        if (tick == 62) realMove(mc, tab[0] + 3, tab[1] + 2);       // onglet Competences dans l'ecran Personnage
        if (tick == 64) realButton(mc, true);
        if (tick == 65) realButton(mc, false);
        if (tick == 66) MMORPG.LOGGER.info("[MMORPG] CLICREEL E : onglet Competences -> {} (attendu SkillsScreen)", screenName(mc));
        if (tick == 67) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if (tick == 68 && mc.gui.screen() instanceof com.mmorpg.client.screen.CharacterScreen cs) {   // deplacer un objet : clic, puis clic
            var slots = mc.player.inventoryMenu.slots;
            net.minecraft.world.inventory.Slot from = null, to = null;
            for (int i = 9; i < 45; i++) {
                var sl = slots.get(i);
                if (from == null && sl.hasItem()) from = sl;
                else if (to == null && !sl.hasItem() && i < 36) to = sl;
            }
            itemTest = new Object[]{from, to, from == null ? "" : from.getItem().getHoverName().getString()};
            int[] a = cs.slotCenter(from);
            realMove(mc, a[0], a[1]);
        }
        if (tick == 69) realButton(mc, true);
        if (tick == 70) realButton(mc, false);
        if (tick == 71) {
            int[] b = ((com.mmorpg.client.screen.CharacterScreen) mc.gui.screen()).slotCenter((net.minecraft.world.inventory.Slot) itemTest[1]);
            realMove(mc, b[0], b[1]);
        }
        if (tick == 72) realButton(mc, true);
        if (tick == 73) realButton(mc, false);
        if (tick == 75) {
            var from = (net.minecraft.world.inventory.Slot) itemTest[0];
            var to = (net.minecraft.world.inventory.Slot) itemTest[1];
            MMORPG.LOGGER.info("[MMORPG] CLICREEL E : objet « {} » deplace de l'emplacement {} vers {} -> depart vide={} arrivee={} (attendu : vide=true, arrivee={})",
                    itemTest[2], from.index, to.index, !from.hasItem(), to.getItem().getHoverName().getString(), itemTest[2]);
            int[] b = ((com.mmorpg.client.screen.CharacterScreen) mc.gui.screen()).slotCenter(to);      // remet l'objet a sa place
            realMove(mc, b[0], b[1]);
        }
        if (tick == 76) realButton(mc, true);
        if (tick == 77) realButton(mc, false);
        if (tick == 78) {
            int[] a = ((com.mmorpg.client.screen.CharacterScreen) mc.gui.screen()).slotCenter((net.minecraft.world.inventory.Slot) itemTest[0]);
            realMove(mc, a[0], a[1]);
        }
        if (tick == 79) realButton(mc, true);
        if (tick == 80) realButton(mc, false);

        if (tick == 90) mc.gui.setScreen(new com.mmorpg.client.screen.HudEditorScreen(null));
        if (tick == 95) {
            var s = mc.gui.screen();
            int[] r = com.mmorpg.client.hud.HudLayout.JOUEUR.rect(s.width, s.height);
            dragFrom = new int[]{r[0] + 20, r[1] + 20, r[0], r[1]};
            realMove(mc, dragFrom[0], dragFrom[1]);
        }
        if (tick == 97) realButton(mc, true);
        if (tick > 97 && tick <= 115) realMove(mc, dragFrom[0] + (tick - 97) * 6, dragFrom[1] + (tick - 97) * 8);
        if (tick == 116) realButton(mc, false);
        if (tick == 118) {
            var s = mc.gui.screen();
            int[] r = com.mmorpg.client.hud.HudLayout.JOUEUR.rect(s.width, s.height);
            MMORPG.LOGGER.info("[MMORPG] CLICREEL glisser cadre joueur : ({},{}) -> ({},{}) (attendu environ +108,+144)", dragFrom[2], dragFrom[3], r[0], r[1]);
            shot(mc, "86_clicreel_glisser");
        }
        if (tick == 120) {
            var s = mc.gui.screen();
            int pwid = 268, ph = 86;
            int px = (s.width - pwid) / 2, py = s.height / 2 - ph / 2 - 10;
            int bw = (pwid - 16 - 9) / 4;
            realMove(mc, px + 8 + 2 * (bw + 3) + bw / 2.0, py + ph - 24 + 8);   // Annuler
        }
        if (tick == 122) realButton(mc, true);
        if (tick == 123) realButton(mc, false);
        if (tick == 126) MMORPG.LOGGER.info("[MMORPG] CLICREEL bouton Annuler -> ecran = {} (attendu aucun), cadre joueur personnalise = {}",
                screenName(mc), com.mmorpg.client.hud.HudLayout.JOUEUR.customized());
        if (tick == 130) {
            mc.options.guiScale().set(2);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (clicreel)");
            stop(mc);
        }
    }

    // ------------------------------------------------------------------ cassage des structures (-Pshowcase=casser)

    private static net.minecraft.core.BlockPos breakOrigin;
    private static net.minecraft.core.BlockPos breakTarget;
    private static int breakStart;

    /** {nom, bloc, decalage du bloc fonctionnel, mode creatif, Maj enfoncee, outil} */
    private static final Object[][] BREAK_TESTS = {
            {"forge (pioche, Maj)", "mmorpg:forge_arcanique", 0, false, true, "minecraft:diamond_pickaxe"},
            {"autel (pioche, Maj)", "mmorpg:autel_invocation", 1, false, true, "minecraft:diamond_pickaxe"},
            {"teleporteur (pioche, Maj, op)", "mmorpg:teleporteur", 2, false, true, "minecraft:diamond_pickaxe"},
            {"forge (main nue, Maj)", "mmorpg:forge_arcanique", 0, false, true, "minecraft:air"},
            {"autel (main nue, Maj)", "mmorpg:autel_invocation", 1, false, true, "minecraft:air"},
            {"forge (pioche, sans Maj)", "mmorpg:forge_arcanique", 0, false, false, "minecraft:diamond_pickaxe"},
            {"forge (epee, sans Maj : menu attendu)", "mmorpg:forge_arcanique", 0, false, false, "mmorpg:epee_seigneur_guerre"},
            {"forge (creatif, Maj)", "mmorpg:forge_arcanique", 0, true, true, "minecraft:air"},
            {"autel (creatif, Maj, epee)", "mmorpg:autel_invocation", 1, true, true, "mmorpg:epee_seigneur_guerre"},
    };

    private static void breakPhase(Minecraft mc, String me) {
        if (tick == 5) {
            breakOrigin = new net.minecraft.core.BlockPos(107, 181, -689);
            cmd(mc, "time set day");
            cmd(mc, "kill @e[type=item]");
        }
        int per = 260;
        int k = (tick - 20) / per;
        int t = (tick - 20) % per;
        if (tick < 20 || k >= BREAK_TESTS.length) {
            if (k == BREAK_TESTS.length && t == 0) {
                mc.options.keyShift.setDown(false);
                cmd(mc, "gamemode creative " + me);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (casser)");
            }
            if (k == BREAK_TESTS.length && t == 10) stop(mc);
            return;
        }
        Object[] test = BREAK_TESTS[k];
        int x0 = breakOrigin.getX() + 8 * ((int) test[2] - 1);
        int z0 = breakOrigin.getZ() - 6;
        var kind = com.mmorpg.block.structure.MultiblockKind.values()[0];
        if (t == 0) {
            mc.options.keyShift.setDown(false);
            cmd(mc, "gamemode " + ((boolean) test[3] ? "creative " : "survival ") + me);
            cmd(mc, String.format("fill %d %d %d %d %d %d air", x0 - 4, breakOrigin.getY(), z0 - 4, x0 + 4, breakOrigin.getY() + 5, z0 + 4));
            cmd(mc, "kill @e[type=item]");
        }
        if (t == 5) cmd(mc, String.format("setblock %d %d %d %s[facing=south]", x0, breakOrigin.getY(), z0, test[1]));
        if (t == 10) {
            cmd(mc, "item replace entity " + me + " weapon.mainhand with " + test[5]);
            // vise une partie de la structure (bloc invisible juste au-dessus du bloc fonctionnel)
            breakTarget = new net.minecraft.core.BlockPos(x0, breakOrigin.getY() + 1, z0);
            for (int dx : new int[]{0, 1, -1, 2, -2}) {                 // premiere partie non vide (le teleporteur a une arche creuse)
                var p = new net.minecraft.core.BlockPos(x0 + dx, breakOrigin.getY() + 1, z0);
                if (!mc.level.getBlockState(p).isAir()) {
                    breakTarget = p;
                    break;
                }
            }
            cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.1f %d %.1f 180 30", me, x0 + 0.5, breakOrigin.getY(), z0 + 3.5));
        }
        if (t == 20) {
            var st = mc.level.getBlockState(breakTarget);
            MMORPG.LOGGER.info("[MMORPG] CASSER {} : cible {} = {}", test[0], breakTarget.toShortString(), st.getBlock().getName().getString());
            mc.options.keyShift.setDown((boolean) test[4]);
        }
        if (t == 25) {
            breakStart = tick;
            mc.gameMode.startDestroyBlock(breakTarget, net.minecraft.core.Direction.SOUTH);
        }
        if (t > 25 && t < 240 && !mc.level.getBlockState(breakTarget).isAir()) {
            mc.gameMode.continueDestroyBlock(breakTarget, net.minecraft.core.Direction.SOUTH);
        }
        if (t == 245) {
            var server = mc.getSingleplayerServer();
            var level = server.getLevel(mc.player.level().dimension());
            boolean masterGone = level.getBlockState(new net.minecraft.core.BlockPos(x0, breakOrigin.getY(), z0)).isAir();
            boolean partGone = level.getBlockState(breakTarget).isAir();
            int drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(breakTarget).inflate(6)).size();
            MMORPG.LOGGER.info("[MMORPG] CASSER {} : partie cassee={} bloc fonctionnel casse={} objets au sol={} ecran={}",
                    test[0], partGone, masterGone, drops, screenName(mc));
            mc.gui.setScreen(null);
        }
    }

    // ------------------------------------------------------------------ mannequins d'entrainement (-Pshowcase=mannequin)

    private static com.mmorpg.entity.dummy.TrainingDummyEntity dummyAt(Minecraft mc, int variant) {
        for (var e : mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(30))) {
            if (e.variant() == variant) return e;
        }
        return null;
    }

    private static com.mmorpg.entity.dummy.TrainingDummyEntity serverDummy(Minecraft mc, int variant) {
        var server = mc.getSingleplayerServer();
        var level = server.getLevel(mc.player.level().dimension());
        for (var e : level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(30))) {
            if (e.variant() == variant) return e;
        }
        return null;
    }

    private static String dummyStats(Minecraft mc, int variant) {
        var d = serverDummy(mc, variant);
        return d == null ? "absent" : String.format(java.util.Locale.ROOT, "%s : DPS=%d total=%d meilleur=%d coups=%d critiques=%d (client : DPS=%d total=%d)",
                d.variantId(), d.dps(), d.total(), d.best(), d.hits(), d.crits(), dummyAt(mc, variant).dps(), dummyAt(mc, variant).total());
    }

    /** Se place a 3 blocs devant un mannequin (par le sud) en le regardant. */
    private static void faceDummy(Minecraft mc, String me, int variant, double fromSide) {
        var d = dummyAt(mc, variant);
        if (d == null) return;
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f %.1f", me, d.getX() + fromSide, d.getY(), d.getZ() + 3.0, 180F - (float) Math.toDegrees(Math.atan2(fromSide, 3.0)), 8F));
    }

    private static void hitDummy(Minecraft mc, int variant) {
        var d = dummyAt(mc, variant);
        if (d == null) return;
        mc.gameMode.attack(mc.player, d);
    }

    private static void dummyPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 15) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "mmorpg classe " + me + " guerrier");
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "give " + me + " mmorpg:epee_seigneur_guerre");
            cmd(mc, "kill @e[type=mmorpg:mannequin_entrainement]");
            cmd(mc, "kill @e[type=mmorpg:roi_gobelin]");
        }
        if (tick == 30) {
            String[] v = {"standard", "blinde", "magique"};
            for (int i = 0; i < 3; i++) {
                cmd(mc, String.format(java.util.Locale.ROOT, "execute positioned %d 181 %d rotated 180 0 run mmorpg mannequin %s", 100 + i * 7, -690, v[i]));
            }
            mc.player.getInventory().setSelectedSlot(0);
        }
        if (tick == 36) {
            for (int i = 0; i < 3; i++) MMORPG.LOGGER.info("[MMORPG] MANNEQUIN pose : {} present={}", i, dummyAt(mc, i) != null);
            faceDummy(mc, me, 0, 0);
        }
        // 1) mannequin standard : 6 coups a l'epee (melee), cadence du cooldown
        for (int k = 0; k < 6; k++) if (tick == 50 + k * 12) hitDummy(mc, 0);
        if (tick == 54) shot(mc, "100_mannequin_coup");
        if (tick == 135) {
            MMORPG.LOGGER.info("[MMORPG] MANNEQUIN standard apres 6 coups : {}", dummyStats(mc, 0));
            shot(mc, "101_mannequin_panneau");
        }
        // 2) blinde : memes coups => moins de degats (defense physique)
        if (tick == 140) faceDummy(mc, me, 1, 0);
        for (int k = 0; k < 6; k++) if (tick == 150 + k * 12) hitDummy(mc, 1);
        if (tick == 235) MMORPG.LOGGER.info("[MMORPG] MANNEQUIN blinde apres 6 coups : {}", dummyStats(mc, 1));
        // 3) magique (melee) : moins de resistance physique que le blinde
        if (tick == 240) faceDummy(mc, me, 2, 0);
        for (int k = 0; k < 6; k++) if (tick == 250 + k * 12) hitDummy(mc, 2);
        if (tick == 335) {
            MMORPG.LOGGER.info("[MMORPG] MANNEQUIN magique (melee) apres 6 coups : {}", dummyStats(mc, 2));
            shot(mc, "102_mannequin_magique");
        }
        // 4) mage : sorts (degats magiques) sur le blindé puis sur le magique
        if (tick == 340) {
            cmd(mc, "mmorpg classe " + me + " mage");
            cmd(mc, "mmorpg niveau " + me + " 100");
            var sd = serverDummy(mc, 1);
            if (sd != null) sd.resetStats();
            sd = serverDummy(mc, 2);
            if (sd != null) sd.resetStats();
        }
        if (tick == 360) faceDummy(mc, me, 1, 0);
        for (int k = 0; k < 4; k++) if (tick == 375 + k * 30) ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 0, ""));
        if (tick == 520) MMORPG.LOGGER.info("[MMORPG] MANNEQUIN blinde (sorts) : {}", dummyStats(mc, 1));
        if (tick == 525) faceDummy(mc, me, 2, 0);
        for (int k = 0; k < 4; k++) if (tick == 540 + k * 30) ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 0, ""));
        if (tick == 685) {
            MMORPG.LOGGER.info("[MMORPG] MANNEQUIN magique (sorts) : {}", dummyStats(mc, 2));
            shot(mc, "103_mannequin_sorts");
        }
        // 5) remise a zero : accroupi + clic droit
        if (tick == 690) {
            var d = serverDummy(mc, 2);
            var server = mc.getSingleplayerServer();
            var sp = server.getPlayerList().getPlayerByName(me);
            sp.setShiftKeyDown(true);
            sp.interactOn(d, net.minecraft.world.InteractionHand.MAIN_HAND, d.position());
            sp.setShiftKeyDown(false);
        }
        if (tick == 700) MMORPG.LOGGER.info("[MMORPG] MANNEQUIN magique apres remise a zero : {}", dummyStats(mc, 2));
        if (tick == 706) shot(mc, "104_mannequin_reset");
        if (tick == 740) {
            // immortalite : degats enormes
            var sd = serverDummy(mc, 2);
            sd.hurtServer((net.minecraft.server.level.ServerLevel) sd.level(), sd.damageSources().generic(), 1.0E9F);
            MMORPG.LOGGER.info("[MMORPG] MANNEQUIN apres 1e9 degats vanilla : vivant={} {}", sd.isAlive(), dummyStats(mc, 2));
            cmd(mc, "mmorpg mannequin supprimer");
        }
        if (tick == 760) {
            MMORPG.LOGGER.info("[MMORPG] MANNEQUIN apres suppression : {} restants", mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(40)).size());
            cmd(mc, "gamemode creative " + me);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (mannequin)");
        }
        if (tick == 770) stop(mc);
    }

    /** Direction de l'inclinaison : coup venu du sud, camera a l'est (le mannequin doit pencher vers le nord, donc vers la droite de l'image). */
    private static void dummyTiltPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 15) {
            cmd(mc, "time set day");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg classe " + me + " guerrier");
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "kill @e[type=mmorpg:mannequin_entrainement]");
            cmd(mc, "kill @e[type=mmorpg:roi_gobelin]");
            cmd(mc, "execute positioned 107.5 181 -690.5 rotated 180 0 run mmorpg mannequin standard");
            cmd(mc, "tp " + me + " 112.5 181 -690.5 90 10");
        }
        int[] t = {60, 140, 220};          // 1) coup du sud  2) coup de l'est (camera)  3) coup de l'ouest
        double[][] from = {{107.5, -687.5}, {110.5, -690.5}, {104.5, -690.5}};
        for (int k = 0; k < 3; k++) {
            if (tick == t[k] - 2) {
                var d = serverDummy(mc, 0);
                var sp = mc.getSingleplayerServer().getPlayerList().getPlayerByName(me);
                if (d != null && sp != null) {
                    sp.snapTo(from[k][0], 181, from[k][1]);                 // l'attaquant se place la ou le coup arrive
                    com.mmorpg.combat.RpgCombat.dealDamage(sp, d, 800, false, false);
                    sp.snapTo(112.5, 181, -690.5, 90, 10);
                }
            }
            for (int dt : new int[]{3, 6, 10, 16}) {
                if (tick == t[k] + dt) {
                    var d = dummyAt(mc, 0);
                    MMORPG.LOGGER.info("[MMORPG] INCLINAISON coup {} +{} ticks : tiltX={} tiltZ={}", k + 1, dt, d.tiltX, d.tiltZ);
                    shot(mc, "110_incl_" + (k + 1) + "_" + dt);
                }
            }
        }
        if (tick == 300) {
            cmd(mc, "mmorpg mannequin supprimer");
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (mannequin2)");
        }
        if (tick == 310) stop(mc);
    }

    // ------------------------------------------------------------------ hub, essais de classe, choix (-Pshowcase=hub / cinematique)

    private static net.minecraft.server.level.ServerPlayer sp(Minecraft mc, String me) {
        return mc.getSingleplayerServer().getPlayerList().getPlayerByName(me);
    }

    private static com.mmorpg.entity.hub.ClassMasterEntity master(Minecraft mc, int kind) {
        var server = mc.getSingleplayerServer();
        var level = server.getLevel(mc.player.level().dimension());
        for (var e : level.getEntitiesOfClass(com.mmorpg.entity.hub.ClassMasterEntity.class, mc.player.getBoundingBox().inflate(120))) {
            if (e.kind() == kind) return e;
        }
        return null;
    }

    private static String hubState(Minecraft mc, String me) {
        var p = sp(mc, me);
        var d = com.mmorpg.server.RpgPlayers.get(p);
        var hd = com.mmorpg.hub.ClassTrial.data(p);
        int items = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (!p.getInventory().getItem(i).isEmpty()) items++;
        return String.format(java.util.Locale.ROOT, "classe=%s niveau=%d xp=%d or=%d essai=%s objets=%d main=%s tete=%s off=%s barre=%s pos=(%.0f,%.0f,%.0f)",
                d.playerClass, d.level, d.xp, d.gold, hd.inTrial, items, p.getMainHandItem().getHoverName().getString(),
                p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getHoverName().getString(), p.getOffhandItem().getHoverName().getString(), String.join(",", d.skillBar), p.getX(), p.getY(), p.getZ());
    }

    private static final int[] HUB_ORIGIN = {0, 96, 0};

    private static void hubView(Minecraft mc, String me, double dx, double dy, double dz, double tx, double ty, double tz) {
        double yaw = Math.toDegrees(Math.atan2(-(tx - dx), tz - dz));
        double pitch = -Math.toDegrees(Math.atan2(ty - dy, Math.hypot(tx - dx, tz - dz)));
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f %.1f", me, HUB_ORIGIN[0] + 0.5 + dx, HUB_ORIGIN[1] + dy - 1.62, HUB_ORIGIN[2] + 0.5 + dz, yaw, pitch));
    }

    private static int diamonds(Minecraft mc, String me) {
        var p = sp(mc, me);
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var st = p.getInventory().getItem(i);
            if (st.is(net.minecraft.world.item.Items.DIAMOND)) n += st.getCount();
        }
        return n;
    }

    private static void hubPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg classe " + me + " mage");
            cmd(mc, "mmorpg niveau " + me + " 20");
            cmd(mc, "give " + me + " minecraft:diamond 12");
            cmd(mc, "mmorpg or " + me + " 30");
            cmd(mc, String.format("tp %s %d %d %d", me, HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]));
        }
        if (tick == 30) cmd(mc, "mmorpg hub");
        if (tick == 60) cmd(mc, "hub");
        if (tick == 80) {
            MMORPG.LOGGER.info("[MMORPG] HUB construit : {} maitres, {} mannequins",
                    mc.level.getEntitiesOfClass(com.mmorpg.entity.hub.ClassMasterEntity.class, mc.player.getBoundingBox().inflate(120)).size(),
                    mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(120)).size());
            hubView(mc, me, -105, 72, 110, 8, 0, -18);
        }
        if (tick == 110) shot(mc, "120_hub_vue_generale");
        if (tick == 112) hubView(mc, me, -6, 5, -6, -22, 7, -22);
        if (tick == 135) shot(mc, "121_hub_zone_guerrier");
        if (tick == 137) hubView(mc, me, 6, 5, -6, 22, 7, -22);
        if (tick == 160) shot(mc, "122_hub_zone_mage");
        if (tick == 162) hubView(mc, me, -6, 5, 6, -22, 7, 22);
        if (tick == 185) shot(mc, "123_hub_zone_archer");
        if (tick == 187) hubView(mc, me, 6, 5, 6, 22, 7, 22);
        if (tick == 210) shot(mc, "124_hub_zone_tank");
        if (tick == 212) hubView(mc, me, 0, 7, 12, 0, 3, 24);
        if (tick == 235) shot(mc, "125_hub_porte_teleporteur");
        if (tick == 237) hubView(mc, me, 0, 6, -44, 0, 6, -74);
        if (tick == 270) shot(mc, "126_hub_salle_epreuve");
        if (tick == 272) hubView(mc, me, 34, 7, 0, 58, 3, 0);
        if (tick == 300) shot(mc, "127_hub_aire_entrainement");

        // ---- essai d'une classe : Guerrier niveau 50 depuis un joueur Mage niveau 20 avec sac
        if (tick == 303) {
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "item replace entity " + me + " armor.head with minecraft:iron_helmet");
            cmd(mc, "item replace entity " + me + " weapon.offhand with minecraft:shield");
        }
        if (tick == 305) {
            hubView(mc, me, -14, 1.7, -14, -20, 2.4, -20);
            MMORPG.LOGGER.info("[MMORPG] AVANT essai : {}", hubState(mc, me));
        }
        if (tick == 320) com.mmorpg.hub.HubManager.interact(sp(mc, me), master(mc, 0));
        if (tick == 345) {
            MMORPG.LOGGER.info("[MMORPG] DIALOGUE Maitre Guerrier ouvert : ecran = {}", screenName(mc));
            shot(mc, "128_dialogue_guerrier");
        }
        if (tick == 347) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CHOOSE, PlayerClass.GUERRIER.ordinal(), 0));
        if (tick == 352) MMORPG.LOGGER.info("[MMORPG] CHOISIR avec une classe deja choisie (refus attendu) : {}", hubState(mc, me));
        if (tick == 355) {
            mc.gui.setScreen(null);
            ClientNet.send(new Payloads.HubAction(Payloads.HubAction.TRY, PlayerClass.GUERRIER.ordinal(), 50));
        }
        if (tick == 400) {
            MMORPG.LOGGER.info("[MMORPG] PENDANT essai Guerrier 50 : {}", hubState(mc, me));
            shot(mc, "129_essai_guerrier");
        }
        // coups sur un mannequin : XP et or ne doivent pas bouger
        if (tick == 405) {
            var d = mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(40));
            if (!d.isEmpty()) {
                var t = d.get(0);
                cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f 0 8", me, t.getX(), t.getY(), t.getZ() + 3));
            }
        }
        for (int k = 0; k < 5; k++) if (tick == 420 + k * 12) {
            var d = mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, mc.player.getBoundingBox().inflate(6));
            if (!d.isEmpty()) mc.gameMode.attack(mc.player, d.get(0));
        }
        if (tick == 500) {
            MMORPG.LOGGER.info("[MMORPG] APRES coups : {}", hubState(mc, me));
            shot(mc, "130_essai_coups");
        }
        // coup mortel : le joueur doit etre relance, pas tue
        if (tick == 510) {
            var p = sp(mc, me);
            p.hurtServer((net.minecraft.server.level.ServerLevel) p.level(), p.damageSources().generic(), 1.0E9F);
            com.mmorpg.combat.RpgCombat.dealDamage(p, p, 1.0E9, false, false);
        }
        if (tick == 530) MMORPG.LOGGER.info("[MMORPG] APRES coup mortel (essai doit continuer) : vivant={} {}", sp(mc, me).isAlive(), hubState(mc, me));
        // changement de classe pendant l'essai
        if (tick == 535) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.TRY, PlayerClass.TANK.ordinal(), 75));
        if (tick == 560) MMORPG.LOGGER.info("[MMORPG] ESSAI change en Tank 75 : {}", hubState(mc, me));
        if (tick == 565) {
            mc.gui.setScreen(null);
            var g = master(mc, 4);
            if (g != null) com.mmorpg.hub.HubManager.interact(sp(mc, me), g);
        }
        if (tick == 590) {
            MMORPG.LOGGER.info("[MMORPG] DIALOGUE Gardien : ecran = {}", screenName(mc));
            shot(mc, "131_dialogue_gardien");
        }
        // sortie : restauration complete attendue (Mage 20, 12 diamants, 30 pieces d'or, position d'origine)
        if (tick == 592) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.QUIT, 0, 0));
        if (tick == 625) {
            mc.gui.setScreen(null);
            MMORPG.LOGGER.info("[MMORPG] APRES sortie : {} diamants={}", hubState(mc, me), diamonds(mc, me));
            shot(mc, "132_apres_restauration");
        }
        // reprise apres plantage : le joueur est sauvegarde en plein essai puis restaure a la connexion
        if (tick == 630) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.TRY, PlayerClass.ARCHER.ordinal(), 100));
        if (tick == 660) {
            var p = sp(mc, me);
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess());
            p.saveWithoutId(out);
            String saved = out.buildResult().toString();
            MMORPG.LOGGER.info("[MMORPG] SAUVEGARDE JOUEUR pendant l'essai : contient l'etat d'essai = {} ({} octets)", saved.contains("inTrial"), saved.length());
            com.mmorpg.hub.ClassTrial.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
        }
        if (tick == 690) MMORPG.LOGGER.info("[MMORPG] APRES reconnexion simulee : {} diamants={}", hubState(mc, me), diamonds(mc, me));
        // joueur sans classe : teleporteur ferme, choix interdit sans essai, puis essai + choix
        if (tick == 700) {
            cmd(mc, "mmorpg classe " + me + " aucune");
            sp(mc, me).getData(com.mmorpg.registry.ModAttachments.HUB).triedMask = 0;
            com.mmorpg.hub.HubManager.forceNonOperator = true; // joueur ordinaire : le choix exige un essai et le teleporteur exige une classe
        }
        if (tick == 715) {
            mc.gui.setScreen(null);
            MMORPG.LOGGER.info("[MMORPG] JOUEUR SANS CLASSE : {} operateur={}", hubState(mc, me), com.mmorpg.hub.HubManager.isOperator(sp(mc, me)));
            hubView(mc, me, -14, 1.7, -14, -20, 2.4, -20);
        }
        if (tick == 720) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CHOOSE, PlayerClass.MAGE.ordinal(), 0));
        if (tick == 730) MMORPG.LOGGER.info("[MMORPG] CHOISIR sans essai (refus attendu) : {}", hubState(mc, me));
        if (tick == 735) {
            var p = sp(mc, me);
            var tp = com.mmorpg.hub.HubLayout.pos(new net.minecraft.core.BlockPos(HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]), com.mmorpg.hub.HubLayout.TELEPORTER);
            com.mmorpg.server.TeleportManager.onUse((net.minecraft.server.level.ServerLevel) p.level(), tp, p);
        }
        if (tick == 750) MMORPG.LOGGER.info("[MMORPG] TELEPORTEUR sans classe : ecran = {} (attendu aucun)", screenName(mc));
        if (tick == 755) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.TRY, PlayerClass.MAGE.ordinal(), 25));
        if (tick == 785) MMORPG.LOGGER.info("[MMORPG] ESSAI Mage 25 : {}", hubState(mc, me));
        if (tick == 790) {
            var g = master(mc, 4);
            if (g != null) com.mmorpg.hub.HubManager.interact(sp(mc, me), g);
        }
        if (tick == 810) shot(mc, "133_choix_apres_essai");
        if (tick == 812) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CHOOSE, PlayerClass.MAGE.ordinal(), 0));
        if (tick == 840) {
            mc.gui.setScreen(null);
            MMORPG.LOGGER.info("[MMORPG] APRES choix du Mage : {}", hubState(mc, me));
            var p = sp(mc, me);
            var tp = com.mmorpg.hub.HubLayout.pos(new net.minecraft.core.BlockPos(HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]), com.mmorpg.hub.HubLayout.TELEPORTER);
            com.mmorpg.server.TeleportManager.onUse((net.minecraft.server.level.ServerLevel) p.level(), tp, p);
        }
        if (tick == 860) {
            com.mmorpg.hub.HubManager.forceNonOperator = false;
            MMORPG.LOGGER.info("[MMORPG] TELEPORTEUR avec classe : ecran = {} (attendu TeleporterScreen)", screenName(mc));
            mc.gui.setScreen(null);
        }
        // protection du hub : un joueur ordinaire en survie ne peut pas casser les blocs du hub ; /hub ramene au point d'apparition
        if (tick == 862) {
            var pl = sp(mc, me);
            var lvl = (net.minecraft.server.level.ServerLevel) pl.level();
            var target = new net.minecraft.core.BlockPos(HUB_ORIGIN[0] + 18, HUB_ORIGIN[1] - 1, HUB_ORIGIN[2]);
            com.mmorpg.hub.HubManager.forceNonOperator = true;
            pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            boolean before = !lvl.getBlockState(target).isAir();
            pl.gameMode.destroyBlock(target);
            boolean kept = !lvl.getBlockState(target).isAir();
            MMORPG.LOGGER.info("[MMORPG] PROTECTION : bloc present avant={} apres casse={} (attendu true/true) dansHub={}", before, kept,
                    com.mmorpg.hub.HubProtection.inHub(lvl, target));
            com.mmorpg.hub.HubManager.forceNonOperator = false;
            cmd(mc, "tp " + me + " " + (HUB_ORIGIN[0] + 200) + " " + (HUB_ORIGIN[1] + 5) + " " + (HUB_ORIGIN[2] + 200));
        }
        if (tick == 866) cmd(mc, "hub");
        if (tick == 880) {
            var pl = sp(mc, me);
            var sp0 = com.mmorpg.hub.HubLayout.center(new net.minecraft.core.BlockPos(HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]), com.mmorpg.hub.HubLayout.SPAWN);
            MMORPG.LOGGER.info("[MMORPG] COMMANDE /hub : distance au point d'apparition = {} (attendu < 2)", String.format(java.util.Locale.ROOT, "%.1f", pl.position().distanceTo(sp0)));
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (hub)");
        }
        if (tick == 890) stop(mc);
    }

    /** Revue visuelle du hub : construction puis captures sous des angles fixes ({@code {x, y, z, cibleX, cibleY, cibleZ}} relatifs a l'origine). */
    private static final Object[][] DESIGN_VIEWS = {
            {"aerienne", -105.0, 72.0, 110.0, 8.0, 0.0, -18.0},
            {"dessous", -80.0, -34.0, 90.0, 0.0, -14.0, 0.0},
            {"portail", 0.0, 6.0, 66.0, 0.0, 12.0, 35.0},
            {"place", 0.0, 26.0, 44.0, 0.0, 2.0, 0.0},
            {"rotonde", 16.0, 9.0, 18.0, 0.0, 8.0, 0.0},
            {"interieur", 5.0, 2.5, 5.0, 0.0, 13.0, 0.0},
            {"guerrier", -6.0, 5.0, -6.0, -22.0, 7.0, -22.0},
            {"mage", 6.0, 5.0, -6.0, 22.0, 7.0, -22.0},
            {"archer", -6.0, 5.0, 6.0, -22.0, 7.0, 22.0},
            {"tank", 6.0, 5.0, 6.0, 22.0, 7.0, 22.0},
            {"teleporteur", 0.0, 7.0, 12.0, 0.0, 3.0, 24.0},
            {"voie", 0.0, 4.0, -26.0, 0.0, 4.0, -52.0},
            {"arene", 0.0, 6.0, -44.0, 0.0, 6.0, -74.0},
            {"arene_haut", 0.0, 14.0, -56.0, 0.0, 4.0, -84.0},
            {"aire", 34.0, 7.0, 0.0, 58.0, 3.0, 0.0},
            {"promenade", -32.0, 7.0, 18.0, -8.0, 3.0, -8.0},
    };

    private static void hubDesignPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            cmd(mc, "time set 6000");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, String.format("tp %s %d %d %d", me, HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]));
        }
        if (tick == 30) cmd(mc, "mmorpg hub");
        if (tick == 60) cmd(mc, "hub");
        if (tick == 31) cmd(mc, "gamemode creative " + me);
        int first = 420;
        for (int i = 0; i < DESIGN_VIEWS.length; i++) {
            Object[] v = DESIGN_VIEWS[i];
            if (tick == first + i * 60) {
                cmd(mc, "gamemode spectator " + me);
                hubView(mc, me, (double) v[1], (double) v[2], (double) v[3], (double) v[4], (double) v[5], (double) v[6]);
            }
            if (tick == first + i * 60 + 40) {
                MMORPG.LOGGER.info("[MMORPG] PERF vue {} : {} fps, entites {}, particules [{}]", v[0], mc.getFps(), mc.level.getEntityCount(), mc.particleEngine.countParticles());
                java.util.Map<String, Integer> byType = new java.util.TreeMap<>();
                for (var en : mc.level.entitiesForRendering()) byType.merge(net.minecraft.world.entity.EntityType.getKey(en.getType()).toString(), 1, Integer::sum);
                StringBuilder sb = new StringBuilder();
                byType.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(6).forEach(en -> sb.append(en.getKey()).append('=').append(en.getValue()).append(' '));
                MMORPG.LOGGER.info("[MMORPG] PERF types : {}", sb);
            }
            if (tick == first + i * 60 + 45) shot(mc, String.format("15%02d_hub_%s", i, v[0]));
        }
        int night = first + DESIGN_VIEWS.length * 60 + 10;
        if (tick == night) {
            cmd(mc, "time set 18000");
            hubView(mc, me, 0.0, 26.0, 44.0, 0.0, 2.0, 0.0);
        }
        if (tick == night + 40) shot(mc, "1590_hub_nuit_place");
        if (tick == night + 45) hubView(mc, me, -6.0, 5.0, -6.0, -22.0, 7.0, -22.0);
        if (tick == night + 85) shot(mc, "1591_hub_nuit_guerrier");
        if (tick == night + 90) hubView(mc, me, 6.0, 5.0, -6.0, 22.0, 7.0, -22.0);
        if (tick == night + 130) shot(mc, "1592_hub_nuit_mage");
        if (tick == night + 140) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (hubdesign)");
            stop(mc);
        }
    }

    private static boolean cineSaw;
    private static int cineEnd = -1;
    private static int cineVerifTick = -1;

    /** Echantillonne le parcours : camera dans un bloc, ou ligne de vue vers la cible bouchee (intervalles de temps). */
    private static void verifyCinematicPath(Minecraft mc) {
        float total = com.mmorpg.client.cinematic.CinematicClient.debugTotal();
        StringBuilder inside = new StringBuilder();
        StringBuilder blocked = new StringBuilder();
        boolean in = false;
        boolean bl = false;
        float inStart = 0;
        float blStart = 0;
        String inWhat = "";
        String blWhat = "";
        for (float t = 0F; t <= total; t += 0.1F) {
            var v = com.mmorpg.client.cinematic.CinematicClient.debugSample(t);
            if (v == null) return;
            boolean isInside = !mc.level.getBlockState(net.minecraft.core.BlockPos.containing(v[0])).isAir();
            var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(v[0], v[1], net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
            double dist = v[0].distanceTo(v[1]);
            boolean isBlocked = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getLocation().distanceTo(v[0]) < dist * 0.55 && dist > 3;
            if (isInside && !in) {
                inStart = t;
                inWhat = mc.level.getBlockState(net.minecraft.core.BlockPos.containing(v[0])).getBlock().getDescriptionId() + String.format(java.util.Locale.ROOT, "@%.0f,%.0f,%.0f", v[0].x - HUB_ORIGIN[0], v[0].y - HUB_ORIGIN[1], v[0].z - HUB_ORIGIN[2]);
            }
            if (!isInside && in) inside.append(String.format(java.util.Locale.ROOT, " [%.1f-%.1f %s]", inStart, t, inWhat));
            if (isBlocked && !bl) {
                blStart = t;
                var hp = hit.getLocation();
                blWhat = mc.level.getBlockState(net.minecraft.core.BlockPos.containing(hp.add(v[1].subtract(v[0]).normalize().scale(0.1)))).getBlock().getDescriptionId()
                        + String.format(java.util.Locale.ROOT, "@%.0f,%.0f,%.0f cam@%.0f,%.0f,%.0f", hp.x - HUB_ORIGIN[0], hp.y - HUB_ORIGIN[1], hp.z - HUB_ORIGIN[2], v[0].x - HUB_ORIGIN[0], v[0].y - HUB_ORIGIN[1], v[0].z - HUB_ORIGIN[2]);
            }
            if (!isBlocked && bl) blocked.append(String.format(java.util.Locale.ROOT, " [%.1f-%.1f %s]", blStart, t, blWhat));
            in = isInside;
            bl = isBlocked;
        }
        MMORPG.LOGGER.info("[MMORPG] PARCOURS camera dans un bloc :{}", inside.length() == 0 ? " aucun" : inside);
        MMORPG.LOGGER.info("[MMORPG] PARCOURS ligne de vue bouchee :{}", blocked.length() == 0 ? " aucune" : blocked);
    }

    /** Cinematique d'arrivee : declenchee par l'arrivee au hub (/hub reset), captures reguliers, puis controle du regard vers le sol et le plafond. */
    private static void cinematicPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode survival " + me);
            cmd(mc, String.format("tp %s %d %d %d", me, HUB_ORIGIN[0], HUB_ORIGIN[1], HUB_ORIGIN[2]));
        }
        if (tick == 30) cmd(mc, "mmorpg hub");
        if (tick == 60) cmd(mc, "hub reset " + me);              // arrivee au hub : la cinematique demarre d'elle-meme
        boolean active = com.mmorpg.client.cinematic.CinematicClient.active();
        if (active) cineSaw = true;
        if (MODE.equals("cineverif") && active && cineVerifTick < 0) cineVerifTick = tick;
        if (MODE.equals("cineverif") && cineVerifTick > 0 && tick == cineVerifTick + 10) {
            verifyCinematicPath(mc);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (cineverif)");
            stop(mc);
        }
        if (cineSaw && !active && cineEnd < 0) {
            cineEnd = tick;
            MMORPG.LOGGER.info("[MMORPG] CINEMATIQUE terminee au tick {} : camera={} verrou serveur={}", tick,
                    mc.getCameraEntity() == mc.player ? "joueur" : "libre", com.mmorpg.hub.CinematicManager.isLocked(sp(mc, me)));
        }
        if (cineSaw && cineEnd < 0 && tick % 120 == 0) {
            MMORPG.LOGGER.info("[MMORPG] CINEMATIQUE tick {} : camera = {}", tick, mc.getCameraEntity() == mc.player ? "joueur" : "libre");
            shot(mc, String.format("14%02d_cine_%d", tick / 120, tick));
        }
        if (cineEnd > 0) {
            int d = tick - cineEnd;
            if (d == 5) verifyCinematicPath(mc);
            if (d == 30) cmd(mc, "tp " + me + " ~ ~ ~ 0 90");
            if (d == 50) {
                MMORPG.LOGGER.info("[MMORPG] REGARD BAS : joueur xRot={} cameraEntite={} fov={}", mc.player.getXRot(), mc.getCameraEntity() == mc.player, mc.options.fov().get());
                shot(mc, "1490_post_bas");
            }
            if (d == 60) cmd(mc, "tp " + me + " ~ ~ ~ 0 -90");
            if (d == 80) {
                MMORPG.LOGGER.info("[MMORPG] REGARD HAUT : joueur xRot={} cameraEntite={}", mc.player.getXRot(), mc.getCameraEntity() == mc.player);
                shot(mc, "1491_post_haut");
            }
            if (d == 90) cmd(mc, "tp " + me + " ~ ~ ~ 90 0");
            if (d == 110) shot(mc, "1492_post_horizon");
            if (d == 120) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (cinematique)");
            }
            if (d == 130) stop(mc);
        }
        if (tick > 4000 && cineEnd < 0) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (cinematique) : la cinematique ne s'est pas terminee");
            stop(mc);
        }
    }

    private static void petsPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            petOrigin = new net.minecraft.core.BlockPos(107, 181, -689);   // plateforme de la vitrine des structures
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "mmorpg familier " + me + " tous");
            ClientNet.send(new Payloads.PetAction(""));
            camera(mc, me, 0, 0, 1.5, 180, 12);
        }
        if (tick == 30) {                                       // vitrine : les 8 familiers alignes face a la camera
            com.mmorpg.pet.PetType[] all = com.mmorpg.pet.PetType.values();
            for (int i = 0; i < all.length; i++) {
                var e = new com.mmorpg.entity.PetEntity(com.mmorpg.registry.ModEntities.PET.get(), mc.level);
                e.displayOnly(all[i], 0.0F);
                e.setId(-5000 - i);                             // entite purement cliente : identifiant negatif reserve
                boolean hover = all[i].move() == com.mmorpg.pet.PetType.Move.HOVER;
                e.setPos(petOrigin.getX() + 0.5 + (i - 3.5) * 1.5, petOrigin.getY() + (hover ? 0.7 : 0.0), petOrigin.getZ() + 0.5 - 5.0);
                mc.level.addEntity(e);
                GALLERY.add(e);
            }
        }
        if (tick == 70) shot(mc, "90_familiers_galerie");
        if (tick == 72) camera(mc, me, -3.0, 0, -1.6, 180, 18);
        if (tick == 90) shot(mc, "91_familiers_gauche");
        if (tick == 92) camera(mc, me, 3.0, 0, -1.6, 180, 18);
        if (tick == 110) shot(mc, "92_familiers_droite");
        if (tick == 112) camera(mc, me, 0, 0, 1.5, 180, 12);
        if (tick == 355) mc.gui.setScreen(null);
        if (tick == 360) {
            shot(mc, "93_familiers_repos_long");                // apres 15 s immobiles : loup endormi, golem et chouette endormis...
            for (var e : GALLERY) e.discard();
            GALLERY.clear();
        }
        // familiers reels qui suivent le joueur (vue a la 3e personne)
        String[] tests = {"golem", "phenix", "fee", "dragonnet", "loup_spectral"};
        int base = 370;
        for (int k = 0; k < tests.length; k++) {
            int t0 = base + k * 150;
            if (tick == t0) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                camera(mc, me, 0, 0, 0, k % 2 == 0 ? -90 : 90, 20);   // regarde dans le sens de la marche (aller-retour)
                ClientNet.send(new Payloads.PetAction(tests[k]));
            }
            if (tick > t0 + 40 && tick < t0 + 90) walk(mc, me, k % 2 == 0 ? 0.1 : -0.1);
            if (tick > t0 && tick <= t0 + 90 && (tick - t0) % 10 == 0) {
                MMORPG.LOGGER.info("[MMORPG] SUIVI t+{} : {} | serveur : {}", tick - t0, petInfo(mc), serverPetInfo(mc));
            }
            if (tick == t0 + 78 || tick == t0 + 138) mc.gui.setScreen(null);
            if (tick == t0 + 80) {
                MMORPG.LOGGER.info("[MMORPG] FAMILIER EN MARCHE : {}", petInfo(mc));
                shot(mc, "9" + (4 + k) + "a_" + tests[k] + "_deplacement");
            }
            if (tick == t0 + 140) {
                MMORPG.LOGGER.info("[MMORPG] FAMILIER A L'ARRET : {}", petInfo(mc));
                shot(mc, "9" + (4 + k) + "b_" + tests[k] + "_arret");
            }
        }
        int end = base + tests.length * 150;
        if (tick == end) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            ClientNet.send(new Payloads.PetAction(""));
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (familiers)");
        }
        if (tick == end + 10) stop(mc);
    }

    // ------------------------------------------------------------------ editeur d'interface (-Pshowcase=hud)

    private static void mouse(net.minecraft.client.gui.screens.Screen s, int button, double x0, double y0, double x1, double y1) {
        var info = new net.minecraft.client.input.MouseButtonInfo(button, 0);
        s.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x0, y0, info), false);
        if (x1 != x0 || y1 != y0) {
            for (int k = 1; k <= 8; k++) {                      // glissement en plusieurs pas, comme une vraie souris
                double x = x0 + (x1 - x0) * k / 8.0, y = y0 + (y1 - y0) * k / 8.0;
                s.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(x, y, info), (x1 - x0) / 8.0, (y1 - y0) / 8.0);
            }
        }
        s.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x1, y1, info));
    }

    /** Glisse un element pour placer son coin haut-gauche en (tx, ty). */
    private static void dragTo(Minecraft mc, com.mmorpg.client.hud.HudLayout.Element e, int tx, int ty) {
        var s = mc.gui.screen();
        int[] r = e.rect(s.width, s.height);
        double gx = r[0] + Math.min(10, r[2] / 2.0), gy = r[1] + Math.min(6, r[3] / 2.0);
        mouse(s, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, gx, gy, tx + (gx - r[0]), ty + (gy - r[1]));
    }

    private static String layoutDump(Minecraft mc) {
        StringBuilder b = new StringBuilder();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        for (var e : com.mmorpg.client.hud.HudLayout.elements()) {
            int[] r = e.rect(w, h);
            b.append(String.format(java.util.Locale.ROOT, "%n   %-11s x=%4d y=%4d %3dx%-3d x%.2f%s", e.id, r[0], r[1], r[2], r[3], e.scale(), e.visible() ? "" : " MASQUE"));
        }
        return b.toString();
    }

    private static void hudPhase(Minecraft mc, String me) {
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 15) {
            com.mmorpg.client.hud.HudLayout.resetAll();
            com.mmorpg.client.hud.HudLayout.save();
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "time set day");
            cmd(mc, "effect give " + me + " minecraft:speed 600 1");
            cmd(mc, "effect give " + me + " minecraft:night_vision 600");
            cmd(mc, "effect give " + me + " minecraft:weakness 600");
            cmd(mc, "tp " + me + " ~ ~ ~ 0 10");
        }
        if (tick == 45) shot(mc, "80_hud_defaut");
        if (tick == 50) mc.gui.setScreen(new com.mmorpg.client.screen.HudEditorScreen(null));
        if (tick == 70) shot(mc, "81_editeur");
        if (tick == 75) {
            var s = mc.gui.screen();
            int w = s.width, h = s.height;
            MMORPG.LOGGER.info("[MMORPG] HUD : ecran {}x{}, disposition par defaut :{}", w, h, layoutDump(mc));
            dragTo(mc, com.mmorpg.client.hud.HudLayout.JOUEUR, 4, h - 62);            // cadre du joueur en bas a gauche
            dragTo(mc, com.mmorpg.client.hud.HudLayout.EFFETS, 6, 4);                 // effets vanilla en haut a gauche
            dragTo(mc, com.mmorpg.client.hud.HudLayout.SORTS, w / 2 - 68, h - 58);    // sorts au-dessus de la barre d'objets
            int[] r = com.mmorpg.client.hud.HudLayout.SORTS.rect(w, h);
            for (int i = 0; i < 5; i++) s.mouseScrolled(r[0] + 20, r[1] + 10, 0, 1);  // +25 %
            int[] q = com.mmorpg.client.hud.HudLayout.QUETES.rect(w, h);
            mouse(s, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, q[0] + 5, q[1] + 5, q[0] + 5, q[1] + 5);                      // clic droit : masquer le suivi des quetes
            dragTo(mc, com.mmorpg.client.hud.HudLayout.FAIM, w / 2 - 91 - 90, h - 20);  // faim a gauche de la barre d'objets
            dragTo(mc, com.mmorpg.client.hud.HudLayout.CHAT, 4, h / 2 - 20);         // chat remonte au-dessus du cadre du joueur
            MMORPG.LOGGER.info("[MMORPG] HUD : apres edition :{}", layoutDump(mc));
        }
        if (tick == 95) shot(mc, "82_editeur_modifie");
        if (tick == 100) mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0));
        if (tick == 125) {
            MMORPG.LOGGER.info("[MMORPG] HUD : ecran apres Echap = {} (attendu aucun)", screenName(mc));
            shot(mc, "83_hud_personnalise");
        }
        if (tick == 130) {
            String before = layoutDump(mc);
            com.mmorpg.client.hud.HudLayout.load();
            String after = layoutDump(mc);
            MMORPG.LOGGER.info("[MMORPG] HUD : rechargement depuis config/mmorpg-hud.json : {}", before.equals(after) ? "IDENTIQUE" : "DIFFERENT" + after);
            mc.getWindow().setWindowed(1600, 900);
        }
        if (tick == 160) {
            MMORPG.LOGGER.info("[MMORPG] HUD : apres redimensionnement 1600x900 :{}", layoutDump(mc));
            shot(mc, "84_hud_redimensionne");
        }
        if (tick == 165) MenuScreen.open(MenuScreen.Tab.QUETES);
        if (tick == 185) {
            var s = mc.gui.screen();
            int pw = Math.min(s.width - 12, 440), ph = Math.min(s.height - 12, 272);
            int left = (s.width - pw) / 2, top = (s.height - ph) / 2;
            mouse(s, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, left + pw - 8 - 9, top + 15, left + pw - 8 - 9, top + 15);   // bouton « Modifier l'interface » du menu
            MMORPG.LOGGER.info("[MMORPG] HUD : bouton du menu -> ecran = {} (attendu HudEditorScreen)", screenName(mc));
        }
        if (tick == 205) shot(mc, "85_editeur_depuis_menu");
        if (tick == 210) {
            com.mmorpg.client.hud.HudLayout.resetAll();                               // laisse la config du client propre
            mc.gui.setScreen(null);
            try {
                java.nio.file.Files.deleteIfExists(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("mmorpg-hud.json"));
            } catch (java.io.IOException ignored) {
            }
            cmd(mc, "effect clear " + me);
            cmd(mc, "gamemode creative " + me);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (hud)");
        }
        if (tick == 220) stop(mc);
    }

    private static void look(Minecraft mc, String me, double dx, double dy, double dz, double tx, double tz, double pitch) {
        double yaw = Math.toDegrees(Math.atan2(-(tx - dx), tz - dz));
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f %.1f", me, structOrigin.getX() + dx + (dx == Math.floor(dx) ? 0.5 : 0),
                structOrigin.getY() + dy, structOrigin.getZ() + dz + (dz == Math.floor(dz) ? 0.5 : 0), yaw, pitch));
    }

    /** Lache le joueur 3 blocs au-dessus du centre du bloc (dx, dz) relatif a l'origine. */
    private static void drop(Minecraft mc, String me, int dx, int dz) {
        cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %.1f %d %.1f 180 0", me, structOrigin.getX() + dx + 0.5, structOrigin.getY() + 3,
                structOrigin.getZ() + dz + 0.5));
    }

    /** Controle des modeles 3D de batons (pack de ressources de test) : 3e personne, 1re personne et icones. */
    private static void staffPhase(Minecraft mc, String me) {
        String[] ids = {"baton_apprenti", "sceptre_givre", "baton_flammes", "baton_arcanique", "sceptre_neant", "baton_archimage"};
        if (tick == 20) {
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "clear " + me);
            for (int i = 0; i < ids.length; i++) cmd(mc, "item replace entity " + me + " hotbar." + i + " with mmorpg:" + ids[i]);
            cmd(mc, "tp " + me + " ~ ~ ~ 0 0");
        }
        int t = tick - 50;
        if (t >= 0 && t < 12 * 25) {
            int i = t / 25, k = t % 25;
            int slot = i % 6;
            boolean third = i < 6;
            if (k == 0) {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.options.setCameraType(third ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
                if (mc.gui.hud.isHidden() != third) mc.gui.hud.toggle();
                mc.gui.hud.getChat().clearMessages(false);
            }
            if (k == 20) shot(mc, (third ? "40_" : "41_") + slot + "_" + ids[slot] + (third ? "_3e" : "_1re"));
        }
        if (t == 12 * 25 + 5) {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (batons)");
        }
        if (t == 12 * 25 + 15) stop(mc);
    }

    private static net.minecraft.core.BlockPos forgePos;
    private static net.minecraft.core.BlockPos tpPos;

    private static void logScreen(Minecraft mc, String what) {
        var sc = mc.gui.screen();
        MMORPG.LOGGER.info("[MMORPG] CLIC {} -> ecran={}", what, sc == null ? "aucun" : sc.getClass().getSimpleName());
    }

    private static void leftClickBlock(Minecraft mc, net.minecraft.core.BlockPos pos) {
        mc.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.UP);
        mc.gameMode.stopDestroyBlock();
    }

    /** Verifie que le clic gauche ouvre les menus (PNJ, Forge, Teleporteur) et que Maj + clic gauche casse le bloc. */
    private static void clickPhase(Minecraft mc, String me) {
        switch (tick) {
            case 20 -> {
                cmd(mc, "time set day");
                cmd(mc, "weather clear");
                cmd(mc, "kill @e[type=mmorpg:pnj]");
                cmd(mc, "mmorpg ville Clicville");
                cmd(mc, "gamemode survival " + me);
            }
            case 40 -> cmd(mc, "tp " + me + " ~-2.5 ~ ~ 90 15");
            case 60 -> {
                var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.QUETES);
                MMORPG.LOGGER.info("[MMORPG] CLIC pnj trouve={}", npc != null);
                if (npc != null) mc.gameMode.attack(mc.player, npc);
            }
            case 85 -> {
                logScreen(mc, "pnj");
                shot(mc, "29_clic_gauche_pnj");
            }
            case 90 -> {
                mc.gui.setScreen(null);
                forgePos = mc.player.blockPosition().offset(0, 0, 2);
                tpPos = net.minecraft.core.BlockPos.containing(mc.player.getX() + 2.5, mc.player.getY(), mc.player.getZ());
                cmd(mc, "setblock " + forgePos.getX() + " " + forgePos.getY() + " " + forgePos.getZ() + " mmorpg:forge_arcanique");
            }
            case 105 -> leftClickBlock(mc, forgePos);
            case 125 -> {
                logScreen(mc, "forge (survie)");
                shot(mc, "30_clic_gauche_forge");
            }
            case 130 -> {
                mc.gui.setScreen(null);
                MMORPG.LOGGER.info("[MMORPG] CLIC teleporteur bloc={}", mc.level.getBlockState(tpPos).getBlock());
                leftClickBlock(mc, tpPos);
            }
            case 155 -> {
                logScreen(mc, "teleporteur");
                shot(mc, "31_clic_gauche_teleporteur");
            }
            case 160 -> {
                mc.gui.setScreen(null);
                cmd(mc, "gamemode creative " + me);
            }
            case 175 -> leftClickBlock(mc, forgePos);
            case 195 -> {
                logScreen(mc, "forge (creatif)");
                MMORPG.LOGGER.info("[MMORPG] CLIC forge intacte apres clic gauche creatif={}", mc.level.getBlockState(forgePos).getBlock());
                mc.gui.setScreen(null);
                mc.options.keyShift.setDown(true);
            }
            case 210 -> leftClickBlock(mc, forgePos);
            case 230 -> {
                logScreen(mc, "forge (maj+clic)");
                MMORPG.LOGGER.info("[MMORPG] CLIC bloc apres maj+clic gauche={}", mc.level.getBlockState(forgePos).getBlock());
                mc.options.keyShift.setDown(false);
                mc.gui.setScreen(null);
                cmd(mc, "gamemode survival " + me);
            }
            case 240 -> MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (clic)");
            case 260 -> stop(mc);
            default -> {
            }
        }
    }

    private static int slotOf(Minecraft mc, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private static int countOf(Minecraft mc, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            var st = mc.player.getInventory().getItem(i);
            if (st.is(item)) n += st.getCount();
        }
        return n;
    }

    private static net.minecraft.core.BlockPos nearestTeleporter(Minecraft mc) {
        net.minecraft.core.BlockPos best = null;
        net.minecraft.core.BlockPos c = mc.player.blockPosition();
        for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(c.offset(-6, -2, -6), c.offset(6, 2, 6))) {
            if (mc.level.getBlockState(q).getBlock() instanceof com.mmorpg.block.TeleporterBlock
                    && (best == null || q.distSqr(c) < best.distSqr(c))) best = q.immutable();
        }
        return best;
    }

    private static net.minecraft.world.InteractionResult useOn(Minecraft mc, net.minecraft.core.BlockPos pos) {
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0),
                net.minecraft.core.Direction.UP, pos, false);
        return mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }

    private static net.minecraft.core.BlockPos placedPos;

    /** Alice (operatrice) : prepare le test, pose un teleporteur (configuration) puis le retire. */
    private static void teleporterAdminPhase(Minecraft mc) {
        int t = tick - stageStart;
        var tpItem = com.mmorpg.registry.ModItems.TELEPORTEUR.get();
        switch (stage) {
            case 0 -> {
                boolean bobOnline = mc.getConnection() != null && mc.getConnection().getOnlinePlayers().stream()
                        .anyMatch(i -> i.getProfile().name().equals("Bob"));
                if (bobOnline && t > 60) {
                    cmd(mc, "time set day");
                    cmd(mc, "kill @e[type=mmorpg:gobelin]");
                    cmd(mc, "gamemode survival Bob");
                    cmd(mc, "clear Bob mmorpg:teleporteur");
                    cmd(mc, "give Bob mmorpg:teleporteur 2");
                    cmd(mc, "tp Bob 29.5 64 -713.5 0 30");
                    cmd(mc, "gamemode creative Alice");
                    cmd(mc, "clear Alice mmorpg:teleporteur");
                    cmd(mc, "give Alice mmorpg:teleporteur 1");
                    cmd(mc, "tp Alice 25.5 64 -713.5 180 30");
                    next();
                }
            }
            case 1 -> {
                if (t == 30) mc.player.getInventory().setSelectedSlot(Math.max(0, slotOf(mc, tpItem)));
                if (t == 40) {
                    placedPos = mc.player.blockPosition().offset(0, 0, -2);
                    MMORPG.LOGGER.info("[MMORPG] TP Alice admin(client)={} main={} cible={} sol={}", com.mmorpg.block.TeleporterBlock.isAdmin(mc.player),
                            mc.player.getMainHandItem(), placedPos, mc.level.getBlockState(placedPos.below()).getBlock());
                    MMORPG.LOGGER.info("[MMORPG] TP Alice resultat pose={}", useOn(mc, placedPos.below()));
                }
                if (t == 60) {
                    logScreen(mc, "Alice pose un teleporteur");
                    MMORPG.LOGGER.info("[MMORPG] TP Alice bloc pose={}", mc.level.getBlockState(placedPos).getBlock());
                    shot(mc, "33_alice_pose_teleporteur");
                }
                if (t == 65) {
                    mc.gui.setScreen(null);
                    next();
                }
            }
            case 2 -> {
                if (t == 5) mc.options.keyShift.setDown(true);
                if (t == 15) leftClickBlock(mc, placedPos);
                if (t == 35) {
                    MMORPG.LOGGER.info("[MMORPG] TP Alice apres Maj+clic gauche={}", mc.level.getBlockState(placedPos).getBlock());
                    mc.options.keyShift.setDown(false);
                    next();
                }
            }
            case 3 -> {
                if (t == 300) {
                    cmd(mc, "gamemode survival Alice");
                    cmd(mc, "gamemode survival Bob");
                    cmd(mc, "execute at Bob run tp Alice ~ ~ ~2 180 10");
                }
                if (t == 330 || t == 350) ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, 0, ""));
                if (t == 340) {
                    for (var e : mc.level.players()) {
                        if (e != mc.player && e.getName().getString().equals("Bob")) {
                            MMORPG.LOGGER.info("[MMORPG] PVP Alice frappe Bob (distance {})", String.format("%.1f", e.distanceTo(mc.player)));
                            mc.gameMode.attack(mc.player, e);
                        }
                    }
                }
                if (t == 380) {
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (tpA)");
                    stop(mc);
                }
            }
            default -> {
            }
        }
    }

    /** Bob (joueur normal) : ne peut ni poser, ni casser, ni configurer un teleporteur, mais peut l'utiliser. */
    private static void teleporterPlayerPhase(Minecraft mc) {
        int t = tick - stageStart;
        var tpItem = com.mmorpg.registry.ModItems.TELEPORTEUR.get();
        switch (stage) {
            case 0 -> {
                if (countOf(mc, tpItem) > 0 && t > 160) {
                    mc.player.getInventory().setSelectedSlot(Math.max(0, slotOf(mc, tpItem)));
                    next();
                }
            }
            case 1 -> {
                if (t == 20) {
                    placedPos = mc.player.blockPosition().offset(2, 0, 0);
                    useOn(mc, placedPos.below());
                }
                if (t == 40) {
                    MMORPG.LOGGER.info("[MMORPG] TP Bob pose -> bloc={} objets={}", mc.level.getBlockState(placedPos).getBlock(), countOf(mc, tpItem));
                    next();
                }
            }
            case 2 -> {
                var tp = nearestTeleporter(mc);
                if (t == 1) {
                    MMORPG.LOGGER.info("[MMORPG] TP Bob teleporteur proche={}", tp);
                    mc.player.getInventory().setSelectedSlot(8);
                    mc.options.keyShift.setDown(true);
                }
                if (tp != null && t == 10) mc.gameMode.startDestroyBlock(tp, net.minecraft.core.Direction.UP);
                if (tp != null && t > 10 && t < 90) mc.gameMode.continueDestroyBlock(tp, net.minecraft.core.Direction.UP);
                if (t == 95) {
                    mc.gameMode.stopDestroyBlock();
                    MMORPG.LOGGER.info("[MMORPG] TP Bob apres 4 s de Maj+clic gauche={}", tp == null ? "?" : mc.level.getBlockState(tp).getBlock());
                    mc.options.keyShift.setDown(false);
                    next();
                }
            }
            case 3 -> {
                var tp = nearestTeleporter(mc);
                if (t == 10 && tp != null) leftClickBlock(mc, tp);
                if (t == 30) {
                    logScreen(mc, "Bob clic gauche teleporteur");
                    shot(mc, "32_bob_teleporteur");
                }
                if (t == 35) {
                    mc.gui.setScreen(null);
                    next();
                }
            }
            case 4 -> {
                var tp = nearestTeleporter(mc);
                if (t == 5) mc.options.keyShift.setDown(true);
                if (t == 25 && tp != null) useOn(mc, tp);
                if (t == 45) {
                    logScreen(mc, "Bob accroupi + clic droit");
                    mc.gui.setScreen(null);
                    mc.options.keyShift.setDown(false);
                    next();
                }
            }
            case 5 -> {
                if (t % 20 == 0) MMORPG.LOGGER.info("[MMORPG] PVP Bob PV {}/{}", (int) ClientData.hp, (int) ClientData.maxHp);
                if (t == 330) MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE (tpB)");
                if (t == 340) stop(mc);
            }
            default -> {
            }
        }
    }

    private static int stage = 0;
    private static int stageStart = 0;

    private static void next() {
        stage++;
        stageStart = tick;
    }

    private static int partySize() {
        return ClientData.party.getListOrEmpty("members").size();
    }

    /** Test a deux clients (Alice = mpA invite et combat, Bob = mpB accepte et observe). */
    private static void multiplayerPhase(Minecraft mc, String me, boolean alice) {
        int t = tick - stageStart;
        if (tick % 40 == 0) MMORPG.LOGGER.info("[MMORPG] MP {} etape {} groupe={} xp={} niv={}", me, stage, partySize(), ClientData.DATA.xp, ClientData.DATA.level);
        switch (stage) {
            case 0 -> {
                if (t > 40) {
                    if (!ClientData.DATA.playerClass.isPlayable()) {
                        ClientNet.send(new Payloads.SelectClass((alice ? PlayerClass.GUERRIER : PlayerClass.MAGE).ordinal()));
                    }
                    mc.gui.setScreen(null);
                    next();
                }
            }
            case 1 -> {
                if (alice) {
                    boolean bobOnline = mc.getConnection() != null && mc.getConnection().getOnlinePlayers().stream()
                            .anyMatch(i -> i.getProfile().name().equals("Bob"));
                    if (bobOnline && t > 40) {
                        ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.INVITE, "Bob"));
                        next();
                    }
                } else if (!ClientData.party.getStringOr("invite", "").isEmpty() && t > 20) {
                    MMORPG.LOGGER.info("[MMORPG] Bob a recu une invitation de {}", ClientData.party.getStringOr("invite", ""));
                    shot(mc, "25_bob_invitation");
                    ClientNet.send(new Payloads.PartyAction(Payloads.PartyAction.ACCEPT, ""));
                    next();
                }
            }
            case 2 -> {
                if (partySize() == 2 && t > 20) {
                    if (alice) {
                        cmd(mc, "mmorpg niveau Alice 30");
                        cmd(mc, "mmorpg niveau Bob 30");
                        cmd(mc, "time set day");
                        cmd(mc, "kill @e[type=mmorpg:pnj]");
                        cmd(mc, "mmorpg ville Arene");
                        cmd(mc, "tp Alice ~ ~ ~-3 0 10");
                        cmd(mc, "tp Bob ~1.5 ~ ~-2 0 10");
                    }
                    next();
                }
            }
            case 3 -> {
                if (alice) {
                    if (t == 40) MenuScreen.open(MenuScreen.Tab.GROUPE);
                    if (t == 70) shot(mc, "26_alice_groupe");
                    if (t == 75) {
                        mc.gui.setScreen(null);
                        for (int i = 0; i < 3; i++) {
                            cmd(mc, "execute at @s positioned ~" + (i - 1) + " ~ ~2 run mmorpg mob gobelin 28");
                        }
                    }
                    // Alice frappe le gobelin le plus proche (le groupe doit partager l'XP)
                    if (t > 90 && t < 330 && t % 12 == 0) {
                        com.mmorpg.entity.mob.GoblinEntity target = null;
                        for (var e : mc.level.entitiesForRendering()) {
                            if (e instanceof com.mmorpg.entity.mob.GoblinEntity g && g.isAlive() && g.distanceToSqr(mc.player) < 16
                                    && (target == null || g.distanceToSqr(mc.player) < target.distanceToSqr(mc.player))) target = g;
                        }
                        if (target != null) {
                            mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                            mc.gameMode.attack(mc.player, target);
                            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                        }
                    }
                    if (t == 200) mc.player.connection.sendCommand("g Bien joué l'équipe !");
                    if (t == 215) shot(mc, "27_alice_hud_groupe");
                    if (t == 340) next();
                } else {
                    if (t == 230) shot(mc, "28_bob_hud_groupe");
                    if (t == 340) next();
                }
            }
            case 4 -> {
                if (t == 1) MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE ({}) xp={} niveau={}", me, ClientData.DATA.xp, ClientData.DATA.level);
                if (t == 20) stop(mc);
            }
            default -> {
            }
        }
    }

    private static int firstSlotWithSword(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() instanceof com.mmorpg.item.RpgWeaponItem) return i;
        }
        return 0;
    }

    /** Fin de vitrine : remet l'option de pause modifiee pour les tests, puis ferme le jeu. */
    private static final java.util.List<Object[]> TRIAL_QUEUE = new java.util.ArrayList<>();
    private static int trialCursor;

    private static void trialAt(int delay, Runnable r) {
        trialCursor += delay;
        TRIAL_QUEUE.add(new Object[]{trialCursor, r});
    }

    private static com.mmorpg.dungeon.TrialData.Trial trialOf(net.minecraft.server.MinecraftServer srv, String id) {
        return com.mmorpg.dungeon.TrialData.get(srv).byId(id);
    }

    private static int gateBlocksLeft(net.minecraft.server.level.ServerLevel lv, com.mmorpg.dungeon.TrialData.Trial t) {
        if (t.gate().size() != 6 || t.gateBlock().isEmpty()) return -1;
        var block = com.mmorpg.dungeon.TrialManager.state(t.gateBlock()).getBlock();
        int n = 0;
        for (int x = t.gate().get(0); x <= t.gate().get(3); x++)
            for (int y = t.gate().get(1); y <= t.gate().get(4); y++)
                for (int z = t.gate().get(2); z <= t.gate().get(5); z++)
                    if (lv.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).is(block)) n++;
        return n;
    }

    /** Verification automatique des epreuves d'un donjon : chute, vagues, enigme, puis entree dans l'arene du boss. */
    private static void trialsPhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 3000 150 3000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire " + id);
        if (tick == 330) {
            var all = new java.util.ArrayList<>(com.mmorpg.dungeon.TrialData.get(srv).all());
            MMORPG.LOGGER.info("[MMORPG] EPREUVES {} : {}", id, all.stream().map(t -> t.id().substring(t.id().lastIndexOf('_') + 1) + "=" + t.kind()).toList());
            for (var tg : all) MMORPG.LOGGER.info("[MMORPG] EPREUVE geometrie {} boite {} herse {} bloc {}", tg.id().substring(tg.id().lastIndexOf('_') + 1), tg.box(), tg.gate(), tg.gateBlock());
            trialCursor = 330;
            trialAt(5, () -> {
                cmd(mc, "gamemode survival");
                srv.execute(() -> {
                    var pl = srv.getPlayerList().getPlayerByName(me);
                    pl.getAbilities().invulnerable = true;
                    pl.onUpdateAbilities();
                });
            });
            for (var t0 : all) {
                String tid = t0.id();
                trialAt(15, () -> MMORPG.LOGGER.info("[MMORPG] EPREUVE debut {} ({})", tid, t0.kind()));
                switch (t0.kind()) {
                    case "jump" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.box().get(1) - 5, t0.pos().get(2) + 6)));
                        trialAt(25, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE chute : position apres chute ({}, {}, {}) ; depart attendu ({}, {}, {})",
                                    (int) p.getX(), (int) p.getY(), (int) p.getZ(), t0.pos().get(0), t0.pos().get(1), t0.pos().get(2));
                        }));
                        trialAt(10, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) + 1)));
                        trialAt(8, () -> {
                            cmd(mc, "tp " + me + " ~ ~ ~ 0 18");
                        });
                        trialAt(24, () -> {
                            com.mmorpg.client.AreaEntryClient.cancel();
                            mc.gui.hud.getChat().clearMessages(true);
                            shot(mc, id + "_epreuve_saut");
                        });
                        trialAt(10, () -> srv.execute(() -> hubTeleport(srv, me, t0.gate().get(0) + 4, t0.gate().get(1), t0.gate().get(2) + 1)));
                        trialAt(25, () -> srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] EPREUVE saut reussi = {}", trialOf(srv, tid).done())));
                    }
                    case "waves" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) - 2)));
                        trialAt(60, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            int n = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(new net.minecraft.core.BlockPos(t0.pos().get(0), t0.pos().get(1), t0.pos().get(2))).inflate(20),
                                    m -> m instanceof com.mmorpg.entity.RpgMob).size();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE vagues : {} monstres apres le declenchement ; herse = {} blocs", n, gateBlocksLeft(lv, t0));
                        }));
                        trialAt(2, () -> {
                            com.mmorpg.client.AreaEntryClient.cancel();
                            shot(mc, id + "_epreuve_vagues");
                        });
                        for (int k = 0; k < 10; k++) {
                            trialAt(40, () -> srv.execute(() -> {
                                var lv = srv.getPlayerList().getPlayerByName(me).level();
                                for (var m : lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(new net.minecraft.core.BlockPos(t0.pos().get(0), t0.pos().get(1), t0.pos().get(2))).inflate(24),
                                        e -> e instanceof com.mmorpg.entity.RpgMob)) m.discard();
                            }));
                        }
                        trialAt(80, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE vagues reussies = {} ; herse = {} blocs restants", trialOf(srv, tid).done(), gateBlocksLeft(lv, trialOf(srv, tid)));
                        }));
                    }
                    case "simon" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) + 3)));
                        trialAt(10, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 10"));
                        trialAt(5, () -> srv.execute(() -> com.mmorpg.dungeon.TrialManager.debugStartSimon(srv.getPlayerList().getPlayerByName(me).level(), tid, srv.getPlayerList().getPlayerByName(me))));
                        trialAt(60, () -> {
                            com.mmorpg.client.AreaEntryClient.cancel();
                            shot(mc, id + "_epreuve_enigme");
                        });
                        trialAt(60 + 16 * (4 + t0.diff()) + 10, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE enigme : sequence {} ; attente de saisie = {}", com.mmorpg.dungeon.TrialManager.debugSequence(tid), com.mmorpg.dungeon.TrialManager.debugInputReady(tid));
                            var seq = com.mmorpg.dungeon.TrialManager.debugSequence(tid);
                            int wrong = (seq.get(0) + 1) % 4;
                            com.mmorpg.dungeon.TrialManager.debugPress(p.level(), tid, p, wrong);
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE enigme : mauvaise dalle -> saisie encore ouverte = {} (attendu false)", com.mmorpg.dungeon.TrialManager.debugInputReady(tid));
                            com.mmorpg.dungeon.TrialManager.debugStartSimon(p.level(), tid, p);
                        }));
                        trialAt(60 + 16 * (4 + t0.diff()) + 10, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            for (int pad : com.mmorpg.dungeon.TrialManager.debugSequence(tid)) com.mmorpg.dungeon.TrialManager.debugPress(p.level(), tid, p, pad);
                        }));
                        trialAt(20, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE enigme resolue = {} ; herse = {} blocs restants", trialOf(srv, tid).done(), gateBlocksLeft(lv, trialOf(srv, tid)));
                        }));
                    }
                    case "order" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) + 3)));
                        trialAt(10, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 10"));
                        trialAt(20, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            var seq = t0.mobs().stream().map(Integer::parseInt).toList();
                            int wrong = (seq.get(0) + 1) % 4;
                            com.mmorpg.dungeon.TrialManager.debugPress(p.level(), tid, p, wrong);
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE ordre : mauvaise dalle -> termine = {} (attendu false) ; sequence gravee {}", trialOf(srv, tid).done(), seq);
                        }));
                        trialAt(5, () -> {
                            com.mmorpg.client.AreaEntryClient.cancel();
                            mc.gui.hud.getChat().clearMessages(true);
                            shot(mc, id + "_epreuve_ordre");
                        });
                        trialAt(10, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            for (String k : t0.mobs()) com.mmorpg.dungeon.TrialManager.debugPress(p.level(), tid, p, Integer.parseInt(k));
                        }));
                        trialAt(15, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE ordre resolue = {} ; herse = {} blocs restants", trialOf(srv, tid).done(), gateBlocksLeft(lv, trialOf(srv, tid)));
                        }));
                    }
                    case "targets" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) - 4)));
                        trialAt(10, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 -5"));
                        trialAt(25, () -> {
                            com.mmorpg.client.AreaEntryClient.cancel();
                            mc.gui.hud.getChat().clearMessages(true);
                            shot(mc, id + "_epreuve_cibles");
                        });
                        trialAt(5, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            for (int k = 0; k < 4; k++) com.mmorpg.dungeon.TrialManager.debugPress(p.level(), tid, p, k);
                        }));
                        trialAt(15, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE cibles reussies = {} ; herse = {} blocs restants", trialOf(srv, tid).done(), gateBlocksLeft(lv, trialOf(srv, tid)));
                        }));
                    }
                    default -> {
                    }
                }
            }
            // remise a zero du donjon puis phases du boss
            trialAt(20, () -> srv.execute(() -> {
                String group = com.mmorpg.dungeon.TrialData.group(all.get(0).id());
                int n = com.mmorpg.dungeon.DungeonManager.reset(srv, group);
                var lv = srv.getPlayerList().getPlayerByName(me).level();
                StringBuilder sb = new StringBuilder();
                for (var t1 : com.mmorpg.dungeon.TrialData.get(srv).inGroup(group)) sb.append(t1.kind()).append(" fait=").append(t1.done()).append(" herse=").append(gateBlocksLeft(lv, t1)).append("; ");
                MMORPG.LOGGER.info("[MMORPG] DONJON remis a zero : {} epreuves ; {}", n, sb);
            }));
            trialAt(15, () -> srv.execute(() -> {
                var p = srv.getPlayerList().getPlayerByName(me);
                var lv = p.level();
                var center = dungeonBase.offset(0, 0, 154);
                hubTeleport(srv, me, center.getX(), center.getY(), center.getZ() - 12);
                String key = switch (id) {
                    case "crypte_gobeline" -> "roi_gobelin";
                    case "catacombes" -> "liche_ancienne";
                    case "forteresse_infernale" -> "seigneur_ignis";
                    case "sanctuaire_glace" -> "titan_glace";
                    default -> "avatar_neant";
                };
                var boss = com.mmorpg.entity.boss.BossManager.spawnBoss(lv, com.mmorpg.entity.boss.BossManager.typeFor(key), key, center.above(), "Test");
                if (boss != null) {
                    var d = boss.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);
                    d.hp = d.maxHp * 0.5F;
                    boss.syncData(com.mmorpg.registry.ModAttachments.MOB);
                    MMORPG.LOGGER.info("[MMORPG] BOSS {} apparu a 50 % de vie", key);
                }
            }));
            trialAt(8, () -> srv.execute(() -> {
                var lv = srv.getPlayerList().getPlayerByName(me).level();
                var center = dungeonBase.offset(0, 0, 154);
                int adds = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(center).inflate(26),
                        m -> m instanceof com.mmorpg.entity.RpgMob && !(m instanceof com.mmorpg.entity.boss.RpgBoss)).size();
                MMORPG.LOGGER.info("[MMORPG] BOSS phase 2 : {} renforts dans l'arene (solo : peu attendus)", adds);
            }));
            trialAt(2, () -> {
                com.mmorpg.client.AreaEntryClient.cancel();
                shot(mc, id + "_boss_phase");
            });
            // entree dans l'antre du boss : l'animation doit se declencher
            trialAt(20, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 124)));
            trialAt(30, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 140)));
            trialAt(14, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 8"));
            trialAt(34, () -> shot(mc, id + "_antre_animation"));
            trialAt(70, () -> shot(mc, id + "_antre_animation2"));
            trialAt(40, () -> {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            });
        }
        if (tick > 330) {
            java.util.Iterator<Object[]> it = TRIAL_QUEUE.iterator();
            while (it.hasNext()) {
                Object[] e = it.next();
                if (tick >= (int) e[0]) {
                    it.remove();
                    ((Runnable) e[1]).run();
                }
            }
        }
    }

    private static void hubTeleport(net.minecraft.server.MinecraftServer srv, String name, double x, double y, double z) {
        var p = srv.getPlayerList().getPlayerByName(name);
        if (p == null) return;
        p.teleportTo(p.level(), x + 0.5, y, z + 0.5, java.util.Set.of(), 0F, 5F, true);
        p.resetFallDistance();
    }

    /** Verification des cinematiques d'un donjon : entree, vagues, antre du boss, phases, victoire. */
    private static void cinePhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode survival");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 3000 150 3000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 10) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
        });
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire " + id);
        if (tick == 330) {
            trialCursor = 330;
            var all = new java.util.ArrayList<>(com.mmorpg.dungeon.TrialData.get(srv).all());
            // 1. entree du donjon : on part de l'exterieur
            trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() - 12)));
            trialAt(25, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 3)));
            trialAt(8, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 6"));
            trialAt(30, () -> shot(mc, id + "_cine_entree_1"));
            trialAt(70, () -> shot(mc, id + "_cine_entree_2"));
            trialAt(80, () -> shot(mc, id + "_cine_entree_3"));
            // 2. vagues (si le donjon en a)
            for (var t0 : all) {
                if (!t0.kind().equals("waves")) continue;
                String tid = t0.id();
                trialAt(60, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.pos().get(2) - 8)));
                for (int w = 1; w <= 3; w++) {
                    int wave = w;
                    trialAt(w == 1 ? 18 : 20, () -> shot(mc, id + "_cine_vague_" + wave));
                    trialAt(30, () -> srv.execute(() -> {
                        var lv = srv.getPlayerList().getPlayerByName(me).level();
                        for (var m : lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(new net.minecraft.core.BlockPos(t0.pos().get(0), t0.pos().get(1), t0.pos().get(2))).inflate(24),
                                e -> e instanceof com.mmorpg.entity.RpgMob)) m.discard();
                    }));
                    trialAt(2, () -> { });
                    trialAt(100, () -> { });
                }
            }
            // 3. antre du boss : entree, combat, phases, victoire
            trialAt(40, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 122)));
            trialAt(30, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 136)));
            trialAt(8, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 6"));
            trialAt(35, () -> shot(mc, id + "_cine_antre"));
            trialAt(120, () -> srv.execute(() -> {
                var lv = srv.getPlayerList().getPlayerByName(me).level();
                var center = dungeonBase.offset(0, 0, 154);
                String key = switch (id) {
                    case "crypte_gobeline" -> "roi_gobelin";
                    case "catacombes" -> "liche_ancienne";
                    case "forteresse_infernale" -> "seigneur_ignis";
                    case "sanctuaire_glace" -> "titan_glace";
                    default -> "avatar_neant";
                };
                bossRef = com.mmorpg.entity.boss.BossManager.spawnBoss(lv, com.mmorpg.entity.boss.BossManager.typeFor(key), key, center.above(), "Test");
            }));
            trialAt(30, () -> shot(mc, id + "_cine_boss_phase1"));
            trialAt(100, () -> srv.execute(() -> setBossHp(0.5F)));
            trialAt(30, () -> shot(mc, id + "_cine_boss_phase2"));
            trialAt(120, () -> srv.execute(() -> setBossHp(0.2F)));
            trialAt(30, () -> shot(mc, id + "_cine_boss_phase3"));
            trialAt(110, () -> srv.execute(() -> {
                var p = srv.getPlayerList().getPlayerByName(me);
                if (bossRef != null) com.mmorpg.combat.RpgCombat.dealDamage(p, bossRef, 1.0E9, false, false);
            }));
            trialAt(40, () -> shot(mc, id + "_cine_victoire_1"));
            trialAt(70, () -> shot(mc, id + "_cine_victoire_2"));
            trialAt(70, () -> shot(mc, id + "_cine_victoire_3"));
            trialAt(60, () -> {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            });
        }
        if (tick > 330) {
            java.util.Iterator<Object[]> it = TRIAL_QUEUE.iterator();
            while (it.hasNext()) {
                Object[] e = it.next();
                if (tick >= (int) e[0]) {
                    it.remove();
                    ((Runnable) e[1]).run();
                }
            }
        }
    }

    private static net.minecraft.world.entity.Mob bossRef;

    private static void setBossHp(float fraction) {
        if (bossRef == null) return;
        var d = bossRef.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);
        if (d == null) return;
        d.hp = d.maxHp * fraction;
        bossRef.syncData(com.mmorpg.registry.ModAttachments.MOB);
    }

    /** Verification des donjons instancies : porte, exemplaire par groupe, reutilisation, objet d'invocation. */
    private static void instancesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 3000 150 3000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 50) srv.execute(() -> com.mmorpg.dungeon.InstanceManager.debugForgetAll(srv));
        if (tick == 60) cmd(mc, "mmorpg donjon catacombes");
        if (tick == 100) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var gate = com.mmorpg.world.WaypointData.get(srv).all().stream().filter(w -> w.name.startsWith("Catacombes") && com.mmorpg.dungeon.InstanceManager.isGate(srv, w.id)).findFirst().orElse(null);
            MMORPG.LOGGER.info("[MMORPG] INSTANCE porte creee = {} ; est une porte = {}", gate != null, gate != null && com.mmorpg.dungeon.InstanceManager.isGate(srv, gate.id));
            if (gate != null) {
                boolean ok = com.mmorpg.dungeon.InstanceManager.enter(p, gate.id);
                MMORPG.LOGGER.info("[MMORPG] INSTANCE entree = {} ; dimension {} ; position ({}, {}, {})", ok, p.level().dimension().identifier(), (int) p.getX(), (int) p.getY(), (int) p.getZ());
            }
        });
        if (tick == 200) {
            cmd(mc, "tp " + me + " ~ ~ ~ 0 8");
        }
        if (tick == 230) shot(mc, "instance_entree");
        if (tick == 240) srv.execute(() -> {
            String pool = "catacombes:0";
            var other = com.mmorpg.dungeon.InstanceManager.allocate(srv, pool, "autre-groupe");
            var mine = com.mmorpg.dungeon.InstanceManager.allocate(srv, pool, com.mmorpg.dungeon.InstanceManager.ownerKey(srv.getPlayerList().getPlayerByName(me)));
            MMORPG.LOGGER.info("[MMORPG] INSTANCE deux groupes -> exemplaires distincts = {} ; exemplaires construits = {} (attendu 2)",
                    other != null && mine != null && other.index != mine.index, com.mmorpg.dungeon.InstanceManager.count(srv, pool));
            com.mmorpg.dungeon.InstanceManager.release(srv, pool, "autre-groupe");
            var reuse = com.mmorpg.dungeon.InstanceManager.allocate(srv, pool, "troisieme-groupe");
            MMORPG.LOGGER.info("[MMORPG] INSTANCE exemplaire libere reutilise = {} ; exemplaires construits = {} (attendu 2)",
                    reuse != null && other != null && reuse.index == other.index, com.mmorpg.dungeon.InstanceManager.count(srv, pool));
        });
        if (tick == 300) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var lv = p.level();
            String group = null;
            for (var t : com.mmorpg.dungeon.TrialData.get(srv).all()) if (Math.abs(t.pos().get(0) - p.getX()) < 40) group = com.mmorpg.dungeon.TrialData.group(t.id());
            if (group == null) {
                MMORPG.LOGGER.info("[MMORPG] INSTANCE aucune epreuve trouvee pres du joueur");
                return;
            }
            var trials = com.mmorpg.dungeon.TrialData.get(srv).inGroup(group);
            var key = com.mmorpg.registry.ModItems.get("grimoire_interdit");
            var altar = new net.minecraft.core.BlockPos(trials.get(0).pos().get(0), trials.get(0).pos().get(1), trials.get(0).pos().get(2));
            MMORPG.LOGGER.info("[MMORPG] INSTANCE groupe {} : {} epreuves", group, trials.size());
            for (var t : trials) com.mmorpg.dungeon.TrialManager.debugComplete(lv, t.id(), p);
            int parts = group.lastIndexOf('_');
            int oz = Integer.parseInt(group.substring(parts + 1));
            int ox = Integer.parseInt(group.substring(group.lastIndexOf('_', parts - 1) + 1, parts));
            var arena = new net.minecraft.core.BlockPos(ox, trials.get(0).pos().get(1), oz + 154);
            int drops = lv.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(arena).inflate(16), e -> e.getItem().is(key)).size();
            MMORPG.LOGGER.info("[MMORPG] INSTANCE objet d'invocation au sol apres la derniere epreuve = {} (attendu 1)", drops);
            var chest = lv.getBlockEntity(new net.minecraft.core.BlockPos(ox, trials.get(0).pos().get(1), oz + 186));
            boolean inChest = false;
            if (chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity cbe) {
                for (int i = 0; i < cbe.getContainerSize(); i++) if (cbe.getItem(i).is(key)) inChest = true;
            }
            MMORPG.LOGGER.info("[MMORPG] INSTANCE objet d'invocation dans le coffre du tresor = {} (attendu false)", inChest);
        });
        if (tick == 320) {
            cmd(mc, "tp " + me + " ~ ~ ~ 0 8");
        }
        if (tick == 380) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static net.minecraft.core.BlockPos dungeonBase;
    /** Prises de vue d'un donjon : (x, z relatifs, hauteur de depart, cap, inclinaison, nom). Le sol est a y = 1. */
    private static final double[][] DUNGEON_SHOTS = {
            {0, 3, 1, 0, -4}, {0, 6, 1, 0, -42}, {0, 24, 1, 0, 0}, {0, 38, 1, 0, -6}, {0, 44, 1, 20, -48},
            {0, 63, 1, 0, 12}, {0, 88, 1, 0, -4}, {0, 98, 1, -35, -40}, {0, 113, 1, 0, 0}, {0, 130, 1, 0, 0},
            {0, 138, 1, 0, -28}, {0, 168, 1, 180, -15}, {0, 175, 1, 0, 0}, {0, 178, 1, 0, 0}};

    private static void dungeonPhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 3000 150 3000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire " + id);
        if (tick == 320 && dungeonBase != null) srv.execute(() -> {
            var lv = srv.getPlayerList().getPlayerByName(me).level();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) sb.append(lv.getBlockState(dungeonBase.offset(i % 6 - 2, -1, 4 + i / 6)).getBlock().getDescriptionId()).append(' ');
            MMORPG.LOGGER.info("[MMORPG] SOL {}", sb);
        });
        int first = 330;
        int step = 22;
        int n = (tick - first) / step;
        if (tick >= first && n < DUNGEON_SHOTS.length) {
            int local = (tick - first) % step;
            double[] sh = DUNGEON_SHOTS[n];
            if (local == 0 && dungeonBase != null) {
                cmd(mc, String.format(java.util.Locale.ROOT, "tp %s %d %d %d %.1f %.1f", me,
                        dungeonBase.getX() + (int) sh[0], dungeonBase.getY() + (int) sh[2] - 1, dungeonBase.getZ() + (int) sh[1], sh[3], sh[4]));
            }
            if (local == 14) {
                com.mmorpg.client.AreaEntryClient.cancel();
                mc.gui.hud.getChat().clearMessages(true);
            }
            if (local == 16) shot(mc, id + "_" + String.format("%02d", n));
        }
        if (tick == first + step * DUNGEON_SHOTS.length + 20) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static void hubSpot(net.minecraft.server.MinecraftServer srv, String name, com.mmorpg.rpg.PlayerClass cls) {
        var p = srv.getPlayerList().getPlayerByName(name);
        var hub = com.mmorpg.hub.HubData.get(srv);
        if (p == null || !hub.exists()) return;
        int[] sp = com.mmorpg.hub.HubLayout.classSpot(cls);
        var o = hub.origin();
        p.teleportTo(hub.level(srv), o.getX() + sp[0] + 8, o.getY(), o.getZ() + sp[2] + 8, java.util.Set.of(), 135F, 8F, true);
    }

    private static void spawnDuelist(net.minecraft.server.MinecraftServer srv, String name) {
        var p = srv.getPlayerList().getPlayerByName(name);
        if (p == null) return;
        var e = com.mmorpg.registry.ModEntities.TRAINING_DUMMY.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (e == null) return;
        e.snapTo(p.getX() + 2.5, p.getY(), p.getZ(), 90F, 0F);
        e.setup(3);
        p.level().addFreshEntity(e);
        MMORPG.LOGGER.info("[MMORPG] DUELLISTE apparu en {} {} {} variante {}", e.getX(), e.getY(), e.getZ(), e.variantId());
    }

    private static void zoneStep(net.minecraft.server.MinecraftServer srv, String name, int step) {
        var p = srv.getPlayerList().getPlayerByName(name);
        if (p == null) return;
        var level = p.level();
        var target = p.blockPosition().relative(net.minecraft.core.Direction.EAST, 2);
        switch (step) {
            case 0 -> {
                com.mmorpg.zone.ZoneManager.pick(p, p.blockPosition().offset(-6, -1, -6));
                com.mmorpg.zone.ZoneManager.pick(p, p.blockPosition().offset(6, -1, 6));
            }
            case 1, 3 -> {
                level.setBlock(target, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                p.gameMode.destroyBlock(target);
                boolean intact = level.getBlockState(target).is(net.minecraft.world.level.block.Blocks.STONE);
                MMORPG.LOGGER.info("[MMORPG] ZONE casse un bloc ({}) : bloc {} (attendu {})", step == 1 ? "construction interdite" : "construction autorisee",
                        intact ? "intact" : "casse", step == 1 ? "intact" : "casse");
            }
            case 2 -> {
                var z = com.mmorpg.zone.ZoneManager.zoneAt(level, p.blockPosition());
                MMORPG.LOGGER.info("[MMORPG] ZONE a la position du joueur : {}", z == null ? "aucune" : z.name() + " / " + com.mmorpg.zone.ZoneManager.describe(z));
            }
            case 5 -> {
                for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
                    level.setBlock(p.blockPosition().offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
            case 4 -> {
                var far = p.blockPosition().offset(60, 0, 0);
                MMORPG.LOGGER.info("[MMORPG] ZONE hors zone (60 blocs) : {} (attendu aucune)", com.mmorpg.zone.ZoneManager.zoneAt(level, far) == null ? "aucune" : "dans une zone");
            }
            default -> {
            }
        }
    }

    /** Vie RPG du joueur cote serveur (fraction >= 0 : la regle d'abord), pour la verification du duelliste. */
    private static void duelState(net.minecraft.server.MinecraftServer srv, String name, String label, double setFraction) {
        var p = srv.getPlayerList().getPlayerByName(name);
        if (p == null) return;
        var d = com.mmorpg.server.RpgPlayers.get(p);
        double max = d.stats.get(com.mmorpg.rpg.Stat.MAX_HP);
        if (setFraction >= 0) d.hp = (float) (max * setFraction);
        MMORPG.LOGGER.info("[MMORPG] DUELLISTE {} : vie {} / {}", label, d.hp, max);
    }

    private static void stop(Minecraft mc) {
        mc.options.pauseOnLostFocus = true;
        mc.options.save();
        mc.stop();
    }

    private static void cmd(Minecraft mc, String command) {
        mc.player.connection.sendCommand(command);
    }

    private static void editorClick(net.minecraft.client.gui.screens.Screen screen,int x,int y) {
        screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
    }
    private static void editorText(net.minecraft.client.gui.screens.Screen screen,String label,String value) {
        for(var child:screen.children()) if(child instanceof net.minecraft.client.gui.components.EditBox box&&box.getMessage().getString().equals(label)) box.setValue(value);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                msg -> MMORPG.LOGGER.info("[MMORPG] Capture {}", name));
    }

    private static CompoundTag fakeTeleporters() {
        ListTag list = new ListTag();
        String[][] rows = {{"Capitale d'Eldoria", "0", "1"}, {"Port de Valmer", "0", "10"}, {"Crypte Gobeline", "1", "15"},
                {"Catacombes de la Liche", "1", "35"}, {"Citadelle du Néant", "1", "90"}, {"Col des Brumes", "2", "1"}};
        for (int i = 0; i < rows.length; i++) {
            CompoundTag e = new CompoundTag();
            e.putString("id", new java.util.UUID(0, i + 1).toString());
            e.putString("name", rows[i][0]);
            e.putInt("category", Integer.parseInt(rows[i][1]));
            e.putString("dim", "Surface");
            e.putBoolean("sameDim", true);
            e.putInt("x", 200 * i);
            e.putInt("y", 70);
            e.putInt("z", -150 * i);
            e.putInt("minLevel", Integer.parseInt(rows[i][2]));
            e.putBoolean("current", i == 0);
            list.add(e);
        }
        CompoundTag t = new CompoundTag();
        t.put("list", list);
        t.putInt("total", 9);
        t.putString("current", "Capitale d'Eldoria");
        t.putBoolean("scroll", false);
        return t;
    }
}
