package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PowerInventoryS2C(ResourceLocation powerId, CompoundTag contents) {
    public static final ResourceLocation CHANNEL = Apoli.id("power_inventory");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(powerId);
        buf.writeNbt(contents);
    }

    public static PowerInventoryS2C read(FriendlyByteBuf buf) {
        return new PowerInventoryS2C(buf.readResourceLocation(), buf.readNbt());
    }
}
