package dev.overgrown.apoli.mixin.trade;

import dev.overgrown.apoli.power.builtin.ExtraTradesPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerExtraTradesMixin {

    @Shadow @Nullable protected MerchantOffers offers;

    @Shadow public abstract MerchantOffers getOffers();

    @Unique private Map<ResourceLocation, MerchantOffers> apoli$rolledTrades = Map.of();

    @Unique private List<MerchantOffer> apoli$shownTrades = List.of();

    @Inject(method = "setTradingPlayer", at = @At("HEAD"))
    private void apoli$showExtraTrades(@Nullable Player player, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        this.apoli$hideExtraTrades();
        if (player == null || self.level().isClientSide()) return;
        if (this.apoli$rolledTrades.isEmpty()) this.apoli$rolledTrades = new LinkedHashMap<>();
        List<MerchantOffer> extra = ExtraTradesPower.offersFor(self, player, this.apoli$rolledTrades);
        if (extra.isEmpty()) return;
        this.getOffers().addAll(extra);
        this.apoli$shownTrades = extra;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void apoli$saveWithoutExtraTrades(CompoundTag tag, CallbackInfo ci) {
        if (!this.apoli$shownTrades.isEmpty() && this.offers != null) this.offers.removeAll(this.apoli$shownTrades);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void apoli$saveExtraTrades(CompoundTag tag, CallbackInfo ci) {
        if (!this.apoli$shownTrades.isEmpty() && this.offers != null) this.offers.addAll(this.apoli$shownTrades);
        ExtraTradesPower.save((AbstractVillager) (Object) this, this.apoli$rolledTrades, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void apoli$loadExtraTrades(CompoundTag tag, CallbackInfo ci) {
        Map<ResourceLocation, MerchantOffers> loaded = ExtraTradesPower.load((AbstractVillager) (Object) this, tag);
        if (!loaded.isEmpty()) this.apoli$rolledTrades = loaded;
    }

    @Unique
    private void apoli$hideExtraTrades() {
        if (this.apoli$shownTrades.isEmpty()) return;
        if (this.offers != null) this.offers.removeAll(this.apoli$shownTrades);
        this.apoli$shownTrades = List.of();
    }
}
