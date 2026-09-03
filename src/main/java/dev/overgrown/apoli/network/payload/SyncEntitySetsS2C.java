package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncEntitySetsS2C(ResourceLocation powerId, List<UUID> members) implements CustomPacketPayload {

    public static final Type<SyncEntitySetsS2C> TYPE = new Type<>(Apoli.id("sync_entity_sets"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntitySetsS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncEntitySetsS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeVarInt(members.size());
        for (int i = 0; i < members.size(); i++) buf.writeUUID(members.get(i));
    }

    public static SyncEntitySetsS2C read(FriendlyByteBuf buf) {
        ResourceLocation powerId = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<UUID> members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) members.add(buf.readUUID());
        return new SyncEntitySetsS2C(powerId, members);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
