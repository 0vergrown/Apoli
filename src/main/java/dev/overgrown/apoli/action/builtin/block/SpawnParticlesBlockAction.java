package dev.overgrown.apoli.action.builtin.block;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ExprVector;
import dev.overgrown.apoli.data.ParticleBroadcast;
import dev.overgrown.apoli.data.ParticleEffect;
import dev.overgrown.apoli.data.ParticlePlacement;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class SpawnParticlesBlockAction implements ActionType<BlockCtx, SpawnParticlesBlockAction.Cfg> {

    public enum Anchor implements StringRepresentable {
        HIT("hit"),
        CENTER("center"),
        CORNER("corner");

        public static final Codec<Anchor> CODEC = StringRepresentable.fromEnum(Anchor::values);
        private final String name;

        Anchor(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(
        ParticleEffect particle,
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
        Anchor anchor
    ) {}

    private static final ExprVector DEFAULT_SPREAD = ExprVector.of(0.25f, 0.25f, 0.25f);
    private static final Expression ZERO = Expression.constant(0.0);
    private static final Expression ONE = Expression.constant(1.0);
    private static final Optional<Space> WORLD = Optional.of(Space.WORLD);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ParticleEffect.CODEC.fieldOf("particle").forGetter(Cfg::particle),
            Expression.INT_OR_EXPR.optionalFieldOf("count", ONE).forGetter(Cfg::count),
            ParticlePlacement.SPEED_CODEC.optionalFieldOf("speed", ParticlePlacement.NO_SPEED).forGetter(Cfg::speed),
            Codec.BOOL.optionalFieldOf("force", false).forGetter(Cfg::force),
            ExprVector.SCALAR_OR_VECTOR.optionalFieldOf("spread", DEFAULT_SPREAD).forGetter(Cfg::spread),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_x", ZERO).forGetter(Cfg::offsetX),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_y", ZERO).forGetter(Cfg::offsetY),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("offset_z", ZERO).forGetter(Cfg::offsetZ),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_x", ZERO).forGetter(Cfg::velocityX),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_y", ZERO).forGetter(Cfg::velocityY),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("velocity_z", ZERO).forGetter(Cfg::velocityZ),
            Anchor.CODEC.optionalFieldOf("anchor", Anchor.HIT).forGetter(Cfg::anchor)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Entity actor = ctx.actor();
        ParticleOptions opts = cfg.particle.resolve(level, actor);
        if (opts == null) return;

        Vec3 origin = anchor(cfg.anchor, ctx)
            .add(cfg.offsetX.eval(actor), cfg.offsetY.eval(actor), cfg.offsetZ.eval(actor));
        Vec3 velocity = new Vec3(cfg.velocityX.eval(actor), cfg.velocityY.eval(actor), cfg.velocityZ.eval(actor));
        float scalarSpeed = ParticlePlacement.scalarSpeed(actor, cfg.speed);
        if (velocity.lengthSqr() < 1.0e-9) {
            Vector vector = ParticlePlacement.speedVector(actor, cfg.speed);
            if (vector != null) velocity = new Vec3(vector.x(), vector.y(), vector.z());
        }
        int count = Math.max(0, cfg.count.evalInt(actor));
        Vector spread = cfg.spread.resolve(actor);

        List<Packet<?>> packets = null;
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (!ParticleBroadcast.inRange(player, cfg.force, origin.x, origin.y, origin.z)) continue;
            if (packets == null) {
                packets = ParticlePlacement.packets(opts, cfg.force, origin, velocity, count, spread,
                    scalarSpeed, level, actor, null, WORLD);
            }
            for (int p = 0; p < packets.size(); p++) {
                ParticleBroadcast.send(level, player, packets.get(p));
            }
        }
    }

    private static Vec3 anchor(Anchor anchor, BlockCtx ctx) {
        BlockPos pos = ctx.pos();
        return switch (anchor) {
            case HIT -> ctx.hit() != null ? ctx.hit() : Vec3.atCenterOf(pos);
            case CENTER -> Vec3.atCenterOf(pos);
            case CORNER -> Vec3.atLowerCornerOf(pos);
        };
    }
}
