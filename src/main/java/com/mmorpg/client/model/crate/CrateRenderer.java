package com.mmorpg.client.model.crate;

import com.mmorpg.block.crate.CrateBlock;
import com.mmorpg.block.crate.CrateBlockEntity;
import com.mmorpg.crafting.ForgeRecipes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Au-dessus de chaque caisse : son nom (texte flottant face au joueur) et, pendant l'ouverture, une roue d'objets
 * qui defile en ralentissant puis s'arrete sur le gain (agrandi, qui flotte et tourne).
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int VISIBLE = 3;               // objets de chaque cote du centre

    public static class State extends BlockEntityRenderState {
        public String label = "";
        public int color = 0xFFFFFF;
        public boolean spinning, showing;
        public float position, showTime, time;
        public final List<ItemStackRenderState> items = new ArrayList<>();
        public final List<Float> offsets = new ArrayList<>();
        public String winner = "";
    }

    private final ItemModelResolver items;
    private final Font font;

    public CrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
        this.font = ctx.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static float easeOut(float t) {
        return 1 - (float) Math.pow(1 - t, 4);
    }

    @Override
    public void extractRenderState(CrateBlockEntity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, breakProgress);
        if (be.getBlockState().getBlock() instanceof CrateBlock block) {
            state.label = block.tier.label;
            state.color = block.tier.color;
        }
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        float elapsed = now - be.startTick + partial;
        state.time = (now + partial) / 20f;
        state.items.clear();
        state.offsets.clear();
        state.spinning = !be.reel.isEmpty() && elapsed >= 0 && elapsed < CrateBlockEntity.SPIN_TICKS;
        state.showing = !be.reel.isEmpty() && elapsed >= CrateBlockEntity.SPIN_TICKS && elapsed < CrateBlockEntity.SPIN_TICKS + CrateBlockEntity.SHOW_TICKS;
        state.winner = be.winnerName;
        if (state.spinning) {
            float p = be.resultIndex * easeOut(elapsed / CrateBlockEntity.SPIN_TICKS);
            int center = Mth.floor(p);
            for (int k = -VISIBLE; k <= VISIBLE + 1; k++) {
                int idx = center + k;
                if (idx < 0 || idx >= be.reel.size()) continue;
                state.items.add(resolve(be, be.reel.get(idx), idx));
                state.offsets.add(idx - p);
            }
        } else if (state.showing) {
            state.showTime = elapsed - CrateBlockEntity.SPIN_TICKS;
            state.items.add(resolve(be, be.reel.get(Math.min(be.resultIndex, be.reel.size() - 1)), 0));
            state.offsets.add(0f);
        }
    }

    private ItemStackRenderState resolve(CrateBlockEntity be, String id, int seed) {
        Item item = ForgeRecipes.resolveItem(id);
        ItemStackRenderState s = new ItemStackRenderState();
        items.updateForTopItem(s, new ItemStack(item == null || item == Items.AIR ? Items.GOLD_NUGGET : item), ItemDisplayContext.FIXED,
                be.getLevel(), null, seed);
        return s;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        // ---- nom de la caisse
        float labelY = state.spinning || state.showing ? 3.25f : 2.05f;
        text(pose, collector, camera, labelY, Component.literal("✦ " + state.label + " ✦"), 0xFF000000 | state.color, 0.03f);
        if (!state.spinning && !state.showing) {
            text(pose, collector, camera, labelY - .28f, Component.literal("Clic droit pour ouvrir"), 0xFFD8D0C0, 0.018f);
            return;
        }
        // ---- roue
        pose.pushPose();
        pose.translate(0.5f, 2.35f, 0.5f);
        pose.rotate(camera.orientation);
        if (state.spinning) {
            for (int i = 0; i < state.items.size(); i++) {
                float off = state.offsets.get(i);
                float d = Math.abs(off);
                if (d > VISIBLE + .5f) continue;
                // roue : les objets suivent un arc (x = sin, z = cos) et retrecissent sur les bords
                float angle = off * 0.42f;
                float scale = 0.55f * (1.15f - d * 0.18f) * (d < .5f ? 1.25f : 1f);
                pose.pushPose();
                pose.translate(Mth.sin(angle) * 1.25f, 0, (Mth.cos(angle) - 1) * 1.25f);
                pose.rotate(Axis.YP, -angle);
                pose.scale(scale, scale, scale);
                state.items.get(i).submit(pose, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
            pose.pushPose();      // fleche indiquant le gain
            pose.translate(0, -0.55f, 0.05f);
            pose.scale(0.035f, -0.035f, 0.035f);
            String arrow = "▲";
            collector.submitText(pose, -font.width(arrow) / 2f, 0, Component.literal(arrow).getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    FULL_BRIGHT, 0xFF000000 | state.color, 0, 0);
            pose.popPose();
        } else if (!state.items.isEmpty()) {
            float t = state.showTime;
            float pop = Math.min(1, t / 6f);
            float scale = 0.8f + 0.35f * pop + 0.05f * Mth.sin(state.time * 6);
            pose.pushPose();
            pose.translate(0, 0.08f * Mth.sin(state.time * 3), 0);
            pose.rotateDegrees(Axis.YP, t * 8);
            pose.scale(scale, scale, scale);
            state.items.get(0).submit(pose, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
        if (state.showing) text(pose, collector, camera, 1.75f, Component.literal(state.winner), 0xFFFFE070, 0.022f);
    }

    private void text(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, float y, Component text, int color, float scale) {
        pose.pushPose();
        pose.translate(0.5f, y, 0.5f);
        pose.rotate(camera.orientation);
        pose.scale(scale, -scale, scale);
        float w = font.width(text);
        collector.submitText(pose, -w / 2, 0, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, color, 0x50000000, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrateBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 4, 0);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
