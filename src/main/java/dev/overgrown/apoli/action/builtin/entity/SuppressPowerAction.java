package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class SuppressPowerAction implements ActionType<EntityCtx, SuppressPowerAction.Cfg> {
    public static final ResourceLocation DEFAULT_SOURCE = Apoli.id("suppressed");
    public static final List<ResourceLocation> DEFAULT_SOURCES = List.of(DEFAULT_SOURCE);

    public record Cfg(List<ResourceLocation> powers, List<String> tags, List<ResourceLocation> sources) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SingleOrList.of(IdCodecs.ID).optionalFieldOf("power", List.of()).forGetter(Cfg::powers),
            SingleOrList.of(com.mojang.serialization.Codec.STRING).optionalFieldOf("tags", List.of()).forGetter(Cfg::tags),
            SingleOrList.of(IdCodecs.ID).optionalFieldOf("source", DEFAULT_SOURCES).forGetter(Cfg::sources)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        for (ResourceLocation power : cfg.powers) {
            apply(holder, power, cfg.sources);
        }
        for (String tag : cfg.tags) {
            for (ResourceLocation power : dev.overgrown.apoli.power.ApoliPowers.withTag(tag)) {
                if (!holder.hasPower(power)) continue;
                apply(holder, power, cfg.sources);
            }
        }
    }

    private static void apply(PowerContainer holder, ResourceLocation power, List<ResourceLocation> sources) {
        for (ResourceLocation source : sources) {
            holder.suppressPower(power, source);
        }
    }
}
