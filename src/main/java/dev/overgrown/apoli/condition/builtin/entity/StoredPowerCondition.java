package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PowerStoragePower;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class StoredPowerCondition implements ConditionType<EntityCtx, StoredPowerCondition.Cfg> {

    public record Cfg(Optional<ResourceLocation> storage, Optional<ResourceLocation> power, List<String> tags,
                      Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("storage", IdCodecs.ID).forGetter(Cfg::storage),
            LoggedOptionalField.strict("power", IdCodecs.ID).forGetter(Cfg::power),
            LoggedOptionalField.of("tags", SingleOrList.of(Codec.STRING), List.of()).forGetter(Cfg::tags),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.raw());
        if (container == null || container.isEmpty()) return cfg.comparison().compare(0, cfg.compareTo());
        if (container.powersOfType(ApoliIds.POWER_STORAGE).isEmpty()) {
            return cfg.comparison().compare(0, cfg.compareTo());
        }

        int[] matches = new int[1];
        PowerLookup.forEachEntry(ctx.raw(), ApoliIds.POWER_STORAGE, PowerStoragePower.Config.class, (storageId, storage) -> {
            if (cfg.storage().isPresent() && !cfg.storage().get().equals(storageId)) return;
            List<ResourceLocation> powers = PowerStoragePower.live(container, storageId);
            for (int i = 0; i < powers.size(); i++) {
                ResourceLocation id = powers.get(i);
                if (cfg.power().isPresent() && !cfg.power().get().equals(id)) continue;
                if (!cfg.tags().isEmpty() && !ApoliPowers.hasAnyTag(id, cfg.tags())) continue;
                matches[0]++;
            }
        });
        return cfg.comparison().compare(matches[0], cfg.compareTo());
    }
}
