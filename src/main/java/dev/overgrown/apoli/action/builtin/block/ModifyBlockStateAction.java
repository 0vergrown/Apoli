package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public final class ModifyBlockStateAction implements ActionType<BlockCtx, ModifyBlockStateAction.Cfg> {
    public record Cfg(
        String property,
        String operation,
        Optional<Integer> change,
        Optional<Boolean> value,
        Optional<String> enumValue,
        boolean cycle
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("property").forGetter(Cfg::property),
            Codec.STRING.optionalFieldOf("operation", "add").forGetter(Cfg::operation),
            Codec.INT.optionalFieldOf("change").forGetter(Cfg::change),
            Codec.BOOL.optionalFieldOf("value").forGetter(Cfg::value),
            Codec.STRING.optionalFieldOf("enum").forGetter(Cfg::enumValue),
            Codec.BOOL.optionalFieldOf("cycle", false).forGetter(Cfg::cycle)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        BlockState state = ctx.state();
        Property<?> property = state.getBlock().getStateDefinition().getProperty(cfg.property);
        if (property == null) return;
        BlockState newState = apply(property, state, cfg);
        if (newState != null && newState != state) {
            ctx.level().setBlock(ctx.pos(), newState, 3);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> BlockState apply(Property<T> property, BlockState state, Cfg cfg) {
        if (cfg.cycle) return state.cycle(property);
        T current = state.getValue(property);
        if (cfg.value.isPresent() && current instanceof Boolean) {
            return state.setValue((Property) property, cfg.value.get());
        }
        if (cfg.enumValue.isPresent()) {
            Optional<T> parsed = property.getValue(cfg.enumValue.get());
            return parsed.map(v -> state.setValue(property, v)).orElse(state);
        }
        if (cfg.change.isPresent() && current instanceof Integer n) {
            int target = "set".equals(cfg.operation) ? cfg.change.get() : (n + cfg.change.get());
            Optional<T> parsed = property.getValue(Integer.toString(target));
            return parsed.map(v -> state.setValue(property, v)).orElse(state);
        }
        return state;
    }
}
