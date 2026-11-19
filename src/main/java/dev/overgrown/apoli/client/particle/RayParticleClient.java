package dev.overgrown.apoli.client.particle;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.RayParticlesS2C;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Environment(EnvType.CLIENT)
public final class RayParticleClient {
    private static final int MAX_POINTS = 4096;

    private RayParticleClient() {}

    public static void spawn(RayParticlesS2C payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        double length = payload.length();
        double step = Math.max(0.1, payload.spacing());
        int points = (int) Math.min(MAX_POINTS, Math.ceil(length / step));
        CustomParticle.aimNext(payload.dirX(), payload.dirY(), payload.dirZ());
        try {
            for (int i = 0; i < points; i++) {
                double t = i * step;
                if (t >= length) break;
                level.addParticle(payload.particle(), false,
                    payload.x() + payload.dirX() * t, payload.y() + payload.dirY() * t,
                    payload.z() + payload.dirZ() * t, 0.0, 0.0, 0.0);
            }
        } catch (RuntimeException e) {
            Apoli.LOGGER.warn("Could not spawn ray particles {}", payload.particle(), e);
        } finally {
            CustomParticle.stopAiming();
        }
    }
}
