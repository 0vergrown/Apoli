package dev.overgrown.apoli.client.possession;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.entity.possession.Possession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class PossessionArms {
    private PossessionArms() {}

    public static boolean render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                 boolean right) {
        Entity host = Possession.hostOf(player);
        if (!(host instanceof LivingEntity living)) return false;
        EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer
            && livingRenderer.getModel() instanceof HumanoidModel<?> humanoid) {
            drawArm(pose, buffers, light, living, renderer, humanoid, right);
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity host,
                                EntityRenderer<? super LivingEntity> renderer, HumanoidModel<?> humanoid, boolean right) {
        HumanoidModel raw = humanoid;
        raw.attackTime = 0.0f;
        raw.crouching = false;
        raw.swimAmount = 0.0f;
        raw.setupAnim(host, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        ModelPart arm = right ? humanoid.rightArm : humanoid.leftArm;
        arm.xRot = 0.0f;
        ResourceLocation texture = renderer.getTextureLocation(host);
        arm.render(pose, buffers.getBuffer(humanoid.renderType(texture)), light, OverlayTexture.NO_OVERLAY);
    }
}
