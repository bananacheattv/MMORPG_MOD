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
                "marteau_titan", "lame_runique", "hache_berserker", "epee_seigneur_guerre", "lame_demoniaque"};
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
                {"paladin", "masse_gardien"}, {"dieu_guerre", "excalibur_celeste"}, {"neant_primordial", "marteau_egide"}};
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
    private static void stop(Minecraft mc) {
        mc.options.pauseOnLostFocus = true;
        mc.options.save();
        mc.stop();
    }

    private static void cmd(Minecraft mc, String command) {
        mc.player.connection.sendCommand(command);
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
