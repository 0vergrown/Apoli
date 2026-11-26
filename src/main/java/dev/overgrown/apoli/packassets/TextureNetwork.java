package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// Data pack textures on the wire. Every message carries the generation of the list it belongs to.
public final class TextureNetwork {
    private TextureNetwork() {}

    // Keeps each request packet well under the 32 KB limit of client packets.
    public static final int REQUEST_BATCH = 100;

    public record Listed(ResourceLocation id, String hash, int size) {}

    public record Manifest(int generation, List<Listed> textures) implements CustomPacketPayload {
        public static final Type<Manifest> TYPE = new Type<>(Apoli.id("textures_manifest"));
        public static final StreamCodec<FriendlyByteBuf, Manifest> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf), Manifest::read);

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeVarInt(textures.size());
            for (Listed t : textures) {
                buf.writeResourceLocation(t.id);
                buf.writeUtf(t.hash, 40);
                buf.writeVarInt(t.size);
            }
        }

        public static Manifest read(FriendlyByteBuf buf) {
            int generation = buf.readVarInt();
            int count = Math.max(0, Math.min(buf.readVarInt(), PackTextures.MAX_COUNT));
            List<Listed> textures = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                textures.add(new Listed(buf.readResourceLocation(), buf.readUtf(40), buf.readVarInt()));
            }
            return new Manifest(generation, textures);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Request(int generation, boolean first, List<ResourceLocation> ids) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Apoli.id("textures_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf), Request::read);

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeBoolean(first);
            buf.writeVarInt(ids.size());
            for (ResourceLocation id : ids) buf.writeResourceLocation(id);
        }

        public static Request read(FriendlyByteBuf buf) {
            int generation = buf.readVarInt();
            boolean first = buf.readBoolean();
            int count = Math.max(0, Math.min(buf.readVarInt(), REQUEST_BATCH));
            List<ResourceLocation> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) ids.add(buf.readResourceLocation());
            return new Request(generation, first, ids);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Chunk(int generation, ResourceLocation id, int offset, byte[] data) implements CustomPacketPayload {
        public static final Type<Chunk> TYPE = new Type<>(Apoli.id("textures_chunk"));
        public static final StreamCodec<FriendlyByteBuf, Chunk> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> p.write(buf), Chunk::read);

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeResourceLocation(id);
            buf.writeVarInt(offset);
            buf.writeByteArray(data);
        }

        public static Chunk read(FriendlyByteBuf buf) {
            return new Chunk(buf.readVarInt(), buf.readResourceLocation(), buf.readVarInt(), buf.readByteArray(Outbox.CHUNK));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
