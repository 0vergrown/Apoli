package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.client.render.FeatureRenderers;
import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import dev.overgrown.apoli.power.builtin.PreventFeatureRenderPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererPreventFeatureMixin {

    @WrapWithCondition(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    private boolean apoli$preventFeatureRender(RenderLayer<?, ?> layer, PoseStack pose, MultiBufferSource buffer,
                                               int packedLight, Entity rendered, float limbSwing, float limbSwingAmount,
                                               float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Entity source = rendered instanceof LivingEntity living
            ? ClientDisguiseManager.powerSource(living)
            : rendered;
        if (layer instanceof HumanoidArmorLayer<?, ?, ?>
            && InvisibilityPower.hidesArmor(source)
            && rendered.isInvisibleTo(Minecraft.getInstance().player)) {
            return false;
        }
        return !PreventFeatureRenderPower.prevents(source, FeatureRenderers.keysFor(layer.getClass()));
    }
}
