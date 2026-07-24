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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            applyVisibility(root, render.bodyParts());
            VertexConsumer consumer = buffers.getBuffer(OverlayRenderTypes.forMode(render.mode(), render.texture()));
            boolean scaled = render.scale() != 1.0F;
            if (scaled) {
                pose.pushPose();
                pose.scale(render.scale(), render.scale(), render.scale());
            }
            root.render(pose, consumer, light, OverlayTexture.NO_OVERLAY,
                render.red(), render.green(), render.blue(), render.alpha());
            if (scaled) {
                pose.popPose();
            }
        }
    }

    private void syncPose(ModelPart root, PlayerModel<AbstractClientPlayer> model) {
        copyBone(root, "head", model.head);
        copyBone(root, "hat", model.hat);
        copyBone(root, "body", model.body);
        copyBone(root, "right_arm", model.rightArm);
        copyBone(root, "left_arm", model.leftArm);
        copyBone(root, "right_leg", model.rightLeg);
        copyBone(root, "left_leg", model.leftLeg);
    }

    private void copyBone(ModelPart root, String name, ModelPart source) {
        ModelPart bone = findChild(root, name);
        if (bone != null) {
            bone.copyFrom(source);
        }
    }

    private static ModelPart findChild(ModelPart root, String name) {
        if (root.hasChild(name)) {
            return root.getChild(name);
        }
        String flat = ModelParts.normalize(name);
        for (String candidate : new String[]{flat, name.replace("_", "")}) {
            if (root.hasChild(candidate)) {
                return root.getChild(candidate);
            }
        }
        return null;
    }

    private void applyVisibility(ModelPart root, List<String> bodyParts) {
        if (bodyParts.isEmpty()) {
            for (String bone : SKELETON) {
                ModelPart part = findChild(root, bone);
                if (part != null) {
                    part.visible = true;
                }
            }
            return;
        }
        Set<String> allowed = new HashSet<>();
        for (String part : bodyParts) {
            allowed.add(ModelParts.normalize(part));
        }
        for (String bone : SKELETON) {
            ModelPart part = findChild(root, bone);
            if (part != null) {
                part.visible = allowed.contains(ModelParts.normalize(bone));
            }
        }
    }
}
