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

    public CustomProjectileEntity(EntityType<? extends CustomProjectileEntity> type, Level level) {
        super(type, level);
    }

    public CustomProjectileEntity(EntityType<? extends CustomProjectileEntity> type, Entity owner, Level level) {
        super(type, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TEXTURE, "");
        this.entityData.define(MODEL_POWER, "");
        this.entityData.define(ITEM, net.minecraft.world.item.ItemStack.EMPTY);
        this.entityData.define(RETURN_PULL, 0.0F);
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

    public void setModelPower(ResourceLocation powerId) {
        this.entityData.set(MODEL_POWER, powerId == null ? "" : powerId.toString());
    }

    public ResourceLocation getModelPower() {
        String s = this.entityData.get(MODEL_POWER);
        return s.isEmpty() ? null : ResourceLocation.tryParse(s);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURN_PULL) > 0.0F;
    }

    public void startReturning(float pull) {
        this.entityData.set(RETURN_PULL, pull);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        float pull = this.entityData.get(RETURN_PULL);
        if (pull > 0.0F && !this.steerHome(pull)) return;
        super.tick();
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
