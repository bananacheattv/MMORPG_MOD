package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.cinematic.CinematicClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/**
 * Musique d'ambiance des donjons, des mines et des plaines (assets/mmorpg/sounds/music/donjon_*.ogg, voir tools/build_music.py) : le serveur indique la piste
 * quand le joueur entre dans un donjon, dans l'antre d'un boss, ou en sort (piste vide). Les pistes se succedent en fondu, et les
 * musiques de Minecraft sont coupees tant qu'une piste joue.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class DungeonMusicClient {
    private static String wanted = "";
    private static Track music;
    /** Ticks consecutifs ou le moteur de son ne joue plus la piste (Minecraft coupe tous les sons au changement de dimension). */
    private static int silent;

    private DungeonMusicClient() {
    }

    /** Piste demandee par le serveur (nom d'un donjon, « boss », « mines », « plaines », ou vide pour arreter). */
    public static void set(String track) {
        wanted = track == null ? "" : track;
    }

    /** Test automatique : piste voulue et piste qui joue. */
    public static String debugState() {
        return "voulue « " + wanted + " » ; joue « " + (music == null ? "" : music.track + (music.leaving() ? " (fondu de sortie)" : "")) + " »";
    }

    private static boolean active(Minecraft mc) {
        return !wanted.isEmpty() && mc.level != null && mc.player != null && !CinematicClient.active();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = active(mc);
        if (music != null && music.isStopped()) music = null;
        if (music != null) {
            if (mc.getSoundManager().isActive(music)) silent = 0;
            else if (++silent > 20) {
                music = null;
                silent = 0;
            }
        }
        if (music != null && (!want || !music.track.equals(wanted))) music.fadeOut();
        if (want && (music == null || music.leaving())) {
            if (music == null || !music.track.equals(wanted)) {
                Track fresh = new Track(wanted);
                mc.getSoundManager().play(fresh);
                MMORPG.LOGGER.info("[MMORPG] Musique de donjon : {} (lecture = {})", wanted, mc.getSoundManager().isActive(fresh));
                if (music != null) music.fadeOut();
                music = fresh;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        wanted = "";
        if (music != null) music.fadeOut();
    }

    /** Tant qu'une piste de donjon est voulue, aucune autre musique que la sienne. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC && !(event.getSound() instanceof Track) && active(Minecraft.getInstance())) {
            event.setSound(null);
        }
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private static final float FADE_IN_TICKS = 100F;
        private static final float FADE_OUT_TICKS = 60F;
        final String track;
        private int age;
        private int out = -1;

        Track(String track) {
            super(SoundEvent.createVariableRangeEvent(MMORPG.id(track.equals("mines") || track.equals("plaines") ? track + ".ambiance" : "dungeon." + track)), SoundSource.MUSIC, RandomSource.create());
            this.track = track;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0001F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void fadeOut() {
            if (out < 0) out = 0;
        }

        boolean leaving() {
            return out >= 0;
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
            this.volume = Math.max(0.0001F, v * 0.85F * ClientConfig.ambientMusic());
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
