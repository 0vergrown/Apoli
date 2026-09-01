package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.overgrown.apoli.data.ScrollDirection;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.power.PowerKeys;
import dev.overgrown.apoli.power.builtin.ActionOnScrollWheelPower;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ApoliKeyCommand {

    private static final List<String> VANILLA_KEYS = List.of(
        "key.attack", "key.use", "key.jump", "key.sneak", "key.sprint",
        "key.forward", "key.back", "key.left", "key.right",
        "key.drop", "key.inventory", "key.swapOffhand", "key.pickItem",
        "key.chat", "key.command", "key.playerlist", "key.togglePerspective", "key.smoothCamera",
        "key.hotbar.1", "key.hotbar.2", "key.hotbar.3", "key.hotbar.4", "key.hotbar.5",
        "key.hotbar.6", "key.hotbar.7", "key.hotbar.8", "key.hotbar.9");

    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = (ctx, builder) -> {
        Set<String> keys = new LinkedHashSet<>(PowerKeys.knownKeys());
        keys.addAll(VANILLA_KEYS);
        return SharedSuggestionProvider.suggest(keys, builder);
    };

    private ApoliKeyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("apoli:key")
            .requires(ApoliPermissions.require("apoli.command.key", 2))
            .then(Commands.literal("press")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(KEY_SUGGESTIONS)
                        .executes(ctx -> press(ctx, 1))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                            .executes(ctx -> press(ctx, IntegerArgumentType.getInteger(ctx, "duration")))))))
            .then(Commands.literal("release")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(KEY_SUGGESTIONS)
                        .executes(ApoliKeyCommand::release))))
            .then(Commands.literal("clear")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ApoliKeyCommand::clear)))
            .then(Commands.literal("list")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ApoliKeyCommand::list)))
            .then(Commands.literal("scroll")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.literal("up")
                        .executes(ctx -> scroll(ctx, ScrollDirection.UP, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                            .executes(ctx -> scroll(ctx, ScrollDirection.UP,
                                IntegerArgumentType.getInteger(ctx, "count")))))
                    .then(Commands.literal("down")
                        .executes(ctx -> scroll(ctx, ScrollDirection.DOWN, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                            .executes(ctx -> scroll(ctx, ScrollDirection.DOWN,
                                IntegerArgumentType.getInteger(ctx, "count"))))))));
    }

    private static int press(CommandContext<CommandSourceStack> ctx, int duration) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        String key = StringArgumentType.getString(ctx, "key");
        for (Entity target : targets) KeyDispatch.force(target, key, duration, false);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Held " + key + " for " + duration + " tick(s) on " + count + " entit" + (count == 1 ? "y" : "ies")), true);
        return count;
    }

    private static int release(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        String key = StringArgumentType.getString(ctx, "key");
        for (Entity target : targets) KeyDispatch.force(target, key, 1, true);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Released " + key + " on " + count + " entit" + (count == 1 ? "y" : "ies")), true);
        return count;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int cleared = 0;
        for (Entity target : targets) {
            Set<String> forced = new LinkedHashSet<>(HeldKeys.forcedKeys(target.getUUID()));
            if (forced.isEmpty()) continue;
            for (String key : forced) KeyDispatch.force(target, key, 1, true);
            cleared++;
        }
        int total = cleared;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared forced keys on " + total + " entit"
            + (total == 1 ? "y" : "ies")), true);
        return cleared;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int found = 0;
        for (Entity target : targets) {
            Set<String> held = HeldKeys.serverHeldSet(target.getUUID());
            Set<String> forced = HeldKeys.forcedKeys(target.getUUID());
            if (held.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal(name(target) + ": no keys held")
                    .withStyle(ChatFormatting.GRAY), false);
                continue;
            }
            found++;
            List<String> sorted = new ArrayList<>(held);
            java.util.Collections.sort(sorted);
            StringBuilder line = new StringBuilder(name(target)).append(": ");
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0) line.append(", ");
                line.append(sorted.get(i));
                if (forced.contains(sorted.get(i))) line.append(" (forced)");
            }
            String text = line.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        }
        return found;
    }

    private static int scroll(CommandContext<CommandSourceStack> ctx, ScrollDirection direction, int count)
            throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int fired = 0;
        for (Entity target : targets) fired += ActionOnScrollWheelPower.scroll(target, direction, count);
        int total = fired;
        ctx.getSource().sendSuccess(() -> Component.literal("Scrolled " + direction.getSerializedName()
            + " ×" + count + " — " + total + " power(s) fired"), true);
        return fired;
    }

    private static String name(Entity entity) {
        return entity instanceof Player
            ? entity.getName().getString()
            : entity.getName().getString() + "#" + entity.getId();
    }
}
