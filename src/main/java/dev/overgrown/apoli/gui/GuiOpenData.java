package dev.overgrown.apoli.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record GuiOpenData(ResourceLocation power, double[] sliders) {
    private static final int MAX_SLIDERS = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, GuiOpenData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> data.write(buf),
        GuiOpenData::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
        buf.writeVarInt(sliders.length);
        for (double value : sliders) buf.writeDouble(value);
    }

    public static GuiOpenData read(FriendlyByteBuf buf) {
        ResourceLocation power = buf.readResourceLocation();
        int count = Math.min(buf.readVarInt(), MAX_SLIDERS);
        double[] sliders = new double[count];
        for (int k = 0; k < count; k++) sliders[k] = buf.readDouble();
        return new GuiOpenData(power, sliders);
    }
}
