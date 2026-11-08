package dev.overgrown.apoli.client.glow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
final class GlowVertices {

    private GlowVertices() {}

    static void outline(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setUv(u, v);
    }

    static void solid(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                      float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(0xFFFFFFFF)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
