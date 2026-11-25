package dev.overgrown.apoli.mixin.block;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.PreventBeeAngerPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @WrapWithCondition(method = "angerNearbyBees", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private boolean apoli$preventBeeAnger(Bee bee, LivingEntity target,
                                          @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        return !PreventBeeAngerPower.keepsCalm(target, bee, level, pos, null, false);
    }
}
