package dev.overgrown.apoli.mixin.enchantment;

import dev.overgrown.apoli.power.builtin.ModifyEnchantabilityPower;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuEnchantabilityMixin {

    @Unique private Player apoli$enchanter;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL"))
    private void apoli$rememberEnchanter(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.apoli$enchanter = inventory.player;
    }

    @Inject(method = "getEnchantmentList", at = @At("HEAD"))
    private void apoli$beginEnchanting(RegistryAccess registries, ItemStack stack, int slot, int cost,
                                       CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        ModifyEnchantabilityPower.begin(this.apoli$enchanter);
    }

    @Inject(method = "getEnchantmentList", at = @At("RETURN"))
    private void apoli$endEnchanting(RegistryAccess registries, ItemStack stack, int slot, int cost,
                                     CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        ModifyEnchantabilityPower.end();
    }
}
