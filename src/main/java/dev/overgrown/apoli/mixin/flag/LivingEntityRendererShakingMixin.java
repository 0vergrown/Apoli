package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ShakingPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererShakingMixin {

    @ModifyReturnValue(method = "isShaking(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean apoli$shakingPower(boolean original, LivingEntity entity) {
        return original || ShakingPower.isShaking(entity);
    }
}
