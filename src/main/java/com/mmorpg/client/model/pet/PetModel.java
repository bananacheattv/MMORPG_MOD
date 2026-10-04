package com.mmorpg.client.model.pet;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Familier issu de Blockbench (art/familiers_blockbench). Les animations en boucle sont melangees selon l'etat :
 * repos / marche / course au sol, vol sur place / deplacement en vol, assis puis endormi quand le familier reste
 * immobile, et une animation de joie a l'apparition. Toutes s'additionnent a la pose de repos, comme dans Blockbench.
 */
public class PetModel extends EntityModel<PetRenderState> {
    /**
     * Indices des animations (-1 si absente). moveRef / fastRef : vitesses (blocs par tick) de pleine marche / de course ;
     * airSleep : l'animation de "sommeil" se joue aussi en vol (canalisation du feu follet) ; happyTicks : duree de la joie.
     */
    public record Profile(int idle, int move, int fast, int sit, int sleep, int flyIdle, int fly, int happy, float moveRef, float fastRef,
                          boolean airSleep, int happyTicks) {
    }

    public static final int SIT_DELAY = 100;
    public static final int SLEEP_DELAY = 300;
    private final KeyframeAnimation[] anims;
    private final AnimationDefinition[] defs;
    private final Profile profile;

    public PetModel(ModelPart root, AnimationDefinition[] definitions, Profile profile) {
        super(root, RenderTypes::entityTranslucent);
        this.profile = profile;
        this.defs = definitions;
        this.anims = new KeyframeAnimation[definitions.length];
        for (int i = 0; i < definitions.length; i++) {
            this.anims[i] = definitions[i].bake(root);
        }
    }

    private static float ramp(float v, float start, float len) {
        return Math.max(0.0F, Math.min(1.0F, (v - start) / len));
    }

    /** Joue une animation en boucle (meme si elle n'est pas definie comme boucle, ex. le saut du slime). */
    private void loop(int i, float ticks, float weight) {
        if (i < 0 || weight <= 0.001F) return;
        long ms = (long) (ticks * 50.0F);
        long len = (long) (defs[i].lengthInSeconds() * 1000.0F);
        anims[i].apply(len > 0 ? ms % len : ms, weight);
    }

    @Override
    public void setupAnim(PetRenderState s) {
        super.setupAnim(s);
        Profile p = profile;
        float t = s.ageInTicks;
        float moveW = ramp(s.speed, 0.0F, p.moveRef());
        float fastW = p.fast() >= 0 ? ramp(s.speed, p.fastRef(), p.fastRef() * 0.5F) : 0.0F;
        float sleepW = p.sleep() >= 0 ? ramp(s.still, SLEEP_DELAY, 25.0F) : 0.0F;
        float sitW = p.sit() >= 0 ? ramp(s.still, SIT_DELAY, 15.0F) * (1.0F - sleepW) : 0.0F;
        float calm = 1.0F - sitW - sleepW;
        float happyW = p.happy() >= 0 && t < p.happyTicks() ? Math.min(1.0F, (p.happyTicks() - t) / 6.0F) : 0.0F;
        float base = 1.0F - 0.7F * happyW;
        float air = p.flyIdle() >= 0 || p.fly() >= 0 ? s.fly : 0.0F;
        float ground = 1.0F - air;
        // au sol
        loop(p.idle(), t, ground * (1.0F - moveW) * calm * base);
        loop(p.sit(), t, ground * (1.0F - moveW) * sitW * base);
        loop(p.sleep(), t, ground * (1.0F - moveW) * sleepW * base);
        loop(p.move(), t, ground * moveW * (1.0F - fastW) * base);
        loop(p.fast(), t, ground * moveW * fastW * base);
        // en vol
        float hover = air * (1.0F - moveW) * base;
        loop(p.flyIdle() >= 0 ? p.flyIdle() : p.fly(), t, p.airSleep() ? hover * (1.0F - sleepW) : hover);
        if (p.airSleep()) loop(p.sleep(), t, hover * sleepW);
        loop(p.fly() >= 0 ? p.fly() : p.flyIdle(), t, air * moveW * base);
        // joie a l'apparition (une seule fois)
        if (happyW > 0.0F) {
            anims[p.happy()].apply((long) (t * 50.0F) % Math.max(1L, (long) (defs[p.happy()].lengthInSeconds() * 1000.0F)), happyW);
        }
    }
}
