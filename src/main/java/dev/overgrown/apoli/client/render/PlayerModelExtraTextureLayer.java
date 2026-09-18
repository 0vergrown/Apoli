package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.model.ExtraTexturedModel;
import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class PlayerModelExtraTextureLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerModelExtraTextureLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(this.getParentModel() instanceof ExtraTexturedModel model)) return;
        List<ModelPart> parts = model.extraTexturedParts();
        if (parts.isEmpty()) return;

        ResourceLocation declared = ModifyPlayerModelPower.firstActiveModelTexture(player);
        ResourceLocation texture = declared == null
            ? model.defaultExtraTexture()
            : DynamicTextures.resolve(declared, player);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).render(pose, consumer, light, overlay);
        }
    }
}
