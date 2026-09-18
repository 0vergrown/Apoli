package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

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
        for (int i = 0; i < layers.size(); i++) {
            ResolvedLayer layer = layers.get(i);
            ResourceLocation texture = DynamicTextures.resolve(layer.texture(slim), player);
            TextureOverlays.render(model, layer, texture, 1.0F, ageInTicks, player, pose, buffers, light);
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
