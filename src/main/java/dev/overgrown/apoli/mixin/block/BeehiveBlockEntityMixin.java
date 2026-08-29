package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PreventBeeAngerPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public class BeehiveBlockEntityMixin {

    @Redirect(method = "emptyAllLivingFromHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    public void apoli$emptyAllLivingFromHive(Bee instance, LivingEntity livingEntity, Player player, BlockState blockState) {
        List<Boolean> anger = new ArrayList<>();

        PowerLookup.forEach(livingEntity, Apoli.id("prevent_bee_anger"), PreventBeeAngerPower.Config.class, config -> {

            BiEntityCtx bi = BiEntityCtx.of(instance, livingEntity, player.level());
            BlockCtx block = new BlockCtx(((BlockEntity) (Object) this).getBlockPos(), blockState, player.level());

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