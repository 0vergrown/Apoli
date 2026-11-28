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

        float body = -1.0F;
        List<ResolvedLayer> overlays = CustomModelRenderPower.collectTextureOverlays(player);
        for (int i = 0; i < overlays.size(); i++) {
            ResolvedLayer layer = overlays.get(i);
            float alpha = 1.0F;
            if (!layer.visibleWhileInvisible()) {
                if (body < 0.0F) body = BodyVisibility.alpha(player);
                if (body <= 0.0F) continue;
                alpha = body;
            }
            ResourceLocation texture = DynamicTextures.resolve(layer.texture(slim), player);
            TextureOverlays.render(humanoid, layer, texture, alpha, ageInTicks, player, pose, buffers, light);
        }

        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(player);
        AnimationPlayback.beginRender(player);
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            AnimationPlayback.Track track = AnimationPlayer.track(player, render);
            float alpha = 1.0F;
            if (!render.visibleWhileInvisible()) {
                if (body < 0.0F) body = BodyVisibility.alpha(player);
                if (body <= 0.0F) continue;
                alpha = body;
            }
            CustomModel custom = CustomModelManager.get(render, render.bindBodyParts(), player);
            if (custom == null) continue;
            if (render.bindBodyParts()) {
                GeometryRenderer.syncHumanoid(custom, humanoid);
            } else {
                GeometryRenderer.resetAll(custom);
            }
            AnimationPlayer.apply(player, track, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, player, alpha);
        }
    }

    private static boolean isSlim(AbstractClientPlayer player) {
        return "slim".equals(player.getModelName());
    }
}
