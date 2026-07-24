package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TextBar;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.network.payload.TextDisplayS2C;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TextAction implements ActionType<EntityCtx, TextAction.Cfg> {
    public record Segment(Component text, Optional<EntityCondition> condition) {}

    private static final Codec<Segment> SEGMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
        TextComponent.CODEC.fieldOf("text").forGetter(Segment::text),
        EntityCondition.CODEC.optionalFieldOf("condition").forGetter(Segment::condition)
    ).apply(i, Segment::new));

    private static final Codec<List<Segment>> TEXT_CODEC = Codec.either(
        SEGMENT_CODEC.listOf(),
        TextComponent.CODEC.xmap(
            c -> new Segment(c, Optional.empty()),
            Segment::text)
    ).xmap(
        either -> either.map(java.util.function.Function.identity(), List::of),
        list -> com.mojang.datafixers.util.Either.left(list)
    );

    public record Cfg(
        TextBar bar,
        List<Segment> text,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated,
        int fadeIn,
        int duration,
        int fadeOut
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TextBar.CODEC.optionalFieldOf("bar", TextBar.ACTIONBAR).forGetter(Cfg::bar),
            TEXT_CODEC.fieldOf("text").forGetter(Cfg::text),
            Codec.BOOL.optionalFieldOf("bold", false).forGetter(Cfg::bold),
            Codec.BOOL.optionalFieldOf("italic", false).forGetter(Cfg::italic),
            Codec.BOOL.optionalFieldOf("underlined", false).forGetter(Cfg::underlined),
            Codec.BOOL.optionalFieldOf("strikethrough", false).forGetter(Cfg::strikethrough),
            Codec.BOOL.optionalFieldOf("obfuscated", false).forGetter(Cfg::obfuscated),
            Codec.INT.optionalFieldOf("fadein", 10).forGetter(Cfg::fadeIn),
            Codec.INT.optionalFieldOf("duration", 70).forGetter(Cfg::duration),
            Codec.INT.optionalFieldOf("fadeout", 20).forGetter(Cfg::fadeOut)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof ServerPlayer player)) return;
        Component composed = compose(cfg, ctx);
        sendDeduped(player, cfg, composed);
    }

    private static Component compose(Cfg cfg, EntityCtx ctx) {
        MutableComponent out = Component.empty();
        Style base = baseStyle(cfg);
        if (base != Style.EMPTY) out.setStyle(base);
        for (int i = 0; i < cfg.text.size(); i++) {
            Segment segment = cfg.text.get(i);
            if (segment.condition().isPresent() && !segment.condition().get().test(ctx)) continue;
            out.append(segment.text());
        }
        return out;
    }

    private static Style baseStyle(Cfg cfg) {
        Style style = Style.EMPTY;
        if (cfg.bold) style = style.applyFormat(ChatFormatting.BOLD);
        if (cfg.italic) style = style.applyFormat(ChatFormatting.ITALIC);
        if (cfg.underlined) style = style.applyFormat(ChatFormatting.UNDERLINE);
        if (cfg.strikethrough) style = style.applyFormat(ChatFormatting.STRIKETHROUGH);
        if (cfg.obfuscated) style = style.applyFormat(ChatFormatting.OBFUSCATED);
        return style;
    }

    private static final class BarState {
        Component lastText;
        int fadeIn, stay, fadeOut;
        long refreshAt = Long.MAX_VALUE;
    }

    private static final Map<UUID, BarState[]> STATES = new ConcurrentHashMap<>();

    private static void sendDeduped(ServerPlayer player, Cfg cfg, Component composed) {
        BarState[] states = STATES.computeIfAbsent(player.getUUID(), k -> new BarState[TextBar.VALUES.length]);
        int slot = cfg.bar.ordinal();
        BarState state = states[slot];
        if (state == null) states[slot] = state = new BarState();

        long now = player.serverLevel().getGameTime();
        boolean permanent = cfg.duration < 0;
        boolean changed = state.lastText == null
            || !composed.equals(state.lastText)
            || state.fadeIn != cfg.fadeIn || state.stay != cfg.duration || state.fadeOut != cfg.fadeOut;
        if (!changed && (permanent || now < state.refreshAt)) return;

        state.lastText = composed;
        state.fadeIn = cfg.fadeIn;
        state.stay = cfg.duration;
        state.fadeOut = cfg.fadeOut;
        state.refreshAt = permanent ? Long.MAX_VALUE : now + Math.max(1, cfg.duration / 2);
        ApoliNetwork.sendTextDisplay(player, new TextDisplayS2C(cfg.bar, composed, cfg.fadeIn, cfg.duration, cfg.fadeOut));
    }

    public static void onPlayerLeave(UUID player) {
        STATES.remove(player);
    }
}
