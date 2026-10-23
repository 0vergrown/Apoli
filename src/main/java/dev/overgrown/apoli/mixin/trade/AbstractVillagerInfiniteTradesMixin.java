package dev.overgrown.apoli.mixin.trade;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.overgrown.apoli.power.builtin.InfiniteTradesPower;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerInfiniteTradesMixin {

    @Inject(method = "notifyTrade", at = @At("HEAD"))
    private void apoli$rememberUses(MerchantOffer offer, CallbackInfo ci, @Share("apoli$uses") LocalIntRef uses) {
        uses.set(offer.getUses());
    }

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void apoli$keepInStock(MerchantOffer offer, CallbackInfo ci, @Share("apoli$uses") LocalIntRef uses) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!InfiniteTradesPower.applies(self)) return;
        ((MerchantOfferAccessor) offer).apoli$setUses(uses.get());
        InfiniteTradesPower.resendOffers(self);
    }
}
