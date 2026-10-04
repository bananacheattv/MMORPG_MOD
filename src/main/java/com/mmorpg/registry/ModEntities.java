package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.PetEntity;
import com.mmorpg.entity.RpgMonster;
import com.mmorpg.entity.boss.FrostTitanBoss;
import com.mmorpg.entity.boss.GoblinKingBoss;
import com.mmorpg.entity.boss.IgnisBoss;
import com.mmorpg.entity.boss.LichBoss;
import com.mmorpg.entity.boss.RpgBoss;
import com.mmorpg.entity.boss.VoidAvatarBoss;
import com.mmorpg.entity.mob.CrystalGolemEntity;
import com.mmorpg.entity.mob.CursedSkeletonEntity;
import com.mmorpg.entity.mob.DarkWolfEntity;
import com.mmorpg.entity.mob.FireElementalEntity;
import com.mmorpg.entity.mob.GoblinEntity;
import com.mmorpg.entity.mob.IceWraithEntity;
import com.mmorpg.entity.mob.OrcEntity;
import com.mmorpg.entity.mob.VoidKnightEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MMORPG.MODID);
    /** Entites disposant d'un oeuf d'apparition (monstres et boss). */
    public static final Map<String, DeferredHolder<EntityType<?>, ? extends EntityType<?>>> SPAWNABLE = new LinkedHashMap<>();

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinEntity>> GOBLIN = mob("gobelin",
            ENTITIES.registerEntityType("gobelin", GoblinEntity::new, MobCategory.MONSTER, b -> b.sized(0.55F, 1.6F).clientTrackingRange(8)));
    public static final DeferredHolder<EntityType<?>, EntityType<DarkWolfEntity>> DARK_WOLF = mob("loup_sombre",
            ENTITIES.registerEntityType("loup_sombre", DarkWolfEntity::new, MobCategory.MONSTER, b -> b.sized(0.7F, 0.9F).clientTrackingRange(10)));
    public static final DeferredHolder<EntityType<?>, EntityType<CursedSkeletonEntity>> CURSED_SKELETON = mob("squelette_maudit",
            ENTITIES.registerEntityType("squelette_maudit", CursedSkeletonEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.99F).clientTrackingRange(8)));
    public static final DeferredHolder<EntityType<?>, EntityType<OrcEntity>> ORC = mob("orc_guerrier",
            ENTITIES.registerEntityType("orc_guerrier", OrcEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8)));
    public static final DeferredHolder<EntityType<?>, EntityType<FireElementalEntity>> FIRE_ELEMENTAL = mob("elementaire_feu",
            ENTITIES.registerEntityType("elementaire_feu", FireElementalEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8)));
    public static final DeferredHolder<EntityType<?>, EntityType<IceWraithEntity>> ICE_WRAITH = mob("spectre_givre",
            ENTITIES.registerEntityType("spectre_givre", IceWraithEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8)));
    public static final DeferredHolder<EntityType<?>, EntityType<CrystalGolemEntity>> CRYSTAL_GOLEM = mob("golem_cristal",
            ENTITIES.registerEntityType("golem_cristal", CrystalGolemEntity::new, MobCategory.MONSTER, b -> b.sized(1.4F, 2.7F).clientTrackingRange(10)));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidKnightEntity>> VOID_KNIGHT = mob("chevalier_neant",
            ENTITIES.registerEntityType("chevalier_neant", VoidKnightEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).fireImmune().clientTrackingRange(8)));

    public static final DeferredHolder<EntityType<?>, EntityType<GoblinKingBoss>> GOBLIN_KING = mob("roi_gobelin",
            ENTITIES.registerEntityType("roi_gobelin", GoblinKingBoss::new, MobCategory.MONSTER, b -> b.sized(1.8F, 3.0F).clientTrackingRange(12)));
    public static final DeferredHolder<EntityType<?>, EntityType<LichBoss>> LICH = mob("liche_ancienne",
            ENTITIES.registerEntityType("liche_ancienne", LichBoss::new, MobCategory.MONSTER, b -> b.sized(1.2F, 3.2F).clientTrackingRange(12)));
    public static final DeferredHolder<EntityType<?>, EntityType<IgnisBoss>> IGNIS = mob("seigneur_ignis",
            ENTITIES.registerEntityType("seigneur_ignis", IgnisBoss::new, MobCategory.MONSTER, b -> b.sized(1.8F, 4.0F).fireImmune().clientTrackingRange(12)));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostTitanBoss>> FROST_TITAN = mob("titan_glace",
            ENTITIES.registerEntityType("titan_glace", FrostTitanBoss::new, MobCategory.MONSTER, b -> b.sized(3.0F, 6.0F).clientTrackingRange(12)));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidAvatarBoss>> VOID_AVATAR = mob("avatar_neant",
            ENTITIES.registerEntityType("avatar_neant", VoidAvatarBoss::new, MobCategory.MONSTER, b -> b.sized(1.4F, 4.0F).fireImmune().clientTrackingRange(14)));

    public static final DeferredHolder<EntityType<?>, EntityType<MagicProjectile>> MAGIC_PROJECTILE =
            ENTITIES.registerEntityType("projectile_magique", MagicProjectile::new, MobCategory.MISC,
                    b -> b.sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<com.mmorpg.entity.SpellProjectile>> SPELL_PROJECTILE =
            ENTITIES.registerEntityType("projectile_sort", com.mmorpg.entity.SpellProjectile::new, MobCategory.MISC,
                    b -> b.sized(0.35F, 0.35F).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<com.mmorpg.entity.SkillFxEntity>> SKILL_FX =
            ENTITIES.registerEntityType("effet_competence", com.mmorpg.entity.SkillFxEntity::new, MobCategory.MISC,
                    b -> b.sized(0.2F, 0.2F).clientTrackingRange(10).updateInterval(1).noSave().fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<com.mmorpg.entity.NpcEntity>> NPC =
            ENTITIES.registerEntityType("pnj", com.mmorpg.entity.NpcEntity::new, MobCategory.MISC,
                    b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<PetEntity>> PET =
            ENTITIES.registerEntityType("familier", PetEntity::new, MobCategory.MISC,
                    b -> b.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));

    private ModEntities() {
    }

    private static <T extends EntityType<?>> DeferredHolder<EntityType<?>, T> mob(String key, DeferredHolder<EntityType<?>, T> holder) {
        SPAWNABLE.put(key, holder);
        return holder;
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOBLIN.get(), RpgMonster.createAttributes().build());
        event.put(DARK_WOLF.get(), RpgMonster.createAttributes().build());
        event.put(CURSED_SKELETON.get(), CursedSkeletonEntity.createAttributes().build());
        event.put(ORC.get(), RpgMonster.createAttributes().build());
        event.put(FIRE_ELEMENTAL.get(), RpgMonster.createAttributes().build());
        event.put(ICE_WRAITH.get(), RpgMonster.createAttributes().build());
        event.put(CRYSTAL_GOLEM.get(), RpgMonster.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.8).build());
        event.put(VOID_KNIGHT.get(), RpgMonster.createAttributes().build());
        event.put(GOBLIN_KING.get(), RpgBoss.createBossAttributes().build());
        event.put(LICH.get(), RpgBoss.createBossAttributes().build());
        event.put(IGNIS.get(), RpgBoss.createBossAttributes().build());
        event.put(FROST_TITAN.get(), RpgBoss.createBossAttributes().build());
        event.put(VOID_AVATAR.get(), RpgBoss.createBossAttributes().build());
        event.put(NPC.get(), com.mmorpg.entity.NpcEntity.createAttributes().build());
    }
}
