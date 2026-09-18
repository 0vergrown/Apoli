package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TickRateS2C(int entityId, int rate, int baseRate) {
    public static final ResourceLocation CHANNEL = Apoli.id("tick_rate");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(rate + 1);
        buf.writeVarInt(baseRate);
    }

    public static TickRateS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int rate = buf.readVarInt() - 1;
        int baseRate = Math.max(1, buf.readVarInt());
        return new TickRateS2C(entityId, rate, baseRate);
    }
}
