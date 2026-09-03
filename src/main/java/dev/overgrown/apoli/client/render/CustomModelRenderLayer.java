package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.List;

public class CustomModelRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final boolean slim;

    public CustomModelRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();

        List<ResolvedLayer> layers = CustomModelRenderPower.collectTextureOverlays(player);
        for (ResolvedLayer layer : layers) {
            ResourceLocation texture = DynamicTextures.resolve(layer.texture(slim), player);
            int color = FastColor.ARGB32.colorFromFloat(layer.alpha(), layer.red(), layer.green(), layer.blue());
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(layer.mode(), texture));
            boolean scaled = layer.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(layer.scale(), layer.scale(), layer.scale());
            }
            if (layer.wholeModel()) {
                model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
            } else {
                for (String partName : layer.bodyParts()) {
                    for (ModelPart part : ModelPartLookup.resolve(model, ModelParts.normalize(partName))) {
                        part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
                    }
                }
            }
            if (scaled) {
                pose.popPose();
            }
        }

        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(player);
        if (geometry.isEmpty()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> rest = PlayerRestPose.get();
        for (GeometryRender render : geometry) {
            CustomModel custom = CustomModelManager.get(render.model());
            if (custom == null) {
                continue;
            }
            GeometryRenderer.syncPlayer(custom, model, rest);
            AnimationPlayer.apply(player, render, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, player);
        }
    }
}
