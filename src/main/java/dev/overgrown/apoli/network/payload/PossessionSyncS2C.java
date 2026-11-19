package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PossessionSyncS2C(int possessorId, int hostId) {
    public static final ResourceLocation CHANNEL = Apoli.id("possession_sync");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(possessorId);
        buf.writeVarInt(hostId + 1);
    }

    public static PossessionSyncS2C read(FriendlyByteBuf buf) {
        int possessorId = buf.readVarInt();
        return new PossessionSyncS2C(possessorId, buf.readVarInt() - 1);
    }
}
