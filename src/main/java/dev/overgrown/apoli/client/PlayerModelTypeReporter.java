package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.PlayerModelType;
import dev.overgrown.apoli.network.payload.PlayerModelTypeC2S;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public final class PlayerModelTypeReporter {

    private static @Nullable PlayerModelType lastSent;

    private PlayerModelTypeReporter() {}

    public static void reset() {
        lastSent = null;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastSent = null;
            return;
        }
        PlayerModelType current = "slim".equals(mc.player.getModelName())
            ? PlayerModelType.SLIM
            : PlayerModelType.WIDE;
        if (current == lastSent) return;
        if (!ClientPlayNetworking.canSend(PlayerModelTypeC2S.CHANNEL)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new PlayerModelTypeC2S(current).write(buf);
        ClientPlayNetworking.send(PlayerModelTypeC2S.CHANNEL, buf);
        dev.overgrown.apoli.entity.PlayerModelTypes.set(mc.player.getUUID(), current);
        lastSent = current;
    }
}
