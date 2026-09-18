package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class DisguiseCustomModelLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public DisguiseCustomModelLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T dummy,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(ClientDisguiseManager.renderActor() instanceof AbstractClientPlayer player)) return;
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoid)) return;

        boolean slim = isSlim(player);

        List<ResolvedLayer> overlays = CustomModelRenderPower.collectTextureOverlays(player);
        for (int i = 0; i < overlays.size(); i++) {
            ResolvedLayer layer = overlays.get(i);
            ResourceLocation texture = DynamicTextures.resolve(layer.texture(slim), player);
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(layer.mode(), texture));
            boolean scaled = layer.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(layer.scale(), layer.scale(), layer.scale());
            }
            if (layer.wholeModel()) {
                humanoid.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                    layer.red(), layer.green(), layer.blue(), layer.alpha());
            } else {
                for (String partName : layer.bodyParts()) {
                    for (ModelPart part : ModelPartLookup.resolve(humanoid, ModelParts.normalize(partName))) {
                        part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                            layer.red(), layer.green(), layer.blue(), layer.alpha());
                    }
                }
            }
            if (scaled) {
                pose.popPose();
            }
        }

        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(player);
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            CustomModel custom = CustomModelManager.get(render.model());
            if (custom == null) continue;
            GeometryRenderer.syncHumanoid(custom, humanoid);
            AnimationPlayer.apply(player, render, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, player);
        }
    }

    private static boolean isSlim(AbstractClientPlayer player) {
        return "slim".equals(player.getModelName());
    }
}
