package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class GrantAllPowersAction implements ActionType<EntityCtx, GrantAllPowersAction.Cfg> {
    public static final ResourceLocation DEFAULT_SOURCE = Apoli.id("grant_all_powers");

    public record Cfg(ResourceLocation source, Optional<String> namespace, boolean includeHidden) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("source", DEFAULT_SOURCE).forGetter(Cfg::source),
            Codec.STRING.optionalFieldOf("namespace").forGetter(Cfg::namespace),
            Codec.BOOL.optionalFieldOf("include_hidden", true).forGetter(Cfg::includeHidden)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        PowerContainer holder = PowerContainer.of(ctx.entity());
        if (holder == null) return;
        for (ResourceLocation id : ApoliPowers.grantableIds()) {
            if (cfg.namespace.isPresent() && !cfg.namespace.get().equals(id.getNamespace())) continue;
            if (!cfg.includeHidden) {
                Power p = ApoliPowers.get(id);
                if (p != null && p.hidden()) continue;
            }
            holder.addPower(id, cfg.source);
        }
    }
}
