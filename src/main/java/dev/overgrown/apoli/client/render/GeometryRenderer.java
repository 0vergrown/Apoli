package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GeometryRenderer {
    private GeometryRenderer() {}

    @Nullable
    public static ModelPart findBone(ModelPart root, String name) {
        if (root.hasChild(name)) {
            return root.getChild(name);
        }
        String flat = ModelParts.normalize(name);
        if (root.hasChild(flat)) {
            return root.getChild(flat);
        }
        String stripped = name.replace("_", "");
        return root.hasChild(stripped) ? root.getChild(stripped) : null;
    }

    public static void syncBone(ModelPart root, String name, @Nullable ModelPart source) {
        if (source == null) {
            return;
        }
        ModelPart bone = findBone(root, name);
        if (bone != null) {
            bone.copyFrom(source);
        }
    }

    public static void applyVisibility(ModelPart root, List<String> bodyParts, String[] skeleton) {
        boolean wholeModel = bodyParts.isEmpty();
        for (String name : skeleton) {
            ModelPart bone = findBone(root, name);
            if (bone != null) {
                bone.visible = wholeModel || listed(bodyParts, name);
            }
        }
    }

    public static void draw(GeometryRender render, ModelPart root, PoseStack pose, MultiBufferSource buffers, int light) {
        int color = FastColor.ARGB32.colorFromFloat(render.alpha(), render.red(), render.green(), render.blue());
        VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(render.mode(), render.texture()));
        boolean scaled = render.scale() != 1.0F;
        if (scaled) {
            pose.pushPose();
            pose.scale(render.scale(), render.scale(), render.scale());
        }
        root.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
        if (scaled) {
            pose.popPose();
        }
    }

    private static boolean listed(List<String> bodyParts, String bone) {
        String normalized = ModelParts.normalize(bone);
        for (int i = 0; i < bodyParts.size(); i++) {
            if (ModelParts.normalize(bodyParts.get(i)).equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
