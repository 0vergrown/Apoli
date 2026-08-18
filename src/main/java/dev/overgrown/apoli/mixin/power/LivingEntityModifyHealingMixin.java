package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyHealingHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityModifyHealingMixin extends Entity {

    public LivingEntityModifyHealingMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float modifyHealingApplied(float originalValue) {
        return ModifyHealingHandler.modify(this, originalValue);
    }
}
