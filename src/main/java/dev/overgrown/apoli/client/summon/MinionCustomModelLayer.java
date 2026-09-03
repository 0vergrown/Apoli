package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.GeometryRenderer;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

import java.util.List;

public class MinionCustomModelLayer extends RenderLayer<MinionEntity, MinionModel> {
    private final MinionRenderer renderer;
    private MinionModel restPose;

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
        MinionModel rest = this.restPose();
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            CustomModel custom = CustomModelManager.get(render.model());
            if (custom == null) {
                continue;
            }
            GeometryRenderer.resetAll(custom);
            for (String name : MinionModel.BONES) {
                GeometryRenderer.syncBone(custom, name, model.bone(name), rest.bone(name));
            }
            AnimationPlayer.apply(minion, render, custom, partialTick);
            GeometryRenderer.applyVisibility(custom, render.bodyParts());
            GeometryRenderer.draw(render, custom, pose, buffers, light, minion);
        }
    }

    private MinionModel restPose() {
        if (this.restPose == null) {
            this.restPose = new MinionModel(Minecraft.getInstance().getEntityModels().bakeLayer(SummonModelLayers.MINION));
        }
        return this.restPose;
    }
}
