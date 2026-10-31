package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Un renderer pour chaque type du registre ModEntities, avec les textures du mod. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ClientRenderers {
    private ClientRenderers() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        for(int i=0;i<10;i++){final int tier=i;event.registerEntityRenderer(ModEntities.GUILD_GUARDIANS.get(i).get(),c->new com.mmorpg.client.model.YggdrasilRenderer(c,tier));}
        event.registerEntityRenderer(ModEntities.ARENA_DRONE.get(),com.mmorpg.client.model.ArenaDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.ARENA_CRYSTAL.get(),com.mmorpg.client.model.ArenaCrystalRenderer::new);
        ModEntities.IMPORTED.forEach((key, type) -> event.registerEntityRenderer(type.get(), c -> new com.mmorpg.client.model.imported.ImportedMobRenderer(c, key)));
        event.registerEntityRenderer(ModEntities.CRYPT_ZOMBIE.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"zombie_des_cryptes"));
        event.registerEntityRenderer(ModEntities.VENOM_SPIDER.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"araignee_venimeuse"));
        event.registerEntityRenderer(ModEntities.BANDIT.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"bandit_arbaletrier"));
        event.registerEntityRenderer(ModEntities.GOBLIN.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"gobelin"));
        event.registerEntityRenderer(ModEntities.DARK_WOLF.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"loup_sombre"));
        event.registerEntityRenderer(ModEntities.CURSED_SKELETON.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"squelette_maudit"));
        event.registerEntityRenderer(ModEntities.ORC.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"orc_guerrier"));
        event.registerEntityRenderer(ModEntities.FIRE_ELEMENTAL.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"elementaire_feu"));
        event.registerEntityRenderer(ModEntities.ICE_WRAITH.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"spectre_givre"));
        event.registerEntityRenderer(ModEntities.CRYSTAL_GOLEM.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"golem_cristal"));
        event.registerEntityRenderer(ModEntities.VOID_KNIGHT.get(),c->new com.mmorpg.client.model.imported.ClassicMobRenderer<>(c,"chevalier_neant"));
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
        ModEntities.MOUNTS.forEach((type, entity) -> event.registerEntityRenderer(entity.get(),
                c -> new com.mmorpg.client.model.mount.MountRenderer(c, type)));     // montures 3D animees
        event.registerEntityRenderer(ModEntities.HUB_GUIDE.get(), com.mmorpg.client.model.imported.HubGuideRenderer::new);
        event.registerEntityRenderer(ModEntities.MASCOT.get(), com.mmorpg.client.model.pet.MascotRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC.get(), com.mmorpg.client.model.imported.AuthoredNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), com.mmorpg.client.model.dummy.DummyRenderer::new);
        event.registerEntityRenderer(ModEntities.CLASS_MASTER.get(), com.mmorpg.client.model.imported.AuthoredMasterRenderer::new);
    }
}
