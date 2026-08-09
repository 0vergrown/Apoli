package dev.overgrown.apoli.mixin.typetag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyTypeTagPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMobTypeMixin {

    @ModifyReturnValue(method = "getMobType()Lnet/minecraft/world/entity/MobType;", at = @At("RETURN"))
    private MobType apoli$modifyMobType(MobType original) {
        if (!ModifyTypeTagPower.active()) return original;
        return ModifyTypeTagPower.resolveMobType((LivingEntity) (Object) this, original);
    }
}
