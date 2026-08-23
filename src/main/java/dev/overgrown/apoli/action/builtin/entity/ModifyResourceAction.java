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
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public final class ModifyResourceAction implements ActionType<EntityCtx, ModifyResourceAction.Cfg> {

    public record Cfg(
        AttributeModifier modifier,
        ResourceLocation resource,
        Optional<Expression> position,
        Optional<ResourceLocation> from,
        Optional<Expression> fromPosition
    ) {}

    private static final MapCodec<Cfg> CANONICAL = dev.overgrown.apoli.alias.AliasingMapCodec.wrap(
        RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier",
                new AttributeModifier(AttributeModifierOperation.SET_BASE, Expression.constant(0.0),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())).forGetter(Cfg::modifier),
            IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource),
            Expression.INT_OR_EXPR.optionalFieldOf("position").forGetter(Cfg::position),
            IdCodecs.ID.optionalFieldOf("from").forGetter(Cfg::from),
            Expression.INT_OR_EXPR.optionalFieldOf("from_position").forGetter(Cfg::fromPosition)
        ).apply(i, Cfg::new)),
        java.util.Map.of(
            "index", "position",
            "slot", "position",
            "from_resource", "from",
            "from_index", "from_position",
            "from_slot", "from_position"));

    private static final MapCodec<Cfg> WITH_LEGACY = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(
                ops.createString("modifier"),
                ops.createString("resource"),
                ops.createString("position"),
                ops.createString("from"),
                ops.createString("from_position"),
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
            DataResult<ResourceLocation> resourceR = IdCodecs.ID.parse(ops, resourceRaw);
            if (resourceR.error().isPresent()) {
                return DataResult.error(() -> "Invalid 'resource': " + resourceR.error().get().message());
            }
            Expression change = Expression.constant(0);
            if (changeRaw != null) {
                DataResult<Expression> c = Expression.INT_OR_EXPR.parse(ops, changeRaw);
                if (c.error().isPresent()) {
                    return DataResult.error(() -> "Invalid 'change': " + c.error().get().message());
                }
                change = c.result().get();
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
                change,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
            DataResult<Optional<Expression>> positionR = Expression.INT_OR_EXPR
                .optionalFieldOf("position").decode(ops, input);
            Optional<Expression> position = positionR.result().orElse(Optional.empty());
            return DataResult.success(new Cfg(synth, resourceR.result().get(), position,
                Optional.empty(), Optional.empty()));
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
        Entity entity = ctx.entity();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null) return;

        if (cfg.from.isPresent()) {
            copy(cfg, entity, container);
            return;
        }

        int size = PowerResources.size(container, cfg.resource);
        if (cfg.position.isEmpty() && size > 1) {
            for (int slot = 0; slot < size; slot++) applyAt(cfg, entity, container, slot);
            return;
        }
        if (cfg.position.isEmpty()) {
            OptionalInt cur = PowerResources.read(container, cfg.resource);
            if (cur.isEmpty()) return;
            double next = cfg.modifier.applyToValue(cur.getAsInt(), entity, container);
            PowerResources.write(container, cfg.resource, (int) Math.round(next));
            return;
        }
        applyAt(cfg, entity, container, cfg.position.get().evalIntWith(entity, container, 0));
    }

    private static void applyAt(Cfg cfg, Entity entity, PowerContainer container, int slot) {
        OptionalInt cur = PowerResources.readAt(container, cfg.resource, slot);
        if (cur.isEmpty()) return;
        double next = cfg.modifier.applyToValue(cur.getAsInt(), entity, container);
        PowerResources.writeAt(container, cfg.resource, slot, (int) Math.round(next));
    }

    private static void copy(Cfg cfg, Entity entity, PowerContainer container) {
        ResourceLocation source = cfg.from.get();
        boolean wholeTable = cfg.position.isEmpty() && cfg.fromPosition.isEmpty();
        if (wholeTable) {
            int destination = Math.max(1, PowerResources.size(container, cfg.resource));
            int available = Math.max(1, PowerResources.size(container, source));
            int slots = Math.min(destination, available);
            for (int slot = 0; slot < slots; slot++) {
                OptionalInt value = PowerResources.readAt(container, source, slot);
                if (value.isEmpty()) continue;
                writeInto(cfg, entity, container, slot, value.getAsInt());
            }
            return;
        }
        int fromSlot = cfg.fromPosition.isPresent()
            ? cfg.fromPosition.get().evalIntWith(entity, container, 0)
            : 0;
        OptionalInt value = PowerResources.readAt(container, source, fromSlot);
        if (value.isEmpty()) return;
        int toSlot = cfg.position.isPresent()
            ? cfg.position.get().evalIntWith(entity, container, 0)
            : fromSlot;
        writeInto(cfg, entity, container, toSlot, value.getAsInt());
    }

    private static void writeInto(Cfg cfg, Entity entity, PowerContainer container, int slot, int incoming) {
        OptionalInt cur = PowerResources.readAt(container, cfg.resource, slot);
        if (cur.isEmpty()) return;
        double next = cfg.modifier.operation().applySingle(cur.getAsInt(), incoming);
        PowerResources.writeAt(container, cfg.resource, slot, (int) Math.round(next));
    }
}
