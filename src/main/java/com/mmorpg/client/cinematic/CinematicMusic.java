package com.mmorpg.client.cinematic;

import com.mmorpg.MMORPG;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;

/**
 * Musique de la cinematique (assets/mmorpg/sounds/music/aeloria_theme.ogg, generee par tools/gen_music.py) : jouee en flux dans la
 * categorie « musique » (le volume de musique du joueur s'applique), avec fondu d'entree et fondu de sortie quand la scene se termine
 * ou est sautee.
 */
final class CinematicMusic extends AbstractTickableSoundInstance {
    private static final float FADE_IN_TICKS = 50F;
    private int age;
    private int fadeOutTicks = -1;
    private int fadeOutLength = 24;

    CinematicMusic() {
        super(SoundEvent.createVariableRangeEvent(MMORPG.id("cinematique.theme")), SoundSource.MUSIC, RandomSource.create());
        this.looping = false;
        this.delay = 0;
        this.volume = 0.0001F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    /** Lance le fondu de sortie (en ticks) ; la piste s'arrete a la fin. */
    void fadeOut(int ticks) {
        if (fadeOutTicks < 0) {
            fadeOutTicks = 0;
            fadeOutLength = Math.max(1, ticks);
        }
    }

    @Override
    public void tick() {
        age++;
        float v = Mth.clamp(age / FADE_IN_TICKS, 0F, 1F);
        v = v * v * (3F - 2F * v);
        if (fadeOutTicks >= 0) {
            fadeOutTicks++;
            float f = 1F - Mth.clamp((float) fadeOutTicks / fadeOutLength, 0F, 1F);
            v *= f;
            if (fadeOutTicks >= fadeOutLength) {
                this.stop();
                return;
            }
        }
        this.volume = Math.max(0.0001F, v);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
