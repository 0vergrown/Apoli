package dev.overgrown.apoli.client;

import dev.overgrown.apoli.entity.CameraPerspectives;
import dev.overgrown.apoli.network.payload.CameraPerspectiveC2S;
import dev.overgrown.apoli.network.payload.CameraTypeC2S;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public final class CameraPerspectiveReporter {

    private static int lastSent = -1;

    private CameraPerspectiveReporter() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastSent = -1;
            return;
        }
        CameraType type = mc.options.getCameraType();
        int current = type.isFirstPerson() ? 0 : type.isMirrored() ? 2 : 1;
        if (lastSent == current) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        if (ClientPlayNetworking.canSend(CameraTypeC2S.CHANNEL)) {
            new CameraTypeC2S(current).write(buf);
            ClientPlayNetworking.send(CameraTypeC2S.CHANNEL, buf);
        } else if (ClientPlayNetworking.canSend(CameraPerspectiveC2S.CHANNEL)) {
            new CameraPerspectiveC2S(current == 0).write(buf);
            ClientPlayNetworking.send(CameraPerspectiveC2S.CHANNEL, buf);
        } else {
            return;
        }
        CameraPerspectives.setType(mc.player.getUUID(), current);
        lastSent = current;
    }

    public static void reset() {
        lastSent = -1;
    }
}
