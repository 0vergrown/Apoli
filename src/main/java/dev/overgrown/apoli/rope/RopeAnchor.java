package dev.overgrown.apoli.rope;

import dev.overgrown.apoli.compat.sable.SableSubLevels;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public sealed interface RopeAnchor permits RopeAnchor.Position, RopeAnchor.OfEntity, RopeAnchor.OfSubLevel {

    byte KIND_POSITION = 0;
    byte KIND_ENTITY = 1;
    byte KIND_SUB_LEVEL = 2;

    @Nullable Vec3 position(Level level);

    @Nullable Entity entity(Level level);

    boolean movable();

    void write(FriendlyByteBuf buf);

    static RopeAnchor read(FriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case KIND_ENTITY -> new OfEntity(buf.readVarInt(), readVec(buf));
            case KIND_SUB_LEVEL -> new OfSubLevel(buf.readUUID(), readVec(buf));
            default -> new Position(readVec(buf));
        };
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
        @Override
        public Vec3 position(Level level) {
            return pos;
        }

        @Override
        public Entity entity(Level level) {
            return null;
        }

        @Override
        public boolean movable() {
            return false;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(KIND_POSITION);
            writeVec(buf, pos);
        }
    }

    record OfSubLevel(UUID subLevel, Vec3 local) implements RopeAnchor {

        @Override
        public @Nullable Vec3 position(Level level) {
            return SableSubLevels.toWorld(level, subLevel, local);
        }

        @Override
        public Entity entity(Level level) {
            return null;
        }

        @Override
        public boolean movable() {
            return true;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(KIND_SUB_LEVEL);
            buf.writeUUID(subLevel);
            writeVec(buf, local);
        }
    }

    record OfEntity(int networkId, Vec3 offset) implements RopeAnchor {

        @Override
        public Vec3 position(Level level) {
            Entity e = entity(level);
            return e == null ? null : e.getBoundingBox().getCenter().add(offset);
        }

        @Override
        public Entity entity(Level level) {
            return level.getEntity(networkId);
        }

        @Override
        public boolean movable() {
            return true;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(KIND_ENTITY);
            buf.writeVarInt(networkId);
            writeVec(buf, offset);
        }
    }
}
