package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.quest.DefaultQuests;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** La Mascotte (pet dragon) : guide du tutoriel. Elle parle au joueur dans une bulle et dans le chat, et relance les quetes du groupe « tutoriel ». */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MascotGuide {
    public static final String GROUP = com.mmorpg.quest.QuestNpcPlan.GROUP_TUTORIAL;
    private static final Map<UUID, Long> LAST_REMINDER = new HashMap<>();

    private MascotGuide() {
    }

    /** Replique de la Mascotte : bulle au-dessus de sa tete (client du joueur) et copie dans le chat. */
    public static void say(ServerPlayer p, String text) {
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.MASCOT, text, "", 0xFFB060FF));
        p.sendSystemMessage(Component.literal("<Mascotte> ").withColor(0xC890FF).append(Component.literal(text).withColor(0xFFFFFF)));
    }

    /** Anciens joueurs : tutoriel considere comme termine (aucune quete de depart ni equipement redonnes). */
    public static void skipTutorial(PlayerData d) {
        d.completedQuests.addAll(DefaultQuests.tutorialIds());
        d.dirty = true;
    }

    /** Ecran ouvert par le joueur (cle « screen:NomEcran » envoyee par le client) : progression des objectifs de decouverte. */
    public static void screen(ServerPlayer p, String screen) {
        if (screen.isEmpty() || screen.length() > 40 || !screen.chars().allMatch(Character::isLetterOrDigit)) return;
        QuestManager.onAction(p, "screen:" + screen, 1);
    }

    /** Premiere quete du tutoriel non terminee qui est active, sinon disponible. */
    private static String pending(PlayerData d) {
        String available = null;
        for (String id : DefaultQuests.tutorialIds()) {
            QuestDef q = ConfigManager.quests().get(id);
            if (q == null || d.completedQuests.contains(id)) continue;
            if (d.activeQuests.containsKey(id)) return id;
            if (available == null && QuestManager.isAvailable(d, id, q, GROUP)) available = id;
        }
        return available;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            if (!d.playerClass.isPlayable() || !p.isAlive() || !(p.level().getEntity(MascotEntity.ID) instanceof MascotEntity m) || m.distanceTo(p) > 9) continue;
            long now = server.getTickCount();
            if (now - LAST_REMINDER.getOrDefault(p.getUUID(), -10_000L) < 20 * 40) continue;
            String id = pending(d);
            if (id == null) continue;
            QuestDef q = ConfigManager.quests().get(id);
            LAST_REMINDER.put(p.getUUID(), now);
            if (d.activeQuests.containsKey(id)) say(p, QuestManager.completable(p, d, id) ? "Tu as réussi « " + q.name + " » ! Clique sur moi pour ta récompense." : q.speech.isEmpty() ? q.description : q.speech);
            else say(p, "J'ai une mission pour toi : « " + q.name + " ». Clique sur moi !");
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_REMINDER.remove(e.getEntity().getUUID());
    }
}
