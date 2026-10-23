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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ActionOnBreedPower extends PowerType<ActionOnBreedPower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BiEntityAction> parentsAction
    ) {}

    private static final List<ResourceLocation> RUNNING = new ArrayList<>(2);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            LoggedOptionalField.of("parents_action", BiEntityAction.CODEC).forGetter(Config::parentsAction)
        ).apply(i, Config::new));
    }

    public static void fire(Animal parent, Animal mate, AgeableMob baby) {
        ServerPlayer breeder = parent.getLoveCause();
        if (breeder == null) breeder = mate.getLoveCause();
        if (breeder == null || !PowerLookup.holdsType(breeder, ApoliIds.ACTION_ON_BREED)) return;
        BiEntityCtx babyCtx = BiEntityCtx.of(breeder, baby, parent.level());
        PowerLookup.forEachEntry(breeder, ApoliIds.ACTION_ON_BREED, Config.class, (powerId, cfg) -> {
            if (RUNNING.contains(powerId)) return;
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(babyCtx)) return;
            RUNNING.add(powerId);
            try {
                cfg.bientityAction().ifPresent(action -> action.run(babyCtx));
                cfg.parentsAction().ifPresent(action -> action.run(BiEntityCtx.of(parent, mate, parent.level())));
            } finally {
                RUNNING.remove(RUNNING.size() - 1);
            }
        });
    }
}
