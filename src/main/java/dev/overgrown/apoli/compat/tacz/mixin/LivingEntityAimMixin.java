package dev.overgrown.apoli.compat.tacz.mixin;

import com.tacz.guns.entity.shooter.LivingEntityAim;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.tacz.TaczGuns;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityAim.class)
public abstract class LivingEntityAimMixin {

    @Shadow @Final private LivingEntity shooter;

    @ModifyVariable(method = "aim", at = @At("HEAD"), argsOnly = true)
    private boolean apoli$preventAim(boolean isAim) {
        return isAim && !GunPowers.prevents(shooter, GunPowers.PREVENT_AIM, TaczGuns.gunId(shooter.getMainHandItem()));
    }
}
