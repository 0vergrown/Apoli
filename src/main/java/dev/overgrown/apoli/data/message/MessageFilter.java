package dev.overgrown.apoli.data.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MessageFilter {
    private final String filter;
    private final Optional<EntityAction> beforeAction;
    private final Optional<EntityAction> afterAction;
    private final boolean prevent;
    private final Pattern compiled;

    public MessageFilter(String filter, Optional<EntityAction> beforeAction, Optional<EntityAction> afterAction, boolean prevent) {
        this.filter = filter;
        this.beforeAction = beforeAction;
        this.afterAction = afterAction;
        this.prevent = prevent;
        Pattern pattern;
        try {
            pattern = Pattern.compile(TranslationKeyResolver.expandPattern(filter));
        } catch (PatternSyntaxException e) {
            pattern = Pattern.compile(Pattern.quote(filter));
        }
        this.compiled = pattern;
    }

    public static final MapCodec<MessageFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("filter").forGetter(f -> f.filter),
        EntityAction.CODEC.optionalFieldOf("before_action").forGetter(f -> f.beforeAction),
        EntityAction.CODEC.optionalFieldOf("after_action").forGetter(f -> f.afterAction),
        Codec.BOOL.optionalFieldOf("prevent", false).forGetter(f -> f.prevent)
    ).apply(i, MessageFilter::new));

    public static final Codec<MessageFilter> CODEC = MAP_CODEC.codec();

    public boolean matches(String message) {
        return compiled.matcher(message).find();
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
