package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower.ResolvedLayer;
import net.minecraft.client.Minecraft;
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

public class PlayerTextureOverlayLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final boolean slim;

    public PlayerTextureOverlayLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<ResolvedLayer> layers = EntityTextureOverlayPower.collectLayers(player);
        if (layers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean firstPerson = mc.player == player && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) return;

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        for (ResolvedLayer layer : layers) {
            ResourceLocation texture = layer.texture(slim);
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
            if (scaled) pose.popPose();
        }
    }
}
