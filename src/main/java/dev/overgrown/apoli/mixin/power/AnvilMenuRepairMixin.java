package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyAnvilPower;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuRepairMixin extends ItemCombinerMenu {

    private AnvilMenuRepairMixin(@Nullable MenuType<?> type, int containerId, Inventory inventory,
                                 ContainerLevelAccess access) {
        super(type, containerId, inventory, access);
    }

    @ModifyExpressionValue(method = "createResult", at = @At(value = "CONSTANT", args = "intValue=4"))
    private int apoli$modifyRepairMaterial(int original) {
        return ModifyAnvilPower.repairMaterial(this.player, this.inputSlots.getItem(0), original);
    }

    @ModifyExpressionValue(method = "createResult", at = @At(value = "CONSTANT", args = "intValue=12"))
    private int apoli$modifyCombineBonus(int original) {
        return ModifyAnvilPower.combineBonus(this.player, this.inputSlots.getItem(0), original);
    }
}
