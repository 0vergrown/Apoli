package dev.overgrown.apoli.client.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import static dev.overgrown.apoli.rope.RopeConstants.ROPE_WIDTH;

public final class RopeRenderer {
    private RopeRenderer() {}

    public static void render(WorldRenderContext context) {
        for (VerletRopeState rope : RopeClientManager.getAll()) {
            if (rope.points.size() < 2) continue;
            drawRope(context, rope);
        }
    }

    private static void drawRope(WorldRenderContext context, VerletRopeState rope) {
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        Camera camera = context.camera();
        if (poseStack == null || consumers == null || camera == null) return;

        Vec3 cameraPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer vc = consumers.getBuffer(RenderType.text(rope.texture));
        float tickDelta = Minecraft.getInstance().getFrameTime();

        for (int i = 0; i < rope.points.size() - 1; i++) {
            RopePoint p0 = rope.points.get(i);
            RopePoint p1 = rope.points.get(i + 1);

            Vec3 a = p0.prevPos.lerp(p0.pos, tickDelta);
            Vec3 b = p1.prevPos.lerp(p1.pos, tickDelta);
            int light = LevelRenderer.getLightColor(context.world(), BlockPos.containing(a.add(b).scale(0.5)));

            Vec3 dir = b.subtract(a);
            if (dir.lengthSqr() < 1e-6) continue;
            dir = dir.normalize();
            Vec3 view = cameraPos.subtract(a).normalize();
            Vec3 right = dir.cross(view);
            if (right.lengthSqr() < 1e-6) continue;
            right = right.normalize().scale(ROPE_WIDTH);

            Vec3 a1 = a.add(right);
            Vec3 a2 = a.subtract(right);
            Vec3 b1 = b.add(right);
            Vec3 b2 = b.subtract(right);

            vertex(poseStack, vc, a1, 0f, 0f, light);
            vertex(poseStack, vc, b1, 0f, 1f, light);
            vertex(poseStack, vc, b2, 1f, 1f, light);
            vertex(poseStack, vc, a2, 1f, 0f, light);
        }

        poseStack.popPose();
    }

    private static void vertex(PoseStack poseStack, VertexConsumer vc, Vec3 pos, float u, float v, int light) {
        vc.vertex(poseStack.last().pose(), (float) pos.x, (float) pos.y, (float) pos.z)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .uv2(light)
            .endVertex();
    }
}
