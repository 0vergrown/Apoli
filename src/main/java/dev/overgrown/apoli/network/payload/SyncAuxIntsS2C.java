package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncAuxIntsS2C(int entityId, Map<ResourceLocation, Integer> auxInt) {

    public static final ResourceLocation CHANNEL = Apoli.id("sync_aux_ints");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(auxInt.size());
        for (Map.Entry<ResourceLocation, Integer> entry : auxInt.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static SyncAuxIntsS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, Integer> values = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            values.put(buf.readResourceLocation(), buf.readVarInt());
        }
        return new SyncAuxIntsS2C(entityId, values);
    }
}
