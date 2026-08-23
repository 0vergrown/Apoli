package dev.overgrown.apoli.data.message;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MessageFilter {

    public enum MatchMode implements StringRepresentable {
        CONTAINS("contains"),
        FULL("full"),
        WORD("word"),
        STARTS_WITH("starts_with"),
        ENDS_WITH("ends_with");

        public static final Codec<MatchMode> CODEC = StringRepresentable.fromEnum(MatchMode::values);
        private final String name;

        MatchMode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final String filter;
    private final MatchMode matchMode;
    private final boolean literal;
    private final boolean caseInsensitive;
    private final boolean inverted;
    private final Optional<String> replacement;
    private final Optional<EntityAction> beforeAction;
    private final Optional<EntityAction> afterAction;
    private final boolean prevent;

    private final boolean deferred;
    private volatile Pattern compiled;

    public MessageFilter(String filter, MatchMode matchMode, boolean literal, boolean caseInsensitive,
                         boolean inverted, Optional<String> replacement, Optional<EntityAction> beforeAction,
                         Optional<EntityAction> afterAction, boolean prevent) {
        this.filter = filter;
        this.matchMode = matchMode;
        this.literal = literal;
        this.caseInsensitive = caseInsensitive;
        this.inverted = inverted;
        this.replacement = replacement;
        this.beforeAction = beforeAction;
        this.afterAction = afterAction;
        this.prevent = prevent;

        this.deferred = !literal && TranslationKeyResolver.hasPlaceholder(filter);
        if (deferred) {
            TranslationKeyResolver.require(filter);
        } else {
            this.compiled = compile(filter, matchMode, literal, caseInsensitive, false);
        }
    }

    private Pattern pattern() {
        Pattern current = compiled;
        if (current != null) return current;
        synchronized (this) {
            if (compiled == null) {
                compiled = compile(filter, matchMode, literal, caseInsensitive, true);
            }
            return compiled;
        }
    }

    private static Pattern compile(String filter, MatchMode matchMode, boolean literal,
                                   boolean caseInsensitive, boolean expandPlaceholders) {
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        if (literal) {
            return Pattern.compile(decorate(Pattern.quote(filter), matchMode), flags);
        }
        String expanded = expandPlaceholders ? TranslationKeyResolver.expandPattern(filter) : filter;
        try {
            return Pattern.compile(decorate(expanded, matchMode), flags);
        } catch (PatternSyntaxException e) {
            Apoli.LOGGER.warn("[Apoli] Message filter \"{}\" is not valid regex ({}); matching it as literal text. "
                + "Set \"literal\": true to silence this.", filter, e.getDescription());
            return Pattern.compile(decorate(Pattern.quote(expanded), matchMode), flags);
        }
    }

    private static String decorate(String pattern, MatchMode matchMode) {
        return switch (matchMode) {
            case CONTAINS -> pattern;
            case FULL -> "^(?:" + pattern + ")$";
            case WORD -> "\\b(?:" + pattern + ")\\b";
            case STARTS_WITH -> "^(?:" + pattern + ")";
            case ENDS_WITH -> "(?:" + pattern + ")$";
        };
    }

    private static final MapCodec<MessageFilter> RECORD_MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("filter").forGetter(f -> f.filter),
        MatchMode.CODEC.optionalFieldOf("match_mode", MatchMode.CONTAINS).forGetter(f -> f.matchMode),
        Codec.BOOL.optionalFieldOf("literal", false).forGetter(f -> f.literal),
        Codec.BOOL.optionalFieldOf("case_insensitive", false).forGetter(f -> f.caseInsensitive),
        Codec.BOOL.optionalFieldOf("inverted", false).forGetter(f -> f.inverted),
        Codec.STRING.optionalFieldOf("replacement").forGetter(f -> f.replacement),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("before_action", EntityAction.CODEC).forGetter(f -> f.beforeAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("after_action", EntityAction.CODEC).forGetter(f -> f.afterAction),
        Codec.BOOL.optionalFieldOf("prevent", false).forGetter(f -> f.prevent)
    ).apply(i, MessageFilter::new));

    public static final MapCodec<MessageFilter> MAP_CODEC = RECORD_MAP_CODEC;

    private static final Codec<MessageFilter> STRING_CODEC = Codec.STRING.xmap(
        MessageFilter::of, f -> f.filter);

    public static final Codec<MessageFilter> CODEC = Codec.either(STRING_CODEC, RECORD_MAP_CODEC.codec()).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        Either::right);

    public static MessageFilter of(String filter) {
        return new MessageFilter(filter, MatchMode.CONTAINS, false, false, false,
            Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public boolean matches(String message) {
        Matcher matcher = pattern().matcher(message);
        return matchMode == MatchMode.FULL ? matcher.matches() : matcher.find();
    }

    public boolean inverted() {
        return inverted;
    }

    public String applyReplacement(String message) {
        if (replacement.isEmpty()) return message;
        try {
            return pattern().matcher(message).replaceAll(replacement.get());
        } catch (RuntimeException e) {
            Apoli.LOGGER.warn("[Apoli] Message filter replacement \"{}\" for pattern \"{}\" failed: {}",
                replacement.get(), filter, e.getMessage());
            return message;
        }
    }

    public boolean hasReplacement() {
        return replacement.isPresent();
    }

    public String filter() {
        return filter;
    }

    public MatchMode matchMode() {
        return matchMode;
    }

    public Optional<EntityAction> beforeAction() {
        return beforeAction;
    }

    public Optional<EntityAction> afterAction() {
        return afterAction;
    }

    public boolean prevent() {
        return prevent;
    }
}
