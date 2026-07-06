package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;


public record DisguiseUpdateS2C(int entityId, Optional<DisguiseData> data) implements CustomPacketPayload {
    public static final Type<DisguiseUpdateS2C> TYPE = new Type<>(Apoli.id("disguise_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisguiseUpdateS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        DisguiseUpdateS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeOptional(data, (b, d) -> d.write(b));
    }

    public static DisguiseUpdateS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Optional<DisguiseData> data = buf.readOptional(DisguiseData::read);
        return new DisguiseUpdateS2C(entityId, data);
    }

    @Override
    public Type<DisguiseUpdateS2C> type() {
        return TYPE;
    }
}
