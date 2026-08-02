package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class UnsuppressPowerAction implements ActionType<EntityCtx, UnsuppressPowerAction.Cfg> {
    public record Cfg(List<ResourceLocation> powers, List<ResourceLocation> sources) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SingleOrList.of(ResourceLocation.CODEC).fieldOf("power").forGetter(Cfg::powers),
            SingleOrList.of(ResourceLocation.CODEC)
                .optionalFieldOf("source", SuppressPowerAction.DEFAULT_SOURCES).forGetter(Cfg::sources)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        for (ResourceLocation power : cfg.powers) {
            for (ResourceLocation source : cfg.sources) {
                holder.unsuppressPower(power, source);
            }
        }
    }
}
