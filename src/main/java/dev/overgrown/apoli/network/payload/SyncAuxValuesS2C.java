package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SyncAuxValuesS2C(int entityId, Object2DoubleMap<ResourceLocation> values) {

    public static final ResourceLocation CHANNEL = Apoli.id("sync_aux_values");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        AuxValues.write(buf, values);
    }

    public static SyncAuxValuesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new SyncAuxValuesS2C(entityId, AuxValues.read(buf));
    }
}
