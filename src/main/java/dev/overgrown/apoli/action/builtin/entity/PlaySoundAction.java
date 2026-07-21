package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class PlaySoundAction implements ActionType<EntityCtx, PlaySoundAction.Cfg> {
    public enum Category implements StringRepresentable {
        MASTER("master", SoundSource.MASTER),
        MUSIC("music", SoundSource.MUSIC),
        RECORD("record", SoundSource.RECORDS),
        WEATHER("weather", SoundSource.WEATHER),
        BLOCK("block", SoundSource.BLOCKS),
        HOSTILE("hostile", SoundSource.HOSTILE),
        NEUTRAL("neutral", SoundSource.NEUTRAL),
        PLAYERS("players", SoundSource.PLAYERS),
        AMBIENT("ambient", SoundSource.AMBIENT),
        VOICE("voice", SoundSource.VOICE);

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);
        private final String name;
        private final SoundSource vanilla;
        Category(String n, SoundSource v) {
            this.name = n; this.vanilla = v;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public SoundSource vanilla() {
            return vanilla;
        }
    }

    public record Cfg(ResourceLocation sound, Optional<Category> category, float volume, float pitch) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("sound").forGetter(Cfg::sound),
            Category.CODEC.optionalFieldOf("category").forGetter(Cfg::category),
            Codec.FLOAT.optionalFieldOf("volume", 1.0f).forGetter(Cfg::volume),
            Codec.FLOAT.optionalFieldOf("pitch", 1.0f).forGetter(Cfg::pitch)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(cfg.sound);
        if (event == null) return;
        Entity e = ctx.raw();
        SoundSource source = cfg.category.map(Category::vanilla).orElse(e.getSoundSource());
        ctx.level().playSound(null, e.getX(), e.getY(), e.getZ(), event, source, cfg.volume, cfg.pitch);
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
