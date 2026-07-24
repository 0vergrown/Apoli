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

    private final List<BiEntityAction> onHitActions = new ArrayList<>();

    public void addOnHitAction(BiEntityAction action) {
        this.onHitActions.add(action);
    }

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
    }

    public void setTexture(ResourceLocation texture) {
        this.entityData.set(TEXTURE, texture == null ? "" : texture.toString());
    }

    public ResourceLocation getTexture() {
        String s = this.entityData.get(TEXTURE);
        return s.isEmpty() ? null : ResourceLocation.tryParse(s);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide || onHitActions.isEmpty()) return;
        if (result.getEntity() instanceof LivingEntity target && this.getOwner() instanceof LivingEntity owner) {
            BiEntityCtx ctx = new BiEntityCtx(owner, target, this.level());
            ModifyProjectileDamageHandler.beginProjectileContext(this);
            try {
                for (BiEntityAction action : onHitActions) {
                    action.run(ctx);
                }
            } finally {
                ModifyProjectileDamageHandler.endProjectileContext();
            }
        }
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(TEXTURE, tag.getString("Texture"));
    }
}
