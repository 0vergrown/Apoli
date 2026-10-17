package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class PreventEntitySelectionPower extends PowerType<PreventEntitySelectionPower.Config> {
    public record Config (
            Optional<EntityCondition> entityCondition,
            Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec( i -> i.group(
                dev.overgrown.apoli.codec.LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
                dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }

    public static boolean prevents(Entity viewer, Entity entity) {
        return PowerLookup.anyActive(viewer, ApoliIds.PREVENT_ENTITY_SELECTION, Config.class,
            PreventEntitySelectionPower::blocks, BiEntityCtx.of(viewer, entity, entity.level()));
    }

    private static boolean blocks(Config config, BiEntityCtx pair) {
        if (config.entityCondition().isPresent()
            && !config.entityCondition().get().test(EntityCtx.of(pair.target(), pair.level()))) return false;
        return config.bientityCondition().isEmpty() || config.bientityCondition().get().test(pair);
    }
}
