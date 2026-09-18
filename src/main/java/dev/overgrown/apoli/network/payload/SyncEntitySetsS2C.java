package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncEntitySetsS2C(ResourceLocation powerId, List<UUID> members) {

    public static final ResourceLocation CHANNEL = Apoli.id("sync_entity_sets");

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
}
