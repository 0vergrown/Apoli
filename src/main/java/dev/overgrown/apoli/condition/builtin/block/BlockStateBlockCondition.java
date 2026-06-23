package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public final class BlockStateBlockCondition implements ConditionType<BlockCtx, BlockStateBlockCondition.Cfg> {
    public record Cfg(
        String property,
        Optional<Comparison> comparison,
        Optional<Integer> compareTo,
        Optional<Boolean> value,
        Optional<String> enumValue
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("property").forGetter(Cfg::property),
            Comparison.CODEC.optionalFieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to").forGetter(Cfg::compareTo),
            Codec.BOOL.optionalFieldOf("value").forGetter(Cfg::value),
            Codec.STRING.optionalFieldOf("enum").forGetter(Cfg::enumValue)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        Property<?> property = ctx.state().getBlock().getStateDefinition().getProperty(cfg.property);
        if (property == null) return false;
        if (cfg.value.isEmpty() && cfg.enumValue.isEmpty() && cfg.comparison.isEmpty()) {
            return true;
        }
        return testProperty(property, cfg, ctx);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> boolean testProperty(Property<T> property, Cfg cfg, BlockCtx ctx) {
        T current = ctx.state().getValue(property);
        if (cfg.value.isPresent() && current instanceof Boolean b) {
            return b.equals(cfg.value.get());
        }
        if (cfg.enumValue.isPresent()) {
            return property.getName(current).equals(cfg.enumValue.get());
        }
        if (cfg.comparison.isPresent() && cfg.compareTo.isPresent() && current instanceof Integer n) {
            return cfg.comparison.get().compare(n, cfg.compareTo.get());
        }
        return false;
    }
}
