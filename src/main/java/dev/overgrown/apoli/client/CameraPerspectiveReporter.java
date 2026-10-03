package dev.overgrown.apoli.client;

import dev.overgrown.apoli.entity.CameraPerspectives;
import dev.overgrown.apoli.network.payload.CameraPerspectiveC2S;
import dev.overgrown.apoli.network.payload.CameraTypeC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

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
        if (ClientPlayNetworking.canSend(CameraTypeC2S.TYPE)) {
            ClientPlayNetworking.send(new CameraTypeC2S(current));
        } else if (ClientPlayNetworking.canSend(CameraPerspectiveC2S.TYPE)) {
            ClientPlayNetworking.send(new CameraPerspectiveC2S(current == 0));
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
