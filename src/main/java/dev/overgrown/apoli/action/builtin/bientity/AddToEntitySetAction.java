package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.OptionalInt;

public final class AddToEntitySetAction implements ActionType<BiEntityCtx, AddToEntitySetAction.Cfg> {
    public record Cfg(ResourceLocation set, Optional<Integer> timeLimit) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("set").forGetter(Cfg::set),
            Codec.INT.optionalFieldOf("time_limit").forGetter(Cfg::timeLimit)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() == null || ctx.target() == null) return;
        PowerContainer container = PowerContainer.of(ctx.actor());
        if (container == null || !container.hasPower(cfg.set)) return;
        EntitySetPower.Cfg setCfg = EntitySetPower.resolveCfg(cfg.set);
        if (setCfg == null) return;
        OptionalInt limit = cfg.timeLimit.map(OptionalInt::of).orElse(OptionalInt.empty());
        EntitySetPower.add(ctx.actor(), cfg.set, setCfg, ctx.target(), limit);
    }
}
