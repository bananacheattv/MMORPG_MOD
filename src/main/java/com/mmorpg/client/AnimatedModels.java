package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.model.AnimatedBossModel;
import com.mmorpg.client.model.AnimatedBossRenderer;
import com.mmorpg.client.model.armor.ArmorModels;
import com.mmorpg.client.model.boss.AvatarNeantModel;
import com.mmorpg.client.model.boss.IgnisModel;
import com.mmorpg.client.model.boss.LicheAncienneModel;
import com.mmorpg.client.model.boss.RoiGobelinModel;
import com.mmorpg.client.model.boss.TitanGlaceModel;
import com.mmorpg.client.model.structure.AutelInvocationModel;
import com.mmorpg.client.model.structure.ForgeArcaniqueModel;
import com.mmorpg.client.model.structure.StructureRenderer;
import com.mmorpg.client.model.structure.TeleporteurModel;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.entity.boss.FrostTitanBoss;
import com.mmorpg.entity.boss.GoblinKingBoss;
import com.mmorpg.entity.boss.IgnisBoss;
import com.mmorpg.entity.boss.LichBoss;
import com.mmorpg.entity.boss.VoidAvatarBoss;
import com.mmorpg.entity.boss.anim.AvatarNeantAnims;
import com.mmorpg.entity.boss.anim.IgnisAnims;
import com.mmorpg.entity.boss.anim.LicheAncienneAnims;
import com.mmorpg.entity.boss.anim.RoiGobelinAnims;
import com.mmorpg.entity.boss.anim.TitanGlaceAnims;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Modeles 3D issus de Blockbench (art/) : boss animes, structures multiblocs et armures. Enregistre les LayerDefinition et l'extension
 * client des armures, et fournit les renderers des boss (utilises par ClientRenderers).
 * Les vitesses de marche compensent la foulee de chaque modele pour que les pieds ne glissent pas.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class AnimatedModels {
    private AnimatedModels() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RoiGobelinModel.LAYER, RoiGobelinModel::createBodyLayer);
        event.registerLayerDefinition(LicheAncienneModel.LAYER, LicheAncienneModel::createBodyLayer);
        event.registerLayerDefinition(IgnisModel.LAYER, IgnisModel::createBodyLayer);
        event.registerLayerDefinition(TitanGlaceModel.LAYER, TitanGlaceModel::createBodyLayer);
        event.registerLayerDefinition(AvatarNeantModel.LAYER, AvatarNeantModel::createBodyLayer);
        event.registerLayerDefinition(AutelInvocationModel.LAYER, AutelInvocationModel::createBodyLayer);
        event.registerLayerDefinition(TeleporteurModel.LAYER, TeleporteurModel::createBodyLayer);
        event.registerLayerDefinition(ForgeArcaniqueModel.LAYER, ForgeArcaniqueModel::createBodyLayer);
        // familiers (art/familiers_blockbench)
        event.registerLayerDefinition(com.mmorpg.client.model.pet.FeuFolletModel.LAYER, com.mmorpg.client.model.pet.FeuFolletModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.BebeSlimeModel.LAYER, com.mmorpg.client.model.pet.BebeSlimeModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.ChouetteSageModel.LAYER, com.mmorpg.client.model.pet.ChouetteSageModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.LoupSpectralModel.LAYER, com.mmorpg.client.model.pet.LoupSpectralModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.GolemPocheModel.LAYER, com.mmorpg.client.model.pet.GolemPocheModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.FeeLumineuseModel.LAYER, com.mmorpg.client.model.pet.FeeLumineuseModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.PhenixDoreModel.LAYER, com.mmorpg.client.model.pet.PhenixDoreModel::createBodyLayer);
        event.registerLayerDefinition(com.mmorpg.client.model.pet.DragonnetNeantModel.LAYER, com.mmorpg.client.model.pet.DragonnetNeantModel::createBodyLayer);
        ArmorModels.registerLayers(event);
    }

    /** Structures multiblocs (autel, teleporteur, forge) : modele 3D dessine par le bloc fonctionnel. */
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.STRUCTURE.get(), StructureRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        ArmorModels.registerExtensions(event);
    }

    public static AnimatedBossRenderer<GoblinKingBoss> goblinKing(EntityRendererProvider.Context c) {
        return new AnimatedBossRenderer<>(c, RoiGobelinModel.LAYER, RoiGobelinModel.animations(),
                new AnimatedBossModel.Profile(RoiGobelinAnims.REPOS, RoiGobelinAnims.MARCHE, RoiGobelinAnims.RAGE_REPOS,
                        RoiGobelinAnims.RAGE_MARCHE, RoiGobelinAnims.MORT, "tete", 7.2F, 2.5F),
                "roi_gobelin", 1.3F, false);
    }

    public static AnimatedBossRenderer<LichBoss> lich(EntityRendererProvider.Context c) {
        return new AnimatedBossRenderer<>(c, LicheAncienneModel.LAYER, LicheAncienneModel.animations(),
                new AnimatedBossModel.Profile(LicheAncienneAnims.REPOS, LicheAncienneAnims.DEPLACEMENT, -1, -1,
                        LicheAncienneAnims.MORT, "tete", 3.0F, 2.5F),
                "liche_ancienne", 0.9F, true);
    }

    public static AnimatedBossRenderer<IgnisBoss> ignis(EntityRendererProvider.Context c) {
        return new AnimatedBossRenderer<>(c, IgnisModel.LAYER, IgnisModel.animations(),
                new AnimatedBossModel.Profile(IgnisAnims.REPOS, IgnisAnims.MARCHE, -1, -1, IgnisAnims.MORT, "tete", 3.6F, 2.5F),
                "ignis", 1.3F, true);
    }

    public static AnimatedBossRenderer<FrostTitanBoss> frostTitan(EntityRendererProvider.Context c) {
        return new AnimatedBossRenderer<>(c, TitanGlaceModel.LAYER, TitanGlaceModel.animations(),
                new AnimatedBossModel.Profile(TitanGlaceAnims.REPOS, TitanGlaceAnims.MARCHE, -1, -1, TitanGlaceAnims.MORT, "tete", 5.2F, 2.5F),
                "titan_glace", 2.2F, true);
    }

    public static AnimatedBossRenderer<VoidAvatarBoss> voidAvatar(EntityRendererProvider.Context c) {
        return new AnimatedBossRenderer<>(c, AvatarNeantModel.LAYER, AvatarNeantModel.animations(),
                new AnimatedBossModel.Profile(AvatarNeantAnims.LEVITATION, AvatarNeantAnims.DEPLACEMENT, -1, -1,
                        AvatarNeantAnims.MORT, "masque", 3.0F, 2.5F),
                "avatar_neant", 1.0F, true);
    }
}
