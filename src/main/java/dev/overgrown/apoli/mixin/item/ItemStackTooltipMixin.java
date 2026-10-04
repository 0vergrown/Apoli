package dev.overgrown.apoli.mixin.item;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.TooltipPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
@Environment(EnvType.CLIENT)
public abstract class ItemStackTooltipMixin {
    @Inject(
        method = "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void apoli$tooltipBelowName(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
                                        CallbackInfoReturnable<List<Component>> cir, @Local List<Component> lines) {
        TooltipPower.appendLines(player, (ItemStack) (Object) this, TooltipPower.Position.BELOW_NAME, lines);
    }

    @Inject(
        method = "getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
        slice = @Slice(from = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/core/component/DataComponents;LORE:Lnet/minecraft/core/component/DataComponentType;")),
        at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V")
    )
    private void apoli$tooltipBelowLore(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
                                        CallbackInfoReturnable<List<Component>> cir, @Local List<Component> lines) {
        TooltipPower.appendLines(player, (ItemStack) (Object) this, TooltipPower.Position.BELOW_LORE, lines);
    }
}
