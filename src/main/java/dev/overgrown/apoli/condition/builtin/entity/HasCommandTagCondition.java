package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class HasCommandTagCondition implements ConditionType<EntityCtx, HasCommandTagCondition.Cfg> {
    public record Cfg(List<String> commandTags) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("command_tag").forGetter(c -> Optional.empty()),
            Codec.STRING.listOf().optionalFieldOf("command_tags", List.of()).forGetter(Cfg::commandTags)
        ).apply(i, (single, list) -> {
            if (single.isEmpty()) return new Cfg(list);
            List<String> merged = new ArrayList<>(list);
            merged.add(single.get());
            return new Cfg(List.copyOf(merged));
        }));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return false;
        Set<String> tags = entity.getTags();
        if (cfg.commandTags.isEmpty()) return !tags.isEmpty();
        for (int i = 0; i < cfg.commandTags.size(); i++) {
            if (tags.contains(cfg.commandTags.get(i))) return true;
        }
        return false;
    }
}
