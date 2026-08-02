package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.PlayerModelType;
import dev.overgrown.apoli.network.payload.PlayerModelTypeC2S;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
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
        PlayerModelType current = mc.player.getSkin().model() == PlayerSkin.Model.SLIM
            ? PlayerModelType.SLIM
            : PlayerModelType.WIDE;
        if (current == lastSent) return;
        PacketDistributor.sendToServer(new PlayerModelTypeC2S(current));
        dev.overgrown.apoli.entity.PlayerModelTypes.set(mc.player.getUUID(), current);
        lastSent = current;
    }
}
