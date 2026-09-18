package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.model.ExtraModelParts;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TextureOverlays {
    private TextureOverlays() {}

    private static final ThreadLocal<List<ModelPart>> PARTS = ThreadLocal.withInitial(() -> new ArrayList<>(8));

    public static void render(EntityModel<?> model, ResolvedLayer layer, ResourceLocation texture, float alphaScale,
                              float ageInTicks, @Nullable Entity subject, PoseStack pose, MultiBufferSource buffers,
                              int light) {
        float alpha = layer.alpha() * alphaScale;
        VertexConsumer consumer = buffers.getBuffer(
            OverlayRenderTypes.forMode(layer.mode(), texture, ageInTicks, layer.scrollSpeed()));
        boolean scaled = layer.scale() != 1.0F;
        if (scaled) {
            pose.pushPose();
            pose.scale(layer.scale(), layer.scale(), layer.scale());
        }
        List<ModelPart> parts = layer.wholeModel() ? null : partsOf(model, layer.bodyParts(), subject);
        if (parts == null) {
            model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, layer.red(), layer.green(), layer.blue(), alpha);
        } else {
            for (int i = 0; i < parts.size(); i++) {
                ModelPart part = parts.get(i);
                if (parts.indexOf(part) != i) continue;
                part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, layer.red(), layer.green(), layer.blue(), alpha);
            }
            parts.clear();
        }
        if (scaled) {
            pose.popPose();
        }
    }

    @Nullable
    private static List<ModelPart> partsOf(EntityModel<?> model, List<BodyPart> bodyParts, @Nullable Entity subject) {
        List<ModelPart> parts = PARTS.get();
        parts.clear();
        if (model instanceof HumanoidModel<?> humanoid) {
            for (int i = 0; i < bodyParts.size(); i++) {
                ModelPartLookup.resolveInto(humanoid, bodyParts.get(i).sided(subject), parts);
            }
            return parts;
        }
        if (model instanceof ExtraModelParts extra) {
            for (int i = 0; i < bodyParts.size(); i++) {
                extra.collectExtraParts(bodyParts.get(i).key(), parts);
            }
            return parts;
        }
        return null;
    }

    public static void renderArm(ResolvedLayer layer, ResourceLocation texture, float alphaScale, float ageInTicks,
                                 ModelPart arm, ModelPart sleeve, PoseStack pose, MultiBufferSource buffers, int light) {
        float alpha = layer.alpha() * alphaScale;
        VertexConsumer consumer = buffers.getBuffer(
            OverlayRenderTypes.forMode(layer.mode(), texture, ageInTicks, layer.scrollSpeed()));
        boolean scaled = layer.scale() != 1.0F;
        if (scaled) {
            pose.pushPose();
            pose.scale(layer.scale(), layer.scale(), layer.scale());
        }
        arm.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, layer.red(), layer.green(), layer.blue(), alpha);
        sleeve.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, layer.red(), layer.green(), layer.blue(), alpha);
        if (scaled) {
            pose.popPose();
        }
    }

    public static boolean affectsArm(HumanoidModel<?> model, ResolvedLayer layer, @Nullable Entity subject,
                                     ModelPart arm, ModelPart sleeve) {
        if (layer.wholeModel()) return true;
        List<BodyPart> bodyParts = layer.bodyParts();
        for (int i = 0; i < bodyParts.size(); i++) {
            BodyPart part = bodyParts.get(i).sided(subject);
            if (ModelPartLookup.affects(model, part, arm) || ModelPartLookup.affects(model, part, sleeve)) return true;
        }
        return false;
    }
}
