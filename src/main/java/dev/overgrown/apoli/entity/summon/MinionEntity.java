package dev.overgrown.apoli.entity.summon;

import dev.overgrown.apoli.Apoli;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class MinionEntity extends Mob implements OwnableEntity, Temporary {
    public static final ResourceLocation TEMPLATE_TEXTURE = Apoli.id("textures/entity/minion_template.png");

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> TEXTURE =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FOLLOW =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> OFFSET_X =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE =
        SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifeTime = 1200;
    @Nullable
    private ResourceLocation summonId;

    public MinionEntity(EntityType<? extends MinionEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(TEXTURE, TEMPLATE_TEXTURE.toString());
        this.entityData.define(FOLLOW, false);
        this.entityData.define(OFFSET_X, 0f);
        this.entityData.define(OFFSET_Y, 0f);
        this.entityData.define(OFFSET_Z, 0f);
        this.entityData.define(SCALE, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.maxLifeTime > 0 && this.tickCount > this.maxLifeTime) {
            this.discard();
            return;
        }
        if (this.isFollowingOwner()) {
            LivingEntity owner = this.getOwner();
            if (owner == null || owner.level() != this.level()) {
                this.discard();
                return;
            }
            this.setYRot(owner.getYHeadRot());
            this.setYHeadRot(owner.getYHeadRot());
            this.setXRot(owner.getXRot());
            Vector3f offset = this.getFollowOffset();
            offset.rotateY(-owner.getYHeadRot() * Mth.DEG_TO_RAD);
            this.setPos(owner.getX() + offset.x, owner.getY() + offset.y, owner.getZ() + offset.z);
        }
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER, Optional.ofNullable(uuid));
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        UUID uuid = this.getOwnerUUID();
        if (uuid == null) return null;
        if (this.level() instanceof ServerLevel server) {
            return server.getEntity(uuid) instanceof LivingEntity living ? living : null;
        }
        return this.level().getPlayerByUUID(uuid);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.setOwnerUUID(owner == null ? null : owner.getUUID());
    }

    public void setTexture(ResourceLocation texture) {
        this.entityData.set(TEXTURE, texture.toString());
    }

    public ResourceLocation getTexture() {

        String raw = this.entityData.get(TEXTURE);
        ResourceLocation parsed = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        return parsed == null ? TEMPLATE_TEXTURE : parsed;
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public float getMinionScale() {
        return this.entityData.get(SCALE);
    }

    public void setFollowingOwner(boolean follow) {
        this.entityData.set(FOLLOW, follow);
        this.setNoGravity(follow);
    }

    public boolean isFollowingOwner() {
        return this.entityData.get(FOLLOW);
    }

    public void setFollowOffset(double x, double y, double z) {
        this.entityData.set(OFFSET_X, (float) x);
        this.entityData.set(OFFSET_Y, (float) y);
        this.entityData.set(OFFSET_Z, (float) z);
    }

    public Vector3f getFollowOffset() {
        return new Vector3f(this.entityData.get(OFFSET_X), this.entityData.get(OFFSET_Y), this.entityData.get(OFFSET_Z));
    }

    public void setSummonId(@Nullable ResourceLocation id) {
        this.summonId = id;
    }

    @Override
    @Nullable
    public ResourceLocation getSummonId() {
        return this.summonId;
    }

    @Override
    public void setMaxLifeTime(int ticks) {
        this.maxLifeTime = ticks;
    }

    @Override
    public int getMaxLifeTime() {
        return this.maxLifeTime;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SCALE.equals(key)) this.refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Texture", this.entityData.get(TEXTURE));
        tag.putBoolean("Follow", this.isFollowingOwner());
        Vector3f offset = this.getFollowOffset();
        tag.putFloat("OffsetX", offset.x);
        tag.putFloat("OffsetY", offset.y);
        tag.putFloat("OffsetZ", offset.z);
        tag.putFloat("MinionScale", this.getMinionScale());
        tag.putInt("MaxLifeTime", this.maxLifeTime);
        if (this.summonId != null) tag.putString("SummonId", this.summonId.toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        this.setCustomNameVisible(false);
        if (tag.hasUUID("Owner")) this.setOwnerUUID(tag.getUUID("Owner"));
        if (tag.contains("Texture")) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Texture"));
            if (parsed != null) this.setTexture(parsed);
        }
        this.setFollowOffset(tag.getFloat("OffsetX"), tag.getFloat("OffsetY"), tag.getFloat("OffsetZ"));
        this.setFollowingOwner(tag.getBoolean("Follow"));
        if (tag.contains("MinionScale")) this.setScale(tag.getFloat("MinionScale"));
        if (tag.contains("MaxLifeTime")) this.maxLifeTime = tag.getInt("MaxLifeTime");
        if (tag.contains("SummonId")) this.summonId = ResourceLocation.tryParse(tag.getString("SummonId"));
    }
}
