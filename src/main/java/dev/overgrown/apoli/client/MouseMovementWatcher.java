package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.MouseMovementC2S;
import dev.overgrown.apoli.power.builtin.ActionOnMouseMovementPower;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public final class MouseMovementWatcher {

    private static final float TURN_TO_DEGREES = 0.15F;
    private static final float EPSILON = 1.0E-4F;

    private static float yaw;
    private static float pitch;

    private MouseMovementWatcher() {}

    public static void onTurn(Entity entity, double rawYaw, double rawPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player) return;
        yaw += (float) rawYaw * TURN_TO_DEGREES;
        pitch += (float) rawPitch * TURN_TO_DEGREES;
    }

    public static void clientTick(Minecraft mc) {
        float sentYaw = yaw;
        float sentPitch = pitch;
        yaw = 0.0F;
        pitch = 0.0F;
        if (mc.player == null || mc.screen != null) return;
        if (Math.abs(sentYaw) < EPSILON && Math.abs(sentPitch) < EPSILON) return;
        if (!ActionOnMouseMovementPower.anyWatching(mc.player)) return;
        if (!ClientPlayNetworking.canSend(MouseMovementC2S.CHANNEL)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new MouseMovementC2S(
            Mth.clamp(sentYaw, -MouseMovementC2S.MAX_DEGREES, MouseMovementC2S.MAX_DEGREES),
            Mth.clamp(sentPitch, -MouseMovementC2S.MAX_DEGREES, MouseMovementC2S.MAX_DEGREES)).write(buf);
        ClientPlayNetworking.send(MouseMovementC2S.CHANNEL, buf);
    }

    public static void reset() {
        yaw = 0.0F;
        pitch = 0.0F;
    }
}
