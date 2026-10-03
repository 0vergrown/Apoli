package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.DelayedActionQueue;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.entity.ProjectileHitActions;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class FireProjectilePower extends PowerType<FireProjectilePower.Config> {
    public record Config(Params params, Hooks hooks, Optional<Return> returning, Optional<Homing> homing,
                         Optional<Reflect> reflective, Expression lifetime) {}

    public record Homing(
        Expression delay,
        Expression duration,
        Expression range,
        Expression angle,
        Expression turnRate,
        Optional<BiEntityCondition> bientityCondition
    ) {
        public static final Codec<Homing> CODEC = RecordCodecBuilder.create(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("delay", Expression.constant(0)).forGetter(Homing::delay),
            Expression.INT_OR_EXPR.optionalFieldOf("duration", Expression.constant(0)).forGetter(Homing::duration),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("range", Expression.constant(8.0)).forGetter(Homing::range),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("angle", Expression.constant(60.0)).forGetter(Homing::angle),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("turn_rate", Expression.constant(8.0)).forGetter(Homing::turnRate),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Homing::bientityCondition)
        ).apply(i, Homing::new));
    }

    public record Reflect(Expression maxBounces, Expression speed) {
        private static final Expression DEFAULT_MAX_BOUNCES = Expression.constant(4);
        private static final Expression DEFAULT_SPEED = Expression.constant(1.0);

        public static final Codec<Reflect> CODEC = dev.overgrown.apoli.alias.AliasingMapCodec.wrap(
            RecordCodecBuilder.<Reflect>mapCodec(i -> i.group(
                Expression.INT_OR_EXPR.optionalFieldOf("max_bounces", DEFAULT_MAX_BOUNCES).forGetter(Reflect::maxBounces),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("speed", DEFAULT_SPEED).forGetter(Reflect::speed)
            ).apply(i, Reflect::new)),
            java.util.Map.of("bounce_speed", "speed")).codec();
    }

    private record ReflectFields(Optional<Either<Boolean, Reflect>> reflective, Optional<Expression> maxBounces,
                                 Optional<Expression> bounceSpeed) {}

    private static final MapCodec<Optional<Reflect>> REFLECTIVE = RecordCodecBuilder.<ReflectFields>mapCodec(i -> i.group(
        dev.overgrown.apoli.codec.LoggedOptionalField.of("reflective", Codec.either(Codec.BOOL, Reflect.CODEC)).forGetter(ReflectFields::reflective),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("max_bounces", Expression.INT_OR_EXPR).forGetter(ReflectFields::maxBounces),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bounce_speed", Expression.FLOAT_OR_EXPR).forGetter(ReflectFields::bounceSpeed)
    ).apply(i, ReflectFields::new)).xmap(
        FireProjectilePower::reflectFrom,
        reflect -> new ReflectFields(reflect.map(r -> Either.<Boolean, Reflect>right(r)), Optional.empty(), Optional.empty()));

    private static Optional<Reflect> reflectFrom(ReflectFields fields) {
        if (fields.reflective().isEmpty()) return Optional.empty();
        return fields.reflective().get().map(
            enabled -> enabled
                ? Optional.of(new Reflect(fields.maxBounces().orElse(Reflect.DEFAULT_MAX_BOUNCES),
                    fields.bounceSpeed().orElse(Reflect.DEFAULT_SPEED)))
                : Optional.empty(),
            Optional::of);
    }

    public record Return(
        boolean onHitEntity,
        boolean onHitBlock,
        int after,
        Expression speed,
        boolean hitWhileReturning,
        Optional<BiEntityAction> bientityActionOnReturn,
        Optional<BiEntityAction> bientityActionOnCatch
    ) {
        public static final Codec<Return> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("on_hit_entity", true).forGetter(Return::onHitEntity),
            Codec.BOOL.optionalFieldOf("on_hit_block", true).forGetter(Return::onHitBlock),
            Codec.INT.optionalFieldOf("after", 0).forGetter(Return::after),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("speed", Expression.constant(2.0)).forGetter(Return::speed),
            Codec.BOOL.optionalFieldOf("hit_while_returning", false).forGetter(Return::hitWhileReturning),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_return", BiEntityAction.CODEC).forGetter(Return::bientityActionOnReturn),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_catch", BiEntityAction.CODEC).forGetter(Return::bientityActionOnCatch)
        ).apply(i, Return::new));
    }

    public record Params(
        Optional<ResourceLocation> entityType,
        Optional<ResourceLocation> textureLocation,
        Expression cooldown,
        Optional<HudRender> hudRender,
        Expression count,
        int interval,
        int startDelay,
        Expression speed,
        Expression divergence,
        Expression maxDistance,
        Optional<ResourceLocation> sound,
        Optional<Nbt> tag,
        boolean allowConditionalCancelling,
        boolean blockActionCancelsMissAction,
        Optional<Key> key,
        Expression offsetX,
        Expression offsetY,
        Expression offsetZ,
        Space space
    ) {
        Params withSpawn(Spawn spawn) {
            return new Params(entityType, textureLocation, cooldown, hudRender, count, interval, startDelay,
                speed, divergence, maxDistance, sound, tag, allowConditionalCancelling,
                blockActionCancelsMissAction, key, spawn.offsetX(), spawn.offsetY(), spawn.offsetZ(), spawn.space());
        }
    }

    private record Spawn(Expression offsetX, Expression offsetY, Expression offsetZ, Space space) {}

    public record Hooks(
        Optional<EntityAction> entityActionBeforeFiring,
        Optional<BiEntityAction> bientityActionAfterFiring,
        Optional<BlockAction> blockActionOnHit,
        Optional<BiEntityAction> bientityActionOnMiss,
        Optional<BiEntityAction> bientityActionOnHit,
        Optional<BiEntityAction> ownerTargetBientityActionOnHit,
        Optional<BiEntityAction> tickBientityAction,
        Optional<BlockCondition> blockCondition,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BiEntityCondition> ownerBientityCondition,
        Optional<EntityAction> projectileAction,
        Optional<EntityAction> shooterAction,
        Optional<BiEntityAction> bientityActionOnBounce,
        Optional<BiEntityAction> bientityActionOnExpire
    ) {}

    private static final Expression NO_OFFSET = Expression.constant(0);
    private static final Expression NO_LIFETIME = Expression.constant(0);

    private static final MapCodec<Params> PARAMS_BODY = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.optionalFieldOf("entity_type").forGetter(Params::entityType),
        dev.overgrown.apoli.data.TextureRef.ID_CODEC.optionalFieldOf("texture_location").forGetter(Params::textureLocation),
        Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Params::cooldown),
        HudRender.CODEC.optionalFieldOf("hud_render").forGetter(Params::hudRender),
        Expression.INT_OR_EXPR.optionalFieldOf("count", Expression.constant(1)).forGetter(Params::count),
        Codec.INT.optionalFieldOf("interval", 0).forGetter(Params::interval),
        Codec.INT.optionalFieldOf("start_delay", 0).forGetter(Params::startDelay),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("speed", Expression.constant(1.5)).forGetter(Params::speed),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("divergence", Expression.constant(1.0)).forGetter(Params::divergence),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("max_distance", Expression.constant(0)).forGetter(Params::maxDistance),
        IdCodecs.ID.optionalFieldOf("sound").forGetter(Params::sound),
        Nbt.CODEC.optionalFieldOf("tag").forGetter(Params::tag),
        Codec.BOOL.optionalFieldOf("allow_conditional_cancelling", false).forGetter(Params::allowConditionalCancelling),
        Codec.BOOL.optionalFieldOf("block_action_cancels_miss_action", false).forGetter(Params::blockActionCancelsMissAction),
        Key.CODEC.optionalFieldOf("key").forGetter(Params::key)
    ).apply(i, (entityType, textureLocation, cooldown, hudRender, count, interval, startDelay, speed, divergence,
                maxDistance, sound, tag, allowConditionalCancelling, blockActionCancelsMissAction, key) ->
        new Params(entityType, textureLocation, cooldown, hudRender, count, interval, startDelay, speed, divergence,
            maxDistance, sound, tag, allowConditionalCancelling, blockActionCancelsMissAction, key,
            NO_OFFSET, NO_OFFSET, NO_OFFSET, Space.WORLD)));

    private static final MapCodec<Spawn> PARAMS_SPAWN = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_x", NO_OFFSET).forGetter(Spawn::offsetX),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_y", NO_OFFSET).forGetter(Spawn::offsetY),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_z", NO_OFFSET).forGetter(Spawn::offsetZ),
        Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Spawn::space)
    ).apply(i, Spawn::new));

    private static final MapCodec<Params> PARAMS =
        Codec.mapPair(PARAMS_BODY, PARAMS_SPAWN).xmap(
            pair -> pair.getFirst().withSpawn(pair.getSecond()),
            params -> Pair.of(params, new Spawn(params.offsetX(), params.offsetY(), params.offsetZ(), params.space())));

    private static final MapCodec<Hooks> HOOKS = RecordCodecBuilder.mapCodec(i -> i.group(
        dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_before_firing", EntityAction.CODEC).forGetter(Hooks::entityActionBeforeFiring),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_after_firing", BiEntityAction.CODEC).forGetter(Hooks::bientityActionAfterFiring),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("block_action_on_hit", BlockAction.CODEC).forGetter(Hooks::blockActionOnHit),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_miss", BiEntityAction.CODEC).forGetter(Hooks::bientityActionOnMiss),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_hit", BiEntityAction.CODEC).forGetter(Hooks::bientityActionOnHit),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("owner_target_bientity_action_on_hit", BiEntityAction.CODEC).forGetter(Hooks::ownerTargetBientityActionOnHit),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("tick_bientity_action", BiEntityAction.CODEC).forGetter(Hooks::tickBientityAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Hooks::blockCondition),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Hooks::bientityCondition),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("owner_bientity_condition", BiEntityCondition.CODEC).forGetter(Hooks::ownerBientityCondition),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("projectile_action", EntityAction.CODEC).forGetter(Hooks::projectileAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("shooter_action", EntityAction.CODEC).forGetter(Hooks::shooterAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_bounce", BiEntityAction.CODEC).forGetter(Hooks::bientityActionOnBounce),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_on_expire", BiEntityAction.CODEC).forGetter(Hooks::bientityActionOnExpire)
    ).apply(i, Hooks::new));

    public static final MapCodec<Config> CONFIG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        PARAMS.forGetter(Config::params),
        HOOKS.forGetter(Config::hooks),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("return", Return.CODEC).forGetter(Config::returning),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("homing", Homing.CODEC).forGetter(Config::homing),
        REFLECTIVE.forGetter(Config::reflective),
        Expression.INT_OR_EXPR.optionalFieldOf("lifetime", NO_LIFETIME).forGetter(Config::lifetime)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CONFIG_CODEC;
    }

    public static void fireBurst(Entity owner, ServerLevel level, Config cfg) {
        playSound(owner, level, cfg.params());
        int count = Math.max(1, cfg.params().count().evalInt(owner));
        for (int n = 0; n < count; n++) fireOne(owner, level, cfg);
    }

    public static void fireVolley(Entity owner, ServerLevel level, Config cfg) {
        Params p = cfg.params();
        int startDelay = Math.max(0, p.startDelay());
        int interval = Math.max(0, p.interval());
        if (startDelay <= 0 && interval <= 0) {
            fireBurst(owner, level, cfg);
            return;
        }
        int count = Math.max(1, p.count().evalInt(owner));
        int shots = interval <= 0 ? 1 : count;
        for (int n = 0; n < shots; n++) {
            int delay = startDelay + n * interval;
            int remaining = interval <= 0 ? count : 1;
            if (delay <= 0) {
                volleyStep(owner, level, cfg, remaining);
            } else {
                DelayedActionQueue.schedule(delay,
                    () -> dev.overgrown.apoli.action.ActionLiveness.alive(owner, level),
                    () -> volleyStep(owner, level, cfg, remaining));
            }
        }
    }

    private static void volleyStep(Entity owner, ServerLevel level, Config cfg, int shots) {
        playSound(owner, level, cfg.params());
        for (int n = 0; n < shots; n++) fireOne(owner, level, cfg);
    }

    private record StateKey(UUID entity, ResourceLocation power) {}

    private static final class FireState {
        int cooldown;
        boolean firing;
        int shotProjectiles;
        int ticksSinceUse;
        boolean finishedStartDelay;
    }

    private final Map<StateKey, FireState> states = new HashMap<>();

    public boolean tryActivate(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null) return false;
        if (!(owner.level() instanceof ServerLevel level)) return false;
        StateKey k = new StateKey(owner.getUUID(), powerId);
        FireState st = states.computeIfAbsent(k, x -> new FireState());
        if (st.cooldown > 0 || st.firing) return false;

        Params p = cfg.params();
        st.cooldown = Math.max(1, PowerResources.cooldownTicks(p.cooldown(), holder));
        if (p.startDelay() <= 0 && p.interval() <= 0) {
            fireBurst(owner, level, cfg);
        } else {
            st.firing = true;
            st.shotProjectiles = 0;
            st.ticksSinceUse = 0;
            st.finishedStartDelay = p.startDelay() <= 0;
        }
        return true;
    }

    @Override

    public boolean isCooldown() {

        return true;

    }

    @Override
    public HudRender hudRender(Config cfg) {
        return cfg.params().hudRender().orElse(null);
    }


    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) {
            return OptionalInt.of(PowerResources.clientCooldown(holder, powerId));
        }
        FireState st = states.get(new StateKey(owner.getUUID(), powerId));
        return OptionalInt.of(st == null ? 0 : Math.max(0, st.cooldown));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) return OptionalInt.empty();
        int clamped = Math.max(0, Math.min(value, Math.max(PowerResources.cooldownTicks(cfg.params().cooldown(), holder), 0)));
        states.computeIfAbsent(new StateKey(owner.getUUID(), powerId), x -> new FireState()).cooldown = clamped;
        if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
            dev.overgrown.apoli.ApoliNetwork.sendActivated(player,
                new dev.overgrown.apoli.network.payload.PowerActivatedS2C(powerId, clamped));
        }
        return OptionalInt.of(clamped);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.params().cooldown(), holder), 0) : 0);
    }

    @Override
    public void tickStored(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        tick(powerId, cfg, holder);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        StateKey k = new StateKey(holder.rawOwner().getUUID(), powerId);
        FireState st = states.get(k);
        if (st == null) return;
        if (st.cooldown > 0) st.cooldown--;
        if (!st.firing) return;

        LivingEntity owner = holder.owner();
        if (owner == null) return;
        if (!(owner.level() instanceof ServerLevel level)) return;
        Params p = cfg.params();
        if (p.allowConditionalCancelling() && !conditionMet(powerId, owner, level)) {
            reset(st);
            return;
        }
        st.ticksSinceUse++;

        if (!st.finishedStartDelay) {
            if (p.startDelay() <= 0 || st.ticksSinceUse % p.startDelay() == 0) st.finishedStartDelay = true;
            else return;
        }

        if (p.interval() <= 0) {
            playSound(owner, level, p);
            int total = Math.max(1, p.count().evalInt(owner));
            while (st.shotProjectiles < total) {
                fireOne(owner, level, cfg);
                st.shotProjectiles++;
            }
            reset(st);
        } else if (st.ticksSinceUse % p.interval() == 0) {
            playSound(owner, level, p);
            fireOne(owner, level, cfg);
            st.shotProjectiles++;
            if (st.shotProjectiles >= Math.max(1, p.count().evalInt(owner))) reset(st);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            states.remove(new StateKey(holder.rawOwner().getUUID(), powerId));
        }
    }

    private static boolean conditionMet(ResourceLocation powerId, LivingEntity owner, ServerLevel level) {
        dev.overgrown.apoli.power.Power power = dev.overgrown.apoli.power.ApoliPowers.get(powerId);
        if (power == null || power.condition().isEmpty()) return true;
        return power.condition().get().test(new EntityCtx(owner, level));
    }

    private static void reset(FireState st) {
        st.firing = false;
        st.shotProjectiles = 0;
        st.ticksSinceUse = 0;
        st.finishedStartDelay = false;
    }

    private static void playSound(Entity owner, ServerLevel level, Params p) {
        p.sound().flatMap(BuiltInRegistries.SOUND_EVENT::getOptional).ifPresent(se ->
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), se, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F)));
    }

    private static void fireOne(Entity owner, ServerLevel level, Config cfg) {
        Params p = cfg.params();
        cfg.hooks().entityActionBeforeFiring().ifPresent(a -> a.run(new EntityCtx(owner, level)));

        Entity projectile;
        if (p.textureLocation().isPresent()) {
            CustomProjectileEntity custom = new CustomProjectileEntity(ApoliEntities.customProjectile(), owner, level);
            ResourceLocation declared = p.textureLocation().get();
            dev.overgrown.apoli.data.TextureRef.Kind kind = dev.overgrown.apoli.data.TextureRef.kindOf(declared);
            if (kind != null && kind.isItem() && owner instanceof LivingEntity holder) {
                custom.setItem(kind == dev.overgrown.apoli.data.TextureRef.Kind.HELD_ITEM
                    ? holder.getMainHandItem() : holder.getOffhandItem());
            } else {
                custom.setTexture(declared);
            }
            cfg.homing().ifPresent(homing -> custom.startHoming(homing, owner));
            projectile = custom;
        } else if (p.entityType().isPresent()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(p.entityType().get()).orElse(null);
            if (type == null) return;
            projectile = type.create(level);
            if (projectile == null) return;
        } else {
            return;
        }

        if (projectile instanceof ProjectileHitActions hooks) {
            hooks.apoli$setFireConfig(cfg);
            hooks.apoli$setMaxRange(p.maxDistance().eval(owner));
            hooks.apoli$setLifetime(cfg.lifetime().evalInt(owner));
            hooks.apoli$setFireCause(dev.overgrown.apoli.attribution.PowerCause.holder(),
                dev.overgrown.apoli.attribution.PowerCause.powerId());
        }

        float yaw = owner.getYRot();
        float pitch = owner.getXRot();
        Vec3 spawn = new Vec3(owner.getX(), owner.getEyeY(), owner.getZ())
            .add(p.space().toGlobal(owner, new Vec3(p.offsetX().eval(owner), p.offsetY().eval(owner), p.offsetZ().eval(owner))));
        projectile.moveTo(spawn.x, spawn.y, spawn.z, yaw, pitch);

        float speed = (float) p.speed().eval(owner);
        float divergence = (float) p.divergence().eval(owner);
        if (projectile instanceof Projectile proj) {
            proj.setOwner(owner);
            proj.shootFromRotation(owner, pitch, yaw, 0.0F, speed, divergence);
        } else {
            float f = 0.017453292F;
            double g = 0.0075;
            double dx = -Math.sin(yaw * f) * Math.cos(pitch * f);
            double dy = -Math.sin(pitch * f);
            double dz = Math.cos(yaw * f) * Math.cos(pitch * f);
            Vec3 v = new Vec3(dx, dy, dz).normalize().add(
                level.random.nextGaussian() * g * divergence,
                level.random.nextGaussian() * g * divergence,
                level.random.nextGaussian() * g * divergence
            ).scale(speed);
            projectile.setDeltaMovement(v);
        }

        p.tag().ifPresent(nbt -> {
            CompoundTag merged = projectile.saveWithoutId(new CompoundTag());
            merged.merge(nbt.tag());
            projectile.load(merged);
        });

        level.addFreshEntity(projectile);

        cfg.hooks().projectileAction().ifPresent(a -> a.run(new EntityCtx(projectile, level)));
        if (projectile instanceof CustomProjectileEntity custom) {
            java.util.List<ResourceLocation> modelPowers = CustomModelRenderPower.geometryPowerIds(projectile);
            if (!modelPowers.isEmpty()) custom.setModelPowers(modelPowers);
        }
        cfg.hooks().bientityActionAfterFiring().ifPresent(a ->
            a.run(dev.overgrown.apoli.condition.context.BiEntityCtx.of(owner, projectile, level)));
        cfg.hooks().tickBientityAction().ifPresent(a ->
            dev.overgrown.apoli.entity.ProjectileTickManager.track(projectile, owner, a));
        cfg.hooks().shooterAction().ifPresent(a -> a.run(new EntityCtx(owner, level)));
    }
}
