package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.mob.CrystalGolemEntity;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Un renderer pour chaque type du registre ModEntities, avec les textures du mod. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ClientRenderers {
    private ClientRenderers() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CRYPT_ZOMBIE.get(), net.minecraft.client.renderer.entity.ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.VENOM_SPIDER.get(), net.minecraft.client.renderer.entity.CaveSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT.get(), net.minecraft.client.renderer.entity.PillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.GOBLIN.get(), c -> humanoid(c, "gobelin"));
        event.registerEntityRenderer(ModEntities.DARK_WOLF.get(), c -> new TexturedMobRenderer<>(c,
                new AdultWolfModel(c.bakeLayer(ModelLayers.WOLF)), WolfRenderState::new, "loup_sombre", 0.4F)
                .extra((entity, state) -> {
                    state.isAngry = true;
                    state.tailAngle = (float) (Math.PI / 4);
                    state.texture = MMORPG.id("textures/entity/loup_sombre.png");
                }));
        event.registerEntityRenderer(ModEntities.CURSED_SKELETON.get(), c -> new HumanoidRenderer<>(c,
                new SkeletonModel<SkeletonRenderState>(c.bakeLayer(ModelLayers.SKELETON)), SkeletonRenderState::new, "squelette_maudit"));
        event.registerEntityRenderer(ModEntities.ORC.get(), c -> humanoid(c, "orc_guerrier"));
        event.registerEntityRenderer(ModEntities.FIRE_ELEMENTAL.get(), c -> new TexturedMobRenderer<>(c,
                new BlazeModel(c.bakeLayer(ModelLayers.BLAZE)), LivingEntityRenderState::new, "elementaire_feu", 0.5F));
        event.registerEntityRenderer(ModEntities.ICE_WRAITH.get(), c -> humanoid(c, "spectre_givre"));
        event.registerEntityRenderer(ModEntities.CRYSTAL_GOLEM.get(), c -> golem(c, "golem_cristal"));
        event.registerEntityRenderer(ModEntities.VOID_KNIGHT.get(), c -> humanoid(c, "chevalier_neant"));
        // boss : modeles animes Blockbench (art/boss_blockbench), voir AnimatedModels
        event.registerEntityRenderer(ModEntities.GOBLIN_KING.get(), AnimatedModels::goblinKing);
        event.registerEntityRenderer(ModEntities.LICH.get(), AnimatedModels::lich);
        event.registerEntityRenderer(ModEntities.IGNIS.get(), AnimatedModels::ignis);
        event.registerEntityRenderer(ModEntities.FROST_TITAN.get(), AnimatedModels::frostTitan);
        event.registerEntityRenderer(ModEntities.VOID_AVATAR.get(), AnimatedModels::voidAvatar);
        event.registerEntityRenderer(ModEntities.MAGIC_PROJECTILE.get(), com.mmorpg.client.fx.MagicProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), com.mmorpg.client.fx.SpellProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.SKILL_FX.get(), com.mmorpg.client.fx.SkillFxRenderer::new);
        event.registerEntityRenderer(ModEntities.PET.get(), com.mmorpg.client.model.pet.PetRenderer::new);     // familiers 3D animes
        event.registerEntityRenderer(ModEntities.MOUNT.get(), net.minecraft.client.renderer.entity.HorseRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC.get(), NpcRenderer::new);
    }

    private static <T extends Mob> HumanoidRenderer<T, HumanoidRenderState> humanoid(EntityRendererProvider.Context c, String texture) {
        return new HumanoidRenderer<>(c, new HumanoidModel<HumanoidRenderState>(c.bakeLayer(ModelLayers.ZOMBIE)),
                HumanoidRenderState::new, texture);
    }

    private static <T extends Mob> TexturedMobRenderer<T, IronGolemRenderState, IronGolemModel> golem(EntityRendererProvider.Context c, String texture) {
        return new TexturedMobRenderer<T, IronGolemRenderState, IronGolemModel>(c, new IronGolemModel(c.bakeLayer(ModelLayers.IRON_GOLEM)),
                IronGolemRenderState::new, texture, 0.7F)
                .extra((entity, state) -> {
                    int anim = entity instanceof CrystalGolemEntity g ? g.attackAnim : 0;
                    state.attackTicksRemaining = anim;
                });
    }

    private static final class HumanoidRenderer<T extends Mob, S extends HumanoidRenderState>
            extends HumanoidMobRenderer<T, S, HumanoidModel<S>> {
        private final Supplier<S> states;
        private final Identifier texture;

        private HumanoidRenderer(EntityRendererProvider.Context c, HumanoidModel<S> model, Supplier<S> states, String texture) {
            super(c, model, 0.5F);
            this.states = states;
            this.texture = MMORPG.id("textures/entity/" + texture + ".png");
        }

        @Override
        public S createRenderState() {
            return states.get();
        }

        @Override
        public Identifier getTextureLocation(S state) {
            return texture;
        }

        @Override
        protected HumanoidModel.ArmPose getArmPose(T mob, net.minecraft.world.entity.HumanoidArm arm) {
            HumanoidModel.ArmPose pose = super.getArmPose(mob, arm);
            if (pose == HumanoidModel.ArmPose.EMPTY && !mob.getItemHeldByArm(arm).isEmpty()) {
                return mob.getItemHeldByArm(arm).is(Items.BOW) && mob.isAggressive() ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
            }
            return pose;
        }

        @Override
        public void extractRenderState(T entity, S state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (state instanceof SkeletonRenderState skeleton) {
                skeleton.isAggressive = entity.isAggressive();
                skeleton.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
            }
        }
    }

    /** Etat de rendu d'un PNJ : apparence choisie. */
    public static final class NpcRenderState extends HumanoidRenderState {
        public int skin;
    }

    private static final class NpcRenderer extends HumanoidMobRenderer<com.mmorpg.entity.NpcEntity, NpcRenderState, HumanoidModel<NpcRenderState>> {
        private NpcRenderer(EntityRendererProvider.Context c) {
            super(c, new HumanoidModel<NpcRenderState>(c.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        }

        @Override
        public NpcRenderState createRenderState() {
            return new NpcRenderState();
        }

        @Override
        public void extractRenderState(com.mmorpg.entity.NpcEntity entity, NpcRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.skin = entity.skin();
        }

        @Override
        public Identifier getTextureLocation(NpcRenderState state) {
            return MMORPG.id("textures/entity/pnj_" + state.skin + ".png");
        }
    }

    private static final class TexturedMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
            extends MobRenderer<T, S, M> {
        private final Supplier<S> states;
        private final Identifier texture;

        private BiConsumer<T, S> extra = (e, s) -> {
        };

        private TexturedMobRenderer(EntityRendererProvider.Context c, M model, Supplier<S> states, String texture, float shadow) {
            super(c, model, shadow);
            this.states = states;
            this.texture = MMORPG.id("textures/entity/" + texture + ".png");
        }

        private TexturedMobRenderer<T, S, M> extra(BiConsumer<T, S> extra) {
            this.extra = extra;
            return this;
        }

        @Override
        public void extractRenderState(T entity, S state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            extra.accept(entity, state);
        }

        @Override
        public S createRenderState() {
            return states.get();
        }

        @Override
        public Identifier getTextureLocation(S state) {
            return texture;
        }
    }
}
