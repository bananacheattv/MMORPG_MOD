package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.quest.DefaultQuests;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.world.entity.EntitySpawnReason;
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
    public static final String NAME = MascotEntity.NAME;
    public static final String GROUP = com.mmorpg.quest.QuestNpcPlan.GROUP_TUTORIAL;
    private static final Map<UUID, Long> LAST_REMINDER = new HashMap<>();
    private static final Map<UUID, UUID> MASCOTS = new HashMap<>();

    private MascotGuide() {
    }

    /** Replique de la Mascotte : bulle au-dessus de sa tete (client du joueur) et copie dans le chat. */
    public static void say(ServerPlayer p, String text) {
        say(p, NAME, text);
    }

    /** Bulle de dialogue d'un PNJ de quetes (titre = nom du PNJ) affichee a l'ecran du joueur, et copie dans le chat. */
    public static void say(ServerPlayer p, String speaker, String text) {
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.MASCOT, text, speaker, 0xFFE0302A));
        p.sendSystemMessage(Component.literal("<" + speaker + "> ").withColor(0xC890FF).append(Component.literal(text).withColor(0xFFFFFF)));
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

    /** Mascotte personnelle du joueur (une par joueur, visible de lui seul) : creee tant que le tutoriel n'est pas termine. */
    private static MascotEntity mine(ServerPlayer p) {
        UUID id = MASCOTS.get(p.getUUID());
        return id != null && p.level().getEntity(id) instanceof MascotEntity m && m.isAlive() ? m : null;
    }

    private static void drop(ServerPlayer p) {
        UUID id = MASCOTS.remove(p.getUUID());
        if (id != null && p.level().getEntity(id) instanceof MascotEntity m) m.discard();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            MascotEntity m = mine(p);
            if (server.getTickCount() % 20 == 0) {
                if (!d.playerClass.isPlayable() || !p.isAlive() || p.isSpectator() || pending(d) == null || !FeatureFlags.allow(p, "quetes")) {
                    if (m != null || MASCOTS.containsKey(p.getUUID())) drop(p);
                    continue;
                }
                if (m == null) {
                    MASCOTS.remove(p.getUUID());
                    m = ModEntities.MASCOT.get().create(p.level(), EntitySpawnReason.COMMAND);
                    m.initFor(p);
                    m.snapTo(target(p, 0), 0, 0);
                    if (p.level().addFreshEntity(m)) MASCOTS.put(p.getUUID(), m.getUUID());
                }
            }
            if (m != null) follow(p, m);
        }
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            if (mine(p) == null || !p.isAlive()) continue;
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

    /** Position voulue : devant le joueur, un peu a sa droite, en vol. */
    private static net.minecraft.world.phys.Vec3 target(ServerPlayer p, int ignored) {
        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        return p.position().add(fx * 1.9 + rx * 1.3, 0.55, fz * 1.9 + rz * 1.3);
    }

    /** Suit le joueur en douceur et le regarde en permanence. */
    private static void follow(ServerPlayer p, MascotEntity m) {
        if (m.level() != p.level()) {
            drop(p);
            return;
        }
        var goal = target(p, m.getId());
        var at = m.position();
        double dist = at.distanceTo(goal);
        var next = dist > 14 ? goal : at.add(goal.subtract(at).scale(Math.min(1.0, 0.2 + dist * 0.02)));
        double dx = p.getX() - next.x, dz = p.getZ() - next.z, dy = p.getEyeY() - (next.y + 0.8);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        m.snapTo(next, yaw, pitch);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_REMINDER.remove(e.getEntity().getUUID());
        if (e.getEntity() instanceof ServerPlayer p) drop(p);
    }
}
