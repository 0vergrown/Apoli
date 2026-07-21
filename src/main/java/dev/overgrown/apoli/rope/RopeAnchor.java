package dev.overgrown.apoli.rope;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public sealed interface RopeAnchor permits RopeAnchor.Position, RopeAnchor.OfEntity {

    @Nullable Vec3 position(Level level);

    @Nullable Entity entity(Level level);

    boolean movable();

    void write(FriendlyByteBuf buf);

    static RopeAnchor read(FriendlyByteBuf buf) {
        if (buf.readBoolean()) return new OfEntity(buf.readVarInt(), readVec(buf));
        return new Position(readVec(buf));
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    record Position(Vec3 pos) implements RopeAnchor {
        @Override public Vec3 position(Level level) { return pos; }
        @Override public Entity entity(Level level) { return null; }
        @Override public boolean movable() { return false; }
        @Override public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(false);
            writeVec(buf, pos);
        }
    }

    record OfEntity(int networkId, Vec3 offset) implements RopeAnchor {
        @Override public Vec3 position(Level level) {
            Entity e = entity(level);
            return e == null ? null : e.getBoundingBox().getCenter().add(offset);
        }
        @Override public Entity entity(Level level) { return level.getEntity(networkId); }
        @Override public boolean movable() { return true; }
        @Override public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(true);
            buf.writeVarInt(networkId);
            writeVec(buf, offset);
        }
    }
}
