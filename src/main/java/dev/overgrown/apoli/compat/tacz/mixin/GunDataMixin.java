package dev.overgrown.apoli.compat.tacz.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.tacz.TaczGuns;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GunData.class)
public abstract class GunDataMixin {

    @ModifyReturnValue(method = "getShootInterval", at = @At("RETURN"))
    private long apoli$modifyFireRate(long interval, LivingEntity shooter, FireMode fireMode, ItemStack gunStack) {
        if (interval <= 0 || shooter == null || !PowerLookup.holdsType(shooter, GunPowers.MODIFY_GUN_FIRE_RATE)) return interval;
        double rpm = GunPowers.modifyStat(shooter, GunPowers.MODIFY_GUN_FIRE_RATE, TaczGuns.gunId(gunStack), 60_000.0 / interval);
        return rpm <= 0 ? interval : Math.max(1L, Math.round(60_000.0 / rpm));
    }
}
