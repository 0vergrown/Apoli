package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyXpGainPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityXpMixin {

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void apoli$beginCooking(ServerPlayer player, CallbackInfo ci) {
        ModifyXpGainPower.beginCooking(player, (AbstractFurnaceBlockEntity) (Object) this);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("RETURN"))
    private void apoli$endCooking(ServerPlayer player, CallbackInfo ci) {
        ModifyXpGainPower.endCooking();
    }

    @ModifyVariable(method = "createExperience", at = @At("HEAD"), argsOnly = true)
    private static float apoli$modifyCookingXp(float xp) {
        return ModifyXpGainPower.modifyCookingXp(xp);
    }
}
