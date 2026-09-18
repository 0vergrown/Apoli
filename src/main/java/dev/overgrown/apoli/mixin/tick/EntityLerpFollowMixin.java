package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.client.ClientTickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class, LivingEntity.class, AbstractArrow.class})
@Environment(EnvType.CLIENT)
public abstract class EntityLerpFollowMixin {

    @Inject(method = "lerpTo(DDDFFIZ)V", at = @At("HEAD"), cancellable = true)
    private void apoli$followServer(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport, CallbackInfo ci) {
        if (ClientTickRates.idle()) return;
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide()) return;
        if (ClientTickRates.captureLerp(self, x, y, z, yRot, xRot)) ci.cancel();
    }
}
