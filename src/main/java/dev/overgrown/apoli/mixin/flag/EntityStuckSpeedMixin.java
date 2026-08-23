package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyBlockStuckSpeedHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityStuckSpeedMixin {

    private static final String MAKE_STUCK_IN_BLOCK =
        "makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V";

    @Shadow
    protected Vec3 stuckSpeedMultiplier;

    @Inject(method = MAKE_STUCK_IN_BLOCK, at = @At("HEAD"), cancellable = true)
    private void apoli$modifyStuckSpeed(BlockState state, Vec3 original, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Vec3 modified = ModifyBlockStuckSpeedHandler.modify(self, state, original);
        if (modified == original) return;
        self.resetFallDistance();
        if (!ModifyBlockStuckSpeedHandler.fullyFree(modified)) this.stuckSpeedMultiplier = modified;
        ci.cancel();
    }

    @ModifyReturnValue(method = "getBlockSpeedFactor()F", at = @At("RETURN"))
    private float apoli$modifyBlockSpeedFactor(float original) {
        return ModifyBlockStuckSpeedHandler.modifySpeedFactor((Entity) (Object) this, original);
    }
}
