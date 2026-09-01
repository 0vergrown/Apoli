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

    public static final Codec<Either<Expression, Vector>> SPEED_CODEC =
        Codec.either(Expression.FLOAT_OR_EXPR, Vector.CODEC);

    public static final Either<Expression, Vector> NO_SPEED = Either.left(Expression.constant(0));

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

    public static Vec3 velocity(Entity entity, @Nullable ModelPartAnchor.Frame frame, Optional<Space> space,
                                float velocityX, float velocityY, float velocityZ,
                                Either<Expression, Vector> speed) {
        Vec3 raw = new Vec3(velocityX, velocityY, velocityZ);
        if (raw.lengthSqr() < 1.0e-9) {
            Vector vector = speed.right().orElse(null);
            if (vector == null) return Vec3.ZERO;
            raw = new Vec3(vector.x(), vector.y(), vector.z());
            if (raw.lengthSqr() < 1.0e-9) return Vec3.ZERO;
        }
        if (frame != null && space.isEmpty()) return frame.direction(raw);
        return space.orElse(Space.WORLD).toGlobal(entity, raw);
    }

    public static float scalarSpeed(Entity entity, Either<Expression, Vector> speed) {
        return speed.left().map(expression -> (float) expression.eval(entity)).orElse(0.0F);
    }

    public static List<Packet<?>> packets(ParticleOptions options, boolean force, Vec3 origin, Vec3 velocity,
                                          int count, Vector spread, float scalarSpeed, ServerLevel level) {
        if (velocity.lengthSqr() < 1.0e-9) {
            return List.of(ParticleBroadcast.packet(options, force, origin.x, origin.y, origin.z,
                count, spread.x(), spread.y(), spread.z(), scalarSpeed));
        }
        int emitted = Math.max(1, Math.min(count, MAX_DIRECTED_PACKETS));
        List<Packet<?>> out = new ArrayList<>(emitted);
        for (int i = 0; i < emitted; i++) {
            double x = origin.x + jitter(level, spread.x());
            double y = origin.y + jitter(level, spread.y());
            double z = origin.z + jitter(level, spread.z());
            out.add(ParticleBroadcast.packet(options, force, x, y, z,
                0, (float) velocity.x, (float) velocity.y, (float) velocity.z, 1.0F));
        }
        return out;
    }

    private static double jitter(ServerLevel level, float spread) {
        return spread == 0.0F ? 0.0 : level.random.nextGaussian() * spread;
    }
}
