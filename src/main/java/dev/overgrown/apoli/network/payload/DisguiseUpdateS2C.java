package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DisguiseUpdateS2C(int entityId, Optional<DisguiseData> data) {
    public static final ResourceLocation CHANNEL = Apoli.id("disguise_update");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeOptional(data, (b, d) -> d.write(b));
    }

    public static DisguiseUpdateS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Optional<DisguiseData> data = buf.readOptional(DisguiseData::read);
        return new DisguiseUpdateS2C(entityId, data);
    }
}
