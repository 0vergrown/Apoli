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
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.FluidHandling;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.ShapeType;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class RaycastAction implements ActionType<EntityCtx, RaycastAction.Cfg> {
    public record Cfg(Params params, Hooks hooks) {}

    public record Params(
        Optional<Float> distance,
        boolean block,
        boolean entity,
        ShapeType shapeType,
        FluidHandling fluidHandling,
        Space space,
        Optional<Vector> direction,
        boolean pierce,
        Optional<ParticleEffect> particle,
        float spacing,
        Optional<Float> entityDistance,
        Optional<Float> blockDistance
    ) {}

    public record Hooks(
        Optional<BiEntityCondition> bientityCondition,
        Optional<BlockCondition> blockCondition,
        Optional<BiEntityAction> bientityAction,
        Optional<BlockAction> blockAction,
        Optional<EntityAction> beforeAction,
        Optional<EntityAction> hitAction,
        Optional<EntityAction> missAction,
        Optional<String> commandAtHit,
        Optional<Float> commandHitOffset,
        Optional<String> commandAlongRay,
        float commandStep,
        boolean commandAlongRayOnlyOnHit
    ) {}

    private record EntityHit(LivingEntity target, Vec3 pos, double distSq) {}

    private static final MapCodec<Params> PARAMS = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.optionalFieldOf("distance").forGetter(Params::distance),
        Codec.BOOL.optionalFieldOf("block", true).forGetter(Params::block),
        Codec.BOOL.optionalFieldOf("entity", true).forGetter(Params::entity),
        ShapeType.CODEC.optionalFieldOf("shape_type", ShapeType.VISUAL).forGetter(Params::shapeType),
        FluidHandling.CODEC.optionalFieldOf("fluid_handling", FluidHandling.ANY).forGetter(Params::fluidHandling),
        Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Params::space),
        Vector.CODEC.optionalFieldOf("direction").forGetter(Params::direction),
        Codec.BOOL.optionalFieldOf("pierce", false).forGetter(Params::pierce),
        ParticleEffect.CODEC.optionalFieldOf("particle").forGetter(Params::particle),
        Codec.FLOAT.optionalFieldOf("spacing", 0.5f).forGetter(Params::spacing),
        Codec.FLOAT.optionalFieldOf("entity_distance").forGetter(Params::entityDistance),
        Codec.FLOAT.optionalFieldOf("block_distance").forGetter(Params::blockDistance)
    ).apply(i, Params::new));

    private static final MapCodec<Hooks> HOOKS = RecordCodecBuilder.mapCodec(i -> i.group(
        BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Hooks::bientityCondition),
        BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Hooks::blockCondition),
        BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Hooks::bientityAction),
        BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Hooks::blockAction),
        EntityAction.CODEC.optionalFieldOf("before_action").forGetter(Hooks::beforeAction),
        EntityAction.CODEC.optionalFieldOf("hit_action").forGetter(Hooks::hitAction),
        EntityAction.CODEC.optionalFieldOf("miss_action").forGetter(Hooks::missAction),
        Codec.STRING.optionalFieldOf("command_at_hit").forGetter(Hooks::commandAtHit),
        Codec.FLOAT.optionalFieldOf("command_hit_offset").forGetter(Hooks::commandHitOffset),
        Codec.STRING.optionalFieldOf("command_along_ray").forGetter(Hooks::commandAlongRay),
        Codec.FLOAT.optionalFieldOf("command_step", 1.0f).forGetter(Hooks::commandStep),
        Codec.BOOL.optionalFieldOf("command_along_ray_only_on_hit", false).forGetter(Hooks::commandAlongRayOnlyOnHit)
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
        LivingEntity source = ctx.entity();
        Level level = ctx.level();
        cfg.hooks.beforeAction.ifPresent(a -> a.run(ctx));

        Vec3 origin = source.getEyePosition();
        Vec3 dir = cfg.params.direction.isPresent()
            ? cfg.params.space.toGlobal(source, new Vec3(cfg.params.direction.get().x(), cfg.params.direction.get().y(), cfg.params.direction.get().z()))
            : source.getViewVector(1f);
        if (dir.lengthSqr() < 1.0e-6) dir = source.getViewVector(1f);
        dir = dir.normalize();

        float blockDist = cfg.params.blockDistance.orElseGet(() -> cfg.params.distance.orElse(20f));
        float entityDist = cfg.params.entityDistance.orElseGet(() -> cfg.params.distance.orElse(blockDist));

        BlockHitResult blockHit = null;
        if (cfg.params.block) {
            Vec3 to = origin.add(dir.scale(blockDist));
            blockHit = level.clip(new ClipContext(origin, to, cfg.params.shapeType.vanilla(),
                cfg.params.fluidHandling.vanilla(), source));
            if (blockHit.getType() == HitResult.Type.MISS) blockHit = null;
        }
        double blockHitDistSq = blockHit != null ? origin.distanceToSqr(blockHit.getLocation()) : Double.POSITIVE_INFINITY;

        boolean anyEntityHit = false;
        Vec3 nearestEntityHit = null;
        double nearestEntityHitDistSq = Double.POSITIVE_INFINITY;
        if (cfg.params.entity) {
            Vec3 endE = origin.add(dir.scale(entityDist));
            AABB box = new AABB(origin, endE).inflate(1.0);
            List<Entity> candidates = level.getEntities(source, box, e ->
                e != source && e instanceof LivingEntity && e.isPickable());
            List<EntityHit> hits = new ArrayList<>();
            for (Entity cand : candidates) {
                if (!(cand instanceof LivingEntity targetLiving)) continue;
                Optional<Vec3> inter = cand.getBoundingBox().clip(origin, endE);
                if (inter.isEmpty()) continue;
                double dSq = origin.distanceToSqr(inter.get());
                if (!cfg.params.pierce && dSq > blockHitDistSq) continue;
                hits.add(new EntityHit(targetLiving, inter.get(), dSq));
            }
            hits.sort(Comparator.comparingDouble(EntityHit::distSq));
            for (EntityHit hit : hits) {
                if (cfg.hooks.bientityCondition.isPresent()
                    && !cfg.hooks.bientityCondition.get().test(new BiEntityCtx(source, hit.target(), level))) continue;
                cfg.hooks.bientityAction.ifPresent(a -> a.run(new BiEntityCtx(source, hit.target(), level)));
                if (!anyEntityHit) {
                    anyEntityHit = true;
                    nearestEntityHit = hit.pos();
                    nearestEntityHitDistSq = hit.distSq();
                }
                if (!cfg.params.pierce) break;
            }
        }

        boolean entityStops = anyEntityHit && !cfg.params.pierce && nearestEntityHitDistSq <= blockHitDistSq;
        Vec3 rayEnd = entityStops ? nearestEntityHit
            : (blockHit != null ? blockHit.getLocation() : origin.add(dir.scale(blockDist)));

        boolean anyHit = anyEntityHit || blockHit != null;
        if (blockHit != null && !entityStops) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (cfg.hooks.blockCondition.isEmpty()
                || cfg.hooks.blockCondition.get().test(new BlockCtx(pos, state, level))) {
                cfg.hooks.blockAction.ifPresent(a -> a.run(new BlockCtx(pos, state, level)));
            }
        }

        if (anyHit) cfg.hooks.hitAction.ifPresent(a -> a.run(ctx));
        else cfg.hooks.missAction.ifPresent(a -> a.run(ctx));

        if (level instanceof ServerLevel serverLevel && cfg.params.particle.isPresent()) {
            ParticleOptions opts = cfg.params.particle.get().resolve(level);
            if (opts != null) {
                double total = origin.distanceTo(rayEnd);
                double step = Math.max(0.1, cfg.params.spacing);
                for (double t = 0; t < total; t += step) {
                    Vec3 p = origin.add(dir.scale(t));
                    serverLevel.sendParticles(opts, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }

        MinecraftServer server = level.getServer();
        if (server != null) {
            CommandSourceStack cmdSrc = source.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            if (cfg.hooks.commandAtHit.isPresent() && blockHit != null && !entityStops) {
                Vec3 hitPos = blockHit.getLocation();
                float off = cfg.hooks.commandHitOffset.orElse(0.0f);
                if (off != 0) {
                    Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
                    hitPos = hitPos.add(normal.scale(off));
                }
                CommandSourceStack atPos = cmdSrc.withPosition(hitPos);
                server.getCommands().performPrefixedCommand(atPos, cfg.hooks.commandAtHit.get());
            }
            if (cfg.hooks.commandAlongRay.isPresent() && (!cfg.hooks.commandAlongRayOnlyOnHit || anyHit)) {
                double total = origin.distanceTo(rayEnd);
                double step = Math.max(0.1, cfg.hooks.commandStep);
                for (double t = 0; t < total; t += step) {
                    Vec3 p = origin.add(dir.scale(t));
                    server.getCommands().performPrefixedCommand(cmdSrc.withPosition(p), cfg.hooks.commandAlongRay.get());
                }
            }
        }
    }
}
