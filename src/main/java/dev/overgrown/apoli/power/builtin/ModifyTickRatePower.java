package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickScope;
import dev.overgrown.apoli.tick.TickState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ModifyTickRatePower extends PowerType<ModifyTickRatePower.Config> {

    public static final ResourceLocation CANONICAL = dev.overgrown.apoli.Apoli.id("modify_tick_rate");

    public enum Target implements StringRepresentable {
        SELF("self"),
        CHUNK("chunk"),
        DIMENSION("dimension"),
        SERVER("server"),
        AREA("area");

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);
        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(
        Target target,
        Optional<Expression> rate,
        boolean frozen,
        Vector radius,
        Shape shape,
        Optional<BiEntityCondition> bientityCondition,
        Optional<Boolean> includeSelf,
        boolean affectChunks,
        int interval
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.<Config>mapCodec(i -> i.group(
            Target.CODEC.optionalFieldOf("target", Target.SELF).forGetter(Config::target),
            Expression.INT_OR_EXPR.optionalFieldOf("rate").forGetter(Config::rate),
            Codec.BOOL.optionalFieldOf("frozen", false).forGetter(Config::frozen),
            Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius", Vector.uniform(16.0f)).forGetter(Config::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Config::shape),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Codec.BOOL.optionalFieldOf("include_self").forGetter(Config::includeSelf),
            Codec.BOOL.optionalFieldOf("affect_chunks", false).forGetter(Config::affectChunks),
            Codec.INT.optionalFieldOf("interval", 1).forGetter(Config::interval)
        ).apply(i, Config::new)).xmap(ModifyTickRatePower::reportIgnoredFilter, Function.identity());
    }

    private static Config reportIgnoredFilter(Config cfg) {
        if (cfg.bientityCondition.isPresent() && cfg.target != Target.AREA) {
            String where = LoggedOptionalField.context();
            dev.overgrown.apoli.Apoli.LOGGER.warn("[Apoli] apoli:modify_tick_rate{} sets 'bientity_condition' "
                + "together with \"target\": \"{}\" — the filter only picks which entities an \"area\" "
                + "affects, so it does nothing here and every entity in scope is affected.",
                where == null ? "" : " of " + where, cfg.target.getSerializedName());
        }
        return cfg;
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        if (!conditionHolds(powerId, owner, level)) return;

        int interval = Math.max(1, cfg.interval);
        if (interval > 1 && owner.tickCount % interval != Math.floorMod(powerId.hashCode(), interval)) return;
        long expiry = server.getTickCount() + interval + 2L;

        switch (cfg.target) {
            case SELF -> write(TickRates.stateFor(owner, TickScope.ENTITY), cfg, owner, expiry);
            case CHUNK -> write(TickRates.chunkState(level.dimension(), owner.chunkPosition().toLong()), cfg, owner, expiry);
            case DIMENSION -> write(TickRates.dimensionState(level.dimension()), cfg, owner, expiry);
            case SERVER -> TickRates.requestServer(server,
                cfg.rate.map(expression -> (float) expression.eval(owner))
                    .orElse(dev.overgrown.apoli.tick.TickRateVanilla.exactRate(server)),
                cfg.frozen, interval + 2);
            case AREA -> applyArea(cfg, owner, level, expiry);
        }

        if (exemptsOwner(cfg)) TickRates.exempt(owner, expiry);
    }

    private static boolean exemptsOwner(Config cfg) {
        return switch (cfg.target) {
            case AREA -> cfg.affectChunks && !cfg.includeSelf.orElse(false);
            case CHUNK, DIMENSION -> cfg.includeSelf.isPresent() && !cfg.includeSelf.get();
            case SELF, SERVER -> false;
        };
    }

    private void applyArea(Config cfg, Entity owner, ServerLevel level, long expiry) {
        Vec3 origin = owner.position();
        int rx = (int) Math.ceil(cfg.radius.x());
        int ry = (int) Math.ceil(cfg.radius.y());
        int rz = (int) Math.ceil(cfg.radius.z());
        int boxHalf = Math.max(rx, Math.max(ry, rz));
        BlockPos center = BlockPos.containing(origin);
        AABB box = AABB.ofSize(origin, boxHalf * 2.0, boxHalf * 2.0, boxHalf * 2.0);
        if (dev.overgrown.apoli.dev.DevParticles.due(level)) {
            dev.overgrown.apoli.dev.DevParticles.outlineShape(level, origin, cfg.shape,
                cfg.radius.x(), cfg.radius.y(), cfg.radius.z());
        }
        List<Entity> nearby = level.getEntities((Entity) null, box, target -> {
            if (target == owner) return cfg.includeSelf.orElse(false);
            BlockPos tp = target.blockPosition();
            return cfg.shape.contains(tp.getX() - center.getX(), tp.getY() - center.getY(),
                tp.getZ() - center.getZ(), rx, ry, rz);
        });
        boolean filtered = cfg.bientityCondition.isPresent();
        for (int i = 0; i < nearby.size(); i++) {
            Entity target = nearby.get(i);
            if (filtered && target != owner
                && !cfg.bientityCondition.get().test(BiEntityCtx.of(owner, target, level))) {
                if (cfg.affectChunks) TickRates.exempt(target, expiry);
                continue;
            }
            write(TickRates.stateFor(target, TickScope.ENTITY), cfg, owner, expiry);
        }
        if (!cfg.affectChunks) return;
        int minX = center.getX() - rx;
        int maxX = center.getX() + rx;
        int minZ = center.getZ() - rz;
        int maxZ = center.getZ() + rz;
        for (int cx = minX >> 4; cx <= (maxX >> 4); cx++) {
            for (int cz = minZ >> 4; cz <= (maxZ >> 4); cz++) {
                write(TickRates.chunkState(level.dimension(), ChunkPos.asLong(cx, cz)), cfg, owner, expiry);
            }
        }
    }

    private void write(TickState state, Config cfg, Entity owner, long expiry) {
        state.setRate(cfg.rate.map(expression -> Math.max(0, expression.evalInt(owner))).orElse(TickState.INHERIT));
        state.setFrozen(cfg.frozen);
        state.setExpiry(expiry);
    }

    private static boolean conditionHolds(ResourceLocation powerId, Entity owner, ServerLevel level) {
        Power power = ApoliPowers.get(powerId);
        if (power == null || power.condition().isEmpty()) return true;
        return power.condition().get().test(new EntityCtx(owner, level));
    }
}
