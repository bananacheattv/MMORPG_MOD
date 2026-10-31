package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mojang.authlib.GameProfile;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.quest.DefaultQuests;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Controle en jeu du tutoriel : un nouveau joueur ne recoit rien, puis fabrique tout son equipement en suivant les quetes de la Mascotte. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevTutorialChecks {
    private DevTutorialChecks() {
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkTutorial")) return;
        var server = event.getServer();
        run(FakePlayerFactory.get(server.overworld(), new GameProfile(UUID.randomUUID(), "TutorialChecks")));
    }

    private static void run(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        var snapshot = d.save();
        var inv = p.getInventory();
        ItemStack[] saved = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < saved.length; i++) saved[i] = inv.getItem(i).copy();
        var level = p.level();
        var forgePos = p.blockPosition().offset(2, 0, 0);
        var oldBlock = level.getBlockState(forgePos);
        MascotEntity mascot = null;
        try {
            inv.clearContent();
            for (var slot : EquipmentSlot.values()) p.setItemSlot(slot, ItemStack.EMPTY);
            var fresh = new PlayerData();
            d.load(fresh.save());
            d.level = 1;
            for (PlayerClass cls : new PlayerClass[]{PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK}) {
                // nouveau joueur : aucun objet n'est offert au choix de la classe
                d.load(new PlayerData().save());
                d.level = 1;
                inv.clearContent();
                for (var slot : EquipmentSlot.values()) p.setItemSlot(slot, ItemStack.EMPTY);
                RpgPlayers.selectClass(p, cls);
                check(inv.isEmpty(), cls + " : l'inventaire doit etre vide apres le choix de classe");
                for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                    check(p.getItemBySlot(slot).isEmpty(), cls + " : aucune armure offerte");
                playTutorial(p, d, cls, forgePos);
            }
            // anciens joueurs : tutoriel termine d'office
            var legacy = new PlayerData();
            MascotGuide.skipTutorial(legacy);
            check(legacy.completedQuests.containsAll(DefaultQuests.tutorialIds()), "migration des anciens joueurs");
            MMORPG.LOGGER.info("[CODEX CHECKS] TUTORIAL PASS: 4 classes, rien d'offert, {} quetes, pioche, houe, arme et armure fabriquees", DefaultQuests.tutorialIds().size());
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] TUTORIAL FAIL", t);
        } finally {
            if (mascot != null) mascot.discard();
            level.setBlock(forgePos, oldBlock, 3);
            for (int i = 0; i < saved.length; i++) inv.setItem(i, saved[i]);
            d.load(snapshot);
            d.dirty = true;
        }
    }

    private static void playTutorial(ServerPlayer p, PlayerData d, PlayerClass cls, net.minecraft.core.BlockPos forgePos) {
        var level = p.level();
        level.setBlock(forgePos, ModBlocks.FORGE_ARCANIQUE.get().defaultBlockState(), 3);
        var mascot = ModEntities.MASCOT.get().create(level, EntitySpawnReason.COMMAND);
        mascot.init();
        mascot.snapTo(p.getX(), p.getY(), p.getZ() + 1, 0, 0);
        level.addFreshEntity(mascot);
        try {
            long gold = d.gold;
            for (String id : DefaultQuests.tutorialIds()) {
                var q = ConfigManager.quests().get(id);
                check(q != null, id + " absente de la configuration");
                check(QuestManager.isAvailable(d, id, q, MascotGuide.GROUP), cls + " : " + id + " indisponible");
                QuestManager.accept(p, id, mascot.getId());
                check(d.activeQuests.containsKey(id), cls + " : " + id + " non acceptee");
                for (var o : q.objectives) {
                    switch (o.type) {
                        case "action" -> {
                            String key = TutorialKit.resolve(o.target.substring(0, o.target.indexOf('|')), cls);
                            if (key.startsWith("craft:")) ForgeManager.craft(p, forgePos, key.substring(6), 1);
                            else QuestManager.onAction(p, key, o.count);
                        }
                        case "kill" -> {
                            for (int i = 0; i < o.count; i++) QuestManager.onKill(p, o.target, "", false);
                        }
                        default -> {
                        }
                    }
                }
                check(QuestManager.completable(p, d, id), cls + " : " + id + " non terminee");
                QuestManager.complete(p, id, mascot.getId());
                check(d.completedQuests.contains(id), cls + " : " + id + " non remise");
            }
            check(QuestManager.isAvailable(d, "spawn_bienvenue", ConfigManager.quests().get("spawn_bienvenue"), "spawn_decouverte_0"), "la decouverte du spawn suit le tutoriel");
            var inv = p.getInventory();
            check(inv.contains(new ItemStack(com.mmorpg.registry.ModItems.get("pioche_debutant"))), cls + " : pioche fabriquee");
            check(inv.contains(new ItemStack(com.mmorpg.registry.ModItems.get("houe_debutant"))), cls + " : houe fabriquee");
            check(inv.contains(new ItemStack(com.mmorpg.registry.ModItems.get(TutorialKit.weapon(cls)))), cls + " : arme fabriquee");
            for (String piece : com.mmorpg.item.ItemDefs.PIECES)
                check(inv.contains(new ItemStack(com.mmorpg.registry.ModItems.get(TutorialKit.set(cls) + "_" + piece))), cls + " : " + piece + " fabrique");
            check(d.gold >= gold, "or non negatif");
        } finally {
            mascot.discard();
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Tutorial check failed: " + label);
    }
}
