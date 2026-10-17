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
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.TogglePower;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class PowerActiveCondition implements ConditionType<EntityCtx, PowerActiveCondition.Cfg> {
    public record Cfg(Optional<ResourceLocation> power, List<String> tags) {}

    private static final int MAX_DEPTH = 16;

    private static final ThreadLocal<Evaluating> EVALUATING = ThreadLocal.withInitial(Evaluating::new);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("power").forGetter(Cfg::power),
            SingleOrList.of(Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(Cfg::tags)
        ).apply(i, Cfg::new)).flatXmap(PowerActiveCondition::requireTarget, DataResult::success);
    }

    private static DataResult<Cfg> requireTarget(Cfg cfg) {
        if (cfg.power.isPresent() || !cfg.tags.isEmpty()) return DataResult.success(cfg);
        return DataResult.error(() -> "apoli:power_active needs a 'power' id, a list of power 'tags', or both");
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null || container.isEmpty()) return false;
        if (cfg.power.isPresent() && isActive(container, cfg.power.get(), ctx)) return true;
        List<String> tags = cfg.tags;
        for (int t = 0; t < tags.size(); t++) {
            List<ResourceLocation> tagged = ApoliPowers.withTag(tags.get(t));
            for (int i = 0; i < tagged.size(); i++) {
                if (isActive(container, tagged.get(i), ctx)) return true;
            }
        }
        return false;
    }

    public static boolean isActive(PowerContainer container, ResourceLocation powerId, EntityCtx ctx) {
        if (!container.hasPower(powerId)) return false;
        if (container.isSuppressed(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return false;
        if (loaded.condition().isPresent() && !conditionHolds(loaded, powerId, ctx)) return false;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        return !(type instanceof TogglePower) || TogglePower.isActive(ctx.entity(), powerId);
    }

    private static boolean conditionHolds(Power loaded, ResourceLocation powerId, EntityCtx ctx) {
        Evaluating evaluating = EVALUATING.get();
        if (!evaluating.enter(powerId)) return false;
        try {
            return loaded.condition().get().test(ctx);
        } finally {
            evaluating.exit();
        }
    }

    private static final class Evaluating {
        private final ResourceLocation[] stack = new ResourceLocation[MAX_DEPTH];
        private int depth;

        boolean enter(ResourceLocation powerId) {
            if (depth >= MAX_DEPTH) return false;
            for (int i = 0; i < depth; i++) {
                if (stack[i].equals(powerId)) return false;
            }
            stack[depth++] = powerId;
            return true;
        }

        void exit() {
            stack[--depth] = null;
        }
    }
}
