package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public final class ModifyResourceAction implements ActionType<EntityCtx, ModifyResourceAction.Cfg> {

    public record Cfg(AttributeModifier modifier, ResourceLocation resource) {}

    private static final MapCodec<Cfg> CANONICAL = RecordCodecBuilder.mapCodec(i -> i.group(
        AttributeModifier.CODEC.fieldOf("modifier").forGetter(Cfg::modifier),
        ResourceLocation.CODEC.fieldOf("resource").forGetter(Cfg::resource)
    ).apply(i, Cfg::new));

    private static final MapCodec<Cfg> WITH_LEGACY = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(
                ops.createString("modifier"),
                ops.createString("resource"),
                ops.createString("change"),
                ops.createString("operation")
            );
        }

        @Override
        public <T> DataResult<Cfg> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (input.get("modifier") != null) {
                return CANONICAL.decode(ops, input);
            }
            T changeRaw = input.get("change");
            T operationRaw = input.get("operation");
            T resourceRaw = input.get("resource");
            if (changeRaw == null && operationRaw == null) {
                return CANONICAL.decode(ops, input);
            }
            if (resourceRaw == null) {
                return DataResult.error(() -> "Legacy change_resource requires a 'resource' field");
            }
            DataResult<ResourceLocation> resourceR = ResourceLocation.CODEC.parse(ops, resourceRaw);
            if (resourceR.error().isPresent()) {
                return DataResult.error(() -> "Invalid 'resource': " + resourceR.error().get().message());
            }
            int change = 0;
            if (changeRaw != null) {
                DataResult<Number> n = ops.getNumberValue(changeRaw);
                if (n.error().isPresent()) {
                    return DataResult.error(() -> "Invalid 'change': " + n.error().get().message());
                }
                change = n.result().get().intValue();
            }
            String operationStr = "add";
            if (operationRaw != null) {
                DataResult<String> s = ops.getStringValue(operationRaw);
                if (s.error().isPresent()) {
                    return DataResult.error(() -> "Invalid 'operation': " + s.error().get().message());
                }
                operationStr = s.result().get();
            }
            AttributeModifierOperation op = switch (operationStr) {
                case "add" -> AttributeModifierOperation.ADD_BASE_EARLY;
                case "set" -> AttributeModifierOperation.SET_BASE;
                default -> null;
            };
            if (op == null) {
                String captured = operationStr;
                return DataResult.error(() -> "Legacy 'operation' must be 'add' or 'set', got: " + captured);
            }
            AttributeModifier synth = new AttributeModifier(
                op,
                Expression.constant(change),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
            return DataResult.success(new Cfg(synth, resourceR.result().get()));
        }

        @Override
        public <T> RecordBuilder<T> encode(Cfg input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return CANONICAL.encode(input, ops, prefix);
        }
    };

    @Override
    public MapCodec<Cfg> codec() {
        return WITH_LEGACY;
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null) return;
        OptionalInt cur = ResourcePower.readValue(container, cfg.resource);
        if (cur.isEmpty()) return;
        Map<String, Double> vars = ExpressionContext.forResource(entity, cur.getAsInt());
        Map<String, Double> resources = ExpressionContext.resourceValues(container);
        double next = cfg.modifier.applyToValue(cur.getAsInt(), vars, resources);
        ResourcePower.writeValue(container, cfg.resource, (int) Math.round(next));
    }
}
