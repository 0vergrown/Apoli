package dev.overgrown.apoli.compat.sable.mixin;

import dev.overgrown.apoli.compat.sable.SablePhasing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMoveScopeMixin {

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void apoli$beginSableScope(MoverType type, Vec3 movement, CallbackInfo ci) {
        SablePhasing.begin((Entity) (Object) this);
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void apoli$endSableScope(MoverType type, Vec3 movement, CallbackInfo ci) {
        SablePhasing.end();
    }
}
