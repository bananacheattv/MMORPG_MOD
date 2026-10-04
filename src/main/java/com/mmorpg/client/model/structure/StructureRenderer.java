package com.mmorpg.client.model.structure;

import com.mmorpg.MMORPG;
import com.mmorpg.block.AltarBlock;
import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.StructureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rendu des structures multiblocs (autel, teleporteur, forge) depuis le bloc fonctionnel : modele Blockbench complet
 * oriente selon le bloc, couche lumineuse (runes, cristaux, flammes) plus intense quand la structure est active, et
 * surface du portail du teleporteur, translucide, affichee seulement quand il est actif.
 * Actif : autel = invocation en cours (etat du bloc) ; teleporteur et forge = un joueur a moins de 7 blocs.
 */
public class StructureRenderer implements BlockEntityRenderer<StructureBlockEntity, StructureRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;

    public static class State extends BlockEntityRenderState {
        public MultiblockKind kind = MultiblockKind.FORGE;
        public Direction facing = Direction.NORTH;
        public boolean active;
        public float time;
        public float sinceActive;
    }

    private record Models(StructureModel main, StructureModel glow, @Nullable StructureModel portal, Identifier texture, Identifier emissive) {
    }

    private final Map<MultiblockKind, Models> models = new EnumMap<>(MultiblockKind.class);

    public StructureRenderer(BlockEntityRendererProvider.Context c) {
        models.put(MultiblockKind.AUTEL, models(c, AutelInvocationModel.LAYER, AutelInvocationModel.animations(),
                new int[]{AutelInvocationAnims.INACTIF, AutelInvocationAnims.ACTIVATION, AutelInvocationAnims.ACTIF}, "autel_invocation", null));
        models.put(MultiblockKind.TELEPORTEUR, models(c, TeleporteurModel.LAYER, TeleporteurModel.animations(),
                new int[]{TeleporteurAnims.INACTIF, TeleporteurAnims.ACTIVATION, TeleporteurAnims.ACTIF}, "teleporteur", "portail",
                "plateforme", "arche", "cristal", "pupitre"));
        models.put(MultiblockKind.FORGE, models(c, ForgeArcaniqueModel.LAYER, ForgeArcaniqueModel.animations(),
                new int[]{ForgeArcaniqueAnims.ETEINTE, ForgeArcaniqueAnims.ALLUMAGE, ForgeArcaniqueAnims.ACTIVE}, "forge_arcanique", null));
    }

    private static Models models(BlockEntityRendererProvider.Context c, ModelLayerLocation layer, AnimationDefinition[] defs, int[] anims,
                                 String key, @Nullable String portal, String... siblings) {
        return new Models(
                new StructureModel(c.bakeLayer(layer), defs, anims, StructureModel.Pass.MAIN, portal, siblings),
                new StructureModel(c.bakeLayer(layer), defs, anims, StructureModel.Pass.GLOW, portal, siblings),
                portal == null ? null : new StructureModel(c.bakeLayer(layer), defs, anims, StructureModel.Pass.PORTAL, portal, siblings),
                MMORPG.id("textures/entity/structure/" + key + ".png"),
                MMORPG.id("textures/entity/structure/" + key + "_emissive.png"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StructureBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        MultiblockKind kind = MultiblockKind.of(bs.getBlock());
        if (kind == null) return;
        state.kind = kind;
        state.facing = bs.hasProperty(MultiblockMasterBlock.FACING) ? bs.getValue(MultiblockMasterBlock.FACING) : Direction.NORTH;
        boolean active;
        if (kind == MultiblockKind.AUTEL) {
            active = bs.getValue(AltarBlock.ACTIVE);
        } else {
            var player = Minecraft.getInstance().player;
            active = player != null && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) < 7 * 7;
        }
        float time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() + partialTicks) / 20.0F;
        if (active != be.clientActive) {
            be.clientActive = active;
            be.clientActiveSince = time;
        }
        state.active = active;
        state.time = time;
        state.sinceActive = time - be.clientActiveSince;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Models m = models.get(state.kind);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, switch (state.facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        });
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(m.main(), state, poseStack, RenderTypes.entityCutout(m.texture()), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0);
        float glow = state.active ? 0.8F + 0.2F * Mth.sin(state.time * 4.0F) : 0.35F;
        collector.submitModel(m.glow(), state, poseStack, RenderTypes.entityTranslucentEmissive(m.emissive()), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ARGB.white(glow), null, 0);
        if (m.portal() != null && state.active) {
            collector.submitModel(m.portal(), state, poseStack, RenderTypes.entityTranslucentEmissive(m.texture()), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    -1, null, 0);
        }
        poseStack.popPose();
    }

    /** Toute l'emprise de la structure (sinon elle disparaitrait quand le bloc fonctionnel sort de l'ecran). */
    @Override
    public AABB getRenderBoundingBox(StructureBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(4.0, 0.0, 4.0).expandTowards(0.0, 5.0, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
