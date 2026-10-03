package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CustomProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<String> TEXTURE =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_POWER =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> ITEM =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> RETURN_PULL =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HOMING_TARGET =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HOMING_TURN =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private ProjectileHoming homing;
    private String modelPowersRaw = "";
    private List<ResourceLocation> modelPowers = List.of();

    public CustomProjectileEntity(EntityType<? extends CustomProjectileEntity> type, Level level) {
        super(type, level);
    }

    public CustomProjectileEntity(EntityType<? extends CustomProjectileEntity> type, Entity owner, Level level) {
        super(type, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEXTURE, "");
        builder.define(MODEL_POWER, "");
        builder.define(ITEM, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(RETURN_PULL, 0.0F);
        builder.define(HOMING_TARGET, ProjectileHoming.NONE);
        builder.define(HOMING_TURN, 0.0F);
    }

    public void setTexture(ResourceLocation texture) {
        this.entityData.set(TEXTURE, texture == null ? "" : texture.toString());
    }

    public ResourceLocation getTexture() {
        String s = this.entityData.get(TEXTURE);
        return s.isEmpty() ? null : ResourceLocation.tryParse(s);
    }

    public void setItem(net.minecraft.world.item.ItemStack stack) {
        this.entityData.set(ITEM, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.copy());
    }

    public net.minecraft.world.item.ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setModelPowers(List<ResourceLocation> powerIds) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < powerIds.size(); i++) {
            if (i > 0) joined.append(',');
            joined.append(powerIds.get(i));
        }
        this.entityData.set(MODEL_POWER, joined.toString());
    }

    public List<ResourceLocation> getModelPowers() {
        String raw = this.entityData.get(MODEL_POWER);
        if (!raw.equals(this.modelPowersRaw)) {
            this.modelPowersRaw = raw;
            this.modelPowers = parseModelPowers(raw);
        }
        return this.modelPowers;
    }

    private static List<ResourceLocation> parseModelPowers(String raw) {
        if (raw.isEmpty()) return List.of();
        String[] parts = raw.split(",");
        List<ResourceLocation> ids = new ArrayList<>(parts.length);
        for (String part : parts) {
            ResourceLocation id = ResourceLocation.tryParse(part);
            if (id != null) ids.add(id);
        }
        return List.copyOf(ids);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURN_PULL) > 0.0F;
    }

    public void startReturning(float pull) {
        this.entityData.set(RETURN_PULL, pull);
        this.setNoGravity(true);
        this.homing = null;
        this.entityData.set(HOMING_TARGET, ProjectileHoming.NONE);
    }

    public void startHoming(dev.overgrown.apoli.power.builtin.FireProjectilePower.Homing config, Entity shooter) {
        this.homing = new ProjectileHoming(config, shooter);
        this.entityData.set(HOMING_TURN, this.homing.turnRate());
    }

    public boolean canTarget(Entity entity) {
        return this.canHitEntity(entity);
    }

    @Override
    public void tick() {
        float pull = this.entityData.get(RETURN_PULL);
        if (pull > 0.0F) {
            if (!this.steerHome(pull)) return;
        } else {
            this.seek();
        }
        super.tick();
    }

    private void seek() {
        int targetId = this.entityData.get(HOMING_TARGET);
        if (this.homing != null && !this.level().isClientSide) {
            int next = this.homing.update(this, targetId);
            if (next == ProjectileHoming.FINISHED) {
                this.homing = null;
                next = ProjectileHoming.NONE;
            }
            if (next != targetId) {
                this.entityData.set(HOMING_TARGET, next);
                this.hasImpulse = true;
                targetId = next;
            }
        }
        if (targetId < 0) return;
        Entity target = this.level().getEntity(targetId);
        if (target != null) {
            ProjectileHoming.steer(this, target, this.entityData.get(HOMING_TURN));
        }
    }

    private boolean steerHome(float pull) {
        Entity owner = this.getOwner();
        boolean server = !this.level().isClientSide;
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != this.level()) {
            if (!server) return true;
            this.discard();
            return false;
        }
        Vec3 toOwner = owner.getEyePosition().subtract(this.position());
        if (server && toOwner.lengthSqr() <= Math.max(1.0, this.getDeltaMovement().lengthSqr())) {
            ((ProjectileHitActions) this).apoli$caught();
            this.discard();
            return false;
        }
        this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015 * pull, this.getZ());
        if (!server) this.yOld = this.getY();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(0.05 * pull)));
        return true;
    }

    @Override
    protected void checkInsideBlocks() {
        if (!this.isReturning()) super.checkInsideBlocks();
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.isReturning() && result.getType() == HitResult.Type.BLOCK) return;
        super.onHit(result);
        if (!this.level().isClientSide && !this.isReturning()
            && !((ProjectileHitActions) this).apoli$bouncedThisHit()) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Texture", this.entityData.get(TEXTURE));
        tag.putString("ModelPower", this.entityData.get(MODEL_POWER));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TEXTURE, tag.getString("Texture"));
        this.entityData.set(MODEL_POWER, tag.getString("ModelPower"));
    }
}
