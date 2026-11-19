package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;

public class CloneArmorLayer extends HumanoidArmorLayer<CloneEntity, CloneModel, HumanoidArmorModel<CloneEntity>> {
    private final HumanoidArmorLayer<CloneEntity, CloneModel, HumanoidArmorModel<CloneEntity>> slim;

    public CloneArmorLayer(RenderLayerParent<CloneEntity, CloneModel> parent, EntityRendererProvider.Context context) {
        super(parent, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager());
        this.slim = new HumanoidArmorLayer<>(parent,
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
            new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), context.getModelManager());
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, CloneEntity clone, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (CloneRenderer.resolveSlim(clone)) {
            this.slim.render(pose, buffers, light, clone, limbSwing, limbSwingAmount, partialTick, ageInTicks,
                netHeadYaw, headPitch);
            return;
        }
        super.render(pose, buffers, light, clone, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw,
            headPitch);
    }
}
