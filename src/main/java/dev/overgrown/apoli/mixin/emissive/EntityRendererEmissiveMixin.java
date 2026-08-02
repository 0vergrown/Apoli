package dev.overgrown.apoli.mixin.emissive;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.EmissivePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class EntityRendererEmissiveMixin {

    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
    private int apoli$emissiveBlockLight(int original, Entity entity, BlockPos pos) {
        int luminance = EmissivePower.selfLitLuminanceOf(entity);
        return luminance > original ? luminance : original;
    }
}
