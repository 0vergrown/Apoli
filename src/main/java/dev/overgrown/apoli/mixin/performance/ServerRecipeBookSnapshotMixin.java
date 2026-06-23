package dev.overgrown.apoli.mixin.performance;

import dev.overgrown.apoli.condition.builtin.entity.EntityNbtSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.ServerRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookSnapshotMixin {

    @Inject(method = "toNbt", at = @At("HEAD"), cancellable = true)
    private void apoli$skipDuringConditionSnapshot(CallbackInfoReturnable<CompoundTag> cir) {
        if (EntityNbtSnapshot.isSkippingRecipeBook()) {
            cir.setReturnValue(new CompoundTag());
        }
    }
}
