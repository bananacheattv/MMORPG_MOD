package com.mmorpg.client.fx;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.CastAnim;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Animations de lancement des competences : pose des bras et rotation du corps en 3e personne
 * (vue par tous les joueurs proches), mouvement de la main en 1re personne.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class CastAnimations {
    private record Anim(CastAnim kind, float start, int duration) {
    }

    private static final Map<Integer, Anim> ACTIVE = new HashMap<>();

    private CastAnimations() {
    }

    public static void start(int entityId, int anim, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ACTIVE.put(entityId, new Anim(CastAnim.byId(anim), mc.level.getGameTime(), Math.max(1, duration)));
    }

    private static float now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    /** Progression 0..1 de l'animation en cours, ou -1. */
    private static float progress(Anim a) {
        float p = (now() - a.start) / a.duration;
        return p >= 0 && p <= 1 ? p : -1;
    }

    private static Anim active(int id) {
        Anim a = ACTIVE.get(id);
        if (a == null) return null;
        if (progress(a) < 0) {
            ACTIVE.remove(id);
            return null;
        }
        return a;
    }

    @SubscribeEvent
    public static void onRegisterModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                apply(avatar.getId(), state);
            }
        });
    }

    private static void mainArm(AvatarRenderState s, HumanoidModel.ArmPose pose) {
        if (s.mainArm == HumanoidArm.RIGHT) s.rightArmPose = pose;
        else s.leftArmPose = pose;
    }

    private static void apply(int id, AvatarRenderState s) {
        Anim a = active(id);
        if (a == null) return;
        float p = progress(a);
        switch (a.kind) {
            case RAISE -> {
                if (p < 0.8f) mainArm(s, HumanoidModel.ArmPose.THROW_TRIDENT);
            }
            case CHANNEL, AIM -> mainArm(s, HumanoidModel.ArmPose.BOW_AND_ARROW);
            case BASH -> {
                if (p < 0.7f) mainArm(s, HumanoidModel.ArmPose.CROSSBOW_HOLD);
            }
            case SPIN -> s.bodyRot += 720f * FxDraw.ease(p);
            case SLAM -> {
                if (p < 0.45f) {
                    s.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                    s.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                }
            }
            case LEAP -> {
                s.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                s.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
            }
        }
    }

    /** Vue a la 1re personne : la main accompagne le geste. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getHand() != InteractionHand.MAIN_HAND) return;
        Anim a = active(mc.player.getId());
        if (a == null) return;
        float p = progress(a);
        PoseStack ps = event.getPoseStack();
        switch (a.kind) {
            case RAISE, LEAP -> {
                float k = Mth.sin(p * Mth.PI);
                ps.translate(0, 0.28f * k, -0.12f * k);
                ps.rotate(Axis.XP.rotationDegrees(-40 * k));
            }
            case CHANNEL, AIM -> {
                float k = Math.min(1, p * 4) * (p > 0.8f ? (1 - p) / 0.2f : 1);
                ps.translate(-0.18f * k, 0.1f * k, -0.28f * k);
            }
            case BASH -> {
                float k = Mth.sin(p * Mth.PI);
                ps.translate(-0.12f * k, 0, -0.5f * k);
            }
            case SLAM -> {
                if (p < 0.45f) {
                    float k = p / 0.45f;
                    ps.translate(0, 0.32f * k, 0);
                    ps.rotate(Axis.XP.rotationDegrees(-55 * k));
                } else {
                    float k = (p - 0.45f) / 0.55f;
                    ps.translate(0, 0.32f * (1 - k) - 0.15f * Mth.sin(k * Mth.PI), 0);
                    ps.rotate(Axis.XP.rotationDegrees(-55 * (1 - k) + 35 * Mth.sin(k * Mth.PI)));
                }
            }
            case SPIN -> {
                ps.rotate(Axis.YP.rotationDegrees(25 * Mth.sin(p * Mth.TWO_PI * 2)));
            }
        }
    }
}
