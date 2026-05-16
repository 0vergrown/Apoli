package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.StatusEffectInstanceCodec;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

public final class ApplyEffectAction implements ActionType<EntityCtx, ApplyEffectAction.Cfg> {
    public record Cfg(List<MobEffectInstance> effects) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.either(StatusEffectInstanceCodec.CODEC, Codec.list(StatusEffectInstanceCodec.CODEC))
                .xmap(
                    either -> either.map(List::of, list -> list),
                    list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
                )
                .fieldOf("effect")
                .forGetter(Cfg::effects)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        for (MobEffectInstance effect : cfg.effects) {
            ctx.entity().addEffect(new MobEffectInstance(effect));
        }
    }
}