package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.CooldownPower;
import net.minecraft.resources.ResourceLocation;

public final class TriggerCooldownAction implements ActionType<EntityCtx, TriggerCooldownAction.Cfg> {
    public record Cfg(ResourceLocation resource) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(Cfg::resource)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null) return;
        CooldownPower.trigger(container, cfg.resource);
    }
}
