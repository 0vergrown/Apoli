package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.CameraPerspectiveC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class CameraPerspectiveReporter {

    private static @Nullable Boolean lastSent;

    private CameraPerspectiveReporter() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastSent = null;
            return;
        }
        boolean current = mc.options.getCameraType().isFirstPerson();
        if (lastSent != null && lastSent == current) return;
        if (!ClientPlayNetworking.canSend(CameraPerspectiveC2S.TYPE)) return;
        ClientPlayNetworking.send(new CameraPerspectiveC2S(current));
        dev.overgrown.apoli.entity.CameraPerspectives.set(mc.player.getUUID(), current);
        lastSent = current;
    }
}
