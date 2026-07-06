package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifySlipperinessHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;








@Mixin(LivingEntity.class)
public abstract class LivingEntitySlipperinessMixin extends Entity {

    public LivingEntitySlipperinessMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
        method = "travel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F")
    )
    private float apoli$modifySlipperiness(float original) {
        return ModifySlipperinessHandler.modify(
            (LivingEntity) (Object) this,
            this.getBlockPosBelowThatAffectsMyMovement(),
            original
        );
    }
}
