package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LocalPlayerElytraMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z",
        remap = false))
    private boolean apoli$canElytraFly(boolean original) {
        return original || PowerLookup.hasActive((LivingEntity) (Object) this, ApoliIds.ELYTRA_FLIGHT);
    }
}
