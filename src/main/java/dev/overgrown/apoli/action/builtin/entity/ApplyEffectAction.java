package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EffectSpec;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

public final class ApplyEffectAction implements ActionType<EntityCtx, ApplyEffectAction.Cfg> {
    public record Cfg(List<EffectSpec> effects) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(
            RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
                EffectSpec.LIST_OR_SINGLE.fieldOf("effect").forGetter(Cfg::effects)
            ).apply(i, Cfg::new)),
            Map.of("effects", "effect")
        );
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) return;
        for (EffectSpec spec : cfg.effects) {
            living.addEffect(spec.resolve(living));
        }
    }
}
