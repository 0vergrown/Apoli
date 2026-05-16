package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SyncEntityPowersS2C(int entityId, Map<ResourceLocation, Set<ResourceLocation>> powersBySource) implements CustomPacketPayload {
    public static final Type<SyncEntityPowersS2C> TYPE = new Type<>(Apoli.id("sync_entity_powers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntityPowersS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncEntityPowersS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(powersBySource.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : powersBySource.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeCollection(e.getValue(), FriendlyByteBuf::writeResourceLocation);
        }
    }

    public static SyncEntityPowersS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int n = buf.readVarInt();
        Map<ResourceLocation, Set<ResourceLocation>> map = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation key = buf.readResourceLocation();
            Set<ResourceLocation> srcs = new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation));
            map.put(key, srcs);
        }
        return new SyncEntityPowersS2C(entityId, map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
