package com.mmorpg.entity;

import com.mmorpg.server.QuestManager;
import com.mmorpg.server.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** PNJ des villes : Maitre des Quetes ou Marchand. Immobile, invulnerable et persistant. */
public class NpcEntity extends PathfinderMob {
    public enum Role {
        QUETES("Maître des Quêtes"), MARCHAND("Marchand");

        public final String label;

        Role(String label) {
            this.label = label;
        }

        public static Role byId(int id) {
            Role[] v = values();
            return id >= 0 && id < v.length ? v[id] : QUETES;
        }
    }

    public static final int SKINS = 4;
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private String group = "";

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void setup(Role role, String name, int skin, String group) {
        this.entityData.set(ROLE, role.ordinal());
        this.entityData.set(SKIN, Math.floorMod(skin, SKINS));
        this.group = group == null ? "" : group;
        this.setCustomName(Component.literal(name));
    }

    public Role role() {
        return Role.byId(this.entityData.get(ROLE));
    }

    public int skin() {
        return this.entityData.get(SKIN);
    }

    public String group() {
        return group;
    }

    public String displayName() {
        return this.hasCustomName() ? this.getCustomName().getString() : role().label;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            switch (role()) {
                case QUETES -> QuestManager.openGiver(sp, this);
                case MARCHAND -> ShopManager.open(sp, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("NpcRole", this.entityData.get(ROLE));
        output.putInt("NpcSkin", this.entityData.get(SKIN));
        output.putString("NpcGroup", group);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(ROLE, input.getIntOr("NpcRole", 0));
        this.entityData.set(SKIN, input.getIntOr("NpcSkin", 0));
        this.group = input.getStringOr("NpcGroup", "");
    }
}
