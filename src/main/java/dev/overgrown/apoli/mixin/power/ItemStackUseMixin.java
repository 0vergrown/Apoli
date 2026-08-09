package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ActionOnItemUsePower;
import dev.overgrown.apoli.power.builtin.EdibleItemHandler;
import dev.overgrown.apoli.power.builtin.EdibleItemPower;
import dev.overgrown.apoli.power.builtin.ItemUseActionHandler;
import dev.overgrown.apoli.power.builtin.PreventItemUseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void apoli$itemUseHead(Level level, Player player, InteractionHand hand,
                                   CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (PreventItemUseHandler.isBlocked(player, self, level)) {
            cir.setReturnValue(InteractionResultHolder.fail(self));
            return;
        }
        ItemUseActionHandler.fire(level, player, self, apoli$useTrigger(self, player), ItemUseActionHandler.Phase.BEFORE);
        EdibleItemPower.Config edible = EdibleItemHandler.find(player, self);
        if (edible == null) return;
        if (!EdibleItemHandler.canConsume(player, edible)) {
            cir.setReturnValue(InteractionResultHolder.fail(self));
            return;
        }
        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.consume(self));
        ItemUseActionHandler.fire(level, player, self, ActionOnItemUsePower.Trigger.START, ItemUseActionHandler.Phase.AFTER);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void apoli$edibleUseDuration(LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        EdibleItemPower.Config edible = EdibleItemHandler.find(user, (ItemStack) (Object) this);
        if (edible != null) cir.setReturnValue(EdibleItemHandler.useDuration(user, edible));
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void apoli$edibleUseAnimation(CallbackInfoReturnable<UseAnim> cir) {
        UseAnim animation = EdibleItemHandler.animationFor((ItemStack) (Object) this);
        if (animation != null) cir.setReturnValue(animation);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void apoli$itemUseAfter(Level level, Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        InteractionResultHolder<ItemStack> result = cir.getReturnValue();
        if (result == null || !result.getResult().consumesAction()) return;
        ItemStack self = (ItemStack) (Object) this;
        ItemUseActionHandler.fire(level, player, self, apoli$useTrigger(self, player), ItemUseActionHandler.Phase.AFTER);
    }

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void apoli$itemUseDuringBefore(Level level, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        EdibleItemHandler.find(user, (ItemStack) (Object) this);
        ItemUseActionHandler.fire(level, user, (ItemStack) (Object) this,
            ActionOnItemUsePower.Trigger.DURING, ItemUseActionHandler.Phase.BEFORE);
    }

    @Inject(method = "onUseTick", at = @At("RETURN"))
    private void apoli$itemUseDuringAfter(Level level, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        ItemUseActionHandler.fire(level, user, (ItemStack) (Object) this,
            ActionOnItemUsePower.Trigger.DURING, ItemUseActionHandler.Phase.AFTER);
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void apoli$itemUseStopBefore(Level level, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        ItemUseActionHandler.fire(level, user, (ItemStack) (Object) this,
            ActionOnItemUsePower.Trigger.STOP, ItemUseActionHandler.Phase.BEFORE);
    }

    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void apoli$itemUseStopAfter(Level level, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
        ItemUseActionHandler.fire(level, user, (ItemStack) (Object) this,
            ActionOnItemUsePower.Trigger.STOP, ItemUseActionHandler.Phase.AFTER);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void apoli$itemUseFinishBefore(Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (user instanceof Player player && PreventItemUseHandler.isBlocked(player, self, level)) {
            user.stopUsingItem();
            cir.setReturnValue(self);
            return;
        }
        ItemUseActionHandler.fire(level, user, self, ActionOnItemUsePower.Trigger.FINISH, ItemUseActionHandler.Phase.BEFORE);
        EdibleItemPower.Config edible = EdibleItemHandler.find(user, self);
        if (edible == null) return;
        cir.setReturnValue(EdibleItemHandler.finish(level, user, self, edible));
        ItemUseActionHandler.fire(level, user, self, ActionOnItemUsePower.Trigger.FINISH, ItemUseActionHandler.Phase.AFTER);
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void apoli$itemUseFinishAfter(Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        ItemUseActionHandler.fire(level, user, (ItemStack) (Object) this,
            ActionOnItemUsePower.Trigger.FINISH, ItemUseActionHandler.Phase.AFTER);
    }

    @Unique
    private static ActionOnItemUsePower.Trigger apoli$useTrigger(ItemStack stack, Player player) {
        return stack.getUseDuration(player) == 0
            ? ActionOnItemUsePower.Trigger.INSTANT
            : ActionOnItemUsePower.Trigger.START;
    }
}
