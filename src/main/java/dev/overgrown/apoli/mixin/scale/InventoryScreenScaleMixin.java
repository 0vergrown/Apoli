package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.client.render.ScaleRender;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
@OnlyIn(Dist.CLIENT)
public abstract class InventoryScreenScaleMixin {

    private static final String RENDER_ENTITY = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";

    @Inject(method = RENDER_ENTITY, at = @At("HEAD"))
    private static void apoli$beginDoll(GuiGraphics graphics, float x, float y, float scale, Vector3f offset,
                                        Quaternionf rotation, @Nullable Quaternionf cameraRotation,
                                        LivingEntity entity, CallbackInfo ci) {
        ScaleRender.beginDoll();
    }

    @Inject(method = RENDER_ENTITY, at = @At("RETURN"))
    private static void apoli$endDoll(GuiGraphics graphics, float x, float y, float scale, Vector3f offset,
                                      Quaternionf rotation, @Nullable Quaternionf cameraRotation,
                                      LivingEntity entity, CallbackInfo ci) {
        ScaleRender.endDoll();
    }

    @ModifyExpressionValue(method = "renderEntityInInventoryFollowsAngle", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;getBbHeight()F"))
    private static float apoli$dollHeight(float height, @Local(argsOnly = true) LivingEntity entity) {
        return ScaleRender.dollHeight(entity, height);
    }
}
