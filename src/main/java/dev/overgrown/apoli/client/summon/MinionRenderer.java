package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MinionRenderer extends MobRenderer<MinionEntity, MinionModel> {
    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, new MinionModel(context.bakeLayer(SummonModelLayers.MINION)), 0.5f);
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
