package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GeometryRenderer {
    private GeometryRenderer() {}

    public static void resetAll(CustomModel model) {
        CustomModel.Bone[] all = model.bones();
        for (int i = 0; i < all.length; i++) {
            all[i].reset();
        }
    }

    public static void syncPlayer(CustomModel custom, PlayerModel<AbstractClientPlayer> live,
                                  PlayerModel<AbstractClientPlayer> rest) {
        resetAll(custom);
        syncBone(custom, ModelParts.HEAD, live.head, rest.head);
        syncBone(custom, ModelParts.HAT, live.hat, rest.hat);
        syncBone(custom, ModelParts.BODY, live.body, rest.body);
        syncBone(custom, ModelParts.RIGHT_ARM, live.rightArm, rest.rightArm);
        syncBone(custom, ModelParts.LEFT_ARM, live.leftArm, rest.leftArm);
        syncBone(custom, ModelParts.RIGHT_LEG, live.rightLeg, rest.rightLeg);
        syncBone(custom, ModelParts.LEFT_LEG, live.leftLeg, rest.leftLeg);
    }

    public static void syncBone(CustomModel model, String normalizedName, @Nullable ModelPart live, @Nullable ModelPart rest) {
        if (live == null) {
            return;
        }
        CustomModel.Bone[] bound = model.bones(normalizedName);
        for (int i = 0; i < bound.length; i++) {
            pose(bound[i], live, rest);
        }
    }

    private static void pose(CustomModel.Bone bone, ModelPart live, @Nullable ModelPart rest) {
        bone.reset();
        ModelPart part = bone.part;
        part.xScale = live.xScale;
        part.yScale = live.yScale;
        part.zScale = live.zScale;
        if (rest == null) {
            part.xRot += live.xRot;
            part.yRot += live.yRot;
            part.zRot += live.zRot;
            return;
        }
        part.x += live.x - rest.x;
        part.y += live.y - rest.y;
        part.z += live.z - rest.z;
        part.xRot += live.xRot - rest.xRot;
        part.yRot += live.yRot - rest.yRot;
        part.zRot += live.zRot - rest.zRot;
    }

    public static void applyVisibility(CustomModel model, List<String> bodyParts) {
        CustomModel.Bone[] all = model.bones();
        if (bodyParts.isEmpty()) {
            for (int i = 0; i < all.length; i++) {
                all[i].part.visible = true;
            }
            return;
        }
        for (int i = 0; i < all.length; i++) {
            all[i].part.visible = false;
        }
        for (int i = 0; i < bodyParts.size(); i++) {
            CustomModel.Bone[] bound = model.bones(ModelParts.normalize(bodyParts.get(i)));
            for (int j = 0; j < bound.length; j++) {
                bound[j].part.visible = true;
            }
        }
    }

    public static void draw(GeometryRender render, CustomModel model, PoseStack pose, MultiBufferSource buffers, int light) {
        draw(render, model, pose, buffers, light, null);
    }

    public static void draw(GeometryRender render, CustomModel model, PoseStack pose, MultiBufferSource buffers,
                            int light, @Nullable Entity subject) {
        VertexConsumer consumer = buffers.getBuffer(
            OverlayRenderTypes.forMode(render.mode(), DynamicTextures.resolve(render.texture(), subject)));
        int color = FastColor.ARGB32.colorFromFloat(render.alpha(), render.red(), render.green(), render.blue());
        boolean scaled = render.scale() != 1.0F;
        if (scaled) {
            pose.pushPose();
            pose.scale(render.scale(), render.scale(), render.scale());
        }
        model.root().render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (scaled) {
            pose.popPose();
        }
    }

    public static void drawSlot(GeometryRender render, CustomModel model, String normalizedName, float alphaScale,
                                PoseStack pose, MultiBufferSource buffers, int light) {
        drawSlot(render, model, normalizedName, alphaScale, pose, buffers, light, null);
    }

    public static void drawSlot(GeometryRender render, CustomModel model, String normalizedName, float alphaScale,
                                PoseStack pose, MultiBufferSource buffers, int light, @Nullable Entity subject) {
        CustomModel.Bone[] bound = model.bones(normalizedName);
        if (bound.length == 0) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(
            OverlayRenderTypes.forMode(render.mode(), DynamicTextures.resolve(render.texture(), subject)));
        int color = FastColor.ARGB32.colorFromFloat(render.alpha() * alphaScale, render.red(), render.green(), render.blue());
        boolean scaled = render.scale() != 1.0F;
        if (scaled) {
            pose.pushPose();
            pose.scale(render.scale(), render.scale(), render.scale());
        }
        for (int i = 0; i < bound.length; i++) {
            bound[i].part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
        }
        if (scaled) {
            pose.popPose();
        }
    }
}
