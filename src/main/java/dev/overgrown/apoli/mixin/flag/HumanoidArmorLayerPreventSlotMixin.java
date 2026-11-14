package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.client.render.FeatureRenderers;
import dev.overgrown.apoli.power.builtin.PreventFeatureRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
@Environment(EnvType.CLIENT)
public abstract class HumanoidArmorLayerPreventSlotMixin {

    @WrapWithCondition(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"))
    private boolean apoli$preventArmorSlot(HumanoidArmorLayer<?, ?, ?> layer, PoseStack pose, MultiBufferSource buffers,
                                           LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> model) {
        String[] keys = FeatureRenderers.armorSlotKeys(slot);
        if (keys.length == 0) return true;
        return !PreventFeatureRenderPower.prevents(ClientDisguiseManager.powerSource(entity), keys,
            Minecraft.getInstance().player);
    }
}
