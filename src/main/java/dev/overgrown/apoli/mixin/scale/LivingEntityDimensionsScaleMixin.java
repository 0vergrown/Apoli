package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDimensionsScaleMixin {

    @ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
    private EntityDimensions apoli$scaleDimensions(EntityDimensions original) {
        return Scales.scaledDimensions((LivingEntity) (Object) this, original);
    }
}
