package com.mmorpg.client.model.chest;

import com.mmorpg.client.model.imported.ChestAnimState;
import com.mmorpg.client.model.imported.ImportedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/** Coffre anime importe (Blockbench) : modele + etoile lumineuse, pose comme les structures (axe Y inverse, 24 px de haut). */
public final class ChestModels {
    private static final int FULL_BRIGHT = 0xF000F0;
    private final ImportedModel main, glow;

    public ChestModels(String key) {
        main = new ImportedModel("chests", key);
        glow = new ImportedModel("chests", key, true);
    }

    /** Angle (degres) de rotation autour de Y pour un bloc oriente. */
    public static float yaw(Direction facing) {
        return switch (facing) {
            case EAST -> -90f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    public void submit(ChestAnimState state, PoseStack pose, SubmitNodeCollector collector, Identifier texture, int light, float yawDegrees, float scale) {
        pose.pushPose();
        pose.translate(0.5f, 0f, 0.5f);
        pose.rotateDegrees(Axis.YP, yawDegrees);
        pose.scale(scale, scale, scale);
        pose.translate(0f, 1.5f, 0f);
        pose.scale(-1f, -1f, 1f);
        collector.submitModel(main, state, pose, RenderTypes.entityTranslucent(texture), light, OverlayTexture.NO_OVERLAY, -1, null, 0);
        if (state.ticks >= 0)
            collector.submitModel(glow, state, pose, RenderTypes.entityTranslucentEmissive(texture), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0);
        pose.popPose();
    }

    /** Temps d'animation : ouverture (rebond puis couvercle + etoile) jusqu'a 2,0 s tenue, fermeture 2,4 s -> 2,75 s, puis repos. */
    public static float ticks(boolean open, float sinceOpen, float sinceClose) {
        if (open) return Math.min(5f + Math.max(sinceOpen, 0f), 40f);
        if (sinceClose >= 0 && 48f + sinceClose < 55f) return 48f + sinceClose;
        return -1f;
    }
}
