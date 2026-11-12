package com.mmorpg.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * La Mascotte : pet dragon qui guide le tutoriel. Une Mascotte par joueur (rendue pour son proprietaire seulement), elle le suit
 * et le regarde en permanence ; un clic ouvre une bulle de dialogue (pas de menu). Jamais sauvegardee.
 */
public final class MascotEntity extends NpcEntity {
    /** Ancien UUID fixe de la Mascotte unique du spawn (nettoyage des anciens mondes). */
    public static final UUID ID = com.mmorpg.quest.QuestNpcPlan.MASCOT_ID;
    public static final String NAME = "Mascotte";
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(MascotEntity.class, EntityDataSerializers.INT);
    /** Taille de Flammeche (1 = dragonnet ; grandit a chaque Eveil). */
    private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(MascotEntity.class, EntityDataSerializers.FLOAT);
    /** Eclosion : 0 = pas d'oeuf, 1 a 100 = l'oeuf tremble de plus en plus fort avant d'eclore. */
    private static final EntityDataAccessor<Integer> EGG = SynchedEntityData.defineId(MascotEntity.class, EntityDataSerializers.INT);

    public MascotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(false);
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(64);
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.28);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, -1);
        builder.define(GROWTH, 1F);
        builder.define(EGG, 0);
    }

    /** Id d'entite du joueur proprietaire (-1 : aucun). */
    public int ownerId() {
        return entityData.get(OWNER);
    }

    /** Proprietaire par id d'entite (vitrine cliente). */
    public void setOwnerId(int id) {
        entityData.set(OWNER, id);
    }

    public float growth() {
        return entityData.get(GROWTH);
    }

    public void setGrowth(float g) {
        entityData.set(GROWTH, g);
    }

    public int egg() {
        return entityData.get(EGG);
    }

    public void setEgg(int progress) {
        entityData.set(EGG, progress);
    }

    public void initFor(ServerPlayer owner) {
        entityData.set(OWNER, owner.getId());
        setup(Role.QUETES, NAME, 0, com.mmorpg.server.MascotGuide.GROUP);
    }

    @Override
    public void tick() {
        super.tick();
        // ancienne Mascotte unique d'un monde existant (sans proprietaire) : retiree
        if (!level().isClientSide() && (ownerId() == -1 || !(level().getEntity(ownerId()) instanceof ServerPlayer))) discard();
    }

    @Override
    protected void registerGoals() {
        // aucune IA : la position et le regard sont pilotes par MascotGuide
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND && sp.getId() == ownerId())
        {
            if (egg() > 0) return InteractionResult.SUCCESS;
            com.mmorpg.server.QuestManager.onNpcTalk(sp, this);       // Flammeche relaie les donneurs du lore absents
            com.mmorpg.server.QuestManager.talk(sp, this);
        }
        return InteractionResult.SUCCESS;
    }
}
