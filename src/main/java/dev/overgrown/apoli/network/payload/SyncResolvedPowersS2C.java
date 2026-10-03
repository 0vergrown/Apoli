package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncResolvedPowersS2C(List<ResourceLocation> powers, int[] targets) implements CustomPacketPayload {
    public static final Type<SyncResolvedPowersS2C> TYPE = new Type<>(Apoli.id("sync_resolved_powers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncResolvedPowersS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncResolvedPowersS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(powers.size());
        for (int i = 0; i < powers.size(); i++) {
            buf.writeResourceLocation(powers.get(i));
            buf.writeVarInt(targets[i] + 1);
        }
    }

    public static SyncResolvedPowersS2C read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ResourceLocation> powers = new ArrayList<>(count);
        int[] targets = new int[count];
        for (int i = 0; i < count; i++) {
            powers.add(buf.readResourceLocation());
            targets[i] = buf.readVarInt() - 1;
        }
        return new SyncResolvedPowersS2C(powers, targets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
