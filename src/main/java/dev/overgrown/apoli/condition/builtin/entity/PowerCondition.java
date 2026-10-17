package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class PowerCondition implements ConditionType<EntityCtx, PowerCondition.Cfg> {
    public record Cfg(Optional<ResourceLocation> power, List<String> tags, Optional<ResourceLocation> source) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("power").forGetter(Cfg::power),
            SingleOrList.of(Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(Cfg::tags),
            IdCodecs.ID.optionalFieldOf("source").forGetter(Cfg::source)
        ).apply(i, Cfg::new)).flatXmap(PowerCondition::requireTarget, DataResult::success);
    }

    private static DataResult<Cfg> requireTarget(Cfg cfg) {
        if (cfg.power.isPresent() || !cfg.tags.isEmpty()) return DataResult.success(cfg);
        return DataResult.error(() -> "apoli:power needs a 'power' id, a list of power 'tags', or both");
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null || container.isEmpty()) return false;
        if (cfg.power.isPresent() && holds(container, cfg.power.get(), cfg.source)) return true;
        List<String> tags = cfg.tags;
        for (int t = 0; t < tags.size(); t++) {
            List<ResourceLocation> tagged = ApoliPowers.withTag(tags.get(t));
            for (int i = 0; i < tagged.size(); i++) {
                if (holds(container, tagged.get(i), cfg.source)) return true;
            }
        }
        return false;
    }

    private static boolean holds(PowerContainer container, ResourceLocation power, Optional<ResourceLocation> source) {
        if (!container.hasPower(power)) return false;
        return source.isEmpty() || container.sourcesOf(power).contains(source.get());
    }
}
