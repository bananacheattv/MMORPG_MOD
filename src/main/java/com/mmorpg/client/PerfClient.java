package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Reglages de performance cote client : limiteur de particules adaptatif et distance d'affichage des PNJ. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class PerfClient {
    private static final float MIN_KEEP = 0.15F;
    private static volatile float keep = 1F;       // part des particules conservees (1 = toutes)
    private static float acc;                       // accumulateur : choix regulier, sans hasard
    private static float smoothFps = 60F;
    public static long dropped, kept;

    private PerfClient() {
    }

    public static float keepRatio() {
        return keep;
    }

    /** Appelee pour chaque particule ajoutee : faux si elle doit etre ecartee pour tenir les FPS. */
    public static boolean allowParticle() {
        float k = keep;
        if (k >= 0.999F) return true;
        acc += k;
        if (acc >= 1F) {
            acc -= 1F;
            kept++;
            return true;
        }
        dropped++;
        return false;
    }

    /** Vrai si l'entite est assez proche pour etre dessinee selon la distance choisie pour les PNJ. */
    public static boolean withinNpcDistance(Entity e, double camX, double camY, double camZ) {
        double d = ClientConfig.NPC_RENDER_DISTANCE.get();
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        return dx * dx + dy * dy + dz * dz <= d * d;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.ADAPTIVE_PARTICLES.get() || mc.level == null) {
            keep = 1F;
            return;
        }
        int fps = mc.getFps();
        smoothFps += (fps - smoothFps) * 0.1F;
        int target = ClientConfig.PARTICLE_TARGET_FPS.get();
        if (smoothFps < target) keep = Math.max(MIN_KEEP, keep - 0.02F);                 // sous la cible : moins de particules
        else if (smoothFps > target + 12) keep = Math.min(1F, keep + 0.01F);            // confortablement au-dessus : on restaure
    }
}
