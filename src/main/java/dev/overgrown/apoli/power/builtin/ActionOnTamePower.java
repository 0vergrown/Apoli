package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ActionOnTamePower extends PowerType<ActionOnTamePower.Config> {
    public record Config(Optional<BiEntityAction> bientityAction, Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static void fire(Player tamer, Entity animal) {
        if (tamer.level().isClientSide() || !PowerLookup.holdsType(tamer, ApoliIds.ACTION_ON_TAME)) return;
        BiEntityCtx ctx = BiEntityCtx.of(tamer, animal, tamer.level());
        PowerLookup.forEach(tamer, ApoliIds.ACTION_ON_TAME, Config.class, cfg -> {
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            cfg.bientityAction().ifPresent(action -> action.run(ctx));
        });
    }
}
