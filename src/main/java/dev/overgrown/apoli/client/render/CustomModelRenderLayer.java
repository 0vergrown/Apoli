package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
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

public class CustomModelRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final String[] SKELETON = {"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"};
    private final boolean slim;

    public CustomModelRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        boolean firstPerson = mc.player == player && mc.options.getCameraType().isFirstPerson();
        if (firstPerson) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();

        List<ResolvedLayer> layers = CustomModelRenderPower.collectTextureOverlays(player);
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
            if (scaled) {
                pose.popPose();
            }
        }

        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(player);
        for (GeometryRender render : geometry) {
            ModelPart root = CustomModelManager.getBaked(render.model());
            if (root == null) {
                continue;
            }
            syncPose(root, model);
            GeometryRenderer.applyVisibility(root, render.bodyParts(), SKELETON);
            GeometryRenderer.draw(render, root, pose, buffers, light);
        }
    }

    private void syncPose(ModelPart root, PlayerModel<AbstractClientPlayer> model) {
        GeometryRenderer.syncBone(root, "head", model.head);
        GeometryRenderer.syncBone(root, "hat", model.hat);
        GeometryRenderer.syncBone(root, "body", model.body);
        GeometryRenderer.syncBone(root, "right_arm", model.rightArm);
        GeometryRenderer.syncBone(root, "left_arm", model.leftArm);
        GeometryRenderer.syncBone(root, "right_leg", model.rightLeg);
        GeometryRenderer.syncBone(root, "left_leg", model.leftLeg);
    }
}
