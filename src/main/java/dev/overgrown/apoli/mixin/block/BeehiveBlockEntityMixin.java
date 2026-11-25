package dev.overgrown.apoli.mixin.block;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.PreventBeeAngerPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeehiveBlockEntity.class)
public class BeehiveBlockEntityMixin {

    @WrapWithCondition(method = "emptyAllLivingFromHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private boolean apoli$preventBeeAnger(Bee bee, LivingEntity target, @Local(argsOnly = true) BlockState state) {
        BlockEntity hive = (BlockEntity) (Object) this;
        return !PreventBeeAngerPower.keepsCalm(target, bee, target.level(), hive.getBlockPos(), state, true);
    }
}
