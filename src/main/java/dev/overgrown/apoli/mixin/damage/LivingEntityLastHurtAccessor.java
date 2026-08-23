package dev.overgrown.apoli.mixin.damage;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityLastHurtAccessor {

    @Accessor("lastHurt")
    float apoli$getLastHurt();
}
