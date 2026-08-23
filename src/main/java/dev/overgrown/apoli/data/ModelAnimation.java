package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record ModelAnimation(List<Entry> entries) {

    public static final Codec<ModelAnimation> CODEC = Codec.either(Entry.CODEC, Codec.list(Entry.CODEC))
        .xmap(
            either -> either.map(e -> new ModelAnimation(List.of(e)), ModelAnimation::new),
            animation -> animation.entries.size() == 1
                ? Either.left(animation.entries.get(0))
                : Either.right(animation.entries)
        );

    @Nullable
    public Entry select(EntityCtx ctx) {
        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            if (entry.condition.isPresent() && !entry.condition.get().test(ctx)) continue;
            return entry;
        }
        return null;
    }

    public record Entry(
        ResourceLocation animation,
        Optional<String> name,
        Optional<EntityCondition> condition,
        float speed,
        Optional<Boolean> loop
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            IdCodecs.ID.fieldOf("animation").forGetter(Entry::animation),
            Codec.STRING.optionalFieldOf("name").forGetter(Entry::name),
            LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Entry::condition),
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Entry::speed),
            Codec.BOOL.optionalFieldOf("loop").forGetter(Entry::loop)
        ).apply(i, Entry::new));

        public int key() {
            int result = this.animation.hashCode();
            result = 31 * result + this.name.map(String::hashCode).orElse(0);
            return result;
        }
    }
}
