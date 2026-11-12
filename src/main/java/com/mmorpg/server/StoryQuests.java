package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.quest.StoryBook;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/** Histoire principale : chapitres rendus automatiquement, cinematique a l'acceptation, rejouable depuis le journal. */
@EventBusSubscriber(modid = com.mmorpg.MMORPG.MODID)
public final class StoryQuests {
    private StoryQuests() {
    }

    /** Joue une scene cinematique chez le joueur (jamais en combat : l'appelant verifie). */
    public static void play(ServerPlayer p, String scene) {
        if (StoryBook.get(scene) == null) return;
        CompoundTag t = new CompoundTag();
        t.putString("scene", scene);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.STORY_SCENE, t));
    }

    /** Quete d'histoire acceptee : la scene du chapitre se joue juste apres. */
    public static void onAccept(ServerPlayer p, QuestDef q) {
        if (q == null || q.story.isEmpty()) return;
        var level = p.level();
        Scheduler.later(level, 30, () -> {
            if (p.isAlive() && !p.isRemoved()) play(p, q.story);
        });
    }

    /** Fin d'un chapitre : le dernier declenche l'epilogue. */
    public static void onFinish(ServerPlayer p, QuestDef q) {
        FlammecheScenes.onFinish(p, q);
        if (q != null && q.lore != null && "QP-250".equals(q.lore.code)) {
            Scheduler.later(p.level(), 160, () -> {
                if (p.isAlive() && !p.isRemoved()) play(p, StoryBook.EPILOGUE);
            });
            return;
        }
        if (q == null || q.story.isEmpty() || !q.story.equals("hist_12")) return;
        Scheduler.later(p.level(), 100, () -> {
            if (p.isAlive() && !p.isRemoved()) play(p, StoryBook.EPILOGUE);
        });
    }

    /** Rejouer depuis le journal : seulement les chapitres en cours ou deja termines. */
    public static void replay(ServerPlayer p, String questId) {
        PlayerData d = RpgPlayers.get(p);
        QuestDef q = ConfigManager.quests().get(questId);
        if (q == null || q.story.isEmpty() || !d.completedQuests.contains(questId) && !d.activeQuests.containsKey(questId)) return;
        play(p, q.story);
    }

    /** Chapitres debloques (en cours ou termines), pour l'onglet « Histoire » du journal. */
    public static ListTag tag(ServerPlayer p, PlayerData d) {
        ListTag list = new ListTag();
        for (Map.Entry<String, QuestDef> e : ConfigManager.quests().entrySet()) {
            QuestDef q = e.getValue();
            if (q.story.isEmpty()) continue;
            boolean done = d.completedQuests.contains(e.getKey());
            if (!done && !d.activeQuests.containsKey(e.getKey())) continue;
            CompoundTag t = QuestManager.questTag(p, d, e.getKey(), q);
            t.putBoolean("replay", true);
            t.putBoolean("finished", done);
            list.add(t);
        }
        return list;
    }

    /** Les chapitres se rendent seuls : plus de PNJ a retrouver. */
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 7) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            if (d.activeQuests.isEmpty() || !p.isAlive() || p.isSpectator()) continue;
            for (String id : new java.util.ArrayList<>(d.activeQuests.keySet())) {
                QuestDef q = ConfigManager.quests().get(id);
                if (q == null || q.story.isEmpty() && !q.auto || !QuestManager.completable(p, d, id)) continue;
                QuestManager.finish(p, d, id, q);
            }
        }
    }
}
