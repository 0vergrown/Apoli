package dev.overgrown.apoli.mixin.flag;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAirAccessor {

    @Invoker("increaseAirSupply")
    int apoli$increaseAirSupply(int air);

    @Invoker("decreaseAirSupply")
    int apoli$decreaseAirSupply(int air);
}
