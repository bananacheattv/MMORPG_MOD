package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Mises en scene de Flammeche, le dragonnet du lore (la Mascotte personnelle du joueur) :
 * <ul>
 *     <li>eclosion de l'oeuf brulant (QP-002) : l'oeuf tremble, se fissure, eclate dans les flammes et le dragonnet grandit ;</li>
 *     <li>il suit le joueur pendant le chapitre 1, puis s'envole vers le Seuil (QP-012) ;</li>
 *     <li>a chaque Eveil (niveaux 25/50/75/100) il revient, grandit dans une spirale de feu (stades 2 a 5) puis repart ;</li>
 *     <li>pendant Noel et Paques, une fois retrouve, il suit le joueur avec une aura de saison.</li>
 * </ul>
 * Tout est visible du seul joueur concerne (Mascotte personnelle, particules envoyees a lui seul).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FlammecheScenes {
    public static final String NAME = "Flammèche";
    public static final String HATCH_QUEST = "qp_002", FAREWELL_QUEST = "qp_012";
    static final int HATCH_TICKS = 140, GROW_TICKS = 40, AWAKEN_TICKS = 90, AWAKEN_LINGER = 160, FAREWELL_TICKS = 50;
    private static final Pattern STAGE = Pattern.compile("stade (\\d)");

    enum Kind { HATCH, AWAKEN, FAREWELL }

    private static final class Scene {
        final Kind kind;
        int tick;
        float from, to;
        int stage;

        Scene(Kind kind) {
            this.kind = kind;
        }
    }

    private static final Map<UUID, Scene> SCENES = new HashMap<>();

    private FlammecheScenes() {
    }

    /** Taille du dragonnet selon le stade (1 a 5). */
    public static float scale(int stage) {
        return 1F + 0.28F * (Math.max(1, Math.min(5, stage)) - 1);
    }

    private static int stageOf(QuestDef q) {
        if (q == null || q.lore == null) return 0;
        var m = STAGE.matcher(q.lore.companion);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** Stade atteint : 1 + Eveils termines (d'apres la colonne Compagnon des quetes terminees). */
    public static int stage(PlayerData d) {
        int s = 1;
        for (String id : d.completedQuests) s = Math.max(s, stageOf(ConfigManager.quests().get(id)));
        return s;
    }

    /** Flammeche accompagne-t-il le joueur en ce moment ? */
    public static boolean companion(ServerPlayer p, PlayerData d) {
        if (!p.isAlive() || p.isSpectator() || !d.playerClass.isPlayable()) return false;
        if (SCENES.containsKey(p.getUUID())) return true;
        boolean hatched = d.completedQuests.contains(HATCH_QUEST) || d.activeQuests.containsKey(HATCH_QUEST);
        if (hatched && !d.completedQuests.contains(FAREWELL_QUEST)) return true;
        for (String id : d.activeQuests.keySet()) {
            QuestDef q = ConfigManager.quests().get(id);
            if (q != null && q.lore != null && q.lore.companion.startsWith("Flammèche revient")) return true;
        }
        for (String id : d.completedQuests) {
            QuestDef q = ConfigManager.quests().get(id);
            if (q != null && q.lore != null && "Compagnon".equals(q.lore.type) && QuestManager.windowOpen(q)) return true;
        }
        return false;
    }

    /** Une scene est en cours : la Mascotte ne suit pas et ne doit pas etre retiree. */
    public static boolean busy(ServerPlayer p) {
        return SCENES.containsKey(p.getUUID());
    }

    /** Habillage hors scene : nom et taille du stade quand c'est Flammeche (et non la Mascotte du tutoriel). */
    static void dress(ServerPlayer p, PlayerData d, MascotEntity m, boolean tutorial) {
        if (busy(p)) return;
        if (!tutorial && !NAME.equals(m.displayName())) m.setCustomName(Component.literal(NAME));
        float g = tutorial ? 1F : scale(stage(d));
        if (m.growth() != g) m.setGrowth(g);
    }

    /** Flammeche ne suit plus : il s'envole vers le Seuil au lieu de disparaitre. Vrai tant que la scene tient la Mascotte. */
    static boolean holds(ServerPlayer p, MascotEntity m) {
        if (busy(p)) return true;
        if (m == null || !NAME.equals(m.displayName()) || m.egg() > 0) return false;
        Scene s = new Scene(Kind.FAREWELL);
        SCENES.put(p.getUUID(), s);
        line(p, NAME, "Je retourne au Seuil... On se reverra, promis !");
        sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 1.6F);
        return true;
    }

    /** Fin d'une quete du lore : Eveil (le dragonnet grandit). */
    public static void onFinish(ServerPlayer p, QuestDef q) {
        int stage = stageOf(q);
        if (stage < 2 || q.lore == null || !q.lore.companion.contains("plus grand")) return;
        MascotEntity m = MascotGuide.ensure(p);
        if (m == null) return;
        Scene s = new Scene(Kind.AWAKEN);
        s.from = scale(stage - 1);
        s.to = scale(stage);
        s.stage = stage;
        m.setCustomName(Component.literal(NAME));
        m.setGrowth(s.from);
        SCENES.put(p.getUUID(), s);
    }

    /** Lance l'eclosion (aussi apres une reconnexion tant que l'etape n'est pas validee). */
    static void hatch(ServerPlayer p) {
        MascotEntity m = MascotGuide.ensure(p);
        if (m == null) return;
        m.getNavigation().stop();
        m.setCustomName(Component.literal(NAME));
        m.setEgg(1);
        m.setGrowth(0.3F);
        SCENES.put(p.getUUID(), new Scene(Kind.HATCH));
        line(p, "Ysméralde", "Regarde ! L'œuf brûlant au pied du Seuil... il bouge !");
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            Scene s = SCENES.get(p.getUUID());
            if (s == null) {
                if (server.getTickCount() % 20 == 3 && d.activeQuests.containsKey(HATCH_QUEST) && !QuestManager.completable(p, d, HATCH_QUEST)
                        && p.isAlive() && !p.isSpectator()) hatch(p);
                else if (server.getTickCount() % 8 == 0) aura(p, d);
                continue;
            }
            MascotEntity m = MascotGuide.mascot(p);
            if (m == null || m.level() != p.level() || !p.isAlive()) {
                if (m != null) m.setEgg(0);
                SCENES.remove(p.getUUID());
                continue;
            }
            s.tick++;
            switch (s.kind) {
                case HATCH -> hatchTick(p, m, s);
                case AWAKEN -> awakenTick(p, d, m, s);
                case FAREWELL -> farewellTick(p, m, s);
            }
        }
    }

    // ------------------------------------------------------------------ eclosion
    private static void hatchTick(ServerPlayer p, MascotEntity m, Scene s) {
        double x = m.getX(), y = m.getY(), z = m.getZ();
        if (s.tick <= HATCH_TICKS) {
            m.setEgg(Math.max(1, s.tick * 100 / HATCH_TICKS));
            float k = s.tick / (float) HATCH_TICKS;
            if (s.tick % 3 == 0) particles(p, ParticleTypes.SMALL_FLAME, x, y + 0.6, z, 2 + (int) (k * 6), 0.35, 0.35, 0.35, 0.01);
            if (s.tick % 7 == 0) particles(p, ParticleTypes.SMOKE, x, y + 1.1, z, 2, 0.2, 0.1, 0.2, 0.01);
            if (k > 0.6 && s.tick % 4 == 0) particles(p, ParticleTypes.LAVA, x, y + 0.8, z, 1, 0.2, 0.1, 0.2, 0);
            if (s.tick == 35 || s.tick == 70 || s.tick == 100 || s.tick == 120 || s.tick == 132)
                sound(p, s.tick < 100 ? SoundEvents.TURTLE_EGG_CRACK : SoundEvents.SNIFFER_EGG_CRACK, 1F, 0.8F + k * 0.6F);
            if (s.tick == 100) line(p, "Ysméralde", "Il se fissure ! Recule un peu...");
            return;
        }
        if (s.tick == HATCH_TICKS + 1) {
            m.setEgg(0);
            m.setGrowth(0.3F);
            particles(p, ParticleTypes.EXPLOSION, x, y + 0.7, z, 1, 0, 0, 0, 0);
            particles(p, ParticleTypes.FLAME, x, y + 0.7, z, 70, 0.3, 0.4, 0.3, 0.12);
            particles(p, ParticleTypes.LAVA, x, y + 0.7, z, 12, 0.3, 0.3, 0.3, 0);
            particles(p, ParticleTypes.TOTEM_OF_UNDYING, x, y + 1, z, 40, 0.4, 0.6, 0.4, 0.3);
            sound(p, SoundEvents.TURTLE_EGG_HATCH, 1.2F, 0.9F);
            sound(p, SoundEvents.FIRECHARGE_USE, 0.8F, 1.2F);
            title(p, "§6Flammèche est né !", "§eUn dragonnet de Braise s'attache à vous", 5, 70, 20);
        }
        int g = s.tick - HATCH_TICKS;
        float k = Math.min(1F, g / (float) GROW_TICKS);
        m.setGrowth(0.3F + (scale(1) - 0.3F) * (1 - (1 - k) * (1 - k)));
        if (g % 4 == 0) particles(p, ParticleTypes.FLAME, x, y + 0.5, z, 3, 0.4, 0.3, 0.4, 0.02);
        if (g == GROW_TICKS) {
            sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.7F, 1.8F);
            sound(p, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
            Scheduler.later(p.level(), 90, () -> {          // apres le titre, pour ne pas cacher le dragonnet
                if (p.isAlive() && !p.isRemoved()) MascotGuide.say(p, NAME, "Croaa ! ... Je veux dire : salut ! Je m'appelle Flammèche, et je te suis partout !");
            });
            SCENES.remove(p.getUUID());
            QuestManager.onAction(p, "lore:eclosion", 1);
        }
    }

    // ------------------------------------------------------------------ Eveil
    private static void awakenTick(ServerPlayer p, PlayerData d, MascotEntity m, Scene s) {
        double x = m.getX(), y = m.getY(), z = m.getZ();
        if (s.tick <= AWAKEN_TICKS) {
            float k = s.tick / (float) AWAKEN_TICKS;
            m.getNavigation().stop();
            m.setGrowth(s.from + (s.to - s.from) * k * k * (3 - 2 * k));
            double h = 2.6 * k * scale(s.stage) / scale(1);
            for (int arm = 0; arm < 3; arm++) {
                double a = s.tick * 0.35 + arm * Math.PI * 2 / 3;
                double r = 1.4 - 0.6 * k;
                particles(p, ParticleTypes.FLAME, x + Math.cos(a) * r, y + h, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            if (s.tick % 5 == 0) particles(p, ParticleTypes.END_ROD, x, y + 0.2, z, 3, 0.6, 0.1, 0.6, 0.05);
            if (s.tick == 1) {
                sound(p, SoundEvents.BEACON_ACTIVATE, 1F, 1.2F);
                title(p, "§6Éveil", "§eFlammèche revient à vos côtés", 10, 50, 10);
            }
            if (s.tick == AWAKEN_TICKS) {
                particles(p, ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.2, z, 80, 0.6, 1, 0.6, 0.4);
                particles(p, ParticleTypes.FLAME, x, y + 1, z, 60, 0.5, 0.6, 0.5, 0.15);
                sound(p, SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.7F);
                title(p, "§6Flammèche grandit !", "§eStade " + s.stage + " sur 5", 5, 70, 20);
                line(p, NAME, s.stage >= 5 ? "Regarde-moi ! Un vrai dragon de Braise, comme toi tu es devenu une légende !"
                        : "Tu as senti ça ? Ton Éveil m'a fait grandir aussi !");
            }
            return;
        }
        if (s.tick % 10 == 0) particles(p, ParticleTypes.SMALL_FLAME, x, y + 1, z, 2, 0.4, 0.4, 0.4, 0.01);
        if (s.tick >= AWAKEN_TICKS + AWAKEN_LINGER) {
            SCENES.remove(p.getUUID());
            if (!companion(p, d) && !holds(p, m)) MascotGuide.release(p);
        }
    }

    // ------------------------------------------------------------------ depart vers le Seuil
    private static void farewellTick(ServerPlayer p, MascotEntity m, Scene s) {
        if (s.tick == 1) {
            m.getNavigation().stop();
            m.setNoGravity(true);
        }
        double rise = 0.02 + 0.006 * s.tick;
        m.setDeltaMovement(0, 0, 0);
        m.snapTo(m.getX() + 0.03, m.getY() + rise, m.getZ(), m.getYRot() + 6, 0);
        particles(p, ParticleTypes.FLAME, m.getX(), m.getY() + 0.4, m.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        if (s.tick % 6 == 0) particles(p, ParticleTypes.CLOUD, m.getX(), m.getY(), m.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
        if (s.tick >= FAREWELL_TICKS) {
            particles(p, ParticleTypes.FIREWORK, m.getX(), m.getY() + 0.5, m.getZ(), 30, 0.3, 0.3, 0.3, 0.1);
            sound(p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7F, 1.3F);
            SCENES.remove(p.getUUID());
            MascotGuide.release(p);
        }
    }

    // ------------------------------------------------------------------ aura de saison
    private static void aura(ServerPlayer p, PlayerData d) {
        MascotEntity m = MascotGuide.mascot(p);
        if (m == null || !NAME.equals(m.displayName())) return;
        ParticleOptions type = null;
        for (String id : d.completedQuests) {
            QuestDef q = ConfigManager.quests().get(id);
            if (q == null || q.lore == null || !"Compagnon".equals(q.lore.type) || !QuestManager.windowOpen(q)) continue;
            type = q.lore.event.startsWith("Noël") ? ParticleTypes.SNOWFLAKE : ParticleTypes.CHERRY_LEAVES;
        }
        if (type != null) particles(p, type, m.getX(), m.getY() + 1.4, m.getZ(), 2, 0.5, 0.4, 0.5, 0.01);
    }

    // ------------------------------------------------------------------ outils
    private static void particles(ServerPlayer p, ParticleOptions type, double x, double y, double z, int n, double dx, double dy, double dz, double speed) {
        p.level().sendParticles(p, type, true, false, x, y, z, n, dx, dy, dz, speed);
    }

    /** Replique pendant une scene : dans le chat (la bulle de dialogue cacherait l'animation). */
    private static void line(ServerPlayer p, String speaker, String text) {
        p.sendSystemMessage(Component.literal(speaker + " : ").withColor(0xFFB050).append(Component.literal(text).withColor(0xFFF0D8)));
    }

    private static void sound(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s),
                SoundSource.NEUTRAL, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }

    private static void title(ServerPlayer p, String title, String sub, int in, int stay, int out) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        SCENES.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        SCENES.clear();
    }
}
