package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.RestrictArmorPower;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class RestrictArmorTickMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void apoli$dropRestrictedArmor(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;
        if (!PowerLookup.hasActive(self, Apoli.id("restrict_armor"))) return;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            boolean blocked = stackBlocked(self, slot, stack);
            if (blocked) {
                self.setItemSlot(slot, ItemStack.EMPTY);
                self.drop(stack, false, true);
            }
        }
    }

    private static boolean stackBlocked(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        boolean[] hit = new boolean[]{false};
        ItemCtx ctx = new ItemCtx(stack, entity.level(), entity);
        PowerLookup.forEach(entity, Apoli.id("restrict_armor"), RestrictArmorPower.Config.class, cfg -> {
            if (hit[0]) return;
            if (RestrictArmorPower.blocks(cfg, slot, ctx)) hit[0] = true;
        });
        return hit[0];
    }
}
