package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// Data pack assets on the wire: the server lists its files, the client asks for the ones it lacks, and the server
// sends them in chunks.
public final class AssetNetwork {
    private AssetNetwork() {}

    public static final ResourceLocation MANIFEST = Apoli.id("assets_manifest");
    public static final ResourceLocation REQUEST = Apoli.id("assets_request");
    public static final ResourceLocation CHUNK = Apoli.id("assets_chunk");
    public static final int MAX_PATH = 256;
    public static final int MANIFEST_BATCH = 1000;
    public static final int REQUEST_BATCH = 60;

    // One file: its path under assets/, the SHA-1 of its contents and its size in bytes.
    public record Listed(String path, String hash, int size) {}

    // packHash names the whole set, so a client that already has it does nothing. first starts a new list and last
    // completes it.
    public record Manifest(int generation, String packHash, boolean first, boolean last, List<Listed> files) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeUtf(packHash, 40);
            buf.writeBoolean(first);
            buf.writeBoolean(last);
            buf.writeVarInt(files.size());
            for (Listed f : files) {
                buf.writeUtf(f.path, MAX_PATH);
                buf.writeUtf(f.hash, 40);
                buf.writeVarInt(f.size);
            }
        }

        public static Manifest read(FriendlyByteBuf buf) {
            int generation = buf.readVarInt();
            String packHash = buf.readUtf(40);
            boolean first = buf.readBoolean();
            boolean last = buf.readBoolean();
            int count = Math.max(0, Math.min(buf.readVarInt(), MANIFEST_BATCH));
            List<Listed> files = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                files.add(new Listed(buf.readUtf(MAX_PATH), buf.readUtf(40), buf.readVarInt()));
            }
            return new Manifest(generation, packHash, first, last, files);
        }
    }

    public record Request(int generation, boolean first, List<String> paths) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeBoolean(first);
            buf.writeVarInt(paths.size());
            for (String path : paths) buf.writeUtf(path, MAX_PATH);
        }

        public static Request read(FriendlyByteBuf buf) {
            int generation = buf.readVarInt();
            boolean first = buf.readBoolean();
            int count = Math.max(0, Math.min(buf.readVarInt(), REQUEST_BATCH));
            List<String> paths = new ArrayList<>(count);
            for (int i = 0; i < count; i++) paths.add(buf.readUtf(MAX_PATH));
            return new Request(generation, first, paths);
        }
    }

    // data is the slice of the file that starts at offset.
    public record Chunk(int generation, String path, int offset, byte[] data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(generation);
            buf.writeUtf(path, MAX_PATH);
            buf.writeVarInt(offset);
            buf.writeByteArray(data);
        }

        public static Chunk read(FriendlyByteBuf buf) {
            return new Chunk(buf.readVarInt(), buf.readUtf(MAX_PATH), buf.readVarInt(), buf.readByteArray(Outbox.CHUNK));
        }
    }
}
