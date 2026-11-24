package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.station.FurnaceStation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotStationMixin {
    @Shadow
    @Final
    private Player player;

    @Inject(method = "checkTakeAchievements", at = @At("TAIL"))
    private void apoli$awardStationXp(ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer
            && ((Slot) (Object) this).container instanceof FurnaceStation station) {
            station.awardUsedRecipes(serverPlayer);
        }
    }
}
