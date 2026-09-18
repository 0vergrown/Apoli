package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDimensionsScaleMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (Scales.untouched(self)) return;
        float width = Scales.applied(self, ScaleTypes.HITBOX_WIDTH);
        float height = Scales.applied(self, ScaleTypes.HITBOX_HEIGHT);
        if (width == 1.0F && height == 1.0F) return;
        cir.setReturnValue(cir.getReturnValue().scale(width, height));
    }
}
