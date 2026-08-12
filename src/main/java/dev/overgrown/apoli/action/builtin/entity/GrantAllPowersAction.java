package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerSources;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class GrantAllPowersAction implements ActionType<EntityCtx, GrantAllPowersAction.Cfg> {
    public static final ResourceLocation DEFAULT_SOURCE = Apoli.id("grant_all_powers");

    public record Cfg(Optional<ResourceLocation> source, Optional<String> namespace, boolean includeHidden,
                      List<ResourceLocation> from) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("source").forGetter(Cfg::source),
            Codec.STRING.optionalFieldOf("namespace").forGetter(Cfg::namespace),
            Codec.BOOL.optionalFieldOf("include_hidden", true).forGetter(Cfg::includeHidden),
            SingleOrList.of(IdCodecs.ID).optionalFieldOf("from", List.of()).forGetter(Cfg::from)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        if (cfg.from.isEmpty()) {
            ResourceLocation source = cfg.source.orElse(DEFAULT_SOURCE);
            for (ResourceLocation id : ApoliPowers.grantableIds()) grant(cfg, holder, id, source);
            return;
        }
        for (ResourceLocation from : cfg.from) {
            Set<ResourceLocation> powers = PowerSources.powersOf(from);
            if (powers == null) {
                Apoli.LOGGER.warn("[Apoli] grant_all_powers: '{}' is not a known power source (origin, skill tree or apoli:multiple power).", from);
                continue;
            }
            ResourceLocation source = cfg.source.orElse(from);
            for (ResourceLocation id : powers) grant(cfg, holder, id, source);
        }
    }

    private static void grant(Cfg cfg, PowerContainer holder, ResourceLocation id, ResourceLocation source) {
        if (cfg.namespace.isPresent() && !cfg.namespace.get().equals(id.getNamespace())) return;
        if (!cfg.includeHidden) {
            Power p = ApoliPowers.get(id);
            if (p != null && p.hidden()) return;
        }
        holder.addPower(id, source);
    }
}
