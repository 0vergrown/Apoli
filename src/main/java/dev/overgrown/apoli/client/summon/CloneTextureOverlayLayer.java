package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.client.render.OverlayRenderTypes;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower.ResolvedLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class CloneTextureOverlayLayer extends RenderLayer<CloneEntity, CloneModel> {
    public CloneTextureOverlayLayer(RenderLayerParent<CloneEntity, CloneModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, CloneEntity clone,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity owner = clone.getOwner();
        if (owner == null) return;
        List<ResolvedLayer> layers = EntityTextureOverlayPower.collectLayers(owner);
        if (layers.isEmpty()) return;

        boolean slim = CloneRenderer.resolveSlim(clone);
        CloneModel model = this.getParentModel();
        for (ResolvedLayer layer : layers) {
            ResourceLocation texture = layer.texture(slim);
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(layer.mode(), texture));
            boolean scaled = layer.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(layer.scale(), layer.scale(), layer.scale());
            }
            if (layer.wholeModel()) {
                model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                    layer.red(), layer.green(), layer.blue(), layer.alpha());
            } else {
                for (String partName : layer.bodyParts()) {
                    for (ModelPart part : ModelPartLookup.resolve(model, ModelParts.normalize(partName))) {
                        part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                            layer.red(), layer.green(), layer.blue(), layer.alpha());
                    }
                }
            }
            if (scaled) pose.popPose();
        }
    }
}
