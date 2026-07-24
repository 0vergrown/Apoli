package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.summon.Temporary;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class SetSummonMaxLifeAction implements ActionType<EntityCtx, SetSummonMaxLifeAction.Cfg> {
    public record Cfg(int amount, Optional<ResourceLocation> summonId) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("amount").forGetter(Cfg::amount),
            ResourceLocation.CODEC.optionalFieldOf("summon_id").forGetter(Cfg::summonId)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Temporary summon)) return;
        if (cfg.summonId.isPresent() && !cfg.summonId.get().equals(summon.getSummonId())) return;
        summon.setMaxLifeTime(cfg.amount);
    }
}
