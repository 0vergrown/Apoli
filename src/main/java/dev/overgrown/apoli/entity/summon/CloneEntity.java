package dev.overgrown.apoli.entity.summon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class CloneEntity extends Monster implements OwnableEntity, CrossbowAttackMob, Temporary {
    private static final double COMBAT_SPEED = 1.35;
    private static final float SHOOTING_RANGE = 32f;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SLIM =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> WIDE_TEXTURE =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SLIM_TEXTURE =
        SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.STRING);

    private boolean canAttack = true;
    private boolean canSit = true;
    private boolean followOwner = true;
    private int maxLifeTime = 1200;
    @Nullable
    private ResourceLocation summonId;

    private MeleeAttackGoal meleeGoal;
    private RangedBowAttackGoal<CloneEntity> bowGoal;
    private RangedCrossbowAttackGoal<CloneEntity> crossbowGoal;

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(SLIM, false);
        builder.define(SITTING, false);
        builder.define(CHARGING, false);
        builder.define(WIDE_TEXTURE, "");
        builder.define(SLIM_TEXTURE, "");
    }

    @Override
    protected void registerGoals() {
        this.meleeGoal = new MeleeAttackGoal(this, COMBAT_SPEED, false);
        this.bowGoal = new RangedBowAttackGoal<>(this, COMBAT_SPEED, 20, SHOOTING_RANGE);
        this.crossbowGoal = new RangedCrossbowAttackGoal<>(this, COMBAT_SPEED, SHOOTING_RANGE);

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new DefendOwnerGoal(this));
        this.targetSelector.addGoal(1, new FightForOwnerGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));

        this.updateWeaponGoals();
    }

    public void updateWeaponGoals() {
        if (this.level() == null || this.level().isClientSide || this.meleeGoal == null) return;
        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        this.goalSelector.removeGoal(this.crossbowGoal);
        if (this.isHolding(Items.BOW)) {
            this.goalSelector.addGoal(1, this.bowGoal);
        } else if (this.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
            this.goalSelector.addGoal(1, this.crossbowGoal);
        } else {
            this.goalSelector.addGoal(1, this.meleeGoal);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        LivingEntity owner = this.getOwner();
        boolean expired = this.maxLifeTime > 0 && this.tickCount > this.maxLifeTime;
        if (owner == null || owner.level() != this.level() || expired) {
            this.discard();
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSitting();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player == this.getOwner() && this.canSit) {
            if (!this.level().isClientSide) {
                this.toggleSitting();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        if (this.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
            this.performCrossbowAttack(this, 1.6f);
            return;
        }
        ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        ItemStack arrowStack = this.getProjectile(bow);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, pullProgress, bow);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6f, 14 - this.level().getDifficulty().getId() * 4);

        this.playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
        this.level().addFreshEntity(arrow);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(CHARGING, charging);
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    public boolean canTargetEntity(@Nullable LivingEntity target) {
        LivingEntity owner = this.getOwner();
        if (owner == null || target == null || !this.canAttack || this.isSitting() || target == owner || target == this) return false;
        if (!target.canBeSeenAsEnemy()) return false;
        if (target instanceof OwnableEntity ownable && ownable.getOwner() == owner) return false;
        return true;
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
        return this.level().getPlayerByUUID(uuid);
    }

    public void setOwner(@Nullable Player owner) {
        this.setOwnerUUID(owner == null ? null : owner.getUUID());
    }

    public boolean isOwned() {
        return this.entityData.get(OWNER).isPresent();
    }

    public void setSlim(boolean slim) {
        this.entityData.set(SLIM, slim);
    }

    public boolean isSlim() {
        return this.entityData.get(SLIM);
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
    }

    public void toggleSitting() {
        this.setSitting(!this.isSitting());
        if (this.isSitting()) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
    }

    public void setWideTexture(@Nullable ResourceLocation texture) {
        this.entityData.set(WIDE_TEXTURE, texture == null ? "" : texture.toString());
    }

    public void setSlimTexture(@Nullable ResourceLocation texture) {
        this.entityData.set(SLIM_TEXTURE, texture == null ? "" : texture.toString());
    }

    @Nullable
    public ResourceLocation getCustomTexture(boolean slim) {

        ResourceLocation wide = parseTexture(this.entityData.get(WIDE_TEXTURE));
        ResourceLocation slimTex = parseTexture(this.entityData.get(SLIM_TEXTURE));
        if (slim) return slimTex != null ? slimTex : wide;
        return wide != null ? wide : slimTex;
    }

    @Nullable
    private static ResourceLocation parseTexture(String raw) {
        return raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
    }

    public void setCanAttack(boolean canAttack) {
        this.canAttack = canAttack;
    }

    public void setCanSit(boolean canSit) {
        this.canSit = canSit;
    }

    public void setFollowOwner(boolean followOwner) {
        this.followOwner = followOwner;
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
    public boolean canUsePortal(boolean allowVehicles) {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Slim", this.isSlim());
        tag.putBoolean("Sitting", this.isSitting());
        tag.putString("WideTexture", this.entityData.get(WIDE_TEXTURE));
        tag.putString("SlimTexture", this.entityData.get(SLIM_TEXTURE));
        tag.putBoolean("CanAttack", this.canAttack);
        tag.putBoolean("CanSit", this.canSit);
        tag.putBoolean("FollowOwner", this.followOwner);
        tag.putInt("MaxLifeTime", this.maxLifeTime);
        if (this.summonId != null) tag.putString("SummonId", this.summonId.toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        if (tag.hasUUID("Owner")) this.setOwnerUUID(tag.getUUID("Owner"));
        this.setSlim(tag.getBoolean("Slim"));
        this.setSitting(tag.getBoolean("Sitting"));
        this.entityData.set(WIDE_TEXTURE, tag.getString("WideTexture"));
        this.entityData.set(SLIM_TEXTURE, tag.getString("SlimTexture"));
        if (tag.contains("CanAttack")) this.canAttack = tag.getBoolean("CanAttack");
        if (tag.contains("CanSit")) this.canSit = tag.getBoolean("CanSit");
        if (tag.contains("FollowOwner")) this.followOwner = tag.getBoolean("FollowOwner");
        if (tag.contains("MaxLifeTime")) this.maxLifeTime = tag.getInt("MaxLifeTime");
        if (tag.contains("SummonId")) this.summonId = ResourceLocation.tryParse(tag.getString("SummonId"));
        this.updateWeaponGoals();
    }

    static class FollowOwnerGoal extends Goal {
        private static final double MAX_DISTANCE = 32;
        private static final double MIN_DISTANCE = 6;
        private final CloneEntity clone;
        private LivingEntity owner;
        private int repathCooldown;

        FollowOwnerGoal(CloneEntity clone) {
            this.clone = clone;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.owner = this.clone.getOwner();
            return !this.clone.isSitting() && this.clone.followOwner && this.owner != null
                && this.clone.distanceTo(this.owner) >= MAX_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.clone.isSitting() && this.owner != null && this.clone.followOwner
                && this.clone.distanceTo(this.owner) >= MIN_DISTANCE;
        }

        @Override
        public void start() {
            this.repathCooldown = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            this.clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) return;
            this.clone.getLookControl().setLookAt(this.owner, 10f, this.clone.getMaxHeadXRot());
            if (--this.repathCooldown > 0) return;
            this.repathCooldown = 10;
            this.clone.getNavigation().moveTo(this.owner, COMBAT_SPEED);
        }
    }

    abstract static class OwnerTargetGoal extends TargetGoal {
        protected final CloneEntity clone;
        @Nullable
        protected LivingEntity candidate;
        protected int timestamp;

        OwnerTargetGoal(CloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public void start() {
            this.clone.setTarget(this.candidate);
            super.start();
        }
    }

    static class DefendOwnerGoal extends OwnerTargetGoal {
        DefendOwnerGoal(CloneEntity clone) {
            super(clone);
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.clone.getOwner();
            if (owner == null) return false;
            LivingEntity attacker = owner.getLastHurtByMob();
            int time = owner.getLastHurtByMobTimestamp();
            if (attacker != null && time != this.timestamp && this.clone.canTargetEntity(attacker)) {
                this.candidate = attacker;
                this.timestamp = time;
                return true;
            }
            return false;
        }
    }

    static class FightForOwnerGoal extends OwnerTargetGoal {
        FightForOwnerGoal(CloneEntity clone) {
            super(clone);
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.clone.getOwner();
            if (owner == null) return false;
            LivingEntity victim = owner.getLastHurtMob();
            int time = owner.getLastHurtMobTimestamp();
            if (victim != null && time != this.timestamp && this.clone.canTargetEntity(victim)) {
                this.candidate = victim;
                this.timestamp = time;
                return true;
            }
            return false;
        }
    }
}
