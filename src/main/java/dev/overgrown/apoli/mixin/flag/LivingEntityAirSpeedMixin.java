package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyAirSpeedPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class, Player.class})
public abstract class LivingEntityAirSpeedMixin {

    @ModifyReturnValue(method = "getFlyingSpeed", at = @At("RETURN"))
    private float apoli$modifyAirSpeed(float original) {
        return ModifyAirSpeedPower.modify((LivingEntity) (Object) this, original);
    }
}
