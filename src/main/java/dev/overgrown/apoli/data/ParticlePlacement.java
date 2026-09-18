package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ParticlePlacement {

    public static final Codec<Either<Expression, ExprVector>> SPEED_CODEC =
        Codec.either(Expression.FLOAT_OR_EXPR, ExprVector.CODEC);

    public static final Either<Expression, ExprVector> NO_SPEED = Either.left(Expression.constant(0));

    private static final int MAX_DIRECTED_PACKETS = 64;

    private ParticlePlacement() {}

    public static @Nullable ModelPartAnchor.Frame frame(Entity entity, Optional<String> modelPart) {
        return modelPart.isPresent() ? ModelPartAnchor.frameOf(entity, modelPart.get()) : null;
    }

    public static Vec3 origin(Entity entity, @Nullable ModelPartAnchor.Frame frame, Optional<Space> space,
                              float offsetX, float offsetY, float offsetZ) {
        Vec3 offset = new Vec3(offsetX, offsetY, offsetZ);
        if (frame == null) return entity.position().add(space.orElse(Space.WORLD).toGlobal(entity, offset));
        Vec3 base = entity.position().add(frame.pivot());
        return base.add(space.isPresent() ? space.get().toGlobal(entity, offset) : frame.direction(offset));
    }

    public static @Nullable Vector speedVector(@Nullable Entity entity, Either<Expression, ExprVector> speed) {
        ExprVector vector = speed.right().orElse(null);
        return vector == null ? null : vector.resolve(entity);
    }

    public static Vec3 velocity(Entity entity, @Nullable ModelPartAnchor.Frame frame, Optional<Space> space,
                                float velocityX, float velocityY, float velocityZ,
                                @Nullable Vector speedVector) {
        Vec3 raw = new Vec3(velocityX, velocityY, velocityZ);
        if (raw.lengthSqr() < 1.0e-9) {
            if (speedVector == null) return Vec3.ZERO;
            raw = new Vec3(speedVector.x(), speedVector.y(), speedVector.z());
            if (raw.lengthSqr() < 1.0e-9) return Vec3.ZERO;
        }
        if (frame != null && space.isEmpty()) return frame.direction(raw);
        return space.orElse(Space.WORLD).toGlobal(entity, raw);
    }

    public static float scalarSpeed(@Nullable Entity entity, Either<Expression, ExprVector> speed) {
        Expression expression = speed.left().orElse(null);
        return expression == null ? 0.0F : (float) expression.eval(entity);
    }

    public static List<Packet<?>> packets(ParticleOptions options, boolean force, Vec3 origin, Vec3 velocity,
                                          int count, Vector spread, float scalarSpeed, ServerLevel level,
                                          Entity entity, @Nullable ModelPartAnchor.Frame frame,
                                          Optional<Space> space) {
        boolean directed = velocity.lengthSqr() >= 1.0e-9;
        if (!directed && !orientedSpread(spread, frame, space)) {
            return List.of(ParticleBroadcast.packet(options, force, origin.x, origin.y, origin.z,
                count, spread.x(), spread.y(), spread.z(), scalarSpeed));
        }
        int emitted = Math.max(1, Math.min(count, MAX_DIRECTED_PACKETS));
        List<Packet<?>> out = new ArrayList<>(emitted);
        for (int i = 0; i < emitted; i++) {
            Vec3 at = origin.add(spreadOffset(level, spread, entity, frame, space));
            if (directed) {
                out.add(ParticleBroadcast.packet(options, force, at.x, at.y, at.z,
                    0, (float) velocity.x, (float) velocity.y, (float) velocity.z, 1.0F));
            } else {
                int share = count / emitted + (i < count % emitted ? 1 : 0);
                out.add(ParticleBroadcast.packet(options, force, at.x, at.y, at.z,
                    share, 0.0F, 0.0F, 0.0F, scalarSpeed));
            }
        }
        return out;
    }

    private static boolean orientedSpread(Vector spread, @Nullable ModelPartAnchor.Frame frame,
                                          Optional<Space> space) {
        boolean rotates = space.isPresent() ? space.get() != Space.WORLD : frame != null;
        if (!rotates) return false;
        float x = spread.x();
        float y = spread.y();
        float z = spread.z();
        if (x == 0.0F && y == 0.0F && z == 0.0F) return false;
        return x != y || y != z;
    }

    private static Vec3 spreadOffset(ServerLevel level, Vector spread, Entity entity,
                                     @Nullable ModelPartAnchor.Frame frame, Optional<Space> space) {
        Vec3 raw = new Vec3(jitter(level, spread.x()), jitter(level, spread.y()), jitter(level, spread.z()));
        if (raw.lengthSqr() < 1.0e-12) return Vec3.ZERO;
        if (space.isPresent()) return space.get().toGlobal(entity, raw);
        if (frame != null) return frame.direction(raw);
        return raw;
    }

    private static double jitter(ServerLevel level, float spread) {
        return spread == 0.0F ? 0.0 : level.random.nextGaussian() * spread;
    }
}
