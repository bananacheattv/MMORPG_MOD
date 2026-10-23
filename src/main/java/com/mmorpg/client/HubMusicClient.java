package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.cinematic.CinematicClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/**
 * Musiques d'ambiance du hub (assets/mmorpg/sounds/music/aeloria_hub.ogg, generee par tools/gen_hub_music.py) et de la Cite d'Aeloria,
 * le grand Spawn (aeloria_cite.ogg, tools/gen_cite_music.py) ; remplacez un fichier pour utiliser votre propre piece. Jouee en boucle dans
 * la categorie « musique » tant que le joueur est dans la dimension correspondante, hors cinematique. Fondus d'entree et de sortie ;
 * les musiques vanilla sont coupees pendant ce temps.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class HubMusicClient {
    private static final ResourceKey<Level> HUB = ResourceKey.create(Registries.DIMENSION, MMORPG.id("hub"));
    private static final ResourceKey<Level> SPAWN = ResourceKey.create(Registries.DIMENSION, MMORPG.id("spawn"));
    private static HubMusic music;
    private static String musicEvent = "";

    private HubMusicClient() {
    }

    /** Evenement sonore voulu pour la dimension du joueur (vide : aucune musique d'Aeloria). */
    private static String wantedEvent(Minecraft mc) {
        if (mc.level == null || mc.player == null || CinematicClient.active()) return "";
        if (mc.level.dimension().equals(HUB)) return "hub.ambiance";
        if (mc.level.dimension().equals(SPAWN)) return "cite.ambiance";
        return "";
    }

    private static boolean wanted(Minecraft mc) {
        return !wantedEvent(mc).isEmpty();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        String wantedSound = wantedEvent(mc);
        boolean want = !wantedSound.isEmpty();
        if (music != null && want && !wantedSound.equals(musicEvent)) music.fadeOut();                       // autre monde : la piece change
        if (want && (music == null || music.isStopped()) ) {
            music = new HubMusic(wantedSound);
            musicEvent = wantedSound;
            mc.getSoundManager().play(music);
            MMORPG.LOGGER.info("[MMORPG] Musique {} : lecture demarree = {}", wantedSound, mc.getSoundManager().isActive(music));
        } else if (!want && music != null) {
            music.fadeOut();
            if (music.isStopped()) music = null;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (music != null) music.fadeOut();
    }

    /** Dans le hub, aucune autre musique que la sienne. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC && !(event.getSound() instanceof HubMusic)
                && wanted(Minecraft.getInstance())) {
            event.setSound(null);
        }
    }

    private static final class HubMusic extends AbstractTickableSoundInstance {
        private static final float FADE_IN_TICKS = 160F;
        private static final float FADE_OUT_TICKS = 50F;
        private int age;
        private int out = -1;

        HubMusic(String event) {
            super(SoundEvent.createVariableRangeEvent(MMORPG.id(event)), SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void fadeOut() {
            if (out < 0) out = 0;
        }

        @Override
        public void tick() {
            age++;
            float v = Mth.clamp(age / FADE_IN_TICKS, 0F, 1F);
            v = v * v * (3F - 2F * v);
            if (out >= 0) {
                out++;
                v *= 1F - Mth.clamp(out / FADE_OUT_TICKS, 0F, 1F);
                if (out >= FADE_OUT_TICKS) {
                    this.stop();
                    return;
                }
            }
            this.volume = Math.max(0.0001F, v * 0.9F * ClientConfig.ambientMusic());
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
