package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record HudRender(List<Entry> entries) {
    public static final ResourceLocation DEFAULT_SPRITE_LOCATION = new ResourceLocation("apoli", "textures/gui/resource_bar.png");

    public static final Entry HIDDEN = new Entry(false, DEFAULT_SPRITE_LOCATION, 0, 0, Optional.empty(), false, Optional.empty(), Optional.empty());
    public static final HudRender DONT_RENDER = new HudRender(List.of(HIDDEN));

    public static final Codec<HudRender> CODEC = Codec.either(Entry.CODEC, Codec.list(Entry.CODEC))
        .xmap(
            either -> either.map(e -> new HudRender(List.of(e)), HudRender::new),
            hr -> hr.entries.size() == 1 ? Either.left(hr.entries.get(0)) : Either.right(hr.entries)
        );

    public @Nullable Entry selectEntry(EntityCtx ctx) {
        for (Entry e : entries) {
            if (!e.shouldRender) continue;
            if (e.condition.isPresent() && !e.condition.get().test(ctx)) continue;
            return e;
        }
        return null;
    }

    public Optional<Entry> select(EntityCtx ctx) {
        return Optional.ofNullable(selectEntry(ctx));
    }

    public boolean shouldRender() {
        for (Entry e : entries) if (e.shouldRender) return true;
        return false;
    }

    public record Entry(
        boolean shouldRender,
        ResourceLocation spriteLocation,
        int barIndex,
        int iconIndex,
        Optional<EntityCondition> condition,
        boolean inverted,
        Optional<Integer> order,
        Optional<Expression> max
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("should_render", true).forGetter(Entry::shouldRender),
            IdCodecs.ID.optionalFieldOf("sprite_location", DEFAULT_SPRITE_LOCATION).forGetter(Entry::spriteLocation),
            Codec.INT.optionalFieldOf("bar_index", 0).forGetter(Entry::barIndex),
            Codec.INT.optionalFieldOf("icon_index", 0).forGetter(Entry::iconIndex),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Entry::condition),
            Codec.BOOL.optionalFieldOf("inverted", false).forGetter(Entry::inverted),
            Codec.INT.optionalFieldOf("order").forGetter(Entry::order),
            Expression.INT_OR_EXPR.optionalFieldOf("max").forGetter(Entry::max)
        ).apply(i, Entry::new));
    }
}
