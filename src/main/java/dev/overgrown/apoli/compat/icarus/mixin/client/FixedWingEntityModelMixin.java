package dev.overgrown.apoli.compat.icarus.mixin.client;

import com.r3x.client.models.FixedWingEntityModel;
import dev.cammiescorner.icarus.client.models.WingEntityModel;
import dev.overgrown.apoli.compat.icarus.IcarusWingParts;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FixedWingEntityModel.class)
public abstract class FixedWingEntityModelMixin {

    private static final String SETUP_ANIM = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V";

    @Unique
    private final IcarusWingParts apoli$reWingedParts = new IcarusWingParts();

    @Inject(method = SETUP_ANIM, at = @At("HEAD"), require = 0)
    private void apoli$restoreReWinged(LivingEntity entity, float limbAngle, float limbDistance, float age,
                                       float headYaw, float headPitch, CallbackInfo ci) {
        WingEntityModel<?> self = (WingEntityModel<?>) (Object) this;
        apoli$reWingedParts.restore(self.rightWing, self.leftWing);
    }

    @Inject(method = SETUP_ANIM, at = @At("TAIL"), require = 0)
    private void apoli$editReWinged(LivingEntity entity, float limbAngle, float limbDistance, float age,
                                    float headYaw, float headPitch, CallbackInfo ci) {
        WingEntityModel<?> self = (WingEntityModel<?>) (Object) this;
        apoli$reWingedParts.edit(entity, self.rightWing, self.leftWing);
    }
}
