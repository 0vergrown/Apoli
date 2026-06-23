package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class LocalPlayerElytraMixin {

    @Redirect(method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean apoli$treatElytraFlightAsElytra(ItemStack stack, Item item) {
        if (item == Items.ELYTRA
            && PowerLookup.hasActive((Player) (Object) this, Apoli.id("elytra_flight"))) {
            return true;
        }
        return stack.is(item);
    }

    @Redirect(method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean apoli$elytraAlwaysFlyEnabled(ItemStack stack) {
        if (PowerLookup.hasActive((Player) (Object) this, Apoli.id("elytra_flight"))) {
            return true;
        }
        return net.minecraft.world.item.ElytraItem.isFlyEnabled(stack);
    }
}
