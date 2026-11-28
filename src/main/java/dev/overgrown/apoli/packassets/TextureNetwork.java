package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// Data pack textures on the wire. Every message carries the generation of the list it belongs to.
public final class TextureNetwork {
    private TextureNetwork() {}

    public static final ResourceLocation MANIFEST = Apoli.id("textures_manifest");
    public static final ResourceLocation REQUEST = Apoli.id("textures_request");
    public static final ResourceLocation CHUNK = Apoli.id("textures_chunk");
    // Keeps each request packet well under the 32 KB limit of client packets.
    public static final int REQUEST_BATCH = 100;

    public record Listed(ResourceLocation id, String hash, int size) {}

    public record Manifest(int generation, List<Listed> textures) {
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
    }

    public record Request(int generation, boolean first, List<ResourceLocation> ids) {
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
    }

    public record Chunk(int generation, ResourceLocation id, int offset, byte[] data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeResourceLocation(id);
            buf.writeVarInt(offset);
            buf.writeByteArray(data);
        }

        public static Chunk read(FriendlyByteBuf buf) {
            return new Chunk(buf.readVarInt(), buf.readResourceLocation(), buf.readVarInt(), buf.readByteArray(Outbox.CHUNK));
        }
    }
}
