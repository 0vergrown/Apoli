package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.action.builtin.entity.SuppressPowerAction;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerKeys;
import dev.overgrown.apoli.power.PowerSources;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ApoliPowerCommand {

    private ApoliPowerCommand() {}

    private static final SuggestionProvider<CommandSourceStack> LOADED_POWERS =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> KNOWN_SOURCES =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(PowerSources.knownSources(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:power")
            .requires(ApoliPermissions.require("apoli.command.power", 2));

        root.then(Commands.literal("grant")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(LOADED_POWERS)
                    .executes(ctx -> grant(ctx, null))
                    .then(Commands.argument("source", ResourceLocationArgument.id())
                        .suggests(KNOWN_SOURCES)
                        .executes(ctx -> grant(ctx, ResourceLocationArgument.getId(ctx, "source")))))));

        root.then(revokeTree("revoke"));
        root.then(revokeTree("remove"));

        root.then(Commands.literal("grantall")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("source", ResourceLocationArgument.id())
                    .suggests(KNOWN_SOURCES)
                    .executes(ApoliPowerCommand::grantAll))));

        root.then(revokeAllTree("revokeall"));
        root.then(revokeAllTree("removeall"));

        root.then(Commands.literal("has")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ApoliPowerCommand::listAll)
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .executes(ApoliPowerCommand::has))));

        root.then(Commands.literal("suppress")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.literal("power")
                    .then(Commands.argument("power", ResourceLocationArgument.id())
                        .suggests(ApoliPowerCommand::suggestHeldPowers)
                        .executes(ctx -> suppressPower(ctx, true, null))
                        .then(Commands.argument("source", ResourceLocationArgument.id())
                            .executes(ctx -> suppressPower(ctx, true, ResourceLocationArgument.getId(ctx, "source"))))))
                .then(Commands.literal("key")
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(ApoliPowerCommand::suggestHeldKeys)
                        .executes(ctx -> suppressKey(ctx, true, null))
                        .then(Commands.argument("source", ResourceLocationArgument.id())
                            .executes(ctx -> suppressKey(ctx, true, ResourceLocationArgument.getId(ctx, "source"))))))));

        root.then(Commands.literal("unsuppress")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.literal("all")
                    .executes(ApoliPowerCommand::unsuppressAll))
                .then(Commands.literal("power")
                    .then(Commands.argument("power", ResourceLocationArgument.id())
                        .suggests(ApoliPowerCommand::suggestSuppressedPowers)
                        .executes(ctx -> suppressPower(ctx, false, null))
                        .then(Commands.argument("source", ResourceLocationArgument.id())
                            .executes(ctx -> suppressPower(ctx, false, ResourceLocationArgument.getId(ctx, "source"))))))
                .then(Commands.literal("key")
                    .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(ApoliPowerCommand::suggestHeldKeys)
                        .executes(ctx -> suppressKey(ctx, false, null))
                        .then(Commands.argument("source", ResourceLocationArgument.id())
                            .executes(ctx -> suppressKey(ctx, false, ResourceLocationArgument.getId(ctx, "source"))))))));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);

        dispatcher.register(Commands.literal("power")
            .requires(ApoliPermissions.require("apoli.command.power", 2))
            .redirect(node));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> revokeTree(String literal) {
        return Commands.literal(literal)
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .executes(ApoliPowerCommand::revoke)
                    .then(Commands.literal("from")
                        .then(Commands.argument("source", ResourceLocationArgument.id())
                            .suggests(ApoliPowerCommand::suggestSourcesOfPower)
                            .executes(ApoliPowerCommand::revokeFromSource)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> revokeAllTree(String literal) {
        return Commands.literal(literal)
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("source", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestRevokableSources)
                    .executes(ApoliPowerCommand::revokeAll)));
    }

    private static int grant(CommandContext<CommandSourceStack> ctx, ResourceLocation explicitSource)
        throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        if (ApoliPowers.get(power) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown power: " + power
                + " (no JSON found at data/" + power.getNamespace() + "/powers/" + power.getPath() + ".json)"));
            return 0;
        }
        ResourceLocation source = explicitSource != null ? explicitSource : power;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainerAttachment.getOrCreate(e);
            if (c != null && c.addPower(power, source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Granted " + power + " to " + entities(result) + "."), true);
        return affected;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.removePowerCompletely(power)) affected++;
        }
        final int result = affected;
        if (result == 0) {
            ctx.getSource().sendFailure(Component.literal("No target had " + power + "."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + power
            + " (and any sub-powers) from " + entities(result) + "."), true);
        return affected;
    }

    private static int revokeFromSource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        ResourceLocation source = ResourceLocationArgument.getId(ctx, "source");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.removePower(power, source)) affected++;
        }
        final int result = affected;
        if (result == 0) {
            ctx.getSource().sendFailure(Component.literal("No target had " + power + " from source " + source + "."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + power + " (source " + source
            + ") from " + entities(result) + "."), true);
        return affected;
    }

    private static int grantAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation source = ResourceLocationArgument.getId(ctx, "source");
        Set<ResourceLocation> powers = PowerSources.powersOf(source);
        if (powers == null || powers.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("'" + source
                + "' provides no powers. Use an origin, a skill tree or an apoli:multiple power id."));
            return 0;
        }
        int granted = 0;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainerAttachment.getOrCreate(e);
            if (c == null) continue;
            for (ResourceLocation id : powers) {
                if (c.addPower(id, source)) granted++;
            }
            affected++;
        }
        final int ents = affected;
        final int pows = granted;
        final int available = powers.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Granted " + available + " power(s) from " + source
            + " (" + pows + " new) to " + entities(ents) + "."), true);
        return affected;
    }

    private static int revokeAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation source = ResourceLocationArgument.getId(ctx, "source");
        Set<ResourceLocation> provided = PowerSources.powersOf(source);
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            boolean any = c.removeAllFromSource(source);
            if (provided != null) {
                for (ResourceLocation id : provided) {
                    if (c.removePowerCompletely(id)) any = true;
                }
            }
            if (any) affected++;
        }
        final int result = affected;
        if (result == 0) {
            ctx.getSource().sendFailure(Component.literal("No target had any power from " + source + "."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked all powers from " + source
            + " on " + entities(result) + "."), true);
        return affected;
    }

    private static int has(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int matches = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            boolean held = c != null && c.hasPower(power);
            if (held) matches++;
            boolean suppressed = held && c.isSuppressed(power);
            ctx.getSource().sendSuccess(() -> Component.literal(e.getName().getString() + ": ")
                .append(Component.literal(String.valueOf(held))
                    .withStyle(held ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(suppressed ? Component.literal(" (suppressed)").withStyle(ChatFormatting.GRAY)
                    : Component.empty()), false);
        }
        return matches;
    }

    private static int listAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int total = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            Set<ResourceLocation> powers = c == null ? Set.of() : c.allPowers();
            List<ResourceLocation> sorted = new ArrayList<>(powers);
            sorted.sort(ResourceLocation::compareTo);
            ctx.getSource().sendSuccess(() -> Component.literal(e.getName().getString() + " — "
                + sorted.size() + " power(s):").withStyle(ChatFormatting.GOLD), false);
            for (ResourceLocation id : sorted) {
                boolean sub = ApoliPowers.isSubPower(id);
                boolean suppressed = c.isSuppressed(id);
                Component line = Component.literal("  " + id)
                    .withStyle(suppressed ? ChatFormatting.DARK_GRAY : sub ? ChatFormatting.GRAY : ChatFormatting.WHITE)
                    .copy()
                    .append(Component.literal(" [" + joinIds(c.sourcesOf(id)) + "]").withStyle(ChatFormatting.DARK_GRAY))
                    .append(suppressed ? Component.literal(" suppressed").withStyle(ChatFormatting.RED)
                        : Component.empty());
                ctx.getSource().sendSuccess(() -> line, false);
            }
            total += sorted.size();
        }
        return total;
    }

    private static int suppressPower(CommandContext<CommandSourceStack> ctx, boolean suppress,
                                     ResourceLocation explicitSource) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        ResourceLocation source = explicitSource != null ? explicitSource : SuppressPowerAction.DEFAULT_SOURCE;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            if (suppress ? c.suppressPower(power, source) : c.unsuppressPower(power, source)) affected++;
        }
        return report(ctx, affected, (suppress ? "Suppressed " : "Unsuppressed ") + power
            + " (source " + source + ")", suppress ? "already suppressed" : "was not suppressed");
    }

    private static int suppressKey(CommandContext<CommandSourceStack> ctx, boolean suppress,
                                   ResourceLocation explicitSource) throws CommandSyntaxException {
        String key = StringArgumentType.getString(ctx, "key");
        ResourceLocation source = explicitSource != null ? explicitSource : SuppressPowerAction.DEFAULT_SOURCE;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            for (ResourceLocation power : PowerKeys.heldPowersUsingKey(c, key)) {
                if (suppress ? c.suppressPower(power, source) : c.unsuppressPower(power, source)) affected++;
            }
        }
        return report(ctx, affected, (suppress ? "Suppressed " : "Unsuppressed ") + affected + " power(s) bound to "
            + key + " (source " + source + ")", "no held power uses that key");
    }

    private static int unsuppressAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            for (ResourceLocation power : c.directlySuppressedPowers()) {
                for (ResourceLocation source : c.suppressionSourcesOf(power)) {
                    if (c.unsuppressPower(power, source)) affected++;
                }
            }
        }
        return report(ctx, affected, "Cleared " + affected + " suppression(s)", "nothing was suppressed");
    }

    private static int report(CommandContext<CommandSourceStack> ctx, int affected, String success, String noneReason) {
        if (affected == 0) {
            ctx.getSource().sendFailure(Component.literal("No change (" + noneReason + ")."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(success + "."), true);
        return affected;
    }

    private static CompletableFuture<Suggestions> suggestHeldPowers(CommandContext<CommandSourceStack> ctx,
                                                                    SuggestionsBuilder builder) {
        Set<ResourceLocation> held = new LinkedHashSet<>();
        try {
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) held.addAll(c.allPowers());
            }
        } catch (Exception ignored) {}
        if (held.isEmpty()) return SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);
        return SharedSuggestionProvider.suggestResource(held, builder);
    }

    private static CompletableFuture<Suggestions> suggestSourcesOfPower(CommandContext<CommandSourceStack> ctx,
                                                                        SuggestionsBuilder builder) {
        Set<ResourceLocation> sources = new LinkedHashSet<>();
        try {
            ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) sources.addAll(c.sourcesOf(power));
            }
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(sources, builder);
    }

    private static CompletableFuture<Suggestions> suggestRevokableSources(CommandContext<CommandSourceStack> ctx,
                                                                          SuggestionsBuilder builder) {
        Set<ResourceLocation> sources = new LinkedHashSet<>();
        try {
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) sources.addAll(c.allSources());
            }
        } catch (Exception ignored) {}
        sources.addAll(PowerSources.knownSources());
        return SharedSuggestionProvider.suggestResource(sources, builder);
    }

    private static CompletableFuture<Suggestions> suggestSuppressedPowers(CommandContext<CommandSourceStack> ctx,
                                                                          SuggestionsBuilder builder) {
        Set<ResourceLocation> suppressed = new LinkedHashSet<>();
        try {
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) suppressed.addAll(c.directlySuppressedPowers());
            }
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(suppressed, builder);
    }

    private static CompletableFuture<Suggestions> suggestHeldKeys(CommandContext<CommandSourceStack> ctx,
                                                                  SuggestionsBuilder builder) {
        Set<String> keys = new LinkedHashSet<>();
        try {
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) keys.addAll(PowerKeys.keysHeldBy(c));
            }
        } catch (Exception ignored) {}
        if (keys.isEmpty()) keys.addAll(PowerKeys.knownKeys());
        List<String> quoted = new ArrayList<>(keys.size());
        for (String key : keys) quoted.add('"' + key + '"');
        return SharedSuggestionProvider.suggest(quoted, builder);
    }

    private static String entities(int count) {
        return count + " entit" + (count == 1 ? "y" : "ies");
    }

    private static String joinIds(Collection<ResourceLocation> ids) {
        if (ids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ResourceLocation id : ids) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(id);
        }
        return sb.toString();
    }
}
