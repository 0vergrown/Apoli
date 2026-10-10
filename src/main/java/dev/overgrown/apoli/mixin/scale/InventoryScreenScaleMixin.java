package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.client.render.ScaleRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
@Environment(EnvType.CLIENT)
public abstract class InventoryScreenScaleMixin {

    private static final String RENDER_ENTITY = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";

    @Inject(method = RENDER_ENTITY, at = @At("HEAD"))
    private static void apoli$beginDoll(GuiGraphics graphics, int x, int y, int scale, Quaternionf rotation,
                                        @Nullable Quaternionf cameraRotation, LivingEntity entity, CallbackInfo ci) {
        ScaleRender.beginDoll();
    }

    @Inject(method = RENDER_ENTITY, at = @At("RETURN"))
    private static void apoli$endDoll(GuiGraphics graphics, int x, int y, int scale, Quaternionf rotation,
                                      @Nullable Quaternionf cameraRotation, LivingEntity entity, CallbackInfo ci) {
        ScaleRender.endDoll();
    }
}
