package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.summon.CloneRenderer;
import dev.overgrown.apoli.entity.summon.CloneEntity;
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
        boolean slim = !layers.isEmpty() && entity instanceof CloneEntity clone && CloneRenderer.resolveSlim(clone);
        float body = -1.0F;
        for (int i = 0; i < layers.size(); i++) {
            ResolvedLayer layer = layers.get(i);
            float alpha = 1.0F;
            if (!layer.visibleWhileInvisible()) {
                if (body < 0.0F) body = BodyVisibility.alpha(entity);
                if (body <= 0.0F) continue;
                alpha = body;
            }
            TextureOverlays.render(this.getParentModel(), layer, DynamicTextures.resolve(layer.texture(slim), entity),
                alpha, ageInTicks, entity, pose, buffers, light);
        }
        if (entity instanceof MinionEntity) return;
        List<GeometryRender> geometry = CustomModelRenderPower.collectGeometry(entity);
        AnimationPlayback.beginRender(entity);
        if (geometry.isEmpty()) return;
        Map<String, ModelPart> parts = ModelPartNames.of(this.getParentModel());
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            AnimationPlayback.Track track = AnimationPlayer.track(entity, render);
            float alpha = 1.0F;
            if (!render.visibleWhileInvisible()) {
                if (body < 0.0F) body = BodyVisibility.alpha(entity);
                if (body <= 0.0F) continue;
                alpha = body;
            }
            CustomModel custom = CustomModelManager.get(render, render.bindBodyParts(), entity);
            if (custom == null) continue;
            if (render.bindBodyParts()) {
                GeometryRenderer.syncNamed(custom, parts);
            } else {
                GeometryRenderer.resetAll(custom);
            }
            AnimationPlayer.apply(entity, track, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, entity, alpha);
        }
    }
}
