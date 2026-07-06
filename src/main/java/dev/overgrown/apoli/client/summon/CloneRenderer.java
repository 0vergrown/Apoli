package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.power.builtin.EntityTextureOverlayPower;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class CloneRenderer extends HumanoidMobRenderer<CloneEntity, CloneModel> {
    private final CloneModel wideModel;
    private final CloneModel slimModel;

    public CloneRenderer(EntityRendererProvider.Context context) {
        super(context, new CloneModel(context.bakeLayer(SummonModelLayers.CLONE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new CloneModel(context.bakeLayer(SummonModelLayers.CLONE_SLIM), true);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CloneTextureOverlayLayer(this));
    }

    @Override
    public void render(CloneEntity clone, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model = resolveSlim(clone) ? this.slimModel : this.wideModel;
        if (clone.isSitting()) {
            pose.pushPose();
            pose.translate(0.0, -0.5, 0.0);
            super.render(clone, yaw, partialTick, pose, buffers, light);
            pose.popPose();
        } else {
            super.render(clone, yaw, partialTick, pose, buffers, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity clone) {
        boolean slim = resolveSlim(clone);
        ResourceLocation custom = clone.getCustomTexture(slim);
        if (custom != null) return custom;
        LivingEntity owner = clone.getOwner();
        if (owner != null) {
            EntityTextureOverlayPower.Config overlay = EntityTextureOverlayPower.firstReplace(owner);
            if (overlay != null) {
                return slim ? overlay.slim() : overlay.wide();
            }
        }
        if (owner instanceof AbstractClientPlayer player) return player.getSkin().texture();
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    protected void scale(CloneEntity clone, PoseStack pose, float partialTick) {
        pose.scale(0.9375f, 0.9375f, 0.9375f);
    }

    public static boolean resolveSlim(CloneEntity clone) {
        if (clone.getCustomTexture(false) != null) return clone.isSlim();
        if (clone.getOwner() instanceof AbstractClientPlayer player) {
            return player.getSkin().model() == PlayerSkin.Model.SLIM;
        }
        return clone.isSlim();
    }
}
