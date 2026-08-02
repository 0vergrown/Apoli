package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.GeometryRenderer;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.List;

public class MinionCustomModelLayer extends RenderLayer<MinionEntity, MinionModel> {
    private final MinionRenderer renderer;

    public MinionCustomModelLayer(MinionRenderer renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, MinionEntity minion,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<GeometryRender> geometry = this.renderer.activeGeometry();
        if (geometry.isEmpty()) {
            return;
        }
        MinionModel model = this.getParentModel();
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            ModelPart root = CustomModelManager.getBaked(render.model());
            if (root == null) {
                continue;
            }
            for (String name : MinionModel.BONES) {
                GeometryRenderer.syncBone(root, name, model.bone(name));
            }
            GeometryRenderer.applyVisibility(root, render.bodyParts(), MinionModel.BONES);
            GeometryRenderer.draw(render, root, pose, buffers, light);
        }
    }
}
