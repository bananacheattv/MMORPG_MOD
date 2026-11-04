package com.mmorpg.client.fx;

import com.mmorpg.item.RpgEquipment;
import com.mmorpg.item.RpgItemDef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Lueur et particules pulsantes autour des armes de niveau 50+, plus marquees avec la rarete et le niveau. */
public final class WeaponGlowFx {
    public static final int MIN_LEVEL = 50;

    private WeaponGlowFx() {
    }

    /** Intensite 0..~3 selon rarete et niveau (0 = aucun effet). */
    static float power(RpgItemDef def) {
        if (def == null || def.slot() != RpgItemDef.Slot.WEAPON || def.level() < MIN_LEVEL) return 0;
        return 0.5f + def.rarity().ordinal() * 0.35f + Math.min(def.level() - MIN_LEVEL, 50) / 50f * 0.75f;
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        long t = level.getGameTime();
        for (Player p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(mc.player) > 32 * 32) continue;
            ItemStack s = p.getMainHandItem();
            if (!(s.getItem() instanceof RpgEquipment eq)) continue;
            float pw = power(eq.rpgDef());
            if (pw <= 0) continue;
            play(level, p, pw, eq.rpgDef().rarity().color & 0xFFFFFF, t, p == mc.player && mc.options.getCameraType().isFirstPerson());
        }
    }

    private static void play(ClientLevel level, Player p, float pw, int rgb, long t, boolean firstPerson) {
        // pulsation lente : la densite et la taille varient en sinus
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.2);
        double side = p.getMainArm() == HumanoidArm.RIGHT ? -1 : 1;
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw) * 0.45 + Math.cos(yaw) * 0.4 * side * -1;
        double fz = Math.cos(yaw) * 0.45 + Math.sin(yaw) * 0.4 * side * -1;
        Vec3 c = firstPerson
                ? p.getEyePosition().add(p.getLookAngle().scale(0.9)).add(0, -0.35, 0)
                : new Vec3(p.getX() + fx, p.getY() + 1.0, p.getZ() + fz);
        var r = level.getRandom();
        float size = (float) (0.5 + pw * 0.35 + pulse * 0.3);
        int n = pw > 1.6 ? 2 : 1;
        if (t % 2 == 0 || pw > 1.2) {
            for (int i = 0; i < n; i++) {
                level.addParticle(new DustParticleOptions(rgb, size), c.x + (r.nextDouble() - .5) * .4, c.y + (r.nextDouble() - .3) * .6,
                        c.z + (r.nextDouble() - .5) * .4, 0, 0.01, 0);
            }
        }
        // etincelles : seulement aux hauts niveaux/raretes, rythmees par la pulsation
        if (pw > 1.0 && pulse > 0.8 && t % 3 == 0) {
            level.addParticle(pw > 2.0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, c.x + (r.nextDouble() - .5) * .3, c.y + r.nextDouble() * .5,
                    c.z + (r.nextDouble() - .5) * .3, 0, 0.02, 0);
        }
    }
}
