package com.mmorpg.client.model.pet;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.PetEntity;
import com.mmorpg.pet.PetType;
import com.mmorpg.pet.anim.BebeSlimeAnims;
import com.mmorpg.pet.anim.ChouetteSageAnims;
import com.mmorpg.pet.anim.DragonnetNeantAnims;
import com.mmorpg.pet.anim.FeeLumineuseAnims;
import com.mmorpg.pet.anim.FeuFolletAnims;
import com.mmorpg.pet.anim.GolemPocheAnims;
import com.mmorpg.pet.anim.LoupSpectralAnims;
import com.mmorpg.pet.anim.PhenixDoreAnims;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rendu 3D anime des familiers (modeles Blockbench de art/familiers_blockbench) : texture
 * textures/entity/pet/<cle>.png (translucide : ailes de fee, membranes, enveloppe du slime) et, si elle existe,
 * couche lumineuse <cle>_emissive.png toujours eclairee (flammes, yeux, cristaux, runes).
 */
public class PetRenderer extends EntityRenderer<PetEntity, PetRenderState> {
    private static final int FULL_BRIGHT = 0xF000F0;

    private record Entry(PetModel model, Identifier texture, @Nullable Identifier emissive, float scale) {
    }

    private final Map<PetType, Entry> entries = new EnumMap<>(PetType.class);

    public PetRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.25F;
        add(c, PetType.FEU_FOLLET, FeuFolletModel.LAYER, FeuFolletModel.animations(), "feu_follet", 0.7F,
                new PetModel.Profile(-1, -1, -1, -1, FeuFolletAnims.CANALISATION, FeuFolletAnims.FLOTTEMENT, FeuFolletAnims.DEPLACEMENT,
                        FeuFolletAnims.JOIE, 0.12F, 0.0F, true, FeuFolletAnims.LENGTH_TICKS[FeuFolletAnims.JOIE]));
        add(c, PetType.SLIME, BebeSlimeModel.LAYER, BebeSlimeModel.animations(), "bebe_slime", 0.8F,
                new PetModel.Profile(BebeSlimeAnims.REPOS, BebeSlimeAnims.SAUT, -1, -1, BebeSlimeAnims.REPOS_APLATI, -1, -1,
                        BebeSlimeAnims.DOUBLE_REBOND, 0.08F, 0.0F, false, BebeSlimeAnims.LENGTH_TICKS[BebeSlimeAnims.DOUBLE_REBOND]));
        add(c, PetType.CHOUETTE, ChouetteSageModel.LAYER, ChouetteSageModel.animations(), "chouette_sage", 0.8F,
                new PetModel.Profile(ChouetteSageAnims.REPOS, ChouetteSageAnims.PETITS_PAS, -1, -1, ChouetteSageAnims.SOMMEIL,
                        ChouetteSageAnims.VOL, ChouetteSageAnims.VOL, -1, 0.1F, 0.0F, false, 0));
        add(c, PetType.LOUP_SPECTRAL, LoupSpectralModel.LAYER, LoupSpectralModel.animations(), "loup_spectral", 0.8F,
                new PetModel.Profile(LoupSpectralAnims.REPOS, LoupSpectralAnims.MARCHE, LoupSpectralAnims.COURSE, LoupSpectralAnims.ASSIS,
                        LoupSpectralAnims.SOMMEIL, -1, -1, LoupSpectralAnims.QUEUE_JOYEUSE, 0.12F, 0.24F, false, 30));
        add(c, PetType.GOLEM, GolemPocheModel.LAYER, GolemPocheModel.animations(), "golem_poche", 0.75F,
                new PetModel.Profile(GolemPocheAnims.REPOS, GolemPocheAnims.MARCHE, -1, -1, GolemPocheAnims.SOMMEIL, -1, -1,
                        GolemPocheAnims.JOIE, 0.1F, 0.0F, false, GolemPocheAnims.LENGTH_TICKS[GolemPocheAnims.JOIE]));
        add(c, PetType.FEE, FeeLumineuseModel.LAYER, FeeLumineuseModel.animations(), "fee_lumineuse", 0.75F,
                new PetModel.Profile(-1, -1, -1, -1, -1, FeeLumineuseAnims.FLOTTEMENT, FeeLumineuseAnims.DEPLACEMENT, FeeLumineuseAnims.JOIE,
                        0.12F, 0.0F, false, FeeLumineuseAnims.LENGTH_TICKS[FeeLumineuseAnims.JOIE]));
        add(c, PetType.PHENIX, PhenixDoreModel.LAYER, PhenixDoreModel.animations(), "phenix_dore", 0.7F,
                new PetModel.Profile(PhenixDoreAnims.REPOS, -1, -1, -1, -1, PhenixDoreAnims.VOL, PhenixDoreAnims.PLANE, PhenixDoreAnims.JOIE,
                        0.12F, 0.0F, false, PhenixDoreAnims.LENGTH_TICKS[PhenixDoreAnims.JOIE]));
        add(c, PetType.DRAGONNET, DragonnetNeantModel.LAYER, DragonnetNeantModel.animations(), "dragonnet_neant", 0.8F,
                new PetModel.Profile(DragonnetNeantAnims.REPOS, DragonnetNeantAnims.MARCHE, -1, -1, -1, DragonnetNeantAnims.VOL,
                        DragonnetNeantAnims.VOL, DragonnetNeantAnims.JOIE, 0.1F, 0.0F, false,
                        DragonnetNeantAnims.LENGTH_TICKS[DragonnetNeantAnims.JOIE]));
    }

    private void add(EntityRendererProvider.Context c, PetType type, ModelLayerLocation layer, AnimationDefinition[] anims, String key,
                     float scale, PetModel.Profile profile) {
        Identifier emissive = MMORPG.id("textures/entity/pet/" + key + "_emissive.png");
        boolean hasGlow = Minecraft.getInstance().getResourceManager().getResource(emissive).isPresent();
        entries.put(type, new Entry(new PetModel(c.bakeLayer(layer), anims, profile), MMORPG.id("textures/entity/pet/" + key + ".png"),
                hasGlow ? emissive : null, scale));
    }

    @Override
    public PetRenderState createRenderState() {
        return new PetRenderState();
    }

    @Override
    public void extractRenderState(PetEntity entity, PetRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.type = entity.petType();
        state.speed = entity.animSpeed;
        state.still = entity.stillTicks + partialTicks;
        state.fly = entity.flyBlend;
        state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
    }

    @Override
    public void submit(PetRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Entry e = state.type == null ? null : entries.get(state.type);
        if (e != null) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
            poseStack.scale(e.scale(), e.scale(), e.scale());
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            collector.submitModel(e.model(), state, poseStack, RenderTypes.entityTranslucent(e.texture()), state.lightCoords,
                    OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
            if (e.emissive() != null) {
                collector.submitModel(e.model(), state, poseStack, RenderTypes.entityTranslucentEmissive(e.emissive()), FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, -1, null, 0);
            }
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
