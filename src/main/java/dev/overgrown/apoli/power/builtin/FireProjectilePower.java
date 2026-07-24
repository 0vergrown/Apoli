package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
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
import java.util.UUID;

public final class FireProjectilePower extends PowerType<FireProjectilePower.Config> {
    public record Config(Params params, Hooks hooks) {}

    public record Params(
        Optional<ResourceLocation> entityType,
        Optional<ResourceLocation> textureLocation,
        int cooldown,
        Optional<HudRender> hudRender,
        int count,
        int interval,
        int startDelay,
        float speed,
        float divergence,
        Optional<ResourceLocation> sound,
        Optional<Nbt> tag,
        boolean allowConditionalCancelling,
        boolean blockActionCancelsMissAction,
        Optional<Key> key
    ) {}

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
        Optional<EntityAction> shooterAction
    ) {}

    private static final MapCodec<Params> PARAMS = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.optionalFieldOf("entity_type").forGetter(Params::entityType),
        ResourceLocation.CODEC.optionalFieldOf("texture_location").forGetter(Params::textureLocation),
        Codec.INT.optionalFieldOf("cooldown", 1).forGetter(Params::cooldown),
        HudRender.CODEC.optionalFieldOf("hud_render").forGetter(Params::hudRender),
        Codec.INT.optionalFieldOf("count", 1).forGetter(Params::count),
        Codec.INT.optionalFieldOf("interval", 0).forGetter(Params::interval),
        Codec.INT.optionalFieldOf("start_delay", 0).forGetter(Params::startDelay),
        Codec.FLOAT.optionalFieldOf("speed", 1.5f).forGetter(Params::speed),
        Codec.FLOAT.optionalFieldOf("divergence", 1.0f).forGetter(Params::divergence),
        ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Params::sound),
        Nbt.CODEC.optionalFieldOf("tag").forGetter(Params::tag),
        Codec.BOOL.optionalFieldOf("allow_conditional_cancelling", false).forGetter(Params::allowConditionalCancelling),
        Codec.BOOL.optionalFieldOf("block_action_cancels_miss_action", false).forGetter(Params::blockActionCancelsMissAction),
        Key.CODEC.optionalFieldOf("key").forGetter(Params::key)
    ).apply(i, Params::new));

    private static final MapCodec<Hooks> HOOKS = RecordCodecBuilder.mapCodec(i -> i.group(
        EntityAction.CODEC.optionalFieldOf("entity_action_before_firing").forGetter(Hooks::entityActionBeforeFiring),
        BiEntityAction.CODEC.optionalFieldOf("bientity_action_after_firing").forGetter(Hooks::bientityActionAfterFiring),
        BlockAction.CODEC.optionalFieldOf("block_action_on_hit").forGetter(Hooks::blockActionOnHit),
        BiEntityAction.CODEC.optionalFieldOf("bientity_action_on_miss").forGetter(Hooks::bientityActionOnMiss),
        BiEntityAction.CODEC.optionalFieldOf("bientity_action_on_hit").forGetter(Hooks::bientityActionOnHit),
        BiEntityAction.CODEC.optionalFieldOf("owner_target_bientity_action_on_hit").forGetter(Hooks::ownerTargetBientityActionOnHit),
        BiEntityAction.CODEC.optionalFieldOf("tick_bientity_action").forGetter(Hooks::tickBientityAction),
        BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Hooks::blockCondition),
        BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Hooks::bientityCondition),
        BiEntityCondition.CODEC.optionalFieldOf("owner_bientity_condition").forGetter(Hooks::ownerBientityCondition),
        EntityAction.CODEC.optionalFieldOf("projectile_action").forGetter(Hooks::projectileAction),
        EntityAction.CODEC.optionalFieldOf("shooter_action").forGetter(Hooks::shooterAction)
    ).apply(i, Hooks::new));

    public static final MapCodec<Config> CONFIG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        PARAMS.forGetter(Config::params),
        HOOKS.forGetter(Config::hooks)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CONFIG_CODEC;
    }

    public static void fireBurst(Entity owner, ServerLevel level, Config cfg) {
        playSound(owner, level, cfg.params());
        int count = Math.max(1, cfg.params().count());
        for (int n = 0; n < count; n++) fireOne(owner, level, cfg);
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
        if (!(owner.level() instanceof ServerLevel level)) return false;
        StateKey k = new StateKey(owner.getUUID(), powerId);
        FireState st = states.computeIfAbsent(k, x -> new FireState());
        if (st.cooldown > 0 || st.firing) return false;

        Params p = cfg.params();
        st.cooldown = Math.max(1, p.cooldown());
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
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        StateKey k = new StateKey(holder.owner().getUUID(), powerId);
        FireState st = states.get(k);
        if (st == null) return;
        if (st.cooldown > 0) st.cooldown--;
        if (!st.firing) return;
        if (!(holder.owner().level() instanceof ServerLevel level)) return;

        LivingEntity owner = holder.owner();
        Params p = cfg.params();
        st.ticksSinceUse++;

        if (!st.finishedStartDelay) {
            if (p.startDelay() <= 0 || st.ticksSinceUse % p.startDelay() == 0) st.finishedStartDelay = true;
            else return;
        }

        if (p.interval() <= 0) {
            playSound(owner, level, p);
            while (st.shotProjectiles < Math.max(1, p.count())) {
                fireOne(owner, level, cfg);
                st.shotProjectiles++;
            }
            reset(st);
        } else if (st.ticksSinceUse % p.interval() == 0) {
            playSound(owner, level, p);
            fireOne(owner, level, cfg);
            st.shotProjectiles++;
            if (st.shotProjectiles >= Math.max(1, p.count())) reset(st);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            states.remove(new StateKey(holder.owner().getUUID(), powerId));
        }
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
                0.5F, 0.4F / (owner.getRandom().nextFloat() * 0.4F + 0.8F)));
    }

    private static void fireOne(Entity owner, ServerLevel level, Config cfg) {
        Params p = cfg.params();
        cfg.hooks().entityActionBeforeFiring().ifPresent(a -> a.run(new EntityCtx(owner, level)));

        Entity projectile;
        if (p.textureLocation().isPresent()) {
            CustomProjectileEntity custom = new CustomProjectileEntity(ApoliEntities.customProjectile(), owner, level);
            custom.setTexture(p.textureLocation().get());
            cfg.hooks().bientityActionOnHit().ifPresent(custom::addOnHitAction);
            cfg.hooks().ownerTargetBientityActionOnHit().ifPresent(custom::addOnHitAction);
            projectile = custom;
        } else if (p.entityType().isPresent()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(p.entityType().get()).orElse(null);
            if (type == null) return;
            projectile = type.create(level);
            if (projectile == null) return;
        } else {
            return;
        }

        float yaw = owner.getYRot();
        float pitch = owner.getXRot();
        projectile.moveTo(owner.getX(), owner.getEyeY(), owner.getZ(), yaw, pitch);

        if (projectile instanceof Projectile proj) {
            proj.setOwner(owner);
            proj.shootFromRotation(owner, pitch, yaw, 0.0F, p.speed(), p.divergence());
        } else {
            float f = 0.017453292F;
            double g = 0.0075;
            double dx = -Math.sin(yaw * f) * Math.cos(pitch * f);
            double dy = -Math.sin(pitch * f);
            double dz = Math.cos(yaw * f) * Math.cos(pitch * f);
            Vec3 v = new Vec3(dx, dy, dz).normalize().add(
                level.random.nextGaussian() * g * p.divergence(),
                level.random.nextGaussian() * g * p.divergence(),
                level.random.nextGaussian() * g * p.divergence()
            ).scale(p.speed());
            projectile.setDeltaMovement(v);
        }

        p.tag().ifPresent(nbt -> {
            CompoundTag merged = projectile.saveWithoutId(new CompoundTag());
            merged.merge(nbt.tag());
            projectile.load(merged);
        });

        level.addFreshEntity(projectile);

        cfg.hooks().tickBientityAction().ifPresent(a ->
            dev.overgrown.apoli.entity.ProjectileTickManager.track(projectile, owner, a));
        cfg.hooks().shooterAction().ifPresent(a -> a.run(new EntityCtx(owner, level)));
    }
}
