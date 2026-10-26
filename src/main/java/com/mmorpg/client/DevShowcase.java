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
        if(MODE.equals("merchant_preview")) {
            if(mc.gui.screen() instanceof com.mmorpg.client.screen.MerchantEditorScreen screen) {
                tick++;
                if(tick==1){mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(3);mc.resizeGui();}
                if(tick==175)editorClick(screen,20,85);
                if(tick==180)shot(mc,"merchant_catalogue");
                if(tick==185)screen.showTab(2);
                if(tick==200)shot(mc,"merchant_appearances");
                if(tick==205)mc.gui.setScreen(null);
            } else if(tick>=205) {
                tick++;if(tick==220)shot(mc,"orientation_scene");if(tick==230)stop(mc);
            }
            return;
        }
        if (MODE.equals("spawn_editor_ui")) {
            // GUI verification without a live world or an authenticated account.
            if (mc.gui.screen() == null) return;
            tick++;
            if(tick==30) {
                mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(3);mc.resizeGui();
                CompoundTag data=new CompoundTag();data.putString("position","0, 96, 0");
                mc.gui.setScreen(new com.mmorpg.client.screen.SpawnEditorScreen(data));
            }
            if(tick==50) shot(mc,"spawn_editor_structures");
            if(tick==55 && mc.gui.screen() instanceof com.mmorpg.client.screen.SpawnEditorScreen s) s.showTab(1);
            if(tick==65) shot(mc,"spawn_editor_portals");
            if(tick==70 && mc.gui.screen() instanceof com.mmorpg.client.screen.SpawnEditorScreen s) s.showTab(3);
            if(tick==80) shot(mc,"spawn_editor_text");
            if(tick==90) {
                CompoundTag data=new CompoundTag();data.putString("kind","mine");data.putLong("duration",754000);data.putLong("kills",27);data.putLong("xp",1350);data.putLong("gold",82);
                data.put("items",new CompoundTag()); // No item registries are bound on this menu-only preview.
                mc.gui.setScreen(new com.mmorpg.client.screen.AdventureSummaryScreen(data));
            }
            if(tick==105) shot(mc,"adventure_summary");
            if(tick==115) stop(mc);
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
        if (!ClientData.received) return;
        tick++;
        // la vitrine doit continuer meme si la fenetre perd le focus
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen && !MODE.equals("interface")) mc.gui.setScreen(null);
        String me = mc.player.getName().getString();
        if (MODE.equals("orientation_scene")) {
            if(tick==0)mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayerByName(me);var level=server.overworld();
                com.mmorpg.hub.CinematicManager.finish(p);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                com.mmorpg.server.RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;
                for(int x=-15;x<=15;x++)for(int z=-10;z<=16;z++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,99,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                p.teleportTo(level,8.5,102,11.5,java.util.Set.of(),145F,10F,true);
                com.mmorpg.server.SpawnEditor.open(p,new net.minecraft.core.BlockPos(2,100,0));com.mmorpg.server.SpawnEditor.placeOriented(p,"forge_arcanique","moi");
                com.mmorpg.server.SpawnEditor.open(p,new net.minecraft.core.BlockPos(-4,100,4));com.mmorpg.server.SpawnEditor.placeOriented(p,"caisse_commune_bloc","moi");
                com.mmorpg.server.SpawnEditor.open(p,new net.minecraft.core.BlockPos(-4,100,4));com.mmorpg.server.SpawnEditor.textOriented(p,"Angle libre",1.5f,3,"moi");
            });
            if(tick==25){mc.gui.setScreen(null);mc.getWindow().setWindowed(1280,720);}
            if(tick==70)shot(mc,"orientation_scene");
            if(tick==95)stop(mc);
            return;
        }
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
        if (MODE.equals("admin")) {          // menu d'administration : mot de passe, edition d'un fichier, refus d'une structure invalide
            adminPhase(mc, me);
            return;
        }
        if (MODE.equals("musique")) {          // musique d'ambiance : hub, spawn (cite), plaines
            musiquePhase(mc, me);
            return;
        }
        if (MODE.equals("montures")) {          // apercu du menu des montures et vue a la 3e personne en selle, pour les 12 montures
            monturesPhase(mc, me);
            return;
        }
        if (MODE.equals("residus")) {          // restes de l'ancien marche dans le hub : un texte et un PNJ poses a l'ouest doivent disparaitre tout seuls
            residusPhase(mc, me);
            return;
        }
        if (MODE.equals("sorts")) {          // sorts des monstres 3D : six monstres a la fois attaquent le joueur, on compte les sorts lances
            sortsPhase(mc, me);
            return;
        }
        if (MODE.equals("attaques")) {          // animations d'attaque des 30 monstres 3D : trois instants de chaque coup, vus de trois quarts
            attaquesPhase(mc, me);
            return;
        }
        if (MODE.equals("plaines")) {          // Plaines d'Aeloria : construction, visite des six regions, monstres, portails
            plainesPhase(mc, me);
            return;
        }
        if (MODE.equals("marche")) {          // Marche d'Aeloria : reconstruction du hub puis visite du quartier
            marchePhase(mc, me);
            return;
        }
        if (MODE.equals("services")) {          // banque et hotel des ventes : modeles 3D, interfaces, depot, vente, achat, comptoir
            servicesPhase(mc, me);
            return;
        }
        if (MODE.equals("cultures")) {          // cultures d'Aeloria : stades, recolte a la houe, niveau de semis
            culturesPhase(mc, me);
            return;
        }
        if (MODE.equals("metallurgie")) {      // presse de compression, outils des mines, lingots rares, recettes de la forge
            metallurgiePhase(mc, me);
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
            if (tick == 110) shot(mc, "codex_tableau_aeloria");
            if (tick == 115) mc.options.keyPlayerList.setDown(true);
            if (tick == 130) {
                var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info == null || info.getTabListDisplayName() == null || !info.getTabListDisplayName().getString().contains(ClientData.DATA.playerClass.label)) throw new IllegalStateException("Missing RPG tab name");
                MMORPG.LOGGER.info("[CODEX CHECKS] TAB PASS: synchronized class and level name");
                shot(mc, "codex_tab_aeloria");
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
        if (MODE.equals("premiere")) {
            if (tick == 30) cmd(mc, "hub");
            if (tick == 500) {
                var box = mc.player.getBoundingBox().inflate(90);
                int masters = mc.level.getEntitiesOfClass(com.mmorpg.entity.hub.ClassMasterEntity.class, box).size();
                int npcs = mc.level.getEntitiesOfClass(com.mmorpg.entity.NpcEntity.class, box).size();
                int dummies = mc.level.getEntitiesOfClass(com.mmorpg.entity.dummy.TrainingDummyEntity.class, box).size();
                int texts = mc.level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, box).size();
                MMORPG.LOGGER.info("[MMORPG] PNJ DU HUB : {} maitres de classe, {} PNJ, {} mannequins, {} textes (attendu 5, 1, 12, 9 ou plus)", masters, npcs, dummies, texts);
                shot(mc, "hub_pnj");
            }
            if (tick == 100 || tick == 400) {
                MMORPG.LOGGER.info("[MMORPG] PREMIERE CONNEXION a {} ticks : dimension {} ; position ({}, {}, {}) ; classe {}", tick, mc.level.dimension().identifier(),
                        (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ(), ClientData.DATA.playerClass);
            }
            if (tick == 520) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (MODE.equals("ecransplats")) {          // tous les ecrans du mod en interface plate (fenetre 1280x720, echelle 2 = 640x360)
            if (tick == 5) {
                mc.getWindow().setWindowed(1280, 720);
                mc.options.guiScale().set(2);
                mc.resizeGui();
                cmd(mc, "gamemode survival " + me);
            }
            MenuScreen.Tab[] tabs = MenuScreen.Tab.values();
            int step = (tick - 30) / 25;
            if (tick >= 30 && step < tabs.length) {
                if ((tick - 30) % 25 == 0) MenuScreen.open(tabs[step]);
                if ((tick - 30) % 25 == 18) shot(mc, "plat_" + tabs[step].name().toLowerCase());
            }
            int t2 = tick - 30 - tabs.length * 25;
            if (t2 >= 0) {
                net.minecraft.nbt.CompoundTag empty = new net.minecraft.nbt.CompoundTag();
                java.util.function.Supplier<net.minecraft.client.gui.screens.Screen>[] makers = new java.util.function.Supplier[]{
                        () -> new com.mmorpg.client.screen.ClassSelectScreen(),
                        () -> new com.mmorpg.client.screen.ShopScreen(empty),
                        () -> new com.mmorpg.client.screen.DungeonMasterScreen(empty),
                        () -> new com.mmorpg.client.screen.QuestGiverScreen(empty),
                        () -> new com.mmorpg.client.screen.TeleporterScreen(empty),
                        () -> new com.mmorpg.client.screen.ForgeScreen(mc.player.blockPosition()),
                        () -> new com.mmorpg.client.screen.ClassMasterScreen(empty),
                        () -> new com.mmorpg.client.screen.HudEditorScreen(null)};
                String[] names = {"classe", "boutique", "donjon", "quetegiver", "teleporteur", "forge", "maitreclasse", "hud"};
                int i = t2 / 25;
                if (i < makers.length) {
                    if (t2 % 25 == 0) {
                        try {
                            mc.gui.setScreen(makers[i].get());
                        } catch (Throwable e) {
                            MMORPG.LOGGER.warn("[MMORPG] PLAT {} : {}", names[i], e.toString());
                        }
                    }
                    if (t2 % 25 == 18) shot(mc, "plat_" + names[i]);
                } else if (t2 == makers.length * 25 + 5) {
                    mc.gui.setScreen(null);
                    mc.options.guiScale().set(0);
                    mc.resizeGui();
                    MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                    stop(mc);
                }
            }
            return;
        }
        if (MODE.equals("mines")) {          // construction de la mine, visite de chaque etage, minage et repousse
            minePhase(mc, me);
            return;
        }
        if (MODE.equals("combat")) {          // un monstre et un boss collent au joueur : perd-il des PV ?
            var srv = mc.getSingleplayerServer();
            if (tick == 5) {
                cmd(mc, "gamemode survival");
                cmd(mc, "difficulty normal");
            }
            if (tick == 8) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                pl.getAbilities().invulnerable = false;
                pl.onUpdateAbilities();
                MMORPG.LOGGER.info("[MMORPG] COMBAT joueur : mode {} invulnerable {} difficulte {}", pl.gameMode(), pl.getAbilities().invulnerable, pl.level().getDifficulty());
            });
            if (tick == 20) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lv = pl.level();
                var pos = pl.blockPosition();
                var gob = com.mmorpg.server.MobSpawner.spawnForCommand(lv, "gobelin", pos.offset(2, 0, 0), 10);
                var orc = com.mmorpg.server.MobSpawner.spawnForCommand(lv, "orc_guerrier", pos.offset(-2, 0, 0), 30);
                var boss = com.mmorpg.server.MobSpawner.spawnForCommand(lv, "roi_gobelin", pos.offset(0, 0, 3), 20);
                MMORPG.LOGGER.info("[MMORPG] COMBAT spawn : gobelin {} orc {} boss {}", gob != null, orc != null, boss != null);
            });
            if (tick >= 20 && tick <= 200 && tick % 20 == 0) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var d = com.mmorpg.server.RpgPlayers.get(pl);
                var near = pl.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(12));
                StringBuilder sb = new StringBuilder();
                for (var m : near) sb.append(m.getType().builtInRegistryHolder().key().identifier().getPath()).append("(cible=").append(m.getTarget() == null ? "aucune" : m.getTarget().getName().getString()).append(",pv=").append((int) m.getHealth()).append(") ");
                MMORPG.LOGGER.info("[MMORPG] COMBAT tick {} : PV joueur rpg {} / vanilla {} ; monstres : {}", tick, d.hp, pl.getHealth(), sb);
            });
            if (tick == 210) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (MODE.equals("bossauto")) {          // le boss se reveille tout seul quand on entre au fond de l'arene
            var srv = mc.getSingleplayerServer();
            if (tick == 5) {
                cmd(mc, "gamemode survival");
                cmd(mc, "execute in minecraft:overworld run tp " + me + " 9000 150 9000 0 0");
            }
            if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
            if (tick == 135) cmd(mc, "mmorpg donjon construire crypte_gobeline");
            if (tick == 330) srv.execute(() -> {
                var zone = com.mmorpg.zone.ZoneData.get(srv).all().stream().filter(z -> z.id().endsWith("_boss") && z.minX() > 8000).findFirst().orElse(null);
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO zone = {}", zone == null ? "aucune" : zone.id());
                if (zone != null) {
                    var pl = srv.getPlayerList().getPlayerByName(me);
                    pl.getAbilities().invulnerable = true;
                    pl.onUpdateAbilities();
                    var lv = pl.level();
                    var old = lv.getEntitiesOfClass(com.mmorpg.entity.boss.RpgBoss.class, new net.minecraft.world.phys.AABB(zone.minX() - 60, 0, zone.minZ() - 60, zone.maxX() + 60, 320, zone.maxZ() + 60));
                    MMORPG.LOGGER.info("[MMORPG] BOSSAUTO boss deja presents (restes de tests) : {} -> retires", old.size());
                    old.forEach(net.minecraft.world.entity.Entity::discard);
                    bossTp = new int[]{zone.minX() + 23, dungeonBase.getY() + 1, zone.minZ() + 23 - 6};
                }
            });
            if (tick == 335 && bossTp != null) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                pl.connection.teleport(bossTp[0] + 0.5, bossTp[1], bossTp[2] + 0.5, 0F, 5F);
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO connection.teleport -> position ({}, {}, {})", pl.getX(), pl.getY(), pl.getZ());
            });
            if (tick > 332 && tick < 400 && tick % 12 == 0) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO suivi tick {} : ({}, {}, {})", tick, (int) pl.getX(), (int) pl.getY(), (int) pl.getZ());
            });
            if (tick == 400) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lvl0 = pl.level();
                var zz = com.mmorpg.zone.ZoneManager.zoneAt(lvl0, pl.blockPosition());
                if (zz != null) {
                    int found = 0;
                    for (int xx = zz.minX() + 15; xx <= zz.minX() + 31 && found < 3; xx++) for (int zq = zz.minZ() + 15; zq <= zz.minZ() + 31 && found < 3; zq++) for (int yy = 40; yy < 200 && found < 3; yy++) {
                        var bs = lvl0.getBlockState(new net.minecraft.core.BlockPos(xx, yy, zq));
                        if (bs.getBlock() instanceof com.mmorpg.block.AltarBlock) {
                            found++;
                            MMORPG.LOGGER.info("[MMORPG] BOSSAUTO autel en ({}, {}, {}) ; centre de zone ({}, {})", xx, yy, zq, zz.minX() + 23, zz.minZ() + 23);
                        }
                    }
                    if (found == 0) MMORPG.LOGGER.info("[MMORPG] BOSSAUTO aucun autel trouve dans la zone");
                }
                var z0 = com.mmorpg.zone.ZoneManager.zoneAt(pl.level(), pl.blockPosition());
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO joueur ({}, {}, {}) zone = {} boss = {} dimension = {}", pl.getX(), pl.getY(), pl.getZ(), z0 == null ? "aucune" : z0.id(), z0 != null && z0.boss(), pl.level().dimension());
                var near = pl.level().getEntitiesOfClass(com.mmorpg.entity.boss.RpgBoss.class, pl.getBoundingBox().inflate(48));
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO apres 0,75 s a l'entree : {} boss", near.size());
            });
            if (tick == 640) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var near = pl.level().getEntitiesOfClass(com.mmorpg.entity.boss.RpgBoss.class, pl.getBoundingBox().inflate(48));
                MMORPG.LOGGER.info("[MMORPG] BOSSAUTO apres le delai : {} boss ({})", near.size(), near.isEmpty() ? "-" : near.get(0).getName().getString());
            });
            if (tick == 650) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (MODE.equals("interface")) {
            if (tick == 5) cmd(mc, "gamemode creative");
            if (tick == 20) mc.gui.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            if (tick == 45) shot(mc, "ui_pause");
            if (tick == 50) mc.gui.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(new net.minecraft.client.gui.screens.PauseScreen(true), mc.options));
            if (tick == 75) shot(mc, "ui_options");
            if (tick == 80) mc.gui.setScreen(new net.minecraft.client.gui.screens.options.SoundOptionsScreen(new net.minecraft.client.gui.screens.PauseScreen(true), mc.options));
            if (tick == 105) shot(mc, "ui_son");
            if (tick == 110) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
            if (tick == 135) shot(mc, "ui_creatif");
            if (tick == 140) mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            if (tick == 175) shot(mc, "ui_titre");
            if (tick == 180) {
                mc.gui.setScreen(null);
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (MODE.equals("menumusique")) {
            if (tick == 5) cmd(mc, "gamemode survival");
            if (tick == 20) com.mmorpg.client.screen.MenuScreen.open(com.mmorpg.client.screen.MenuScreen.Tab.PERSONNAGE);
            if (tick == 40) shot(mc, "menu_musique_70");
            if (tick == 45) com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.set(40);
            if (tick == 60) shot(mc, "menu_musique_40");
            if (tick == 65) com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.set(0);
            if (tick == 80) shot(mc, "menu_musique_0");
            if (tick == 85) com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.set(70);
            if (tick == 100) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (MODE.equals("musique")) {
            musicPhase(mc, me);
            return;
        }
        if (MODE.equals("dalles")) {
            dallesPhase(mc, me);
            return;
        }
        if (MODE.equals("mort")) {
            deathPhase(mc, me);
            return;
        }
        if (MODE.equals("sortie")) {
            sortiePhase(mc, me);
            return;
        }
        if (MODE.equals("instances")) {
            instancesPhase(mc, me);
            return;
        }
        if (MODE.startsWith("reveal_")) {
            revealPhase(mc, me, MODE.substring(7));
            return;
        }
        if (MODE.equals("setdonjon")) {
            setDonjonPhase(mc, me);
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
                    cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 45 20");
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
                    cmd(mc, "mmorpg ville Aeloria");
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
                cmd(mc, "mmorpg ville Capitale d'Aeloria");
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
                cmd(mc, "mmorpg ville Aeloria");
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

    private static Object adminField(Object target, String name) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void adminCall(Object target, String method, Class<?>[] types, Object... args) {
        try {
            var m = target.getClass().getDeclaredMethod(method, types);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Menu d'administration : essai avec un mauvais mot de passe, bon mot de passe, ouverture, edition et enregistrement reels. */
    private static void adminPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 20) srv.execute(() -> {
            try {
                com.mmorpg.admin.AdminAuth.setPassword("MotDePasse-Test-42");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        if (tick == 40) cmd(mc, "adminmenu \"mauvais\"");
        if (tick == 60) MMORPG.LOGGER.info("[MMORPG] ADMIN apres mauvais mot de passe : ecran admin ouvert = {} (attendu false)", mc.gui.screen() instanceof com.mmorpg.client.screen.AdminScreen);
        if (tick == 70) {
            // un joueur non authentifie ne recoit rien : on demande un fichier sans etre connecte
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.GET, "shop", 0, 1, ""));
        }
        if (tick == 90) cmd(mc, "adminmenu \"MotDePasse-Test-42\"");
        if (tick == 110) {
            var sc = mc.gui.screen();
            MMORPG.LOGGER.info("[MMORPG] ADMIN apres bon mot de passe : ecran admin ouvert = {} (attendu true)", sc instanceof com.mmorpg.client.screen.AdminScreen);
            if (sc != null) adminCall(sc, "select", new Class<?>[]{String.class}, "shop");
        }
        if (tick == 135) shot(mc, "120_admin_boutique");
        if (tick == 140) {
            var sc = mc.gui.screen();
            @SuppressWarnings("unchecked") var path = (java.util.List<Object>) adminField(sc, "path");
            path.add("buy");
            path.add(0);
        }
        if (tick == 160) shot(mc, "121_admin_article");
        if (tick == 162) {
            // liste deroulante : modification du champ « item » d'un article (propose tous les objets avec leur icone)
            var sc = mc.gui.screen();
            var root0 = (com.google.gson.JsonObject) adminField(sc, "root");
            var item = root0.getAsJsonArray("buy").get(0).getAsJsonObject().get("item");
            adminCall(sc, "startEdit", new Class<?>[]{Object.class, com.google.gson.JsonElement.class, int.class, int.class, int.class}, "item", item, 400, 300, 200);
            ((net.minecraft.client.gui.components.EditBox) adminField(sc, "edit")).setValue("potion");
        }
        if (tick == 164) {
            var sc = mc.gui.screen();
            shot(mc, "121b_admin_liste_objets");
            MMORPG.LOGGER.info("[MMORPG] ADMIN liste deroulante : {} propositions pour le champ item (attendu > 100), objets = {}", ((java.util.List<?>) adminField(sc, "options")).size(), adminField(sc, "optionsAreItems"));
            adminCall(sc, "stopEdit", new Class<?>[]{});
        }
        if (tick == 165) {
            var sc = mc.gui.screen();
            @SuppressWarnings("unchecked") var path = (java.util.List<Object>) adminField(sc, "path");
            path.clear();
            var root = (com.google.gson.JsonObject) adminField(sc, "root");
            // edition reelle d'une valeur numerique : ouverture du champ, saisie, validation (comme un clic puis Entree)
            adminCall(sc, "startEdit", new Class<?>[]{Object.class, com.google.gson.JsonElement.class, int.class, int.class, int.class},
                    "equipmentMultiplier", root.get("equipmentMultiplier"), 400, 300, 120);
            ((net.minecraft.client.gui.components.EditBox) adminField(sc, "edit")).setValue("7.5");
            adminCall(sc, "commitEdit", new Class<?>[]{});
            MMORPG.LOGGER.info("[MMORPG] ADMIN edition : valeur dans l'arbre = {} (attendu 7.5), modifie = {}", root.get("equipmentMultiplier"), adminField(sc, "dirty"));
            // entree ajoutee puis supprimee dans une liste (copie de la derniere)
            var buy = root.getAsJsonArray("buy");
            int n0 = buy.size();
            path.add("buy");
            adminCall(sc, "addEntry", new Class<?>[]{});
            MMORPG.LOGGER.info("[MMORPG] ADMIN ajout dans une liste : {} -> {} entrees (attendu +1)", n0, buy.size());
            buy.remove(buy.size() - 1);
            path.clear();
        }
        if (tick == 180) shot(mc, "122_admin_modifie");
        if (tick == 185) adminCall(mc.gui.screen(), "save", new Class<?>[]{});
        if (tick == 215) {
            srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] ADMIN enregistrement : equipmentMultiplier en memoire = {} (attendu 7.5), sauvegardes = {}",
                    com.mmorpg.config.ConfigManager.shop().equipmentMultiplier, countBackups("shop")));
            shot(mc, "123_admin_enregistre");
        }
        if (tick == 225) {
            // structure invalide : refusee, fichier inchange
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.SAVE, "shop", 0, 1, "{\"buy\": \"pas une liste\"}"));
        }
        if (tick == 250) {
            srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] ADMIN structure invalide : boutique intacte = {} articles (attendu > 10)", com.mmorpg.config.ConfigManager.shop().buy.size()));
            shot(mc, "124_admin_refus");
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.SAVE, "shop", 0, 1, "{ pas du json"));
        }
        if (tick == 275) shot(mc, "125_admin_json_casse");
        if (tick == 280) {
            // retour a la valeur d'origine (6.0) pour ne pas laisser le monde de test modifie
            var sc = mc.gui.screen();
            var root = (com.google.gson.JsonObject) adminField(sc, "root");
            root.addProperty("equipmentMultiplier", 6.0);
            adminCall(sc, "save", new Class<?>[]{});
        }
        if (tick == 292) adminCall(mc.gui.screen(), "select", new Class<?>[]{String.class}, "mobs");
        if (tick == 302) {
            var root = adminField(mc.gui.screen(), "root");
            MMORPG.LOGGER.info("[MMORPG] ADMIN gros fichier : mobs.json charge = {} monstres (attendu > 50)", root instanceof com.google.gson.JsonObject o ? o.size() : -1);
            adminCall(mc.gui.screen(), "save", new Class<?>[]{});
        }
        if (tick == 310) {
            srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] ADMIN valeur restauree : {} (attendu 6.0)", com.mmorpg.config.ConfigManager.shop().equipmentMultiplier));
            ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.LOGOUT, "", 0, 1, ""));
        }
        if (tick == 320) ClientNet.send(new Payloads.AdminRequest(Payloads.AdminRequest.SAVE, "shop", 0, 1, "{}"));
        if (tick == 345) {
            srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] ADMIN apres deconnexion : boutique toujours intacte = {} articles (attendu > 10)", com.mmorpg.config.ConfigManager.shop().buy.size()));
        }
        if (tick == 360) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static long countBackups(String name) {
        try (var files = java.nio.file.Files.list(com.mmorpg.config.ConfigManager.dir().resolve("sauvegardes"))) {
            return files.filter(p -> p.getFileName().toString().startsWith(name + "-")).count();
        } catch (java.io.IOException e) {
            return -1;
        }
    }

    private static void musiquePhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 10) cmd(mc, "gamemode survival " + me);
        if (tick < 100 || !com.mmorpg.plains.PlainsManager.built(srv, 0)) return;
        int t = tick - 100;
        if (t == 0) srv.execute(() -> com.mmorpg.hub.HubManager.goToHub(srv.getPlayerList().getPlayerByName(me), 0));
        if (t == 300) MMORPG.LOGGER.info("[MMORPG] MUSIQUE dans le hub : {}", com.mmorpg.client.HubMusicClient.debugState());
        if (t == 310) srv.execute(() -> com.mmorpg.hub.HubManager.goToSpawn(srv.getPlayerList().getPlayerByName(me)));
        if (t == 700) MMORPG.LOGGER.info("[MMORPG] MUSIQUE dans le spawn : {}", com.mmorpg.client.HubMusicClient.debugState());
        if (t == 710) srv.execute(() -> com.mmorpg.plains.PlainsManager.enter(srv.getPlayerList().getPlayerByName(me), 0));
        if (t == 1000) MMORPG.LOGGER.info("[MMORPG] MUSIQUE dans la plaine : {}", com.mmorpg.client.HubMusicClient.debugState() + " ; " + com.mmorpg.client.DungeonMusicClient.debugState());
        if (t == 1010) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static void monturesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        var all = com.mmorpg.mount.MountType.values();
        if (tick == 5) mc.getWindow().setWindowed(960, 600);
        if (tick == 10) {
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "mmorpg classe " + me + " guerrier");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick < 80 || !com.mmorpg.plains.PlainsManager.built(srv, 0)) return;
        int t = tick - 80, per = 90, idx = t / per, rel = t % per;
        if (idx >= all.length) {
            if (t == all.length * per + 5) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        var type = all[idx];
        if (rel == 0) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var lay = com.mmorpg.plains.PlainsLayout.get(0);
            com.mmorpg.server.MountManager.despawn(pl);
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
            pl.teleportTo(lv, lay.x0() + com.mmorpg.plains.PlainsLayout.PLAZA_X + 0.5, com.mmorpg.plains.PlainsLayout.BASE + 1, com.mmorpg.plains.PlainsLayout.Z0 + com.mmorpg.plains.PlainsLayout.PLAZA_Z + 4.5, java.util.Set.of(), 0F, 0F, true);
            var d = com.mmorpg.server.RpgPlayers.get(pl);
            d.mounts.add(type.id);
            d.nextMountTick = 0;
            d.pvpUntilTick = 0;
            com.mmorpg.server.RpgPlayers.sync(pl);
        });
        if (rel == 6) mc.execute(() -> com.mmorpg.client.screen.MenuScreen.open(com.mmorpg.client.screen.MenuScreen.Tab.MONTURES));
        if (rel == 8) mc.execute(() -> {
            if (mc.gui.screen() instanceof com.mmorpg.client.screen.MountsScreen ms) ms.select(type);
        });
        if (rel == 14) shot(mc, "mont_menu_" + type.id);
        if (rel == 16) mc.execute(() -> mc.gui.setScreen(null));
        if (rel == 20) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            boolean ok = com.mmorpg.server.MountManager.summon(pl, type.id);
            MMORPG.LOGGER.info("[MMORPG] MONTURES {} : invoquee = {}, en selle = {}", type.id, ok, pl.isPassenger());
        });
        if (rel == 26) mc.execute(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        if (rel == 44) shot(mc, "mont_selle_dos_" + type.id);
        if (rel == 48) mc.execute(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        if (rel == 66) shot(mc, "mont_selle_face_" + type.id);
        if (rel == 70) mc.execute(() -> mc.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private static void residusPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 10) cmd(mc, "gamemode spectator " + me);
        if (tick < 60 || com.mmorpg.hub.HubData.get(srv).origin() == null) return;
        int t = tick - 60;
        if (t == 0) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            com.mmorpg.hub.HubManager.goToHub(pl, 0);
        });
        if (t == 20) srv.execute(() -> {
            var hub = com.mmorpg.hub.HubData.get(srv);
            var lv = hub.level(srv);
            var o = hub.origin();
            var src = srv.createCommandSourceStack().withLevel(lv).withSuppressedOutput();
            srv.getCommands().performPrefixedCommand(src, String.format(java.util.Locale.ROOT,
                    "summon minecraft:text_display %d %d %d {Tags:[\"mmorpg_hub\"],text:{text:\"RESIDU\"}}", o.getX() - 100, o.getY() + 5, o.getZ()));
            var npc = com.mmorpg.registry.ModEntities.NPC.get().create(lv, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            npc.snapTo(o.getX() - 96, o.getY(), o.getZ() + 4, 0F, 0F);
            lv.addFreshEntity(npc);
            int n = lv.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(o.getX() - 170, o.getY() - 80, o.getZ() - 140, o.getX() - 52.5, o.getY() + 90, o.getZ() + 140),
                    e -> e instanceof com.mmorpg.entity.NpcEntity || e instanceof net.minecraft.world.entity.Display.TextDisplay).size();
            MMORPG.LOGGER.info("[MMORPG] RESIDUS poses : {} entite(s) a l'ouest du hub (attendu 2 au moins)", n);
        });
        if (t == 300) srv.execute(() -> {
            var hub = com.mmorpg.hub.HubData.get(srv);
            var lv = hub.level(srv);
            var o = hub.origin();
            int n = lv.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(o.getX() - 170, o.getY() - 80, o.getZ() - 140, o.getX() - 52.5, o.getY() + 90, o.getZ() + 140),
                    e -> e instanceof com.mmorpg.entity.NpcEntity || e instanceof net.minecraft.world.entity.Display.TextDisplay).size();
            MMORPG.LOGGER.info("[MMORPG] RESIDUS apres nettoyage : {} (attendu 0)", n);
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        });
    }

    /** Les 30 monstres 3D, six par six, autour du joueur invulnerable de la place de la premiere plaine : les sorts doivent partir. */
    private static void sortsPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(960, 600);
        if (tick == 10) {
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "difficulty normal");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick < 60) return;
        if (!com.mmorpg.plains.PlainsManager.built(srv, 0)) {
            if (tick > 12000) stop(mc);
            return;
        }
        var all = com.mmorpg.entity.mob.ImportedMobs.ALL;
        int batch = 6, per = 330;
        int t = tick - 60, idx = t / per, rel = t % per;
        int batches = (all.length + batch - 1) / batch;
        if (idx >= batches) {
            if (t == batches * per + 5) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        if (rel == 0) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
            pl.setHealth(pl.getMaxHealth());
            var lay = com.mmorpg.plains.PlainsLayout.get(0);
            int plz = 100, plx = lay.pathX(plz);                         // sur le chemin, hors de la place d'entree (la place est une zone sans monstres)
            double cx = lay.x0() + plx + 0.5, cz = com.mmorpg.plains.PlainsLayout.Z0 + plz + 0.5, cy = lay.height(plx, plz) + 1;
            pl.teleportTo(lv, cx, cy, cz, java.util.Set.of(), 0F, 0F, true);
            for (var old : lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(60, 60, 60), m -> m instanceof com.mmorpg.entity.mob.ImportedMobEntity)) old.discard();
            for (int i = 0; i < batch && idx * batch + i < all.length; i++) {
                var e = all[idx * batch + i];
                double a = Math.PI * 2 * i / batch;
                int mx = (int) (cx + Math.cos(a) * 7), mz = (int) (cz + Math.sin(a) * 7);
                var pos = new net.minecraft.core.BlockPos(mx, Math.max(lay.height(mx - lay.x0(), mz - com.mmorpg.plains.PlainsLayout.Z0), com.mmorpg.plains.PlainsLayout.BASE) + 1, mz);
                var mob = com.mmorpg.server.MobSpawner.spawnForCommand(lv, e.key(), pos, 40);
                if (mob instanceof net.minecraft.world.entity.Mob m) m.setTarget(pl);
            }
        });
        if (rel >= 10 && rel < 20 && rel % 3 == 0) srv.execute(() -> {                    // le joueur est soigne en continu : on garde les monstres sur lui
            var pl = srv.getPlayerList().getPlayerByName(me);
            if (pl.isAlive()) pl.setHealth(pl.getMaxHealth());
            for (var m : pl.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(40), x -> x instanceof com.mmorpg.entity.mob.ImportedMobEntity)) {
                if (m.getTarget() != pl) m.setTarget(pl);
            }
        });
        if (rel == 120) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var mobs = pl.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(40), m -> m instanceof com.mmorpg.entity.mob.ImportedMobEntity);
            int targeting = 0;
            for (var m : mobs) if (m.getTarget() != null) targeting++;
            MMORPG.LOGGER.info("[MMORPG] SORTS lot {} : {} monstres presents, {} ont une cible, joueur pv {}", idx + 1, mobs.size(), targeting, pl.getHealth());
        });
        if (rel == 25) srv.execute(() -> {                                                          // essai direct de chaque sort, joueur protege
            var pl = srv.getPlayerList().getPlayerByName(me);
            StringBuilder sb = new StringBuilder();
            if (!pl.isAlive()) {
                MMORPG.LOGGER.info("[MMORPG] SORTS lot {} : joueur mort, essai direct ignore", idx + 1);
                return;
            }
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
            for (var m : pl.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(40), x -> x instanceof com.mmorpg.entity.mob.ImportedMobEntity)) {
                for (var sp : com.mmorpg.entity.mob.ImportedSpells.of(com.mmorpg.entity.mob.ImportedMobs.ALL.length > 0 ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath() : "")) {
                    try {
                        com.mmorpg.entity.mob.ImportedSpells.cast(m, sp, pl);
                        sb.append(sp.id()).append(" ok  ");
                    } catch (Exception ex) {
                        sb.append(sp.id()).append(" ERREUR ").append(ex).append("  ");
                    }
                }
            }
            MMORPG.LOGGER.info("[MMORPG] SORTS lot {} essai direct : {}", idx + 1, sb);
        });
        if (rel == 300) srv.execute(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < batch && idx * batch + i < all.length; i++) {
                var key = all[idx * batch + i].key();
                var spells = com.mmorpg.entity.mob.ImportedSpells.of(key);
                for (var sp : spells) sb.append(key).append('/').append(sp.id()).append('=').append(com.mmorpg.entity.mob.ImportedSpells.CAST_LOG.getOrDefault(key + "/" + sp.id(), 0)).append("  ");
                if (spells.isEmpty()) sb.append(key).append("(aucun sort)  ");
            }
            MMORPG.LOGGER.info("[MMORPG] SORTS lot {} : {}", idx + 1, sb);
        });
    }

    private static net.minecraft.world.entity.Mob atkMob;

    /** Chaque monstre 3D importe est pose seul sur la place de la premiere plaine, fige, puis frappe : captures a 0,25 s, 0,45 s et 0,65 s. */
    private static void attaquesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(960, 600);
        if (tick == 10) {
            cmd(mc, "gamemode spectator " + me);
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (tick < 60) return;
        if (!com.mmorpg.plains.PlainsManager.built(srv, 0)) {
            if (tick % 100 == 0) MMORPG.LOGGER.info("[MMORPG] ATTAQUES attente de la plaine 1");
            if (tick > 12000) stop(mc);
            return;
        }
        var all = com.mmorpg.entity.mob.ImportedMobs.ALL;
        String only = System.getProperty("mmorpg.attaques", "");
        java.util.List<com.mmorpg.entity.mob.ImportedMobs.Entry> list = new java.util.ArrayList<>();
        for (var e : all) if (only.isEmpty() || java.util.Arrays.asList(only.split(",")).contains(e.key())) list.add(e);
        int t = tick - 60, per = 44;
        int idx = t / per, rel = t % per;
        if (idx >= list.size()) {
            if (t == list.size() * per + 5) {
                MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
                stop(mc);
            }
            return;
        }
        var entry = list.get(idx);
        if (rel == 0) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            if (atkMob != null) atkMob.discard();
            var lay = com.mmorpg.plains.PlainsLayout.get(0);
            for (var old : lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(lay.x0() + com.mmorpg.plains.PlainsLayout.PLAZA_X, 40, com.mmorpg.plains.PlainsLayout.Z0 + com.mmorpg.plains.PlainsLayout.PLAZA_Z, 0, 0, 0).inflate(60, 80, 60),
                    m -> m instanceof com.mmorpg.entity.mob.ImportedMobEntity)) old.discard();
            double mx = lay.x0() + com.mmorpg.plains.PlainsLayout.PLAZA_X + 0.5, mz = com.mmorpg.plains.PlainsLayout.Z0 + com.mmorpg.plains.PlainsLayout.PLAZA_Z + 0.5, my = com.mmorpg.plains.PlainsLayout.BASE + 1;
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(MMORPG.id(entry.key()));
            var mob = (net.minecraft.world.entity.Mob) type.create(lv, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            mob.snapTo(mx, my, mz, 0F, 0F);
            mob.setNoAi(true);
            mob.setSilent(true);
            mob.setPersistenceRequired();
            lv.addFreshEntity(mob);
            atkMob = mob;
            double h = entry.scaledHeight(), d = Math.max(3.6, Math.max(h * 2.1, entry.scaledWidth() * 3.2));
            double dx = d * 0.62, dz = d * 0.78;
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            double eye = my + h * 0.55 + 0.1;
            float pitch = (float) Math.toDegrees(Math.atan2(eye - (my + h * 0.5), d)) ;
            pl.teleportTo(lv, mx + dx, eye - 1.62, mz + dz, java.util.Set.of(), yaw, pitch, true);
        });
        if (rel == 14) srv.execute(() -> {
            if (atkMob != null) atkMob.level().broadcastEntityEvent(atkMob, (byte) 4);
        });
        if (rel == 14 + 5) shot(mc, "atk_" + entry.key() + "_a");
        if (rel == 14 + 9) shot(mc, "atk_" + entry.key() + "_b");
        if (rel == 14 + 13) shot(mc, "atk_" + entry.key() + "_c");
    }

    private static int plT0 = -1;
    private static net.minecraft.core.BlockPos plCrop;

    /** Plaines d'Aeloria : six cartes, menu verrouille, visite, monstres par zones, recolte des champs, portail cache, retour au Spawn. */
    private static void plainesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            cmd(mc, "gamemode survival " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (plT0 < 0) {
            if (tick > 40 && tick % 100 == 0) {
                boolean ok = com.mmorpg.plains.PlainsManager.allBuilt(srv) && !com.mmorpg.plains.PlainsManager.building()
                        && com.mmorpg.hub.SpawnData.get(srv).layout() >= com.mmorpg.hub.SpawnBuilder.LAYOUT_VERSION;
                MMORPG.LOGGER.info("[MMORPG] PLAINES attente : {} % (pret = {})", com.mmorpg.plains.PlainsManager.percent(), ok);
                if (ok) plT0 = tick + 40;
            }
            if (tick > 30000) {
                MMORPG.LOGGER.info("[MMORPG] PLAINES : construction jamais terminee");
                stop(mc);
            }
            return;
        }
        int t = tick - plT0;
        // menu : au depart seule la premiere plaine est ouverte
        if (t == 5) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] PLAINES deblocage de depart = {} (attendu 1)", com.mmorpg.server.RpgPlayers.get(pl).plainsUnlocked);
            com.mmorpg.plains.PlainsManager.open(pl);
        });
        if (t == 40) shot(mc, "121_plaines_menu");
        if (t == 45) mc.execute(() -> mc.gui.setScreen(null));
        if (t == 50) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            com.mmorpg.plains.PlainsManager.enterChosen(pl, 2);
        });
        if (t == 70) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] PLAINES plaine verrouillee refusee : plaine ou se trouve le joueur = {} (attendu -1)",
                com.mmorpg.plains.PlainsManager.regionOf(srv.getPlayerList().getPlayerByName(me))));
        if (t == 80) srv.execute(() -> {                                                              // par le menu : ecran de chargement, puis arrivee sur la place
            var pl = srv.getPlayerList().getPlayerByName(me);
            com.mmorpg.plains.PlainsManager.enterChosen(pl, 0);
        });
        if (t == 110) shot(mc, "121_plaines_chargement");
        if (t == 130) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] PLAINES pendant le chargement : plaine = {} (attendu -1), ecran actif = {}",
                com.mmorpg.plains.PlainsManager.regionOf(srv.getPlayerList().getPlayerByName(me)), com.mmorpg.client.LoadingClient.active()));
        if (t == 200) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] PLAINES apres le chargement : plaine = {} (attendu 0), ecran actif = {}",
                com.mmorpg.plains.PlainsManager.regionOf(srv.getPlayerList().getPlayerByName(me)), com.mmorpg.client.LoadingClient.active()));
        // {plaine, colonne x (-1 = chemin), colonne z, hauteur, yaw, pitch, nom}
        double[][] views = {
                {0, 150, 20, 2, 180, 6}, {0, -1, 62, 6, 0, 12}, {0, -1, 82, 8, 270, 22}, {0, -2, 0, 9, 0, 10}, {1, -1, 120, 6, 0, 14}, {2, -1, 120, 6, 0, 14},
                {3, -1, 120, 6, 0, 14}, {4, -1, 120, 6, 0, 14}, {5, -1, 120, 6, 0, 14}, {0, -3, 0, 12, 0, 6}};
        String[] names = {"place", "prairie", "champ", "zone", "marais", "os", "givre", "cendres", "astre", "portail_cache"};
        int t0 = 230, per = 70;
        int idx = (t - t0) / per;
        int rel = (t - t0) % per;
        if (t >= t0 && idx < views.length && rel == 5) {
            double[] v = views[idx];
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
                var lay = com.mmorpg.plains.PlainsLayout.get((int) v[0]);
                int lz, lx;
                if (v[1] == -2) {            // une zone de niveau : au pied du premier totem de la plaine
                    lz = 100;
                    lx = lay.pathX(lz) + 10;
                } else if (v[1] == -3) {     // le portail cache, vu de loin par le nord
                    lx = lay.exitX;
                    lz = lay.exitZ - 16;
                } else {
                    lz = (int) v[2];
                    lx = v[1] < 0 ? lay.pathX(lz) : (int) v[1];
                }
                int y = Math.max(lay.height(lx, lz), com.mmorpg.plains.PlainsLayout.BASE) + (int) v[3];
                pl.teleportTo(lv, lay.x0() + lx + 0.5, y, com.mmorpg.plains.PlainsLayout.Z0 + lz + 0.5, java.util.Set.of(), (float) v[4], (float) v[5], true);
            });
        }
        if (t >= t0 && idx < views.length && rel == 60) shot(mc, "121_plaines_" + names[idx]);
        int end = t0 + views.length * per;
        // niveaux par zones : on echantillonne la plaine 3 et on verifie que le niveau ne suit pas la direction nord-sud
        if (t == end + 5) srv.execute(() -> {
            var lay = com.mmorpg.plains.PlainsLayout.get(2);
            var r = new java.util.Random(7);
            int[] sumByThird = new int[3];
            int[] cnt = new int[3];
            java.util.TreeSet<Integer> levels = new java.util.TreeSet<>();
            for (int i = 0; i < 4000; i++) {
                int lx = 30 + r.nextInt(240), lz = 40 + r.nextInt(280);
                int lvl = lay.mobLevel(lx, lz, r);
                levels.add(lvl);
                int th = Math.min(2, (lz - 40) / 94);
                sumByThird[th] += lvl;
                cnt[th]++;
            }
            MMORPG.LOGGER.info("[MMORPG] PLAINES niveaux de la plaine 3 (33 a 50) : niveau moyen par tiers nord/centre/sud = {} / {} / {} (attendu : proches, sans progression), niveaux distincts = {}",
                    sumByThird[0] / cnt[0], sumByThird[1] / cnt[1], sumByThird[2] / cnt[2], levels.size());
        });
        // monstres autour du joueur dans la plaine 3
        if (t == end + 10) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var lay = com.mmorpg.plains.PlainsLayout.get(2);
            int lz = 150, lx = lay.pathX(lz);
            pl.getAbilities().invulnerable = true;                       // on ne regarde que l'apparition : les monstres ne doivent pas tuer le joueur d'essai
            pl.onUpdateAbilities();
            pl.teleportTo(lv, lay.x0() + lx + 0.5, lay.height(lx, lz) + 1, com.mmorpg.plains.PlainsLayout.Z0 + lz + 0.5, java.util.Set.of(), 0F, 10F, true);
        });
        if (t == end + 270) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var mobs = pl.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(48, 30, 48), m -> m instanceof com.mmorpg.entity.RpgMob);
            int lo = 999, hi = 0;
            java.util.Set<String> kinds = new java.util.TreeSet<>();
            for (var m : mobs) {
                var md = m.getData(com.mmorpg.registry.ModAttachments.MOB);
                lo = Math.min(lo, md.level);
                hi = Math.max(hi, md.level);
                kinds.add(md.key);
            }
            MMORPG.LOGGER.info("[MMORPG] PLAINES monstres autour du joueur (plaine 3) : {} (attendu 12 a 16), niveaux {} a {} (attendu 33 a 50), especes = {}", mobs.size(), lo, hi, kinds);
            MMORPG.LOGGER.info("[MMORPG] PLAINES diagnostic : plaine = {}, monde = {}, fige = {}, construction = {}, pos = {}", com.mmorpg.plains.PlainsManager.regionOf(pl), pl.level().dimension().identifier(),
                    com.mmorpg.dungeon.BossReveal.isLocked(pl), com.mmorpg.plains.PlainsManager.building(), pl.blockPosition());
        });
        if (t == end + 275) shot(mc, "121_plaines_monstres");
        // champs : recolte directe, repousse a heure fixe
        if (t == end + 290) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var lay = com.mmorpg.plains.PlainsLayout.get(0);
            int lz = 82, lx = lay.pathX(lz) + 6;
            pl.teleportTo(lv, lay.x0() + lx + 0.5, lay.height(lx, lz) + 6, com.mmorpg.plains.PlainsLayout.Z0 + lz + 0.5, java.util.Set.of(), 270F, 30F, true);
            plCrop = null;
            for (int dx = -2; dx <= 40 && plCrop == null; dx++) {
                for (int dz = -8; dz <= 8 && plCrop == null; dz++) {
                    for (int y = com.mmorpg.plains.PlainsLayout.BASE; y <= com.mmorpg.plains.PlainsLayout.BASE + 14; y++) {
                        var pos = new net.minecraft.core.BlockPos(lay.x0() + lx + dx, y, com.mmorpg.plains.PlainsLayout.Z0 + lz + dz);
                        if (lv.getBlockState(pos).getBlock() instanceof com.mmorpg.farm.AeloriaCropBlock) {
                            plCrop = pos;
                            break;
                        }
                    }
                }
            }
            MMORPG.LOGGER.info("[MMORPG] PLAINES champ trouve = {}", plCrop);
        });
        if (t == end + 300) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var st = lv.getBlockState(plCrop);
            var block = (com.mmorpg.farm.AeloriaCropBlock) st.getBlock();
            var item = com.mmorpg.registry.ModItems.get(block.crop().product());
            int before = pl.getInventory().countItem(item);
            boolean ok = com.mmorpg.plains.PlainsManager.harvest(lv, pl, plCrop, block, st);
            var after = lv.getBlockState(plCrop);
            MMORPG.LOGGER.info("[MMORPG] PLAINES recolte = {}, objets +{}, age apres = {} (attendu 0)", ok, pl.getInventory().countItem(item) - before, after.getValue(com.mmorpg.farm.AeloriaCropBlock.AGE));
            boolean again = com.mmorpg.plains.PlainsManager.harvest(lv, pl, plCrop, block, after);
            MMORPG.LOGGER.info("[MMORPG] PLAINES seconde recolte immediate refusee = {} (attendu true)", !again);
            int n = com.mmorpg.plains.PlainsManager.respawnAllNow(srv);
            MMORPG.LOGGER.info("[MMORPG] PLAINES cultures en attente de repousse = {}", n);
        });
        if (t == end + 400) srv.execute(() -> {
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            MMORPG.LOGGER.info("[MMORPG] PLAINES age apres repousse = {} (attendu 7)", lv.getBlockState(plCrop).getValue(com.mmorpg.farm.AeloriaCropBlock.AGE));
        });
        // portail cache de la plaine 1 : decouverte, passage a la plaine 2, deblocage
        if (t == end + 430) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var lay = com.mmorpg.plains.PlainsLayout.get(0);
            int gh = lay.height(lay.exitX, lay.exitZ);
            pl.teleportTo(lv, lay.x0() + lay.exitX + 0.5, gh + 1, com.mmorpg.plains.PlainsLayout.Z0 + lay.exitZ - 1.2, java.util.Set.of(), 0F, 0F, true);
        });
        if (t == end + 520) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] PLAINES portail cache : plaine = {} (attendu 1), deblocage = {} (attendu 2)", com.mmorpg.plains.PlainsManager.regionOf(pl),
                    com.mmorpg.server.RpgPlayers.get(pl).plainsUnlocked);
            com.mmorpg.plains.PlainsManager.open(pl);
        });
        if (t == end + 560) shot(mc, "121_plaines_menu_debloque");
        if (t == end + 565) mc.execute(() -> mc.gui.setScreen(null));
        // arche de retour vers le Spawn
        if (t == end + 600) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = srv.getLevel(com.mmorpg.dungeon.InstanceManager.DIM);
            var lay = com.mmorpg.plains.PlainsLayout.get(1);
            pl.teleportTo(lv, lay.x0() + com.mmorpg.plains.PlainsLayout.PLAZA_X + 0.5, com.mmorpg.plains.PlainsLayout.BASE + 1, com.mmorpg.plains.PlainsLayout.Z0 + com.mmorpg.plains.PlainsBuilder.ARCH_Z + 1.5,
                    java.util.Set.of(), 180F, 0F, true);
        });
        if (t == end + 650) {
            srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] PLAINES arche de retour : monde = {} (attendu mmorpg:spawn)", srv.getPlayerList().getPlayerByName(me).level().dimension().identifier()));
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static int marcheT0 = -1;
    private static int hub2Step;

    /** Hub (Grande Nef) et Spawn (Marche) dans deux dimensions : reconstruction, visite, portails et seconde copie du hub. */
    private static void marchePhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 10) {
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (marcheT0 < 0) {
            if (tick == 40) {
                srv.execute(() -> {
                    var hub = com.mmorpg.hub.HubData.get(srv);
                    var lv = hub.level(srv);
                    if (lv != null && hub.origin() != null) com.mmorpg.hub.HubBuilder.build(lv, hub.origin(), 0);       // reconstruction forcee : verifie le plan courant
                    var sl = srv.getLevel(com.mmorpg.hub.SpawnBuilder.DIM);
                    if (sl != null) com.mmorpg.hub.SpawnBuilder.build(sl, com.mmorpg.hub.HubManager.HUB_ORIGIN);
                });
            }
            if (tick > 40 && tick % 20 == 0) {
                var hub = com.mmorpg.hub.HubData.get(srv);
                var spawn = com.mmorpg.hub.SpawnData.get(srv);
                MMORPG.LOGGER.info("[MMORPG] MARCHE plan du hub = {} (attendu {}), plan du spawn = {} (attendu {})", hub.layout(), com.mmorpg.hub.HubBuilder.LAYOUT_VERSION,
                        spawn.layout(), com.mmorpg.hub.SpawnBuilder.LAYOUT_VERSION);
                if (hub.layout() >= com.mmorpg.hub.HubBuilder.LAYOUT_VERSION && spawn.layout() >= com.mmorpg.hub.SpawnBuilder.LAYOUT_VERSION) marcheT0 = tick + 40;
            }
            if (tick > 5000) {
                MMORPG.LOGGER.info("[MMORPG] MARCHE : construction jamais terminee");
                stop(mc);
            }
            return;
        }
        int t = tick - marcheT0;
        int cx = com.mmorpg.hub.HubMarket.CX;
        // {x, y, z, yaw, pitch, monde : 0 hub, 1 spawn}
        double[][] views = {
                {0, 6, 42, 180, 8, 0}, {0, 38, 0, 0, 90, 0}, {-30, 4, 0, 90, 8, 0}, {-44, 5, 0, 90, 8, 0},
                {cx + 44, 6, 0, 90, 8, 1}, {cx + 14, 64, 0, 90, 58, 1}, {cx + 10, 5, 0, 180, 8, 1}, {cx + 21, 1.6, -44.5, 0, 6, 1},
                {cx, 175, 150, 180, 42, 1}, {cx, 8, -50, 180, 6, 1}, {cx, 8, -72, 180, 10, 1}, {cx, 10, 62, 0, 10, 1}, {cx + 8, 12, 96, 270, 20, 1},
                {cx - 58, 10, -30, 90, 14, 1}, {cx + 92, 6, 0, 180, 8, 1}, {cx + 92, 6, 0, 0, 8, 1}, {cx + 96, 8, 0, 270, 6, 1}, {cx + 64, 6, 44, 200, 8, 1}, {cx - 24, 6, -20, 270, 6, 1}};
        String[] names = {"hub_entree", "hub_dessus", "hub_pont", "hub_portail", "spawn_portail_citadelle", "spawn_vue_haute", "spawn_banque", "spawn_taverne",
                "cite_vue_aerienne", "cite_avenue_chateau", "cite_cour_chateau", "cite_port", "cite_phare", "cite_moulin", "cite_halle_guildes", "cite_auberge", "cite_porte_est", "cite_rue", "cite_portail_plaines"};
        int idx = t / 60;
        if (idx < views.length && t % 60 == 5) {
            double[] v = views[idx];
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var hub = com.mmorpg.hub.HubData.get(srv);
                boolean spawn = v[5] > 0;
                var lv = spawn ? srv.getLevel(com.mmorpg.hub.SpawnBuilder.DIM) : hub.level(srv);
                var o = spawn ? com.mmorpg.hub.SpawnData.get(srv).origin() : hub.origin();
                pl.teleportTo(lv, o.getX() + v[0] + 0.5, o.getY() + v[1], o.getZ() + v[2] + 0.5, java.util.Set.of(), (float) v[3], (float) v[4], true);
            });
        }
        if (idx < views.length && t % 60 == 50) shot(mc, "110_" + names[idx]);
        int end = views.length * 60;
        // portails : on entre dans le portail du hub (attendu : le spawn), puis dans celui du spawn (attendu : le hub)
        if (t == end + 5) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var hub = com.mmorpg.hub.HubData.get(srv);
            var o = hub.origin();
            pl.teleportTo(hub.level(srv), o.getX() + com.mmorpg.hub.HubPortals.HUB_GATE_X + 1.5, o.getY() + 1, o.getZ() + 0.5, java.util.Set.of(), 90F, 0F, true);
        });
        if (t == end + 45) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] MARCHE portail du hub : monde = {} (attendu mmorpg:spawn)",
                srv.getPlayerList().getPlayerByName(me).level().dimension().identifier()));
        if (t == end + 50) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var o = com.mmorpg.hub.SpawnData.get(srv).origin();
            pl.teleportTo(srv.getLevel(com.mmorpg.hub.SpawnBuilder.DIM), o.getX() + cx + 38.5, o.getY() + 1, o.getZ() + 0.5, java.util.Set.of(), 90F, 0F, true);
        });
        if (t == end + 100) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var hub = com.mmorpg.hub.HubData.get(srv);
            MMORPG.LOGGER.info("[MMORPG] MARCHE portail du spawn : monde = {} (attendu mmorpg:hub), x relatif = {}", pl.level().dimension().identifier(), pl.getX() - hub.origin().getX());
        });
        // seconde copie du hub (Hub 2) : creation, arrivee et PNJ propres a chaque copie (la construction bloque le serveur : le reste se fait dans la meme tache)
        if (t == end + 110) srv.execute(() -> {
            var hub = com.mmorpg.hub.HubData.get(srv);
            int i = hub.addHub();
            com.mmorpg.hub.HubBuilder.build(hub.level(srv), hub.origin(i), i);
            var pl = srv.getPlayerList().getPlayerByName(me);
            boolean ok = com.mmorpg.hub.HubManager.goToHub(pl, 1);
            int npcs = pl.level().getEntities((net.minecraft.world.entity.Entity) null, com.mmorpg.hub.HubLayout.hubBounds(hub.origin(1)), x -> x instanceof com.mmorpg.entity.NpcEntity).size();
            int npcs1 = pl.level().getEntities((net.minecraft.world.entity.Entity) null, com.mmorpg.hub.HubLayout.hubBounds(hub.origin(0)), x -> x instanceof com.mmorpg.entity.NpcEntity).size();
            MMORPG.LOGGER.info("[MMORPG] MARCHE Hub 2 : arrivee = {}, x = {} (attendu ~320), copies = {}, PNJ du Hub 2 = {}, PNJ du Hub 1 = {}", ok, (int) pl.getX() - hub.origin().getX(), hub.count(), npcs, npcs1);
            hub2Step = tick;
        });
        if (hub2Step > 0 && tick == hub2Step + 60) shot(mc, "110_hub2_arrivee");
        if (hub2Step > 0 && tick == hub2Step + 80) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
        if (t > end + 6000) stop(mc);
    }

    /** Banque et hotel des ventes : vue des modeles 3D puis actions reelles (messages serveur). */
    private static void servicesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 20) {
            cmd(mc, "difficulty peaceful");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "clear " + me);
            cmd(mc, "fill ~-40 180 ~-40 ~40 180 ~40 minecraft:smooth_stone");
            cmd(mc, "fill ~-40 181 ~-40 ~40 195 ~40 minecraft:air");
            cmd(mc, "tp " + me + " ~ 181 ~ 180 0");
        }
        if (tick == 28) cmd(mc, "kill @e[type=!minecraft:player,distance=..80]");
        if (tick == 30) {
            structOrigin = mc.player.blockPosition();
            cmd(mc, "setblock ~ ~ ~-16 mmorpg:banque_aeloria[facing=south]");
            cmd(mc, "setblock ~ ~ ~16 mmorpg:hdv_aeloria[facing=north]");
        }
        if (structOrigin == null) return;
        net.minecraft.core.BlockPos bank = structOrigin.offset(0, 0, -16);
        net.minecraft.core.BlockPos hdv = structOrigin.offset(0, 0, 16);
        if (tick == 45) {
            look(mc, me, 0, 5, -4, 0, -16, 14);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick == 80) shot(mc, "100_banque_3d");
        if (tick == 85) look(mc, me, 0, 4, 4, 0, 16, 14);
        if (tick == 120) shot(mc, "101_hdv_3d");
        // banque : depot de 1000 or, interface, retrait de 400, coffre
        if (tick == 130) {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            look(mc, me, 0, 1, -8, 0, -16, 5);
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                com.mmorpg.rpg.PlayerData d = com.mmorpg.server.RpgPlayers.get(pl);
                d.gold = 5000;
                d.dirty = true;
            });
        }
        if (tick == 140) ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.BANK, com.mmorpg.economy.Bank.ACT_DEPOSIT, 1000, "", 0, bank));
        if (tick == 150) ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.BANK, com.mmorpg.economy.Bank.ACT_WITHDRAW, 400, "", 0, bank));
        if (tick == 160) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                long wallet = com.mmorpg.server.RpgPlayers.get(pl).gold;
                long inBank = com.mmorpg.economy.BankData.get(srv).account(pl.getUUID()).gold;
                MMORPG.LOGGER.info("[MMORPG] SERVICES banque : porte-monnaie {} (attendu 4400), en banque {} (attendu 600)", wallet, inBank);
                com.mmorpg.economy.Bank.open(pl, bank);
            });
        }
        if (tick == 175) shot(mc, "102_banque_ecran");
        if (tick == 180) mc.gui.setScreen(null);
        if (tick == 185) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                pl.getInventory().setItem(5, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("lingot_neantite"), 12));
                com.mmorpg.economy.Bank.action(pl, bank, com.mmorpg.economy.Bank.ACT_OPEN_PAGE, 0, 0);
            });
        }
        if (tick == 205) shot(mc, "103_banque_coffre");
        if (tick == 210) mc.gui.setScreen(null);
        // hotel des ventes : mise en vente, annonce d'un autre vendeur, achat, annulation, comptoir
        if (tick == 215) {
            look(mc, me, 0, 1, 8, 0, 16, 5);
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("pioche_neantite")));
                var data = com.mmorpg.economy.AuctionData.get(srv);
                java.util.UUID other = new java.util.UUID(1, 2);
                data.add(new com.mmorpg.economy.AuctionData.Listing(data.nextId(), other, "Aldric", new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("lingot_astralium"), 3), 900, System.currentTimeMillis() + 36L * 3600_000L));
                data.add(new com.mmorpg.economy.AuctionData.Listing(data.nextId(), other, "Aldric", new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("compresse_glacium"), 2), 450, System.currentTimeMillis() + 5L * 3600_000L));
                data.add(new com.mmorpg.economy.AuctionData.Listing(data.nextId(), other, "Mira", new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("nectar_neant"), 4), 700, System.currentTimeMillis() + 47L * 3600_000L));
            });
        }
        if (tick == 225) ClientNet.send(new Payloads.ServiceAction(Payloads.ServiceAction.AUCTION, com.mmorpg.economy.Auction.ACT_SELL, 2500, "", 0, hdv));
        if (tick == 235) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var data = com.mmorpg.economy.AuctionData.get(srv);
                long mine = data.listings().stream().filter(l -> l.seller().equals(pl.getUUID())).count();
                MMORPG.LOGGER.info("[MMORPG] SERVICES hdv : annonces du joueur {} (attendu 1), main vide = {} (attendu true), total annonces {} (attendu 4)",
                        mine, pl.getMainHandItem().isEmpty(), data.listings().size());
                com.mmorpg.economy.Auction.open(pl, hdv, "", 0);
            });
        }
        if (tick == 255) shot(mc, "104_hdv_ecran");
        if (tick == 260) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var data = com.mmorpg.economy.AuctionData.get(srv);
                long target = data.listings().stream().filter(l -> l.sellerName().equals("Mira")).findFirst().get().id();
                long before = com.mmorpg.server.RpgPlayers.get(pl).gold;
                com.mmorpg.economy.Auction.action(pl, hdv, com.mmorpg.economy.Auction.ACT_BUY, target, "", 0);
                long after = com.mmorpg.server.RpgPlayers.get(pl).gold;
                MMORPG.LOGGER.info("[MMORPG] SERVICES hdv achat : or {} -> {} (attendu -700), nectars en poche {} (attendu 4), gains en attente du vendeur {} (attendu 665)",
                        before, after, pl.getInventory().countItem(com.mmorpg.registry.ModItems.get("nectar_neant")), data.earnings(new java.util.UUID(1, 2)));
                long mineId = data.listings().stream().filter(l -> l.seller().equals(pl.getUUID())).findFirst().get().id();
                com.mmorpg.economy.Auction.action(pl, hdv, com.mmorpg.economy.Auction.ACT_CANCEL, mineId, "", 0);
                MMORPG.LOGGER.info("[MMORPG] SERVICES hdv annulation : pioche de retour = {} (attendu 1)", pl.getInventory().countItem(com.mmorpg.registry.ModItems.get("pioche_neantite")));
                data.addEarnings(pl.getUUID(), 300);
                long g0 = com.mmorpg.server.RpgPlayers.get(pl).gold;
                com.mmorpg.economy.Auction.action(pl, hdv, com.mmorpg.economy.Auction.ACT_CLAIM, 0, "", 0);
                MMORPG.LOGGER.info("[MMORPG] SERVICES hdv comptoir : or {} -> {} (attendu +300)", g0, com.mmorpg.server.RpgPlayers.get(pl).gold);
                // refus : acheter sa propre annonce, mettre en vente a prix nul, sans objet en main
                pl.getInventory().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                int before2 = data.listings().size();
                com.mmorpg.economy.Auction.action(pl, hdv, com.mmorpg.economy.Auction.ACT_SELL, 100, "", 0);
                MMORPG.LOGGER.info("[MMORPG] SERVICES hdv vente sans objet : annonces {} -> {} (attendu inchange)", before2, data.listings().size());
                com.mmorpg.economy.Auction.open(pl, hdv, "lingot", 0);
            });
        }
        if (tick == 285) shot(mc, "105_hdv_ecran_apres");
        if (tick == 300) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    /** Cultures : une rangee par plante (stades 0, 3, 5 et 7), recolte a la houe de Neantite, refus de semis sous le niveau requis. */
    private static void culturesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        String[] ids = {"lueur", "argent", "ambre", "givre", "neant"};
        int[] stages = {0, 3, 5, 7};
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 20) {
            cmd(mc, "difficulty peaceful");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "clear " + me);
            cmd(mc, "fill ~-30 180 ~-30 ~30 180 ~30 minecraft:smooth_stone");
            cmd(mc, "fill ~-30 181 ~-30 ~30 195 ~30 minecraft:air");
            cmd(mc, "tp " + me + " ~ 181 ~ 180 0");
        }
        if (tick == 28) cmd(mc, "kill @e[type=!minecraft:player,distance=..80]");
        if (tick == 30) {
            structOrigin = mc.player.blockPosition();
            for (int r = 0; r < ids.length; r++) {
                cmd(mc, "fill ~-2 ~-1 ~" + (r * 2 - 4) + " ~2 ~-1 ~" + (r * 2 - 4) + " minecraft:farmland[moisture=7]");
                for (int c = 0; c < stages.length; c++) {
                    cmd(mc, "setblock ~" + (c - 2) + " ~ ~" + (r * 2 - 4) + " mmorpg:culture_" + ids[r] + "[age=" + stages[c] + "]");
                }
            }
        }
        if (structOrigin == null) return;
        if (tick == 45) {
            look(mc, me, 6, 4, 2, 0, 0, 40);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick == 75) shot(mc, "90_cultures_stades");
        if (tick == 80) look(mc, me, 1, 2.2, -1, 0.5, 2, 50);
        if (tick == 110) shot(mc, "91_cultures_gros_plan");
        if (tick == 120) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lv = pl.level();
                pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("houe_neantite")));
                pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                int[] tot = new int[2];
                for (int i = 0; i < 40; i++) {
                    var pos = structOrigin.offset(1, 0, -4);
                    lv.setBlock(pos, com.mmorpg.farm.FarmCrops.BLOCKS.get("lueur").get().defaultBlockState().setValue(com.mmorpg.farm.AeloriaCropBlock.AGE, 7), 3);
                    var st = lv.getBlockState(pos);
                    st.getBlock().playerDestroy(lv, pl, pos, st, null, pl.getMainHandItem());
                }
                for (var e : lv.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pl.getBoundingBox().inflate(30))) {
                    if (e.getItem().is(com.mmorpg.registry.ModItems.get("racine_lueur"))) tot[0] += e.getItem().getCount();
                    if (e.getItem().is(com.mmorpg.registry.ModItems.get("graine_lueur"))) tot[1] += e.getItem().getCount();
                }
                MMORPG.LOGGER.info("[MMORPG] CULTURES 40 recoltes a la houe de Neantite : {} racines de Lueur (attendu entre 40 et 80, ~52), {} graines", tot[0], tot[1]);
            });
        }
        if (tick == 150) {
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                com.mmorpg.rpg.PlayerData d = com.mmorpg.server.RpgPlayers.get(pl);
                int old = d.level;
                d.level = 1;
                pl.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("graine_neant"), 3));
                var item = (com.mmorpg.farm.SeedItem) com.mmorpg.registry.ModItems.get("graine_neant");
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(structOrigin.offset(0, -1, 7)), net.minecraft.core.Direction.UP, structOrigin.offset(0, -1, 7), false);
                var lv = pl.level();
                lv.setBlock(structOrigin.offset(0, -1, 7), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 3);
                var res = item.useOn(new net.minecraft.world.item.context.UseOnContext(lv, pl, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item), hit));
                MMORPG.LOGGER.info("[MMORPG] CULTURES semis niveau 1 de l'Orchidee du Neant : {} (attendu FAIL)", res);
                d.level = old;
            });
        }
        if (tick == 180) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    /** Presse de compression en action, ecran de compression, onglet Outils de la forge, pioche en main, tirages de lingots rares. */
    private static void metallurgiePhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) mc.getWindow().setWindowed(1280, 720);
        if (tick == 20) {
            cmd(mc, "difficulty peaceful");
            cmd(mc, "time set day");
            cmd(mc, "weather clear");
            cmd(mc, "gamemode creative " + me);
            cmd(mc, "mmorpg niveau " + me + " 100");
            cmd(mc, "clear " + me);
            cmd(mc, "fill ~-30 180 ~-30 ~30 180 ~30 minecraft:smooth_stone");
            cmd(mc, "fill ~-30 181 ~-30 ~30 195 ~30 minecraft:air");
            cmd(mc, "tp " + me + " ~ 181 ~ 180 0");
        }
        if (tick == 28) cmd(mc, "kill @e[type=!minecraft:player,distance=..80]");
        if (tick == 30) {
            structOrigin = mc.player.blockPosition();
            cmd(mc, "setblock ~ ~ ~-12 mmorpg:presse_compression[facing=south]");
        }
        if (structOrigin == null) return;
        if (tick == 45) {
            look(mc, me, 0, 8, 16, 0, -12, 18);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        }
        if (tick == 75) shot(mc, "80_presse_arret");
        if (tick == 90) look(mc, me, -7, 2.5, 0.5, 0, -12, 14);
        if (tick == 130) shot(mc, "81_presse_active_3q");
        if (tick == 140) look(mc, me, 0, 3, -4, 0, -12, 18);
        if (tick == 170) shot(mc, "82_presse_active_face");
        if (tick == 180) {
            cmd(mc, "give " + me + " mmorpg:lingot_verdite 40");
            cmd(mc, "give " + me + " mmorpg:piece_or 200");
        }
        net.minecraft.core.BlockPos press = structOrigin.offset(0, 0, -12);
        if (tick == 195) look(mc, me, 0, 1, -5, 0, -12, 10);
        if (tick == 200) {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            mc.gui.setScreen(new com.mmorpg.client.screen.CompressorScreen(press));
        }
        if (tick == 215) shot(mc, "83_presse_ecran");
        if (tick == 220) ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.COMPRESS, "verdite", 5, press));
        if (tick == 235) {
            int n = mc.player.getInventory().countItem(com.mmorpg.registry.ModItems.get("compresse_verdite"));
            int left = mc.player.getInventory().countItem(com.mmorpg.registry.ModItems.get("lingot_verdite"));
            MMORPG.LOGGER.info("[MMORPG] METALLURGIE compression : {} minerai(s) compresse(s) (attendu 2), {} lingot(s) restants (attendu 8)", n, left);
            shot(mc, "84_presse_apres");
        }
        if (tick == 245) {
            try {
                var f = ForgeScreen.class.getDeclaredField("tab");
                f.setAccessible(true);
                f.setInt(null, com.mmorpg.crafting.ForgeRecipes.Category.OUTILS.ordinal());
            } catch (ReflectiveOperationException e) {
                MMORPG.LOGGER.warn("[MMORPG] vitrine : onglet outils introuvable", e);
            }
            mc.gui.setScreen(new ForgeScreen(press));
        }
        if (tick == 265) shot(mc, "85_forge_outils");
        if (tick == 270) {
            try {
                var f = ForgeScreen.class.getDeclaredField("tab");
                f.setAccessible(true);
                f.setInt(null, com.mmorpg.crafting.ForgeRecipes.Category.ARMURES.ordinal());
            } catch (ReflectiveOperationException e) {
                MMORPG.LOGGER.warn("[MMORPG] vitrine : onglet armures introuvable", e);
            }
            mc.gui.setScreen(new ForgeScreen(press));
        }
        if (tick == 290) shot(mc, "86_forge_armures");
        if (tick == 295) {
            mc.gui.setScreen(null);
            cmd(mc, "item replace entity " + me + " hotbar.0 with mmorpg:pioche_neantite");
            cmd(mc, "item replace entity " + me + " hotbar.1 with mmorpg:hache_glacium");
            cmd(mc, "item replace entity " + me + " hotbar.2 with mmorpg:pelle_ambrasite");
            cmd(mc, "item replace entity " + me + " hotbar.3 with mmorpg:houe_argentite");
            cmd(mc, "item replace entity " + me + " hotbar.4 with mmorpg:pioche_debutant");
        }
        if (tick == 310) shot(mc, "87_pioche_neantite_en_main");
        if (tick == 315) {
            // tirages : 1500 minerais de Neantite a la pioche de Neantite, puis a la pioche du debutant (refus attendu)
            srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lv = pl.level();
                var pos = pl.blockPosition().above(3);
                var ore = com.mmorpg.mine.MineBlocks.ORES.get(4);
                int[] total = new int[3];
                int ingots = 0, mined = 0;
                for (int i = 0; i < 1500; i++) {
                    if (i % 40 == 0) {
                        for (String id : new String[]{"lingot_aelium", "lingot_orichalque", "lingot_astralium"}) total[java.util.Arrays.asList("lingot_aelium", "lingot_orichalque", "lingot_astralium").indexOf(id)] += pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(id));
                        ingots += pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(ore.ingotId()));
                        pl.getInventory().clearContent();
                        pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("pioche_neantite")));
                    }
                    if (com.mmorpg.mine.MineManager.mine(lv, pl, pos, ore)) mined++;
                }
                for (String id : new String[]{"lingot_aelium", "lingot_orichalque", "lingot_astralium"}) total[java.util.Arrays.asList("lingot_aelium", "lingot_orichalque", "lingot_astralium").indexOf(id)] += pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(id));
                ingots += pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(ore.ingotId()));
                MMORPG.LOGGER.info("[MMORPG] METALLURGIE tirages (pioche de Neantite, minerai de Neantite) : {} extraits, {} lingots de base, aelium {}, orichalque {}, astralium {}", mined, ingots, total[0], total[1], total[2]);
                pl.getInventory().clearContent();
                pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("pioche_debutant")));
                boolean refused = !com.mmorpg.mine.MineManager.mine(lv, pl, pos, ore);
                MMORPG.LOGGER.info("[MMORPG] METALLURGIE pioche du debutant sur la Neantite : refus = {} (attendu true)", refused);
                var ore0 = com.mmorpg.mine.MineBlocks.ORES.get(0);
                boolean ok0 = com.mmorpg.mine.MineManager.mine(lv, pl, pos, ore0);
                MMORPG.LOGGER.info("[MMORPG] METALLURGIE pioche du debutant sur la Verdite : extrait = {} (attendu true)", ok0);
            });
        }
        if (tick == 325) srv.execute(() -> {
            // bonus de pioche : veine d'un coup (accroupi) a la pioche de Neantite, puis a une pioche d'Argentite (aucun bonus)
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = pl.level();
            var base = pl.blockPosition().above(8);
            var ore = com.mmorpg.mine.MineBlocks.ORES.get(0);
            for (int pass = 0; pass < 2; pass++) {
                var origin = base.offset(pass * 8, 0, 0);
                for (int x = 0; x < 5; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 3; z++) lv.setBlock(origin.offset(x, y, z), com.mmorpg.mine.MineBlocks.block(ore).defaultBlockState(), 3);
                pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get(pass == 0 ? "pioche_neantite" : "pioche_argentite")));
                pl.setShiftKeyDown(true);
                com.mmorpg.mine.MineManager.mineAndVein(lv, pl, origin, ore);
                int depleted = 0;
                for (int x = 0; x < 5; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 3; z++) if (lv.getBlockState(origin.offset(x, y, z)).is(com.mmorpg.mine.MineBlocks.EPUISE.get())) depleted++;
                MMORPG.LOGGER.info("[MMORPG] OUTILS veine ({}) : {} minerais epuises sur 30 (attendu {})", pass == 0 ? "Neantite" : "Argentite", depleted, pass == 0 ? 15 : 1);
            }
            // bonus de houe : recolte en zone et replantation (Neantite : rayon 2)
            var field = base.offset(0, 4, 0);
            var crop = com.mmorpg.farm.FarmCrops.BLOCKS.get("lueur").get();
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                lv.setBlock(field.offset(x, -1, z), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 3);
                lv.setBlock(field.offset(x, 0, z), crop.defaultBlockState().setValue(com.mmorpg.farm.AeloriaCropBlock.AGE, 7), 3);
            }
            pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("houe_neantite")));
            var st = lv.getBlockState(field);
            lv.removeBlock(field, false);
            st.getBlock().playerDestroy(lv, pl, field, st, null, pl.getMainHandItem());
            int replanted = 0, mature = 0;
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                var s2 = lv.getBlockState(field.offset(x, 0, z));
                if (s2.getBlock() == crop) { if (s2.getValue(com.mmorpg.farm.AeloriaCropBlock.AGE) == 0) replanted++; else mature++; }
            }
            MMORPG.LOGGER.info("[MMORPG] OUTILS houe de Neantite : {} plantes replantees (attendu 25 : la cassee + rayon 2), {} encore mures (attendu 24)", replanted, mature);
            pl.setShiftKeyDown(false);
        });
        if (tick == 340) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

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

    private static int mineT = -1;
    private static boolean mineIntro;
    private static long mineBuildStart;

    private static void tpMine(net.minecraft.server.MinecraftServer srv, String me, int region, int lx, int lz, float yaw) {
        var pl = srv.getPlayerList().getPlayerByName(me);
        var lv = com.mmorpg.mine.MineManager.dim(srv);
        var lay = com.mmorpg.mine.MineLayout.get(region);
        double x = lay.x0() + lx + 0.5, z = com.mmorpg.mine.MineLayout.Z0 + lz + 0.5;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) lv.getChunk(((int) x >> 4) + dx, ((int) z >> 4) + dz);
        double y = lay.floor(lx, lz) + 1.2;
        pl.teleportTo(lv, x, y, z, java.util.Set.of(), yaw, 8F, true);
        pl.resetFallDistance();
    }

    private static void minePhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode survival");
            cmd(mc, "difficulty normal");
            mineT = -1;
            mineIntro = false;
            mineBuildStart = System.nanoTime();
        }
        if (tick > 20 && mineT < 0 && tick % 40 == 0) srv.execute(() -> {
            if (com.mmorpg.mine.MineManager.allBuilt(srv) && !com.mmorpg.mine.MineManager.building()) {
                mineT = 0;
                MMORPG.LOGGER.info("[MMORPG] MINES construites en {} s", (System.nanoTime() - mineBuildStart) / 1_000_000_000L);
            } else if (tick % 400 == 0) {
                MMORPG.LOGGER.info("[MMORPG] MINES construction : {} %", com.mmorpg.mine.MineManager.percent());
            }
        });
        if (mineT < 0) {
            if (tick > 24000) {
                MMORPG.LOGGER.info("[MMORPG] MINES abandon : construction trop longue");
                stop(mc);
            }
            return;
        }
        mineT++;
        if (!mineIntro) {                                  // menu des mines puis ecran de chargement et arrivee a l'etage 3 (comme par le Maitre des mines)
            int u = mineT;
            if (u == 2) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                com.mmorpg.server.RpgPlayers.get(pl).level = 100;
                pl.getAbilities().invulnerable = true;
                pl.onUpdateAbilities();
                MMORPG.LOGGER.info("[MMORPG] MINES fuites de la caverne : {}", com.mmorpg.mine.MineLayout.get(0).leaks());
                com.mmorpg.mine.MineManager.open(pl, null);
            });
            if (u == 40) shot(mc, "mine_menu");
            if (u == 45) {
                com.mmorpg.client.ClientNet.send(new com.mmorpg.network.Payloads.MineEnter(-1, 0));
                mc.gui.setScreen(null);
            }
            if (u == 70) shot(mc, "mine_chargement");
            if (u == 100) shot(mc, "mine_chargement2");
            if (u == 190) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                MMORPG.LOGGER.info("[MMORPG] MINES arrivee par le menu : dimension {} ; position ({}, {}, {}) ; chargement encore affiche = {}", pl.level().dimension().identifier(),
                        (int) pl.getX(), (int) pl.getY(), (int) pl.getZ(), com.mmorpg.client.LoadingClient.active());
            });
            if (u == 200) shot(mc, "mine_arrivee");
            if (u == 215) {
                mineIntro = true;
                mineT = 0;
            }
            return;
        }
        int t = mineT;
        if (t == 5) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var d = com.mmorpg.server.RpgPlayers.get(pl);
            d.level = 100;
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
            pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            pl.getInventory().setSelectedSlot(0);
            MMORPG.LOGGER.info("[MMORPG] MINES entree : {}", com.mmorpg.mine.MineManager.enterNow(pl, 0));
        });
        if (t == 60) shot(mc, "mine_place");
        // un etage toutes les 140 ticks
        for (int k = 0; k < 5; k++) {
            int base = 80 + k * 140;
            final int sk = k;
            if (t == base) srv.execute(() -> {
                var lay = com.mmorpg.mine.MineLayout.get(sk);
                int lx = lay.exitX + (sk % 2 == 0 ? -30 : 30), lz = lay.exitZ - 30;
                if (lay.kind(lx, lz) != 1) {
                    lx = lay.exitX;
                    lz = lay.exitZ - 14;
                }
                tpMine(srv, me, sk, lx, lz, 0F);
            });
            if (t == base + 100) srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                var lv = pl.level();
                var mobs = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, pl.getBoundingBox().inflate(45, 30, 45), m -> m instanceof com.mmorpg.entity.RpgMob);
                StringBuilder sb = new StringBuilder();
                for (var m : mobs) {
                    var md = m.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);
                    sb.append(md == null ? "?" : md.key + " niv " + md.level).append("; ");
                }
                int ores = 0;
                var bp = pl.blockPosition();
                for (int dx = -30; dx <= 30; dx++) for (int dz = -30; dz <= 30; dz++) for (int dy = -6; dy <= 8; dy++)
                    if (lv.getBlockState(bp.offset(dx, dy, dz)).getBlock() instanceof com.mmorpg.mine.MineOreBlock) ores++;
                MMORPG.LOGGER.info("[MMORPG] MINES mine {} : {} monstres [{}] ; {} minerais autour ; zone = {}", sk, mobs.size(), sb, ores,
                        com.mmorpg.zone.ZoneManager.zoneAt(lv, bp) == null ? "aucune" : com.mmorpg.zone.ZoneManager.zoneAt(lv, bp).name());
            });
            if (t == base + 104) shot(mc, "mine_etage" + sk);
        }
        int mt = 80 + 5 * 140 + 20;
        if (t == mt) srv.execute(() -> tpMine(srv, me, 0, 160, 100, 0F));
        if (t == mt + 20) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lv = pl.level();
            var bp = pl.blockPosition();
            net.minecraft.core.BlockPos found = null;
            for (int r = 1; r <= 40 && found == null; r++)
                for (int dx = -r; dx <= r && found == null; dx++) for (int dz = -r; dz <= r && found == null; dz++) for (int dy = -8; dy <= 10 && found == null; dy++)
                    if (lv.getBlockState(bp.offset(dx, dy, dz)).getBlock() instanceof com.mmorpg.mine.MineOreBlock) found = bp.offset(dx, dy, dz);
            if (found == null) {
                MMORPG.LOGGER.info("[MMORPG] MINES minage : aucun minerai trouve");
                return;
            }
            var ore = ((com.mmorpg.mine.MineOreBlock) lv.getBlockState(found).getBlock()).ore();
            pl.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("pioche_" + com.mmorpg.mine.MineTools.TIER_ID[Math.min(5, ore.tier() + 1)])));
            int before = pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(ore.ingotId()));
            boolean ok = com.mmorpg.mine.MineManager.mine(lv, pl, found, ore);
            int after = pl.getInventory().countItem(com.mmorpg.registry.ModItems.get(ore.ingotId()));
            MMORPG.LOGGER.info("[MMORPG] MINES minage {} : extrait = {} ; lingots {} -> {} ; bloc devenu {} ; repousses en attente = {}", ore.name(), ok, before, after,
                    lv.getBlockState(found).getBlock().getDescriptionId(), com.mmorpg.mine.MineData.get(srv).respawns().size());
            minePos = found;
        });
        if (t == mt + 40) srv.execute(() -> {
            int n = com.mmorpg.mine.MineManager.respawnAllNow(srv);
            MMORPG.LOGGER.info("[MMORPG] MINES repousse forcee de {} minerai(s)", n);
        });
        if (t == mt + 80) srv.execute(() -> {
            var lv = com.mmorpg.mine.MineManager.dim(srv);
            MMORPG.LOGGER.info("[MMORPG] MINES apres repousse : bloc = {}", minePos == null ? "?" : lv.getBlockState(minePos).getBlock().getDescriptionId());
        });
        // portail cache de la mine 1 : decouverte, passage a la mine 2, deblocage ; retour au Spawn par l'arche de la place
        if (t == mt + 100) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            var lay = com.mmorpg.mine.MineLayout.get(0);
            MMORPG.LOGGER.info("[MMORPG] MINES deblocage avant le portail = {} (attendu 1)", com.mmorpg.server.RpgPlayers.get(pl).minesUnlocked);
            var lv = com.mmorpg.mine.MineManager.dim(srv);
            double x = lay.x0() + lay.exitX + 0.5, z = com.mmorpg.mine.MineLayout.Z0 + lay.exitZ - 1.2;
            pl.teleportTo(lv, x, lay.floor(lay.exitX, lay.exitZ) + 1, z, java.util.Set.of(), 0F, 0F, true);
        });
        if (t == mt + 230) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] MINES portail cache : mine = {} (attendu 1), deblocage = {} (attendu 2)", com.mmorpg.mine.MineManager.regionOf(pl), com.mmorpg.server.RpgPlayers.get(pl).minesUnlocked);
            var lay = com.mmorpg.mine.MineLayout.get(1);
            var lv = com.mmorpg.mine.MineManager.dim(srv);
            double x = lay.x0() + com.mmorpg.mine.MineLayout.PLAZA_X + 0.5, z = com.mmorpg.mine.MineLayout.Z0 + com.mmorpg.mine.MineLayout.PLAZA_Z - 9.4;     // devant le voile, comme un joueur qui s'en approche
            pl.teleportTo(lv, x, lay.floor(com.mmorpg.mine.MineLayout.PLAZA_X, com.mmorpg.mine.MineLayout.PLAZA_Z) + 1, z, java.util.Set.of(), 180F, 0F, true);
        });
        if (t == mt + 300) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] MINES arche de retour : monde = {} (attendu mmorpg:spawn)", srv.getPlayerList().getPlayerByName(me).level().dimension().identifier()));
        if (t == mt + 310) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static net.minecraft.core.BlockPos minePos;

    private static int[] bossTp;
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
    /**
     * Existe-t-il, dans un parcours de saut, un chemin de l'entree a la sortie qui ne demande que des enjambees d'un bloc (distance
     * horizontale <= 2,3 entre deux points d'appui, denivele <= 1) ? Tout appui au-dessus de la ligne de chute compte (rebords, piliers...).
     */
    private static boolean walkRouteExists(net.minecraft.server.MinecraftServer srv, com.mmorpg.dungeon.TrialData.Trial tg) {
        var level = srv.getLevel(tg.dim());
        var bx = tg.box();
        int x0 = bx.get(0), y0 = bx.get(1), z0 = bx.get(2), x1 = bx.get(3), z1 = bx.get(5);
        java.util.Set<Long> stand = new java.util.HashSet<>();
        java.util.Deque<int[]> queue = new java.util.ArrayDeque<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 3; z <= z1 + 3; z++) {
                for (int y = y0 - 1; y <= y0 + 8; y++) {
                    var pos = new net.minecraft.core.BlockPos(x, y, z);
                    if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
                    if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) continue;
                    if (!level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty()) continue;
                    stand.add(pos.asLong());
                    if (z < z0) {
                        queue.add(new int[]{x, y, z});
                        seen.add(pos.asLong());
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            if (c[2] > z1) return true;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5) continue;
                    for (int dy = -1; dy <= 1; dy++) {
                        long k = new net.minecraft.core.BlockPos(c[0] + dx, c[1] + dy, c[2] + dz).asLong();
                        if (stand.contains(k) && seen.add(k)) queue.add(new int[]{c[0] + dx, c[1] + dy, c[2] + dz});
                    }
                }
            }
        }
        return false;
    }

    private static void trialsPhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire " + id);
        if (tick == 330) {
            var all = new java.util.ArrayList<>(com.mmorpg.dungeon.TrialData.get(srv).all());
            MMORPG.LOGGER.info("[MMORPG] EPREUVES {} : {}", id, all.stream().map(t -> t.id().substring(t.id().lastIndexOf('_') + 1) + "=" + t.kind()).toList());
            for (var tg : all) MMORPG.LOGGER.info("[MMORPG] EPREUVE geometrie {} boite {} herse {} bloc {}", tg.id().substring(tg.id().lastIndexOf('_') + 1), tg.box(), tg.gate(), tg.gateBlock());
            for (var tg : all) {
                if (!tg.kind().equals("jump")) continue;
                srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] EPREUVE contournement {} : route a pied possible = {} (attendu false)",
                        tg.id().substring(tg.id().lastIndexOf('_') + 1), walkRouteExists(srv, tg)));
            }
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
                        // raccourci : sauter directement a l'arrivee ne valide rien
                        trialAt(10, () -> srv.execute(() -> hubTeleport(srv, me, t0.gate().get(0) + 4, t0.gate().get(1), t0.gate().get(2) + 1)));
                        trialAt(25, () -> srv.execute(() -> {
                            var p = srv.getPlayerList().getPlayerByName(me);
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE saut raccourci : valide = {} (attendu false) ; joueur renvoye au depart = {}", trialOf(srv, tid).done(),
                                    Math.abs(p.getZ() - (t0.pos().get(2) + 0.5)) < 3);
                        }));
                        // trajet complet : un point tous les 6 blocs le long du parcours
                        int zFirst = t0.box().get(2);
                        int zLast = t0.box().get(5);
                        for (int zz = zFirst + 1; zz <= zLast; zz += 6) {
                            int zc = zz;
                            trialAt(8, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), zc)));
                        }
                        trialAt(10, () -> srv.execute(() -> hubTeleport(srv, me, t0.gate().get(0) + 4, t0.gate().get(1), t0.gate().get(2) + 1)));
                        trialAt(25, () -> srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] EPREUVE saut reussi = {} (attendu true)", trialOf(srv, tid).done())));
                    }
                    case "ambush" -> {
                        trialAt(5, () -> srv.execute(() -> hubTeleport(srv, me, t0.pos().get(0), t0.pos().get(1), t0.box().get(2) + 3)));
                        trialAt(50, () -> srv.execute(() -> {
                            var lv = srv.getPlayerList().getPlayerByName(me).level();
                            int n = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(t0.pos().get(0) - 8, t0.pos().get(1) - 2, t0.box().get(2) - 4,
                                    t0.pos().get(0) + 8, t0.pos().get(1) + 8, t0.box().get(5) + 4), m -> m instanceof com.mmorpg.entity.RpgMob).size();
                            MMORPG.LOGGER.info("[MMORPG] EPREUVE embuscade : {} monstres apres l'arrivee", n);
                            for (var m : lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(t0.pos().get(0) - 8, t0.pos().get(1) - 2, t0.box().get(2) - 4,
                                    t0.pos().get(0) + 8, t0.pos().get(1) + 8, t0.box().get(5) + 4), m -> m instanceof com.mmorpg.entity.RpgMob)) m.discard();
                        }));
                        trialAt(60, () -> srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] EPREUVE embuscade repoussee = {}", trialOf(srv, tid).done())));
                    }
                    case "waves", "elite" -> {
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
                var center = dungeonBase.offset(0, 0, com.mmorpg.dungeon.Layout.ARENA.z());
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
                var center = dungeonBase.offset(0, 0, com.mmorpg.dungeon.Layout.ARENA.z());
                int adds = lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(center).inflate(26),
                        m -> m instanceof com.mmorpg.entity.RpgMob && !(m instanceof com.mmorpg.entity.boss.RpgBoss)).size();
                MMORPG.LOGGER.info("[MMORPG] BOSS phase 2 : {} renforts dans l'arene (solo : peu attendus)", adds);
            }));
            trialAt(2, () -> {
                com.mmorpg.client.AreaEntryClient.cancel();
                shot(mc, id + "_boss_phase");
            });
            // entree dans l'antre du boss : l'animation doit se declencher
            trialAt(20, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.GALLERY.z0() + 10)));
            trialAt(30, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.ARENA.z0() + 6)));
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
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
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
            trialAt(40, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.GALLERY.z0() + 8)));
            trialAt(30, () -> srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.ARENA.z0() + 2)));
            trialAt(8, () -> cmd(mc, "tp " + me + " ~ ~ ~ 0 6"));
            trialAt(35, () -> shot(mc, id + "_cine_antre"));
            trialAt(120, () -> srv.execute(() -> {
                var lv = srv.getPlayerList().getPlayerByName(me).level();
                var center = dungeonBase.offset(0, 0, com.mmorpg.dungeon.Layout.ARENA.z());
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
            trialAt(5, () -> srv.execute(() -> {                                                       // le Passeur de sortie est apparu : on lui parle
                var pl = srv.getPlayerList().getPlayerByName(me);
                var npcs = pl.level().getEntitiesOfClass(com.mmorpg.entity.NpcEntity.class, pl.getBoundingBox().inflate(60),
                        n -> n.role() == com.mmorpg.entity.NpcEntity.Role.SORTIE);
                MMORPG.LOGGER.info("[MMORPG] PASSEUR : {} present(s) apres la victoire ; joueur en {}", npcs.size(), pl.blockPosition());
                if (!npcs.isEmpty()) {
                    var n = npcs.get(0);
                    cmd(mc, "tp " + me + " " + n.getX() + " " + n.getY() + " " + (n.getZ() - 4) + " 0 0");
                    sortieNpc = n;
                }
            }));
            trialAt(10, () -> shot(mc, id + "_cine_passeur"));
            trialAt(5, () -> srv.execute(() -> {
                var pl = srv.getPlayerList().getPlayerByName(me);
                if (sortieNpc != null) com.mmorpg.dungeon.InstanceManager.leaveNow(pl, sortieNpc.group());
                MMORPG.LOGGER.info("[MMORPG] PASSEUR : joueur apres le clic en {} (avant : {})", pl.blockPosition(), sortieNpc == null ? "-" : sortieNpc.blockPosition());
            }));
            trialAt(20, () -> shot(mc, id + "_cine_sortie"));
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
    private static com.mmorpg.entity.NpcEntity sortieNpc;

    private static void setBossHp(float fraction) {
        if (bossRef == null) return;
        var d = bossRef.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);
        if (d == null) return;
        d.hp = d.maxHp * fraction;
        bossRef.syncData(com.mmorpg.registry.ModAttachments.MOB);
    }

    /** Musique des donjons : entree dans le donjon, dans l'antre du boss, puis sortie. */
    private static void musicPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire sanctuaire_glace");
        if (tick == 330) MMORPG.LOGGER.info("[MMORPG] MUSIQUE depart hors donjon");
        if (tick == 340) srv.execute(() -> hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + 8));
        if (tick == 520) srv.execute(() -> {
            MMORPG.LOGGER.info("[MMORPG] MUSIQUE dans le hall : piste active cote client = {}", com.mmorpg.client.DungeonMusicClient.debugState());
            hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.ARENA.z0() + 3);
        });
        if (tick == 700) srv.execute(() -> {
            MMORPG.LOGGER.info("[MMORPG] MUSIQUE dans l'antre : piste active cote client = {}", com.mmorpg.client.DungeonMusicClient.debugState());
            hubTeleport(srv, me, dungeonBase.getX(), dungeonBase.getY(), dungeonBase.getZ() + com.mmorpg.dungeon.Layout.ARENA.z0() - 25);
        });
        if (tick == 880) srv.execute(() -> {
            MMORPG.LOGGER.info("[MMORPG] MUSIQUE de retour dans la galerie : piste active cote client = {}", com.mmorpg.client.DungeonMusicClient.debugState());
            hubTeleport(srv, me, dungeonBase.getX() + 200, 150, dungeonBase.getZ() - 200);
        });
        if (tick == 1080) {
            MMORPG.LOGGER.info("[MMORPG] MUSIQUE hors donjon : piste active cote client = {}", com.mmorpg.client.DungeonMusicClient.debugState());
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    /** Clic droit reel sur un bloc, tel que le serveur le recoit : une fois pour chaque main (principale puis secondaire). */
    private static void realClick(net.minecraft.server.level.ServerPlayer p, net.minecraft.core.BlockPos pos) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p, hand, pos, hit));
        }
    }

    private static com.mmorpg.dungeon.TrialData.Trial nearTrial(net.minecraft.server.MinecraftServer srv, String kind) {
        for (var t : com.mmorpg.dungeon.TrialData.get(srv).all()) {
            if (t.kind().equals(kind) && Math.abs(t.pos().get(0) - 6000) < 40 && t.pos().get(2) > 5990) return t;
        }
        return null;
    }

    /** Enigmes a dalles avec de vrais clics (deux evenements par clic, comme en jeu) : sequence puis ordre. */
    private static void dallesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode survival");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 10) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
        });
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire catacombes");
        if (tick == 330) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var t = nearTrial(srv, "simon");
            MMORPG.LOGGER.info("[MMORPG] DALLES enigme de sequence trouvee = {}", t != null);
            hubTeleport(srv, me, t.pos().get(0), t.pos().get(1), t.pos().get(2) + 3);
            realClick(p, new net.minecraft.core.BlockPos(t.pos().get(0), t.pos().get(1), t.pos().get(2)));      // lutrin
        });
        if (tick == 330 + 250) srv.execute(() -> {
            var t = nearTrial(srv, "simon");
            MMORPG.LOGGER.info("[MMORPG] DALLES saisie ouverte = {} ; sequence {}", com.mmorpg.dungeon.TrialManager.debugInputReady(t.id()), com.mmorpg.dungeon.TrialManager.debugSequence(t.id()));
        });
        for (int i = 0; i < 8; i++) {
            int k = i;
            if (tick == 330 + 260 + k * 10) srv.execute(() -> {
                var p = srv.getPlayerList().getPlayerByName(me);
                var t = nearTrial(srv, "simon");
                var seq = com.mmorpg.dungeon.TrialManager.debugSequence(t.id());
                if (k < seq.size() && !t.done()) {
                    int pad = seq.get(k);
                    realClick(p, new net.minecraft.core.BlockPos(t.pads().get(pad * 3), t.pads().get(pad * 3 + 1), t.pads().get(pad * 3 + 2)));
                    MMORPG.LOGGER.info("[MMORPG] DALLES clic {} sur la dalle {} -> {}", k, pad, com.mmorpg.dungeon.TrialManager.debugSimon(t.id()));
                }
            });
        }
        if (tick == 330 + 260 + 100) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] DALLES sequence resolue avec de vrais clics = {} (attendu true)", nearTrial(srv, "simon").done()));
        if (tick == 330 + 380) srv.execute(() -> {
            var t = nearTrial(srv, "order");
            MMORPG.LOGGER.info("[MMORPG] DALLES enigme d'ordre trouvee = {} ; ordre {}", t != null, t.mobs());
            hubTeleport(srv, me, t.pos().get(0), t.pos().get(1), t.pos().get(2) + 3);
        });
        for (int i = 0; i < 4; i++) {
            int k = i;
            if (tick == 330 + 400 + k * 10) srv.execute(() -> {
                var p = srv.getPlayerList().getPlayerByName(me);
                var t = nearTrial(srv, "order");
                int pad = Integer.parseInt(t.mobs().get(k));
                realClick(p, new net.minecraft.core.BlockPos(t.pads().get(pad * 3), t.pads().get(pad * 3 + 1), t.pads().get(pad * 3 + 2)));
            });
        }
        if (tick == 330 + 460) srv.execute(() -> MMORPG.LOGGER.info("[MMORPG] DALLES ordre resolu avec de vrais clics = {} (attendu true)", nearTrial(srv, "order").done()));
        if (tick == 330 + 480) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    /** Ecran de mort personnalise : le joueur (guerrier niveau 20) meurt, l'ecran affiche l'experience perdue ; puis il renait. */
    private static void deathPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) cmd(mc, "gamemode survival");
        if (tick == 20) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var d = com.mmorpg.server.RpgPlayers.get(p);
            d.playerClass = com.mmorpg.rpg.PlayerClass.GUERRIER;
            d.level = 20;
            d.xp = 3000;
            d.dirty = true;
            MMORPG.LOGGER.info("[MMORPG] MORT avant : niveau {} ; xp {}", d.level, d.xp);
        });
        if (tick == 60) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            p.getAbilities().invulnerable = false;
            p.onUpdateAbilities();
            p.kill(p.level());
        });
        if (tick == 75) MMORPG.LOGGER.info("[MMORPG] MORT ecran affiche = {}", mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName());
        if (tick == 80) shot(mc, "mort_1");
        if (tick == 110) shot(mc, "mort_2");
        if (tick == 160) shot(mc, "mort_3");
        if (tick == 170) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var d = com.mmorpg.server.RpgPlayers.get(p);
            MMORPG.LOGGER.info("[MMORPG] MORT apres : niveau {} ; xp {} (attendu 5 % de l'XP du niveau retiree)", d.level, d.xp);
            MMORPG.LOGGER.info("[MMORPG] MORT conservation de l'equipement : keep_inventory = {}", srv.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY));
        });
        if (tick == 180) mc.execute(() -> {
            if (mc.player != null) mc.player.respawn();
            mc.gui.setScreen(null);
        });
        if (tick == 230) {
            MMORPG.LOGGER.info("[MMORPG] MORT ecran apres renaissance = {}", mc.gui.screen() == null ? "aucun" : mc.gui.screen().getClass().getSimpleName());
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static long exitKillTick;

    /** Retour automatique apres la mort du boss : entree dans un exemplaire, mort du boss, compte a rebours, retour et exemplaire remis a neuf. */
    private static void sortiePhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode survival");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 10) srv.execute(() -> {
            var pl = srv.getPlayerList().getPlayerByName(me);
            pl.getAbilities().invulnerable = true;
            pl.onUpdateAbilities();
        });
        if (tick == 30) srv.execute(() -> com.mmorpg.dungeon.InstanceManager.debugForgetAll(srv));
        if (tick == 40) cmd(mc, "mmorpg donjon catacombes");
        if (tick == 80) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            var gate = com.mmorpg.world.WaypointData.get(srv).all().stream().filter(w -> w.name.startsWith("Catacombes") && com.mmorpg.dungeon.InstanceManager.isGate(srv, w.id)).findFirst().orElse(null);
            MMORPG.LOGGER.info("[MMORPG] SORTIE depart ({}, {}, {}) ; entree = {}", (int) p.getX(), (int) p.getY(), (int) p.getZ(),
                    gate != null && com.mmorpg.dungeon.InstanceManager.enter(p, gate.id));
        });
        if (tick == 700) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] SORTIE dans l'instance : dimension {} ; position ({}, {}, {}) ; exemplaire {}", p.level().dimension().identifier(),
                    (int) p.getX(), (int) p.getY(), (int) p.getZ(), com.mmorpg.dungeon.InstanceManager.debugSlot(srv, "catacombes:0", 0));
        });
        if (tick == 710) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            String[] o = com.mmorpg.dungeon.InstanceManager.debugSlot(srv, "catacombes:0", 0).split(",");
            int ox = Integer.parseInt(o[0]), oy = Integer.parseInt(o[1]), oz = Integer.parseInt(o[2]);
            var lv = p.level();
            var altar = new net.minecraft.core.BlockPos(ox, oy, oz + com.mmorpg.dungeon.Layout.ARENA.z());
            hubTeleport(srv, me, ox, oy, oz + com.mmorpg.dungeon.Layout.ARENA.z() - 12);
            bossRef = com.mmorpg.entity.boss.BossManager.spawnBoss(lv, com.mmorpg.entity.boss.BossManager.typeFor("liche_ancienne"), "liche_ancienne", altar.above(), "Test");
            MMORPG.LOGGER.info("[MMORPG] SORTIE boss apparu en ({}, {}, {})", (int) bossRef.getX(), (int) bossRef.getY(), (int) bossRef.getZ());
        });
        if (tick == 790) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            exitKillTick = srv.getTickCount();
            MMORPG.LOGGER.info("[MMORPG] SORTIE mort du boss au tick serveur {}", exitKillTick);
            com.mmorpg.combat.RpgCombat.dealDamage(p, bossRef, 1.0E9, false, false);
        });
        if (tick > 790 && tick % 40 == 0 && tick <= 790 + 40 * 10) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] SORTIE +{} s : dimension {} ; position ({}, {}, {})", (srv.getTickCount() - exitKillTick) / 20, p.level().dimension().identifier(),
                    (int) p.getX(), (int) p.getY(), (int) p.getZ());
        });
        if (tick == 790 + 40 * 10 + 60) srv.execute(() -> {
            MMORPG.LOGGER.info("[MMORPG] SORTIE exemplaire apres le retour : {}", com.mmorpg.dungeon.InstanceManager.debugSlot(srv, "catacombes:0", 0));
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        });
    }

    /** Verification des donjons instancies : porte, exemplaire par groupe, reutilisation, objet d'invocation. */
    private static void instancesPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
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
            var arena = new net.minecraft.core.BlockPos(ox, trials.get(0).pos().get(1), oz + com.mmorpg.dungeon.Layout.ARENA.z());
            int drops = lv.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(arena).inflate(16), e -> e.getItem().is(key)).size();
            MMORPG.LOGGER.info("[MMORPG] INSTANCE objet d'invocation au sol apres la derniere epreuve = {} (attendu 1)", drops);
            var chest = lv.getBlockEntity(new net.minecraft.core.BlockPos(ox, trials.get(0).pos().get(1), oz + com.mmorpg.dungeon.Layout.TREASURE.z() + 3));
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

    /** Cinematique d'apparition du boss a l'autel : camera du rituel puis du surgissement. */
    private static void revealPhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode survival");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 130) srv.execute(() -> dungeonBase = srv.getPlayerList().getPlayerByName(me).blockPosition());
        if (tick == 135) cmd(mc, "mmorpg donjon construire " + id);
        if (tick == 330) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            p.getAbilities().invulnerable = false;
            var altar = dungeonBase.offset(0, 0, com.mmorpg.dungeon.Layout.ARENA.z());
            for (var old : p.level().getEntitiesOfClass(com.mmorpg.entity.boss.RpgBoss.class, new net.minecraft.world.phys.AABB(altar).inflate(80))) old.discard();
            hubTeleport(srv, me, altar.getX() + 3, altar.getY(), altar.getZ() - 8);
            String key = switch (id) {
                case "crypte_gobeline" -> "roi_gobelin";
                case "catacombes" -> "liche_ancienne";
                case "forteresse_infernale" -> "seigneur_ignis";
                case "sanctuaire_glace" -> "titan_glace";
                default -> "avatar_neant";
            };
            MMORPG.LOGGER.info("[MMORPG] REVELATION invocation = {}", com.mmorpg.entity.boss.BossManager.summon(p.level(), altar, key, p));
        });
        if (tick >= 332 && tick <= 332 + 8 * 18 && (tick - 332) % 18 == 0) shot(mc, id + "_revelation_" + String.format("%02d", (tick - 332) / 18));
        if (tick == 332 + 9 * 18 + 40) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] REVELATION camera du joueur rendue = {} ; joueur fige pendant la scene = {}", mc.getCameraEntity() == mc.player, true);
            var altarPos = dungeonBase.offset(0, 0, com.mmorpg.dungeon.Layout.ARENA.z());
            for (var bo : p.level().getEntitiesOfClass(com.mmorpg.entity.boss.RpgBoss.class, new net.minecraft.world.phys.AABB(altarPos).inflate(60))) {
                boolean embedded = !p.level().noCollision(bo, bo.getBoundingBox().deflate(0.02));
                MMORPG.LOGGER.info("[MMORPG] REVELATION boss {} en ({}, {}, {}) ; a {} blocs de l'autel ; coince dans un bloc = {} ; base = {}",
                        bo.getName().getString(), (int) bo.getX(), (int) bo.getY(), (int) bo.getZ(), (int) Math.hypot(bo.getX() - altarPos.getX() - 0.5, bo.getZ() - altarPos.getZ() - 0.5), embedded, ((net.minecraft.world.entity.PathfinderMob) bo).getHomePosition());
            }
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        });
    }

    /** /setdonjon, PNJ, menu, ecran de chargement et entree dans un exemplaire construit par etapes. */
    private static void setDonjonPhase(Minecraft mc, String me) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
        }
        if (tick == 8 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (tick == 30) srv.execute(() -> com.mmorpg.dungeon.InstanceManager.debugForgetAll(srv));
        if (tick == 40) cmd(mc, "setdonjon");
        if (tick == 70) {
            var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.DONJON);
            MMORPG.LOGGER.info("[MMORPG] SETDONJON pnj pose = {} ({})", npc != null, npc == null ? "" : npc.displayName());
            talkTo(mc, npc);
        }
        if (tick == 90) {
            MMORPG.LOGGER.info("[MMORPG] SETDONJON ecran ouvert = {}", screenName(mc));
            shot(mc, "setdonjon_menu");
        }
        if (tick == 100) {
            var npc = nearestNpc(mc, com.mmorpg.entity.NpcEntity.Role.DONJON);
            if (npc != null) com.mmorpg.client.ClientNet.send(new com.mmorpg.network.Payloads.DungeonEnter(npc.getId(), "catacombes", 0));
            mc.gui.setScreen(null);
        }
        if (tick == 108) shot(mc, "setdonjon_chargement_1");
        if (tick == 130) shot(mc, "setdonjon_chargement_2");
        if (tick == 150) shot(mc, "setdonjon_chargement_3");
        if (tick == 170) shot(mc, "setdonjon_chargement_4");
        if (tick == 260) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayerByName(me);
            MMORPG.LOGGER.info("[MMORPG] SETDONJON arrivee : dimension {} ; position ({}, {}, {}) ; chargement encore affiche = {}", p.level().dimension().identifier(),
                    (int) p.getX(), (int) p.getY(), (int) p.getZ(), com.mmorpg.client.LoadingClient.active());
        });
        if (tick == 300) cmd(mc, "tp " + me + " ~ ~ ~ 0 6");
        if (tick == 340) shot(mc, "setdonjon_arrivee");
        if (tick == 360) {
            MMORPG.LOGGER.info("[MMORPG] SHOWCASE TERMINE");
            stop(mc);
        }
    }

    private static net.minecraft.core.BlockPos dungeonBase;
    /** Prises de vue d'un donjon : (x, z relatifs, hauteur de depart, cap, inclinaison, nom). Le sol est a y = 1. */
    private static final double[][] DUNGEON_SHOTS = {
            {0, 3, 1, 0, -4}, {0, 6, 1, 0, -42}, {0, 32, 1, 0, 0}, {0, 52, 1, 0, 2}, {-10, 58, 1, 35, -10},
            {0, 84, 1, 0, 8}, {0, 119, 1, 0, 3}, {0, 131, 1, 180, -8}, {0, 155, 1, 0, 0}, {0, 168, 1, 0, 2},
            {0, 181, 1, 200, -10}, {0, 200, 1, 0, 6}, {0, 235, 1, 0, 2}, {0, 248, 1, 30, -10}, {0, 274, 1, 0, 0},
            {0, 286, 1, 0, 2}, {0, 298, 1, 200, -12}, {0, 320, 1, 0, 0}, {0, 334, 1, 0, 2}, {0, 350, 1, 150, -10},
            {0, 358, 1, 180, -6}, {0, 378, 1, 0, 0}, {0, 392, 1, 0, 0}, {0, 409, 1, 0, 0}, {0, 420, 1, 0, -12},
            {0, 450, 1, 0, 0}};

    private static void dungeonPhase(Minecraft mc, String me, String id) {
        var srv = mc.getSingleplayerServer();
        if (tick == 5) {
            cmd(mc, "gamemode creative");
            cmd(mc, "execute in minecraft:overworld run tp " + me + " 6000 150 6000 0 0");
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
        String[][] rows = {{"Capitale d'Aeloria", "0", "1"}, {"Port de Valmer", "0", "10"}, {"Crypte Gobeline", "1", "15"},
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
        t.putString("current", "Capitale d'Aeloria");
        t.putBoolean("scroll", false);
        return t;
    }
}
