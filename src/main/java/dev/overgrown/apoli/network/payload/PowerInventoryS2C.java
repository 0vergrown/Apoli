package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PowerInventoryS2C(ResourceLocation powerId, CompoundTag contents) implements CustomPacketPayload {
    public static final Type<PowerInventoryS2C> TYPE = new Type<>(Apoli.id("power_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerInventoryS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        PowerInventoryS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeNbt(contents);
    }

    public static PowerInventoryS2C read(FriendlyByteBuf buf) {
        return new PowerInventoryS2C(buf.readResourceLocation(), buf.readNbt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
