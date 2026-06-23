package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;

public final class RemoveFromEntitySetAction implements ActionType<BiEntityCtx, RemoveFromEntitySetAction.Cfg> {
    public record Cfg(ResourceLocation set) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("set").forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.actor());
        if (container == null || !container.hasPower(cfg.set)) return;
        EntitySetPower.Cfg setCfg = EntitySetPower.resolveCfg(cfg.set);
        if (setCfg == null) return;
        EntitySetPower.remove(ctx.actor(), cfg.set, setCfg, ctx.target());
    }
}
