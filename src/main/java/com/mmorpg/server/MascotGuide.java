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

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/** La Mascotte (pet dragon) : guide du tutoriel. Elle parle au joueur dans une bulle et dans le chat, et relance les quetes du groupe « tutoriel ». */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MascotGuide {
    public static final String NAME = MascotEntity.NAME;
    public static final String GROUP = com.mmorpg.quest.QuestNpcPlan.GROUP_TUTORIAL;
    private static final Map<UUID, Long> LAST_REMINDER = new HashMap<>();
    private static final Map<UUID, UUID> MASCOTS = new HashMap<>();
    /** Joueurs pour qui un operateur a force la Mascotte (/mascotte), meme tutoriel termine. */
    private static final Set<UUID> FORCED = new HashSet<>();

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
                boolean forced = FORCED.contains(p.getUUID());
                String pend = pending(d);
                boolean alive = p.isAlive() && !p.isSpectator() && d.playerClass.isPlayable() && pend != null && FeatureFlags.allow(p, "quetes");
                boolean follow = forced || (alive && (followUntil < 0 || step(pend) < followUntil));
                boolean atPost = !follow && alive && postDim != null && postDim.equals(p.level().dimension().identifier().toString());
                if (!follow && !atPost || !p.isAlive() || p.isSpectator()) {
                    if (m != null || MASCOTS.containsKey(p.getUUID())) drop(p);
                    continue;
                }
                if (m == null) {
                    MASCOTS.remove(p.getUUID());
                    m = ModEntities.MASCOT.get().create(p.level(), EntitySpawnReason.COMMAND);
                    m.initFor(p);
                    if (atPost) m.snapTo(postX, postY, postZ, postYaw, 0);
                    else {
                        var ground = safeGround(p, m, target(p, 0));
                        if (ground == null) { m.discard(); continue; }
                        m.snapTo(ground, p.getYRot(), 0);
                    }
                    if (p.level().addFreshEntity(m)) MASCOTS.put(p.getUUID(), m.getUUID());
                }
                if (atPost) look(p, m);
            }
            if (m != null && !fixedFor(p, d)) follow(p, m);
        }
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            if (mine(p) == null || !p.isAlive()) continue;
            long now = server.getTickCount();
            if (now - LAST_REMINDER.getOrDefault(p.getUUID(), -10_000L) < 20 * 40) continue;
            String id = pending(d);
            if (id == null) {
                if (FORCED.contains(p.getUUID()) && !LAST_REMINDER.containsKey(p.getUUID())) {
                    say(p, "Bienvenue sur Aeloria ! Deviens un héros de niveau 100 : quêtes, donjons, guildes et arènes t'attendent. Ouvre le Wiki pour découvrir chaque activité.");
                    LAST_REMINDER.put(p.getUUID(), now);
                }
                continue;
            }
            QuestDef q = ConfigManager.quests().get(id);
            LAST_REMINDER.put(p.getUUID(), now);
            if (d.activeQuests.containsKey(id)) say(p, QuestManager.completable(p, d, id) ? "Tu as réussi « " + q.name + " » ! Clique sur moi pour ta récompense." : q.speech.isEmpty() ? q.description : q.speech);
            else say(p, q.speech.isEmpty() ? "J'ai une mission pour toi : « " + q.name + " ». Clique sur moi !" : q.speech + " Clique sur moi pour commencer.");
            if (d.activeQuests.containsKey(id) && !d.questGuidanceMuted) {
                d.trackedQuest = id; d.dirty = true; GuidanceManager.refresh(p);
            }
        }
    }

    private static int step(String id) {
        return DefaultQuests.tutorialIds().indexOf(id);
    }

    /** Vrai si la Mascotte de ce joueur reste au poste fixe (elle ne le suit plus). */
    private static boolean fixedFor(ServerPlayer p, PlayerData d) {
        if (FORCED.contains(p.getUUID())) return false;
        String pend = pending(d);
        return pend != null && followUntil >= 0 && step(pend) >= followUntil;
    }

    /** Regarde le joueur sans bouger. */
    private static void look(ServerPlayer p, MascotEntity m) {
        double dx = p.getX() - m.getX(), dz = p.getZ() - m.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        m.setYRot(yaw);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
    }

    // ---- reglages persistants de l'operateur (fichier du monde) ----
    /** Nombre de quetes du tutoriel pendant lesquelles elle suit le joueur (-1 : tout le tutoriel). */
    private static int followUntil = -1;
    private static String postDim;
    private static double postX, postY, postZ;
    private static float postYaw;

    private static java.nio.file.Path file(net.minecraft.server.MinecraftServer s) {
        return s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("mmorpg_mascotte.properties");
    }

    @SubscribeEvent
    public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e) {
        followUntil = -1;
        postDim = null;
        var pr = new java.util.Properties();
        try (var in = java.nio.file.Files.newInputStream(file(e.getServer()))) {
            pr.load(in);
            followUntil = Integer.parseInt(pr.getProperty("suit", "-1"));
            if (pr.containsKey("dim")) {
                postDim = pr.getProperty("dim");
                postX = Double.parseDouble(pr.getProperty("x"));
                postY = Double.parseDouble(pr.getProperty("y"));
                postZ = Double.parseDouble(pr.getProperty("z"));
                postYaw = Float.parseFloat(pr.getProperty("yaw", "0"));
            }
        } catch (java.nio.file.NoSuchFileException ignored) {
        } catch (Exception ex) {
            MMORPG.LOGGER.warn("[mascotte] reglages illisibles : {}", ex.toString());
        }
    }

    private static void save(net.minecraft.server.MinecraftServer s) {
        var pr = new java.util.Properties();
        pr.setProperty("suit", Integer.toString(followUntil));
        if (postDim != null) {
            pr.setProperty("dim", postDim);
            pr.setProperty("x", Double.toString(postX));
            pr.setProperty("y", Double.toString(postY));
            pr.setProperty("z", Double.toString(postZ));
            pr.setProperty("yaw", Float.toString(postYaw));
        }
        try (var out = java.nio.file.Files.newOutputStream(file(s))) {
            pr.store(out, "Mascotte : suit = nombre de quetes du tutoriel pendant lesquelles elle suit (-1 = toutes) ; dim/x/y/z/yaw = poste fixe");
        } catch (Exception ex) {
            MMORPG.LOGGER.warn("[mascotte] sauvegarde impossible : {}", ex.toString());
        }
    }

    /** Le guide marche à distance, avec les collisions et les chemins du jeu. */
    static net.minecraft.world.phys.Vec3 target(ServerPlayer p, int ignored) {
        double yaw = Math.toRadians(p.getYRot());
        return p.position().add(Math.sin(yaw) * 3.8 - Math.cos(yaw) * 2, 0,
                -Math.cos(yaw) * 3.8 - Math.sin(yaw) * 2);
    }
    static net.minecraft.world.phys.Vec3 safeGround(ServerPlayer p, MascotEntity m, net.minecraft.world.phys.Vec3 wanted) {
        var level = p.level();
        for (int radius = 0; radius <= 3; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            for (int dy = 3; dy >= -4; dy--) {
                var at = net.minecraft.core.BlockPos.containing(wanted).offset(dx, dy, dz);
                if (!level.hasChunkAt(at) || !level.getWorldBorder().isWithinBounds(at)) continue;
                var point = net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
                if (point.subtract(p.position()).horizontalDistanceSqr() < 9) continue;
                if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) continue;
                var box = m.getBoundingBox().move(point.subtract(m.position()));
                if (level.noCollision(m, box) && !level.containsAnyLiquid(box)) return point;
            }
        }
        return null;
    }
    private static void follow(ServerPlayer p, MascotEntity m) {
        if (m.level() != p.level()) { drop(p); return; }
        double distance = m.position().subtract(p.position()).horizontalDistanceSqr();
        if (m.tickCount % 10 != 0) return;
        if (distance >= 9 && distance <= 25) { m.getNavigation().stop(); look(p, m); return; }
        var radial = m.position().subtract(p.position()).multiply(1, 0, 1);
        var wanted = distance < .01 || distance > 32 * 32 ? target(p, 0)
                : radial.normalize().scale(4).add(p.position());
        var goal = safeGround(p, m, wanted);
        if (goal == null) { m.getNavigation().stop(); return; }
        if (distance > 32 * 32) { m.snapTo(goal, p.getYRot(), 0); m.getNavigation().stop(); }
        else m.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.1);
    }

    /**
     * /mascotte (operateurs) : sans argument ou avec un joueur, l'invoque / la renvoie ; « poser » la fixe a ta position (PNJ persistant,
     * visible de chaque joueur qui a encore des quetes du tutoriel) ; « retirer » enleve le poste ; « suit <n> » : elle accompagne le joueur
     * pendant les n premieres quetes du tutoriel puis reste au poste (« suit tout » : jusqu'a la fin).
     */
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("mascotte").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> toggle(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("joueur", EntityArgument.player()).executes(c -> toggle(c.getSource(), EntityArgument.getPlayer(c, "joueur"))))
                .then(Commands.literal("poser").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    postDim = p.level().dimension().identifier().toString();
                    postX = p.getX();
                    postY = p.getY();
                    postZ = p.getZ();
                    postYaw = p.getYRot();
                    save(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Poste de la Mascotte posé ici (" + (int) postX + " " + (int) postY + " " + (int) postZ + ")."), true);
                    return 1;
                }))
                .then(Commands.literal("retirer").executes(c -> {
                    postDim = null;
                    save(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Poste de la Mascotte retiré."), true);
                    return 1;
                }))
                .then(Commands.literal("suit")
                        .then(Commands.literal("tout").executes(c -> setFollow(c.getSource(), -1)))
                        .then(Commands.argument("quetes", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 200)).executes(c -> setFollow(c.getSource(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "quetes"))))));
    }

    private static int setFollow(net.minecraft.commands.CommandSourceStack src, int n) {
        followUntil = n;
        save(src.getServer());
        src.sendSuccess(() -> Component.literal(n < 0 ? "La Mascotte suit le joueur pendant tout le tutoriel." : "La Mascotte suit le joueur pendant les " + n + " premières quêtes, puis reste à son poste."), true);
        return 1;
    }

    private static int toggle(net.minecraft.commands.CommandSourceStack src, ServerPlayer p) {
        boolean on = FORCED.add(p.getUUID());
        if (!on) {
            FORCED.remove(p.getUUID());
            drop(p);
        }
        String msg = on ? "Mascotte invoquée pour " + p.getGameProfile().name() + "." : "Mascotte renvoyée pour " + p.getGameProfile().name() + ".";
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    @SubscribeEvent
    public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {
        LAST_REMINDER.clear(); MASCOTS.clear(); FORCED.clear();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_REMINDER.remove(e.getEntity().getUUID());
        FORCED.remove(e.getEntity().getUUID());
        if (e.getEntity() instanceof ServerPlayer p) drop(p);
    }
}
