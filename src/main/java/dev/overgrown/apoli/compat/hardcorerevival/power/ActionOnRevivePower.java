package dev.overgrown.apoli.compat.hardcorerevival.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ActionOnRevivePower extends PowerType<ActionOnRevivePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_revive");

    public record Config(Optional<EntityAction> entityAction, Optional<BiEntityAction> bientityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction)
        ).apply(i, Config::new));
    }

    public static void handleRevived(Player player) {
        if (player.level().isClientSide()) return;
        EntityCtx ctx = EntityCtx.of(player, player.level());
        PowerLookup.forEach(player, CANONICAL, Config.class, cfg -> cfg.entityAction().ifPresent(a -> a.run(ctx)));
    }

    public static void handleRescued(Player player, Player rescuer) {
        if (player.level().isClientSide()) return;
        BiEntityCtx ctx = new BiEntityCtx(rescuer, player, player.level());
        PowerLookup.forEach(player, CANONICAL, Config.class, cfg -> cfg.bientityAction().ifPresent(a -> a.run(ctx)));
    }
}
