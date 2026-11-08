package dev.overgrown.apoli.client.glow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class GlowVertices {

    private GlowVertices() {}

    static void outline(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float u, float v) {
        consumer.vertex(pose, x, y, z).uv(u, v).endVertex();
    }

    static void solid(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                      float normalX, float normalY, float normalZ) {
        consumer.vertex(pose.pose(), x, y, z)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(pose.normal(), normalX, normalY, normalZ)
            .endVertex();
    }
}
