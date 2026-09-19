package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class LivingCustomModelLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public LivingCustomModelLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<ResolvedLayer> layers = CustomModelRenderPower.collectTextureOverlays(entity);
        for (int i = 0; i < layers.size(); i++) {
            ResolvedLayer layer = layers.get(i);
            TextureOverlays.render(this.getParentModel(), layer, DynamicTextures.resolve(layer.texture(false), entity),
                1.0F, ageInTicks, entity, pose, buffers, light);
        }
    }
}
