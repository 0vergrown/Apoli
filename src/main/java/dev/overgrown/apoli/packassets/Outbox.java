package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

// Files on their way to players, in 30 KB chunks. A player only gets more once the chunks already handed to the
// connection are written, so a slow connection isn't flooded. What one player can ask for per list is capped.
final class Outbox<K> {
    interface ChunkWriter<K> {
        void write(FriendlyByteBuf buf, int generation, K key, int offset, byte[] slice);
    }

    static final int CHUNK = 30 * 1024;
    private static final int WINDOW = 8;

    private static final class Transfer<K> {
        final K key;
        final byte[] data;
        int offset;

        Transfer(K key, byte[] data) {
            this.key = key;
            this.data = data;
        }
    }

    private static final class State<K> {
        final ArrayDeque<Transfer<K>> queue = new ArrayDeque<>();
        final AtomicInteger inFlight = new AtomicInteger();
        int generation = Integer.MIN_VALUE;
        long served;
        boolean warned;
    }

    private final String what;
    private final ResourceLocation channel;
    private final ChunkWriter<K> writer;
    private final Map<UUID, State<K>> states = new HashMap<>();

    Outbox(String what, ResourceLocation channel, ChunkWriter<K> writer) {
        this.what = what;
        this.channel = channel;
        this.writer = writer;
    }

    void reset(UUID player) {
        State<K> state = states.get(player);
        if (state != null) state.queue.clear();
    }

    void forget(UUID player) {
        states.remove(player);
    }

    void clear() {
        states.clear();
    }

    // budget is the most bytes one player can be sent for one generation (twice the total, so a failed download
    // can be retried).
    void request(ServerPlayer player, int generation, boolean first, List<K> keys, Map<K, byte[]> files,
                 long budget, int maxQueued) {
        State<K> state = states.computeIfAbsent(player.getUUID(), id -> new State<>());
        if (state.generation != generation) {
            state.generation = generation;
            state.served = 0;
            state.warned = false;
            state.queue.clear();
        }
        if (first) state.queue.clear();
        for (K key : keys) {
            byte[] data = files.get(key);
            if (data == null) continue;
            if (state.queue.size() >= maxQueued || state.served + data.length > budget) {
                if (!state.warned) {
                    state.warned = true;
                    Apoli.LOGGER.warn("[Apoli] {} asked for more {} than the server has; the extra requests are ignored.",
                        player.getName().getString(), what);
                }
                return;
            }
            state.served += data.length;
            state.queue.add(new Transfer<>(key, data));
        }
    }

    void tick(List<ServerPlayer> players, int generation) {
        if (states.isEmpty()) return;
        for (ServerPlayer player : players) {
            State<K> state = states.get(player.getUUID());
            if (state == null || state.queue.isEmpty() || player.connection == null) continue;
            if (state.generation != generation) {
                state.queue.clear();
                continue;
            }
            AtomicInteger inFlight = state.inFlight;
            while (inFlight.get() < WINDOW && !state.queue.isEmpty()) {
                Transfer<K> transfer = state.queue.peek();
                int length = Math.min(CHUNK, transfer.data.length - transfer.offset);
                byte[] slice = Arrays.copyOfRange(transfer.data, transfer.offset, transfer.offset + length);
                FriendlyByteBuf buf = PacketByteBufs.create();
                writer.write(buf, generation, transfer.key, transfer.offset, slice);
                inFlight.incrementAndGet();
                // Runs on the network thread once the chunk is written, or the connection failed.
                ServerPlayNetworking.getSender(player).sendPacket(channel, buf,
                    PacketSendListener.thenRun(inFlight::decrementAndGet));
                transfer.offset += length;
                if (transfer.offset >= transfer.data.length) state.queue.poll();
            }
        }
    }
}
