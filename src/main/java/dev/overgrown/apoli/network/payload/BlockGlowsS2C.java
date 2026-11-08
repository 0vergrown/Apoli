package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BlockGlowsS2C(boolean reset, List<Entry> added, long[] removed) {

    public static final int MAX_ENTRIES = 4096;

    public record Entry(long pos, int block, int color, int flags, int remaining) {}

    public static final ResourceLocation CHANNEL = Apoli.id("block_glows");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(reset);
        buf.writeVarInt(added.size());
        for (Entry entry : added) {
            buf.writeLong(entry.pos());
            buf.writeVarInt(entry.block());
            buf.writeInt(entry.color());
            buf.writeByte(entry.flags());
            buf.writeVarInt(entry.remaining() + 1);
        }
        buf.writeVarInt(removed.length);
        for (long pos : removed) buf.writeLong(pos);
    }

    public static BlockGlowsS2C read(FriendlyByteBuf buf) {
        boolean reset = buf.readBoolean();
        int count = buf.readVarInt();
        List<Entry> added = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long pos = buf.readLong();
            int block = buf.readVarInt();
            int color = buf.readInt();
            int flags = buf.readByte();
            int remaining = buf.readVarInt() - 1;
            added.add(new Entry(pos, block, color, flags, remaining));
        }
        int removedCount = buf.readVarInt();
        long[] removed = new long[removedCount];
        for (int i = 0; i < removedCount; i++) removed[i] = buf.readLong();
        return new BlockGlowsS2C(reset, added, removed);
    }
}
