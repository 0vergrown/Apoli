package dev.overgrown.apoli.client;

import dev.overgrown.apoli.entity.CameraPerspectives;
import dev.overgrown.apoli.network.payload.CameraPerspectiveC2S;
import dev.overgrown.apoli.network.payload.CameraTypeC2S;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CameraPerspectiveReporter {

    private static int lastSent = -1;

    private CameraPerspectiveReporter() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            lastSent = -1;
            return;
        }
        CameraType type = mc.options.getCameraType();
        int current = type.isFirstPerson() ? 0 : type.isMirrored() ? 2 : 1;
        if (lastSent == current) return;
        if (mc.getConnection().hasChannel(CameraTypeC2S.TYPE)) {
            PacketDistributor.sendToServer(new CameraTypeC2S(current));
        } else {
            PacketDistributor.sendToServer(new CameraPerspectiveC2S(current == 0));
        }
        CameraPerspectives.setType(mc.player.getUUID(), current);
        lastSent = current;
    }

    public static void reset() {
        lastSent = -1;
    }
}
