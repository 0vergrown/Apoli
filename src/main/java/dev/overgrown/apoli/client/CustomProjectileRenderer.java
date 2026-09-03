package dev.overgrown.apoli.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.GeometryRenderer;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CustomProjectileRenderer extends EntityRenderer<CustomProjectileEntity> {
    private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final ItemRenderer itemRenderer;

    public CustomProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CustomProjectileEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        List<GeometryRender> geometry = resolveGeometry(entity);
        if (geometry.isEmpty() || !renderGeometry(entity, geometry, partialTick, poseStack, buffers, light)) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                renderBillboard(entity, poseStack, buffers, light);
            } else {
                renderItem(entity, stack, poseStack, buffers, light);
            }
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private void renderItem(CustomProjectileEntity entity, ItemStack stack, PoseStack poseStack,
                            MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
            poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
    }

    private void renderBillboard(CustomProjectileEntity entity, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        vertex(consumer, pose, light, 0.0F, 0, 0, 1);
        vertex(consumer, pose, light, 1.0F, 0, 1, 1);
        vertex(consumer, pose, light, 1.0F, 1, 1, 0);
        vertex(consumer, pose, light, 0.0F, 1, 0, 0);
        poseStack.popPose();
    }

    private static List<GeometryRender> resolveGeometry(CustomProjectileEntity entity) {
        GeometryRender synced = CustomModelRenderPower.geometryOf(entity.getModelPower());
        if (synced != null) return List.of(synced);
        return CustomModelRenderPower.collectGeometry(entity);
    }

    private boolean renderGeometry(CustomProjectileEntity entity, List<GeometryRender> geometry, float partialTick,
                                   PoseStack poseStack, MultiBufferSource buffers, int light) {
        float bodyYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float bodyPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyPitch));
        poseStack.translate(0.0F, -1.5F, 0.0F);

        boolean drew = false;
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            CustomModel custom = CustomModelManager.get(render.model());
            if (custom == null) continue;
            GeometryRenderer.resetAll(custom);
            AnimationPlayer.apply(entity, render, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, poseStack, buffers, light, entity);
            drew = true;
        }
        poseStack.popPose();
        return drew;
    }

    @Override
    public ResourceLocation getTextureLocation(CustomProjectileEntity entity) {
        ResourceLocation texture = entity.getTexture();
        return texture != null ? texture : MISSING;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.25F, 0.0F)
            .setColor(255, 255, 255, 255)
            .setUv((float) u, (float) v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
