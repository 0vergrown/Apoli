package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;

public final class InEntitySetCondition implements ConditionType<BiEntityCtx, InEntitySetCondition.Cfg> {
    public record Cfg(ResourceLocation set) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("set").forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() == null || ctx.target() == null) return false;
        PowerContainer container = PowerContainer.of(ctx.actor());
        if (container == null || !container.hasPower(cfg.set)) return false;
        return EntitySetPower.contains(ctx.actor(), cfg.set, ctx.target());
    }
}
