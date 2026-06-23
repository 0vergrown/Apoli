package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;

public final class GrantPowerAction implements ActionType<EntityCtx, GrantPowerAction.Cfg> {
    public record Cfg(ResourceLocation power, ResourceLocation source) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("power").forGetter(Cfg::power),
            ResourceLocation.CODEC.fieldOf("source").forGetter(Cfg::source)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder != null) holder.addPower(cfg.power, cfg.source);
    }
}
