package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ModelAnimation(List<Entry> entries) {

    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    public enum LoopMode {
        ONCE,
        HOLD,
        LOOP;

        public static final Codec<LoopMode> CODEC = Codec.either(Codec.BOOL, Codec.STRING).comapFlatMap(
            either -> either.map(
                flag -> DataResult.success(flag ? LOOP : ONCE),
                LoopMode::byName),
            mode -> mode == HOLD ? Either.right("hold_on_last_frame") : Either.left(mode == LOOP));

        private static DataResult<LoopMode> byName(String raw) {
            return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
                case "true", "loop" -> DataResult.success(LOOP);
                case "false", "once", "play_once" -> DataResult.success(ONCE);
                case "hold", "hold_on_last_frame" -> DataResult.success(HOLD);
                default -> DataResult.error(() -> "'" + raw + "' is not a loop mode. Use true, false, or "
                    + "\"hold_on_last_frame\" to freeze on the final keyframe.");
            };
        }
    }

    public static final Codec<ModelAnimation> CODEC = Codec.either(Entry.CODEC, Entry.LIST_CODEC)
        .xmap(
            either -> either.map(e -> new ModelAnimation(List.of(e)), ModelAnimation::new),
            animation -> animation.entries.size() == 1
                ? Either.left(animation.entries.get(0))
                : Either.right(animation.entries)
        );

    @Nullable
    public Entry select(Entity entity) {
        EntityCtx ctx = null;
        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            if (entry.condition.isPresent()) {
                if (ctx == null) ctx = new EntityCtx(entity, entity.level());
                if (!passes(entry, ctx)) continue;
            }
            return entry;
        }
        return null;
    }

    private static boolean passes(Entry entry, EntityCtx ctx) {
        try {
            return entry.condition.get().test(ctx);
        } catch (Throwable t) {
            if (WARNED.add(entry.animation)) {
                Apoli.LOGGER.warn("[Apoli] A condition on an 'animations' entry for '{}' threw while the model was being drawn, "
                    + "so that entry is being skipped. Conditions that need the server — predicate, command, scoreboard, "
                    + "advancement, stat — cannot be evaluated in a render layer.", entry.animation, t);
            }
            return false;
        }
    }

    public record Entry(
        ResourceLocation animation,
        Optional<String> name,
        Optional<EntityCondition> condition,
        float speed,
        Optional<LoopMode> loop
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            IdCodecs.ID.fieldOf("animation").forGetter(Entry::animation),
            Codec.STRING.optionalFieldOf("name").forGetter(Entry::name),
            LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Entry::condition),
            Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Entry::speed),
            LoopMode.CODEC.optionalFieldOf("loop").forGetter(Entry::loop)
        ).apply(i, Entry::new));

        public static final Codec<List<Entry>> LIST_CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<Entry>, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getStream(input).map(stream -> {
                    List<Entry> out = new ArrayList<>();
                    stream.forEach(element -> CODEC.parse(ops, element)
                        .resultOrPartial(error -> Apoli.LOGGER.warn(
                            "[Apoli] Skipping one entry of an 'animations' list — {}. Every other entry still loads.", error))
                        .ifPresent(out::add));
                    return Pair.of(List.copyOf(out), input);
                });
            }

            @Override
            public <T> DataResult<T> encode(List<Entry> input, DynamicOps<T> ops, T prefix) {
                return CODEC.listOf().encode(input, ops, prefix);
            }
        };

        public int key() {
            int result = this.animation.hashCode();
            result = 31 * result + this.name.map(String::hashCode).orElse(0);
            return result;
        }
    }
}
