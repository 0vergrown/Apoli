package dev.overgrown.apoli.mount;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.payload.MountOffsetS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MountOffsets {
    private MountOffsets() {}

    public record Offset(double x, double y, double z, Space space, MountRotation rotation) {
        public static final Offset NONE = new Offset(0.0, 0.0, 0.0, Space.WORLD, MountRotation.HEAD);

        public Offset(double x, double y, double z, Space space) {
            this(x, y, z, space, MountRotation.HEAD);
        }

        public boolean isZero() {
            return x == 0.0 && y == 0.0 && z == 0.0;
        }
    }

    private static final Map<Integer, Offset> SERVER = new ConcurrentHashMap<>();
    private static final Map<Integer, Offset> CLIENT = new ConcurrentHashMap<>();

    private static Map<Integer, Offset> storeOf(Entity entity) {
        return entity.level().isClientSide() ? CLIENT : SERVER;
    }

    public static void put(@Nullable Entity passenger, @Nullable Offset offset) {
        if (passenger == null) return;
        store(storeOf(passenger), passenger.getId(), offset);
    }

    public static void putClient(int passengerId, @Nullable Offset offset) {
        store(CLIENT, passengerId, offset);
    }

    private static void store(Map<Integer, Offset> offsets, int passengerId, @Nullable Offset offset) {
        if (offset == null || offset.isZero()) offsets.remove(passengerId);
        else offsets.put(passengerId, offset);
    }

    public static void clear(Entity passenger) {
        Map<Integer, Offset> offsets = storeOf(passenger);
        if (!offsets.isEmpty()) offsets.remove(passenger.getId());
    }

    public static void clearServer() {
        SERVER.clear();
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static @Nullable Offset get(Entity passenger) {
        Map<Integer, Offset> offsets = storeOf(passenger);
        return offsets.isEmpty() ? null : offsets.get(passenger.getId());
    }

    public static Vec3 resolve(Entity vehicle, Entity passenger) {
        Offset offset = get(passenger);
        return offset == null ? Vec3.ZERO : resolve(vehicle, offset);
    }

    public static Vec3 resolve(Entity vehicle, Offset offset) {
        Vec3 local = new Vec3(offset.x(), offset.y(), offset.z());
        if (offset.rotation() == MountRotation.BODY && offset.space().isLocal()) {
            return Space.rotateByYaw(local, MountRotation.BODY.yawOf(vehicle));
        }
        return offset.space().toGlobal(vehicle, local);
    }

    public static Vec3 resolve(Entity vehicle, Entity passenger, float partialTick) {
        Offset offset = get(passenger);
        if (offset == null) return Vec3.ZERO;
        Vec3 local = new Vec3(offset.x(), offset.y(), offset.z());
        if (offset.rotation() == MountRotation.BODY && offset.space().isLocal()) {
            return Space.rotateByYaw(local, MountRotation.BODY.yawOf(vehicle, partialTick));
        }
        return offset.space().toGlobal(vehicle, local);
    }

    public static void broadcast(Entity passenger) {
        Offset offset = SERVER.get(passenger.getId());
        ApoliNetwork.broadcastMountOffset(passenger, payload(passenger.getId(), offset == null ? Offset.NONE : offset));
    }

    public static void syncPassengers(Entity vehicle) {
        if (SERVER.isEmpty()) return;
        List<Entity> passengers = vehicle.getPassengers();
        for (int i = 0, n = passengers.size(); i < n; i++) {
            int passengerId = passengers.get(i).getId();
            Offset offset = SERVER.get(passengerId);
            if (offset != null) ApoliNetwork.broadcastMountOffset(vehicle, payload(passengerId, offset));
        }
    }

    public static void syncPairing(ServerPlayer recipient, Entity entity) {
        if (SERVER.isEmpty()) return;
        sendPassengers(recipient, entity);
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) sendPassengers(recipient, vehicle);
    }

    private static void sendPassengers(ServerPlayer recipient, Entity vehicle) {
        List<Entity> passengers = vehicle.getPassengers();
        for (int i = 0, n = passengers.size(); i < n; i++) {
            int passengerId = passengers.get(i).getId();
            Offset offset = SERVER.get(passengerId);
            if (offset != null) ApoliNetwork.sendMountOffset(recipient, payload(passengerId, offset));
        }
    }

    private static MountOffsetS2C payload(int passengerId, Offset offset) {
        return new MountOffsetS2C(passengerId, offset.x(), offset.y(), offset.z(), offset.space(), offset.rotation());
    }
}
