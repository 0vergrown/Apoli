package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.message.MessageFilter;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ActionOnSendingMessagePower extends PowerType<ActionOnSendingMessagePower.Config> {
    public record Config(
        Optional<ResourceLocation> messageType,
        Optional<EntityAction> entityAction,
        List<MessageFilter> filters,
        int priority
    ) {}

    public record Result(boolean prevented, @Nullable String message) {
        public static final Result ALLOW = new Result(false, null);

        public static Result prevent() {
            return new Result(true, null);
        }

        public static Result modified(String message) {
            return new Result(false, message);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("message_type").forGetter(Config::messageType),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            MessageFilter.CODEC.optionalFieldOf("filter").forGetter(c -> Optional.<MessageFilter>empty()),
            MessageFilter.CODEC.listOf().optionalFieldOf("filters", List.of()).forGetter(Config::filters),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, (messageType, entityAction, filter, filters, priority) -> {
            List<MessageFilter> merged = new ArrayList<>(filters);
            filter.ifPresent(merged::add);
            return new Config(messageType, entityAction, List.copyOf(merged), priority);
        }));
    }

    public static Result fire(@Nullable Entity entity, String text, @Nullable ResourceLocation typeId) {
        if (entity == null) return Result.ALLOW;
        List<Config> configs = PowerLookup.active(entity, ApoliIds.ACTION_ON_SENDING_MESSAGE, Config.class);
        if (configs.isEmpty()) return Result.ALLOW;
        if (configs.size() > 1) {
            configs = new ArrayList<>(configs);
            configs.sort(Comparator.comparingInt(Config::priority).reversed());
        }

        String working = text;
        boolean prevented = false;
        for (int i = 0; i < configs.size(); i++) {
            Result result = process(configs.get(i), entity, text, working, typeId);
            if (result.prevented()) prevented = true;
            if (result.message() != null) working = result.message();
        }
        if (prevented) return Result.prevent();
        return working.equals(text) ? Result.ALLOW : Result.modified(working);
    }

    public record Report(boolean matched, List<String> lines) {}

    public static Report diagnose(Entity entity, String text, @Nullable ResourceLocation typeId) {
        List<String> lines = new ArrayList<>();
        List<Config> configs = PowerLookup.active(entity, ApoliIds.ACTION_ON_SENDING_MESSAGE, Config.class);
        if (configs.isEmpty()) {
            lines.add("[Apoli] " + entity.getName().getString()
                + " has no active apoli:action_on_sending_message power (missing, suppressed, or its condition failed).");
            return new Report(false, lines);
        }
        lines.add("[Apoli] " + configs.size() + " active action_on_sending_message power(s); testing \"" + text + "\".");

        boolean matched = false;
        boolean prevented = false;
        String working = text;
        for (Config config : configs) {
            if (config.messageType().isPresent() && !config.messageType().get().equals(typeId)) {
                lines.add("  - skipped: message_type " + config.messageType().get() + " != " + typeId);
                continue;
            }
            if (config.filters().isEmpty()) {
                lines.add("  - no filters, so every message fires entity_action");
                matched = true;
                continue;
            }
            for (MessageFilter filter : config.filters()) {
                boolean hit = filter.matches(text);
                lines.add("  - " + (hit ? "MATCH" : "no match") + ": \"" + filter.filter()
                    + "\" (" + filter.matchMode().getSerializedName()
                    + (filter.inverted() ? ", inverted" : "") + ")");
            }
            List<MessageFilter> selected = select(config.filters(), text);
            if (selected == null) {
                lines.add("  => inert: an inverted filter matched, or no plain filter did");
                continue;
            }
            matched = true;
            lines.add("  => fires entity_action once (" + selected.size() + " filter(s) applied)");
            for (MessageFilter filter : selected) {
                if (filter.hasReplacement()) working = filter.applyReplacement(working);
                if (filter.prevent()) prevented = true;
            }
        }
        if (prevented) {
            lines.add("[Apoli] Result: message would be prevented.");
        } else if (!working.equals(text)) {
            lines.add("[Apoli] Result: message would be rewritten to \"" + working + "\".");
        } else {
            lines.add("[Apoli] Result: message would be sent unchanged.");
        }
        lines.add("[Apoli] (dry run — no entity_action was executed)");
        return new Report(matched, lines);
    }

    public static Result process(Config config, Entity entity, String text, @Nullable ResourceLocation typeId) {
        return process(config, entity, text, text, typeId);
    }

    public static Result process(Config config, Entity entity, String text, String working,
                                 @Nullable ResourceLocation typeId) {
        if (config.messageType().isPresent() && !config.messageType().get().equals(typeId)) {
            return Result.ALLOW;
        }
        EntityCtx ctx = new EntityCtx(entity, entity.level());
        if (config.filters().isEmpty()) {
            config.entityAction().ifPresent(action -> action.run(ctx));
            return Result.ALLOW;
        }

        List<MessageFilter> matched = select(config.filters(), text);
        if (matched == null) {
            return Result.ALLOW;
        }

        for (MessageFilter filter : matched) {
            filter.beforeAction().ifPresent(action -> action.run(ctx));
        }
        config.entityAction().ifPresent(action -> action.run(ctx));

        String current = working;
        boolean prevented = false;
        for (MessageFilter filter : matched) {
            if (filter.hasReplacement()) {
                current = filter.applyReplacement(current);
            }
            filter.afterAction().ifPresent(action -> action.run(ctx));
            if (filter.prevent()) {
                prevented = true;
            }
        }
        if (prevented) {
            return Result.prevent();
        }
        return current.equals(working) ? Result.ALLOW : Result.modified(current);
    }

    public static @Nullable List<MessageFilter> select(List<MessageFilter> filters, String text) {
        List<MessageFilter> matched = null;
        boolean hasPositive = false;
        for (int i = 0; i < filters.size(); i++) {
            MessageFilter filter = filters.get(i);
            boolean hit = filter.matches(text);
            if (filter.inverted()) {
                if (hit) return null;
                continue;
            }
            hasPositive = true;
            if (hit) {
                if (matched == null) matched = new ArrayList<>(2);
                matched.add(filter);
            }
        }
        if (hasPositive) return matched;
        return List.of();
    }
}
