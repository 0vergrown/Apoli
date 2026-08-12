package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.CooldownPower;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class TriggerCooldownAction implements ActionType<EntityCtx, TriggerCooldownAction.Cfg> {
    public record Cfg(ResourceLocation power) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(
            RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
                IdCodecs.ID.fieldOf("power").forGetter(Cfg::power)
            ).apply(i, Cfg::new)),
            Map.of("resource", "power")
        );
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null) return;
        CooldownPower.trigger(container, cfg.power);
    }
}
