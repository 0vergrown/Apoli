package dev.overgrown.apoli.client.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static dev.overgrown.apoli.rope.RopeConstants.ROPE_WIDTH;

public final class RopeRenderer {
    private RopeRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (RopeClientManager.getAll().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float tickDelta = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        boolean drewAny = false;
        for (VerletRopeState rope : RopeClientManager.getAll()) {
            if (rope.points.size() < 2) continue;
            drawRope(poseStack, buffers, camera, level, tickDelta, rope);
            drewAny = true;
        }
        if (drewAny) buffers.endBatch();
    }

    private static void drawRope(PoseStack poseStack, MultiBufferSource consumers, Camera camera,
                                 ClientLevel level, float tickDelta, VerletRopeState rope) {
        Vec3 cameraPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer vc = consumers.getBuffer(RenderType.text(rope.texture));

        for (int i = 0; i < rope.points.size() - 1; i++) {
            RopePoint p0 = rope.points.get(i);
            RopePoint p1 = rope.points.get(i + 1);

            Vec3 a = p0.prevPos.lerp(p0.pos, tickDelta);
            Vec3 b = p1.prevPos.lerp(p1.pos, tickDelta);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(a.add(b).scale(0.5)));

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
        PoseStack.Pose entry = poseStack.last();
        vc.addVertex(entry.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0, 1, 0);
    }
}
