package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerSources;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public final class RevokeAllPowersAction implements ActionType<EntityCtx, RevokeAllPowersAction.Cfg> {
    public record Cfg(List<ResourceLocation> sources) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SingleOrList.of(IdCodecs.ID).fieldOf("source").forGetter(Cfg::sources)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        for (ResourceLocation source : cfg.sources) {
            holder.removeAllFromSource(source);
            Set<ResourceLocation> provided = PowerSources.powersOf(source);
            if (provided == null) continue;
            for (ResourceLocation power : provided) holder.removePowerCompletely(power);
        }
    }
}
