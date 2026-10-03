package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

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
        if (entity instanceof MinionEntity) return;
        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(entity);
        if (geometry.isEmpty()) return;
        Map<String, ModelPart> parts = ModelPartNames.of(this.getParentModel());
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            CustomModel custom = CustomModelManager.get(render.model(), render.bindBodyParts());
            if (custom == null) continue;
            if (render.bindBodyParts()) {
                GeometryRenderer.syncNamed(custom, parts);
            } else {
                GeometryRenderer.resetAll(custom);
            }
            AnimationPlayer.apply(entity, render, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, entity);
        }
    }
}
