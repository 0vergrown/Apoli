package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ExprVector;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.ParticlePlacement;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class SpawnParticlesAction implements ActionType<EntityCtx, SpawnParticlesAction.Cfg> {
    public record Cfg(
        ParticleEffect particle,
        Optional<BiEntityCondition> bientityCondition,
        Expression count,
        Either<Expression, ExprVector> speed,
        boolean force,
        ExprVector spread,
        Expression offsetX,
        Expression offsetY,
        Expression offsetZ,
        Expression velocityX,
        Expression velocityY,
        Expression velocityZ,
        Optional<Space> space,
        Optional<String> modelPart
    ) {}

    private static final ExprVector DEFAULT_SPREAD = ExprVector.of(0.5f, 0.5f, 0.5f);
    private static final Expression ZERO = Expression.constant(0.0);
    private static final Expression HALF = Expression.constant(0.5);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Cfg::particle),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition),
            Expression.INT_OR_EXPR.fieldOf("count").forGetter(Cfg::count),
            ParticlePlacement.SPEED_CODEC.optionalFieldOf("speed", ParticlePlacement.NO_SPEED).forGetter(Cfg::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Cfg::force),
            ExprVector.SCALAR_OR_VECTOR.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Cfg::spread),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_x", ZERO).forGetter(Cfg::offsetX),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_y", HALF).forGetter(Cfg::offsetY),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_z", ZERO).forGetter(Cfg::offsetZ),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_x", ZERO).forGetter(Cfg::velocityX),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_y", ZERO).forGetter(Cfg::velocityY),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_z", ZERO).forGetter(Cfg::velocityZ),
            Space.CODEC.optionalFieldOf("space").forGetter(Cfg::space),
            ModelParts.NAME_CODEC.optionalFieldOf("model_part").forGetter(Cfg::modelPart)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Entity e = ctx.raw();
        if (e == null) return;
        ParticleOptions opts = cfg.particle.resolve(level, e);
        if (opts == null) return;

        dev.overgrown.apoli.data.ModelPartAnchor.Frame frame = ParticlePlacement.frame(e, cfg.modelPart);
        Vec3 origin = ParticlePlacement.origin(e, frame, cfg.space,
            (float) cfg.offsetX.eval(e), (float) cfg.offsetY.eval(e), (float) cfg.offsetZ.eval(e));
        Vector speedVector = ParticlePlacement.speedVector(e, cfg.speed);
        Vec3 velocity = ParticlePlacement.velocity(e, frame, cfg.space,
            (float) cfg.velocityX.eval(e), (float) cfg.velocityY.eval(e), (float) cfg.velocityZ.eval(e),
            speedVector);
        int count = Math.max(0, cfg.count.evalInt(e));
        Vector spread = cfg.spread.resolve(e);
        float scalarSpeed = ParticlePlacement.scalarSpeed(e, cfg.speed);
        List<Packet<?>> packets = null;
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (!dev.overgrown.apoli.data.ParticleBroadcast.inRange(player, cfg.force, origin.x, origin.y, origin.z)) continue;
            if (cfg.bientityCondition.isPresent()
                && !cfg.bientityCondition.get().test(BiEntityCtx.of(e, player, level))) continue;
            if (packets == null) {
                packets = ParticlePlacement.packets(opts, cfg.force, origin, velocity, count, spread,
                    scalarSpeed, level, e, frame, cfg.space);
            }
            for (int p = 0; p < packets.size(); p++) {
                dev.overgrown.apoli.data.ParticleBroadcast.send(level, player, packets.get(p));
            }
        }
    }
}
