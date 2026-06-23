package dev.overgrown.apoli.mixin.entityset;

import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobConvertToMixin {
    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;", at = @At("RETURN"))
    private void apoli$onConvertTo(EntityType<?> entityType, boolean bl, CallbackInfoReturnable<Mob> cir) {
        Mob result = cir.getReturnValue();
        if (result == null) return;
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;
        EntitySetPower.onEntityConverted(self.getUUID(), result.getUUID());
    }
}
