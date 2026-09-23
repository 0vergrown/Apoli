package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class LocalPlayerElytraMixin {

    @ModifyExpressionValue(method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean apoli$treatElytraFlightAsElytra(boolean original) {
        return original || PowerLookup.hasActive((Player) (Object) this, ApoliIds.ELYTRA_FLIGHT);
    }

    @ModifyExpressionValue(method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean apoli$elytraAlwaysFlyEnabled(boolean original) {
        return original || PowerLookup.hasActive((Player) (Object) this, ApoliIds.ELYTRA_FLIGHT);
    }
}
