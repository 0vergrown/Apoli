package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinionRenderer extends MobRenderer<MinionEntity, MinionModel> {
    private List<GeometryRender> geometry = List.of();

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new MinionModel(context.bakeLayer(SummonModelLayers.MINION)), 0.5f);
        this.addLayer(new MinionCustomModelLayer(this));
    }

    @Override
    public void render(MinionEntity minion, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.geometry = CustomModelRenderPower.collectGeometry(minion);
        super.render(minion, yaw, partialTick, pose, buffers, light);
        this.geometry = List.of();
    }

    public List<GeometryRender> activeGeometry() {
        return this.geometry;
    }

    @Override
    @Nullable
    protected RenderType getRenderType(MinionEntity minion, boolean bodyVisible, boolean translucent, boolean glowing) {
        for (int i = 0; i < this.geometry.size(); i++) {
            GeometryRender render = this.geometry.get(i);
            if (!render.renderAsOverlay() && CustomModelManager.get(render.model()) != null) {
                return null;
            }
        }
        return super.getRenderType(minion, bodyVisible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(MinionEntity minion) {
        return minion.getTexture();
    }

    @Override
    protected void scale(MinionEntity minion, PoseStack pose, float partialTick) {
        float scale = minion.getMinionScale();
        pose.scale(scale, scale, scale);
    }
}
