package dev.overgrown.apoli.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.overgrown.apoli.mixin.command.EntitySelectorOptionsAccessor;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.skill.SkillDataAttachment;
import dev.overgrown.apoli.skill.SkillRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ApoliSelectorOptions {

    public interface Handler {
        Predicate<Entity> predicate(ResourceLocation value);

        Collection<ResourceLocation> suggestions();
    }

    private static final DynamicCommandExceptionType NOT_INSTALLED = new DynamicCommandExceptionType(
        name -> Component.literal("The '" + name + "' selector option needs the Origins mod installed."));

    private static final Map<String, Handler> EXTERNAL = new ConcurrentHashMap<>();

    private ApoliSelectorOptions() {}

    public static void register(String name, Handler handler) {
        EXTERNAL.put(name, handler);
    }

    public static void bootstrap() {
        option("power", "Entities holding an Apoli power",
            () -> new PowerHandler(false));
        option("suppressed_power", "Entities whose Apoli power is suppressed",
            () -> new PowerHandler(true));
        option("skill", "Players who purchased a skill",
            ApoliSelectorOptions::skillHandler);
        option("origin", "Players with an origin (needs Origins)",
            () -> EXTERNAL.get("origin"));
        option("origin_layer", "Players who chose an origin in a layer (needs Origins)",
            () -> EXTERNAL.get("origin_layer"));
    }

    private static void option(String name, String tooltip, Supplier<Handler> lookup) {
        EntitySelectorOptionsAccessor.apoli$register(name, parser -> handle(name, lookup, parser),
            parser -> true, Component.literal(tooltip));
    }

    private static void handle(String name, Supplier<Handler> lookup, EntitySelectorParser parser)
        throws CommandSyntaxException {
        Handler handler = lookup.get();
        parser.setSuggestions((builder, consumer) -> {
            Handler current = lookup.get();
            if (current == null) return builder.buildFuture();
            return SharedSuggestionProvider.suggestResource(current.suggestions(), builder);
        });
        boolean inverted = parser.shouldInvertValue();
        int start = parser.getReader().getCursor();
        ResourceLocation value = ResourceLocation.read(parser.getReader());
        if (handler == null) {
            parser.getReader().setCursor(start);
            throw NOT_INSTALLED.createWithContext(parser.getReader(), name);
        }
        Predicate<Entity> predicate = handler.predicate(value);
        parser.addPredicate(inverted ? predicate.negate() : predicate);
    }

    private record PowerHandler(boolean suppressed) implements Handler {
        @Override
        public Predicate<Entity> predicate(ResourceLocation value) {
            return entity -> {
                PowerContainer container = PowerContainer.of(entity);
                if (container == null || !container.hasPower(value)) return false;
                return !suppressed || container.isSuppressed(value);
            };
        }

        @Override
        public Collection<ResourceLocation> suggestions() {
            return ApoliPowers.view().keySet();
        }
    }

    private static @Nullable Handler skillHandler() {
        return new Handler() {
            @Override
            public Predicate<Entity> predicate(ResourceLocation value) {
                return entity -> entity instanceof Player player
                    && SkillDataAttachment.get(player).isPurchased(value);
            }

            @Override
            public Collection<ResourceLocation> suggestions() {
                List<ResourceLocation> ids = new java.util.ArrayList<>();
                SkillRegistry.all().forEach(skill -> ids.add(skill.id()));
                return ids;
            }
        };
    }
}
