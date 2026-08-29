package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PreventBeeAngerPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @Redirect(method = "angerNearbyBees", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    public void apoli$angerNearbyBees(Bee instance, LivingEntity livingEntity, Level level, BlockPos blockPos) {
        List<Boolean> anger = new ArrayList<>();

        PowerLookup.forEach(livingEntity, Apoli.id("prevent_bee_anger"), PreventBeeAngerPower.Config.class, config -> {

           BiEntityCtx bi = BiEntityCtx.of(instance, livingEntity, level);
           BlockCtx block = new BlockCtx(blockPos, level.getBlockState(blockPos), level);

            if ((config.biEntityCondition().isEmpty() || config.biEntityCondition().get().test(bi))
                    && (config.blockCondition().isEmpty() || config.blockCondition().get().test(block))) {

                anger.add(false);

                if (config.biEntityAction().isPresent()) {
                    config.biEntityAction().get().run(bi);
                }
                if (config.blockAction().isPresent()) {
                    config.blockAction().get().run(block);
                }
           }
       });

        if (anger.isEmpty()) {
            instance.setTarget(livingEntity);
        }
    }
}