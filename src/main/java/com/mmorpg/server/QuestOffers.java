package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Propositions automatiques de quetes : le joueur en recoit toujours une nouvelle, a accepter dans une fenetre. */
@EventBusSubscriber(modid = com.mmorpg.MMORPG.MODID)
public final class QuestOffers {
    /** Nombre de quetes de campagne en cours en dessous duquel une nouvelle est proposee. */
    static final int TARGET_ACTIVE = 2;
    private static final int OFFER_COOLDOWN = 20 * 45;
    private static final int DECLINE_COOLDOWN = 20 * 240;

    private record Offer(String quest, long next) {
    }

    private static final Map<UUID, Offer> OFFERS = new HashMap<>();

    private QuestOffers() {
    }

    private static boolean campaign(String id, QuestDef q) {
        return q != null && !q.daily && !q.weekly && !"maitre_classes".equals(q.giver);
    }

    /** Prochaine quete de campagne disponible (niveau croissant), ou null. */
    static String next(PlayerData d) {
        String best = null;
        int bestLevel = Integer.MAX_VALUE;
        for (Map.Entry<String, QuestDef> e : ConfigManager.quests().entrySet()) {
            QuestDef q = e.getValue();
            if (d.declinedQuests.contains(e.getKey()) || !campaign(e.getKey(), q) || !QuestManager.isAvailable(d, e.getKey(), q, q.giver)) continue;
            if (q.minLevel < bestLevel) {
                bestLevel = q.minLevel;
                best = e.getKey();
            }
        }
        return best;
    }

    /** Pas de fenetre en plein combat (coup recu ou donne il y a moins de 10 s) : elle bloquerait les commandes du joueur. */
    private static boolean fighting(ServerPlayer p) {
        return p.tickCount - p.getLastHurtByMobTimestamp() < 200 || p.tickCount - p.getLastHurtMobTimestamp() < 200;
    }

    private static boolean busy(ServerPlayer p) {
        return !p.isAlive() || p.isSpectator() || p.containerMenu != p.inventoryMenu || fighting(p)
                || MountManager.inDungeon(p) || com.mmorpg.hub.ClassTrial.active(p) || com.mmorpg.hub.CinematicManager.isLocked(p);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 0) return;
        long now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Offer old = OFFERS.get(p.getUUID());
            if (old != null && now < old.next) continue;
            PlayerData d = RpgPlayers.get(p);
            if (!d.playerClass.isPlayable() || busy(p) || !FeatureFlags.allow(p, "quetes")) continue;
            long active = d.activeQuests.keySet().stream().filter(id -> campaign(id, QuestManager.def(id))).count();
            if (active >= TARGET_ACTIVE || d.activeQuests.size() >= QuestManager.MAX_ACTIVE) continue;
            String id = next(d);
            if (id == null) continue;
            OFFERS.put(p.getUUID(), new Offer(id, now + OFFER_COOLDOWN));
            Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.QUEST_OFFER, QuestManager.questTag(p, d, id, QuestManager.def(id))));
        }
    }

    public static void accept(ServerPlayer p, String id) {
        Offer o = OFFERS.get(p.getUUID());
        PlayerData d = RpgPlayers.get(p);
        QuestDef q = QuestManager.def(id);
        boolean fromJournal = RpgPlayers.get(p).declinedQuests.contains(id);
        if (!FeatureFlags.allow(p, "quetes") || !fromJournal && (o == null || !o.quest.equals(id)) || !campaign(id, q) || !QuestManager.isAvailable(d, id, q, q.giver)) return;
        if (d.activeQuests.size() >= QuestManager.MAX_ACTIVE) {
            p.sendOverlayMessage(Component.literal("Journal de quêtes plein (" + QuestManager.MAX_ACTIVE + " quêtes maximum).").withColor(0xFF5050));
            return;
        }
        d.declinedQuests.remove(id);
        d.activeQuests.put(id, new int[q.objectives.size()]);
        d.trackedQuest = id;
        d.questGuidanceMuted = false;
        if (!fromJournal) OFFERS.put(p.getUUID(), new Offer("", p.level().getServer().getTickCount() + 100));
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "QUÊTE ACCEPTÉE", q.name, 0xFFD040));
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.QUEST_ACCEPT.get(), SoundSource.PLAYERS, 1f, 1f);
        GuidanceManager.refresh(p);
        d.dirty = true;
        if (!q.speech.isEmpty()) MascotGuide.say(p, q.speech);
    }

    public static void decline(ServerPlayer p, String id) {
        Offer o = OFFERS.get(p.getUUID());
        if (o != null && o.quest.equals(id)) OFFERS.put(p.getUUID(), new Offer("", p.level().getServer().getTickCount() + DECLINE_COOLDOWN));
        {
            PlayerData d = RpgPlayers.get(p);
            QuestDef q = QuestManager.def(id);
            if (campaign(id, q) && QuestManager.isAvailable(d, id, q, q.giver) && d.declinedQuests.add(id)) {
                d.dirty = true;
                p.sendOverlayMessage(Component.literal("Quête gardée dans le journal, onglet Important.").withColor(0xFFD040));
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        OFFERS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        OFFERS.clear();
    }
}
