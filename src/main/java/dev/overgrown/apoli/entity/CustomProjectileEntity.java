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

import java.util.ArrayList;
import java.util.List;

public class CustomProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<String> TEXTURE =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_POWER =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> ITEM =
        SynchedEntityData.defineId(CustomProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

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

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
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
