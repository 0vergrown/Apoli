package dev.overgrown.apoli.mount;

import dev.overgrown.apoli.data.Space;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MountOffsets {
    private MountOffsets() {}

    public record Offset(double x, double y, double z, Space space, MountRotation rotation) {
        public Offset(double x, double y, double z, Space space) {
            this(x, y, z, space, MountRotation.HEAD);
        }

        public boolean isZero() {
            return x == 0.0 && y == 0.0 && z == 0.0;
        }
    }

    private static final Map<Integer, Offset> BY_PASSENGER = new ConcurrentHashMap<>();

    public static void put(@Nullable Entity passenger, @Nullable Offset offset) {
        if (passenger == null) return;
        put(passenger.getId(), offset);
    }

    public static void put(int passengerId, @Nullable Offset offset) {
        if (offset == null || offset.isZero()) BY_PASSENGER.remove(passengerId);
        else BY_PASSENGER.put(passengerId, offset);
    }

    public static void clear(int passengerId) {
        if (!BY_PASSENGER.isEmpty()) BY_PASSENGER.remove(passengerId);
    }

    public static void clearAll() {
        BY_PASSENGER.clear();
    }

    public static @Nullable Offset get(int passengerId) {
        return BY_PASSENGER.isEmpty() ? null : BY_PASSENGER.get(passengerId);
    }

    public static Vec3 resolve(Entity vehicle, Entity passenger) {
        Offset offset = get(passenger.getId());
        if (offset == null) return Vec3.ZERO;
        Vec3 local = new Vec3(offset.x(), offset.y(), offset.z());
        if (offset.rotation() == MountRotation.BODY && offset.space().isLocal()) {
            return Space.rotateByYaw(local, MountRotation.BODY.yawOf(vehicle));
        }
        return offset.space().toGlobal(vehicle, local);
    }

    public static Vec3 resolve(Entity vehicle, Entity passenger, float partialTick) {
        Offset offset = get(passenger.getId());
        if (offset == null) return Vec3.ZERO;
        Vec3 local = new Vec3(offset.x(), offset.y(), offset.z());
        if (offset.rotation() == MountRotation.BODY && offset.space().isLocal()) {
            return Space.rotateByYaw(local, MountRotation.BODY.yawOf(vehicle, partialTick));
        }
        return offset.space().toGlobal(vehicle, local);
    }

    public static void syncTo(ServerPlayer recipient, Entity passenger) {
        Offset offset = get(passenger.getId());
        if (offset == null) return;
        dev.overgrown.apoli.ApoliNetwork.sendMountOffset(recipient,
            new dev.overgrown.apoli.network.payload.MountOffsetS2C(
                passenger.getId(), offset.x(), offset.y(), offset.z(), offset.space(), offset.rotation()));
    }

    public static void syncAll(ServerPlayer recipient) {
        if (BY_PASSENGER.isEmpty()) return;
        for (Map.Entry<Integer, Offset> entry : BY_PASSENGER.entrySet()) {
            Entity passenger = recipient.level().getEntity(entry.getKey());
            if (passenger == null) continue;
            Offset offset = entry.getValue();
            dev.overgrown.apoli.ApoliNetwork.sendMountOffset(recipient,
                new dev.overgrown.apoli.network.payload.MountOffsetS2C(
                    entry.getKey(), offset.x(), offset.y(), offset.z(), offset.space(), offset.rotation()));
        }
    }
}
