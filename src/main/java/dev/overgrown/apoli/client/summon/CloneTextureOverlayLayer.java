package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.TextureOverlays;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.client.render.OverlayRenderTypes;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.ResolvedLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class CloneTextureOverlayLayer extends RenderLayer<CloneEntity, CloneModel> {
    public CloneTextureOverlayLayer(RenderLayerParent<CloneEntity, CloneModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, CloneEntity clone,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity owner = clone.getOwner();
        if (owner == null) return;
        List<ResolvedLayer> layers = CustomModelRenderPower.collectTextureOverlays(owner);
        if (layers.isEmpty()) return;

        boolean slim = CloneRenderer.resolveSlim(clone);
        CloneModel model = this.getParentModel();
        for (int i = 0; i < layers.size(); i++) {
            ResolvedLayer layer = layers.get(i);
            ResourceLocation texture = dev.overgrown.apoli.client.render.DynamicTextures.resolve(layer.texture(slim), owner);
            TextureOverlays.render(model, layer, texture, 1.0F, ageInTicks, owner, pose, buffers, light);
        }
    }
}
