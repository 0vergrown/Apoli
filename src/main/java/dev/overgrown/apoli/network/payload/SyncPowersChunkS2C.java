package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record SyncPowersChunkS2C(int index, int total, byte[] slice) implements CustomPacketPayload {
    public static final Type<SyncPowersChunkS2C> TYPE = new Type<>(Apoli.id("sync_powers_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPowersChunkS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.index);
            buf.writeVarInt(payload.total);
            buf.writeByteArray(payload.slice);
        },
        buf -> new SyncPowersChunkS2C(buf.readVarInt(), buf.readVarInt(), buf.readByteArray()));

    public static byte[] encodeBlob(Map<ResourceLocation, String> rawPowers) {
        FriendlyByteBuf inner = new FriendlyByteBuf(Unpooled.buffer());
        inner.writeVarInt(rawPowers.size());
        for (Map.Entry<ResourceLocation, String> e : rawPowers.entrySet()) {
            inner.writeResourceLocation(e.getKey());
            inner.writeByteArray(e.getValue().getBytes(StandardCharsets.UTF_8));
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, inner.readableBytes() / 8));
            try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
                inner.readBytes(gz, inner.readableBytes());
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            inner.release();
        }
    }

    public static Map<ResourceLocation, String> decodeBlob(byte[] blob) throws IOException {
        byte[] raw;
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(blob))) {
            raw = gz.readAllBytes();
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(raw));
        int n = buf.readVarInt();
        Map<ResourceLocation, String> out = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            out.put(id, new String(buf.readByteArray(), StandardCharsets.UTF_8));
        }
        return out;
    }

    @Override
    public Type<SyncPowersChunkS2C> type() {
        return TYPE;
    }
}
