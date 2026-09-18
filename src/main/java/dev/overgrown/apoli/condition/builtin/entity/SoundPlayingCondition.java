package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.builtin.entity.PlaySoundAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.sound.SoundTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;

public final class SoundPlayingCondition implements ConditionType<EntityCtx, SoundPlayingCondition.Cfg> {

    public record Cfg(ResourceLocation sound, Optional<PlaySoundAction.Category> category,
                      int duration, double range) {
        public Cfg {
            SoundTracker.watch(sound);
        }

        public SoundSource source() {
            return category.map(PlaySoundAction.Category::vanilla).orElse(null);
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("sound").forGetter(Cfg::sound),
            PlaySoundAction.Category.CODEC.optionalFieldOf("category").forGetter(Cfg::category),
            Codec.INT.optionalFieldOf("duration", 20).forGetter(Cfg::duration),
            Codec.DOUBLE.optionalFieldOf("range", (double) SoundTracker.UNKNOWN_RANGE).forGetter(Cfg::range)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!SoundTracker.watching()) return false;
        return SoundTracker.playing(ctx.raw(), cfg.sound, cfg.source(), Math.max(1, cfg.duration), cfg.range);
    }
}
