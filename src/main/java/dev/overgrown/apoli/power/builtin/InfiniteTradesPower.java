package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.Optional;

public final class InfiniteTradesPower extends PowerType<InfiniteTradesPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition) {}

    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.INFINITE_TRADES);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static boolean applies(AbstractVillager merchant) {
        if (!IN_USE.inUse()) return false;
        Player player = merchant.getTradingPlayer();
        if (player == null || player.level().isClientSide() || !PowerLookup.holdsType(player, ApoliIds.INFINITE_TRADES)) {
            return false;
        }
        BiEntityCtx ctx = BiEntityCtx.of(player, merchant, merchant.level());
        return PowerLookup.anyActive(player, ApoliIds.INFINITE_TRADES, Config.class,
            cfg -> cfg.bientityCondition().isEmpty() || cfg.bientityCondition().get().test(ctx));
    }

    public static void resendOffers(AbstractVillager merchant) {
        if (!(merchant.getTradingPlayer() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof MerchantMenu menu)) return;
        int level = merchant instanceof Villager villager ? villager.getVillagerData().getLevel() : 1;
        player.sendMerchantOffers(menu.containerId, merchant.getOffers(), level, merchant.getVillagerXp(),
            merchant.showProgressBar(), merchant.canRestock());
    }
}
