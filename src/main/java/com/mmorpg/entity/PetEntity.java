package com.mmorpg.entity;

import com.mmorpg.pet.PetType;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Familier : flotte a cote de son proprietaire en suivant ses deplacements (animation de flottement),
 * laisse une traine de particules et n'interagit pas avec le monde.
 */
public class PetEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<String> PET = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.INT);
    private ItemStack cachedStack = ItemStack.EMPTY;
    private String cachedId = "";

    public PetEntity(EntityType<? extends PetEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PET, "");
        builder.define(OWNER, -1);
    }

    public void setup(Player owner, PetType type) {
        this.entityData.set(PET, type.id);
        this.entityData.set(OWNER, owner.getId());
        Vec3 p = targetPos(owner, 0);
        this.setPos(p.x, p.y, p.z);
    }

    public PetType petType() {
        return PetType.byId(this.entityData.get(PET));
    }

    public Player owner() {
        Entity e = this.level().getEntity(this.entityData.get(OWNER));
        return e instanceof Player p ? p : null;
    }

    /** Position en vol : a hauteur d'epaule, un peu derriere et sur le cote du maitre (le dragonnet, plus grand, vole plus a l'ecart). */
    private Vec3 targetPos(Player owner, float time) {
        PetType type = petType();
        boolean big = type == PetType.DRAGONNET;
        double yaw = Math.toRadians(owner.yBodyRot);
        double side = big ? 1.25 : 0.85;
        double back = big ? 0.6 : 0.45;
        double x = owner.getX() - Math.cos(yaw) * side + Math.sin(yaw) * back;
        double z = owner.getZ() - Math.sin(yaw) * side - Math.cos(yaw) * back;
        double bob = Math.sin(time * 0.12) * 0.15;
        return new Vec3(x, owner.getY() + owner.getBbHeight() * (big ? 0.62 : 0.85) + bob, z);
    }

    /** Position au sol : a cote et un peu en retrait du maitre ; null s'il n'y a pas de sol praticable. */
    private Vec3 groundPos(Player owner) {
        double yaw = Math.toRadians(owner.yBodyRot);
        double side = 1.0;
        double back = 0.8;
        double x = owner.getX() - Math.cos(yaw) * side + Math.sin(yaw) * back;
        double z = owner.getZ() - Math.sin(yaw) * side - Math.cos(yaw) * back;
        double y = groundY(x, z, owner.getY());
        if (Double.isNaN(y)) y = groundY(owner.getX(), owner.getZ(), owner.getY());
        return Double.isNaN(y) ? null : new Vec3(x, y, z);
    }

    /** Hauteur du sol sous (x, z), cherchee de 2 blocs au-dessus a 5 blocs en dessous de fromY. */
    private double groundY(double x, double z, double fromY) {
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        int top = net.minecraft.util.Mth.floor(fromY) + 2;
        for (int y = top; y >= top - 7; y--) {
            p.set(bx, y, bz);
            if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) continue;
            p.set(bx, y - 1, bz);
            var shape = this.level().getBlockState(p).getCollisionShape(this.level(), p);
            if (!shape.isEmpty()) return y - 1 + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return Double.NaN;
    }

    /** Vrai si le familier vole en ce moment (sinon il marche au sol). */
    public boolean isFlying() {
        return flying;
    }

    private boolean flying = true;
    private int ownerStill;
    private Vec3 lastOwnerPos;
    // etat d'animation, calcule cote client
    public float animSpeed;
    public int stillTicks;
    public float flyBlend = 1.0F;

    private boolean display;

    /** Familier de presentation (vitrine, apercus) : sans maitre, il reste sur place et joue ses animations de repos. */
    public void displayOnly(PetType type, float yaw) {
        this.entityData.set(PET, type.id);
        this.display = true;
        this.flying = type.move() == PetType.Move.HOVER;
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        if (display) {
            if (this.level().isClientSide()) {
                animSpeed = 0.0F;
                stillTicks++;
                flyBlend = flying ? 1.0F : 0.0F;
                particles();
            }
            return;
        }
        Player owner = owner();
        PetType type = petType();
        if (owner == null || !owner.isAlive() || type == null) {
            if (!this.level().isClientSide() && this.tickCount > 40) this.discard();
            return;
        }
        double ownerMove = lastOwnerPos == null ? 0.0 : owner.position().subtract(lastOwnerPos).horizontalDistance();
        lastOwnerPos = owner.position();
        ownerStill = ownerMove > 0.02 ? 0 : ownerStill + 1;
        boolean ownerAirborne = !owner.onGround() && !owner.isInWater() && owner.getVehicle() == null
                && (owner.getAbilities().flying || owner.isFallFlying() || owner.getDeltaMovement().y < -0.6);
        Vec3 ground = type.move() == PetType.Move.HOVER ? null : groundPos(owner);
        flying = switch (type.move()) {
            case HOVER -> true;
            case PERCH -> ground == null || ownerStill < 30 || ownerAirborne;
            case GROUND_FLY -> ground == null || ownerAirborne;
            case GROUND -> false;
        };
        Vec3 target;
        if (flying || ground == null) {
            Vec3 air = targetPos(owner, this.tickCount);
            target = flying ? air : new Vec3(air.x, owner.getY(), air.z);   // pas de sol : a cote du maitre, a ses pieds
        } else {
            target = ground;
        }
        Vec3 pos = this.position();
        Vec3 next;
        if (pos.distanceToSqr(target) > 16 * 16) {
            next = target;
        } else if (flying) {
            next = pos.add(target.subtract(pos).scale(0.22));
        } else {                                                      // au sol : pas limites, suit le relief
            Vec3 d = new Vec3(target.x - pos.x, 0, target.z - pos.z);
            double len = d.length();
            double step = Math.min(len * 0.2, 0.42);
            Vec3 h = len > 1.0E-4 ? d.scale(step / len) : Vec3.ZERO;
            next = new Vec3(pos.x + h.x, pos.y + (target.y - pos.y) * 0.45, pos.z + h.z);
        }
        double dx = next.x - pos.x;
        double dz = next.z - pos.z;
        this.setPos(next.x, next.y, next.z);
        float want = dx * dx + dz * dz > 0.0004 ? (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0) : owner.yBodyRot;
        this.setYRot(this.getYRot() + net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(want - this.getYRot()), -20.0F, 20.0F));
        if (this.level().isClientSide()) {
            float speed = (float) Math.sqrt(dx * dx + dz * dz);
            animSpeed += (speed - animSpeed) * 0.3F;
            stillTicks = speed > 0.03F ? 0 : stillTicks + 1;
            flyBlend = this.tickCount <= 2 ? (flying ? 1.0F : 0.0F) : flyBlend + ((flying ? 1.0F : 0.0F) - flyBlend) * 0.15F;
            particles();
        }
    }

    private void particles() {
        PetType type = petType();
        if (type == null || this.random.nextInt(3) != 0) return;
        double x = this.getX(), y = this.getY() + 0.15, z = this.getZ();
        switch (type) {
            case FEU_FOLLET -> this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
            case PHENIX -> this.level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
            case FEE -> this.level().addParticle(new DustParticleOptions(0xFF90E0, 0.7F), x, y, z, 0, 0, 0);
            case DRAGONNET -> this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0, 0);
            case LOUP_SPECTRAL -> this.level().addParticle(new DustParticleOptions(0xA0D0FF, 0.8F), x, y, z, 0, 0, 0);
            case CHOUETTE -> {
                if (this.random.nextInt(4) == 0) this.level().addParticle(ParticleTypes.ENCHANT, x, y + 0.3, z, 0, -0.2, 0);
            }
            case SLIME -> {
                if (this.random.nextInt(3) == 0) this.level().addParticle(ParticleTypes.ITEM_SLIME, x, y - 0.1, z, 0, 0, 0);
            }
            case GOLEM -> {
                if (this.random.nextInt(4) == 0) this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0, 0);
            }
        }
    }

    @Override
    public ItemStack getItem() {
        String id = this.entityData.get(PET);
        if (!id.equals(cachedId)) {
            cachedId = id;
            PetType t = PetType.byId(id);
            cachedStack = t == null ? ItemStack.EMPTY : new ItemStack(ModItems.PET_SPRITES.get(t).get());
        }
        return cachedStack;
    }

    /**
     * Chaque client calcule lui-meme la position du familier a partir de son maitre (deplacement fluide, sans a-coups
     * dus aux corrections du serveur) ; le serveur garde sa propre copie pour le suivi de l'entite.
     */
    @Override
    protected boolean isLocalClientAuthoritative() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
