package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;

public final class FireProjectileEntityAction implements ActionType<EntityCtx, FireProjectileEntityAction.Cfg> {
    public record Cfg(Params params, Hooks hooks) {}

    public record Params(
        ResourceLocation entityType,
        Optional<ResourceLocation> entityId,
        Optional<ResourceLocation> textureLocation,
        int count,
        float speed,
        float divergence,
        Optional<ResourceLocation> sound,
        Optional<Nbt> tag,
        boolean blockActionCancelsMissAction
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
        Optional<EntityAction> projectileAction
    ) {}

    private static final MapCodec<Params> PARAMS = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("entity_type").forGetter(Params::entityType),
        ResourceLocation.CODEC.optionalFieldOf("entity_id").forGetter(Params::entityId),
        ResourceLocation.CODEC.optionalFieldOf("texture_location").forGetter(Params::textureLocation),
        Codec.INT.optionalFieldOf("count", 1).forGetter(Params::count),
        Codec.FLOAT.optionalFieldOf("speed", 1.5f).forGetter(Params::speed),
        Codec.FLOAT.optionalFieldOf("divergence", 1.0f).forGetter(Params::divergence),
        ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Params::sound),
        Nbt.CODEC.optionalFieldOf("tag").forGetter(Params::tag),
        Codec.BOOL.optionalFieldOf("block_action_cancels_miss_action", false).forGetter(Params::blockActionCancelsMissAction)
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
        EntityAction.CODEC.optionalFieldOf("projectile_action").forGetter(Hooks::projectileAction)
    ).apply(i, Hooks::new));

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            PARAMS.forGetter(Cfg::params),
            HOOKS.forGetter(Cfg::hooks)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        LivingEntity shooter = ctx.entity();
        cfg.hooks.entityActionBeforeFiring.ifPresent(a -> a.run(ctx));
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(cfg.params.entityType);
        if (type == null) return;
        for (int i = 0; i < cfg.params.count; i++) {
            CompoundTag spawnTag = new CompoundTag();
            cfg.params.tag.ifPresent(n -> spawnTag.merge(n.tag()));
            spawnTag.putString("id", cfg.params.entityType.toString());
            Entity spawned = EntityType.loadEntityRecursive(spawnTag, level, e -> {
                e.moveTo(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(), shooter.getYRot(), shooter.getXRot());
                if (e instanceof Projectile p) p.setOwner(shooter);
                return e;
            });
            if (spawned == null) continue;
            if (spawned instanceof Projectile projectile) {
                projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0,
                    cfg.params.speed, cfg.params.divergence);
            }
            level.addFreshEntity(spawned);
            cfg.hooks.projectileAction.ifPresent(a -> {
                if (spawned instanceof LivingEntity living) a.run(new EntityCtx(living, level));
            });
            if (spawned instanceof LivingEntity living) {
                cfg.hooks.bientityActionAfterFiring.ifPresent(a -> a.run(new BiEntityCtx(shooter, living, level)));
            }
        }
    }
}
