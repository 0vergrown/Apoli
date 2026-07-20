package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.action.builtin.entity.GrantAllPowersAction;
import dev.overgrown.apoli.action.builtin.entity.SuppressPowerAction;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ApoliPowerCommand {
    private static final SuggestionProvider<CommandSourceStack> LOADED_POWERS =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);

    private ApoliPowerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:power")
            .requires(ApoliPermissions.require("apoli.command.power", 2));

        root.then(Commands.literal("grant")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(LOADED_POWERS)
                    .then(Commands.argument("source", ResourceLocationArgument.id())
                        .suggests(LOADED_POWERS)
                        .executes(ApoliPowerCommand::grant))
                    .executes(ctx -> grant(ctx, ResourceLocationArgument.getId(ctx, "power"))))));

        root.then(Commands.literal("grantall")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("source", ResourceLocationArgument.id())
                    .executes(ApoliPowerCommand::grantAll))
                .executes(ApoliPowerCommand::grantAll)));

        root.then(Commands.literal("revoke")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .then(Commands.argument("source", ResourceLocationArgument.id())
                        .suggests(ApoliPowerCommand::suggestSourcesOfPower)
                        .executes(ApoliPowerCommand::revoke))
                    .executes(ctx -> revoke(ctx, ResourceLocationArgument.getId(ctx, "power"))))));

        root.then(Commands.literal("revokeall")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("source", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestAllHeldSources)
                    .executes(ApoliPowerCommand::revokeAll))));

        root.then(Commands.literal("remove")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .executes(ApoliPowerCommand::remove))));

        root.then(Commands.literal("clear")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ApoliPowerCommand::clear)));

        root.then(Commands.literal("suppress")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .then(Commands.argument("source", ResourceLocationArgument.id())
                        .executes(ApoliPowerCommand::suppress))
                    .executes(ApoliPowerCommand::suppress))));

        root.then(Commands.literal("unsuppress")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestSuppressedPowers)
                    .then(Commands.argument("source", ResourceLocationArgument.id())
                        .executes(ApoliPowerCommand::unsuppress))
                    .executes(ApoliPowerCommand::unsuppress))));

        root.then(Commands.literal("has")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(LOADED_POWERS)
                    .executes(ApoliPowerCommand::has))));

        root.then(Commands.literal("list")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ApoliPowerCommand::list)));

        root.then(Commands.literal("sources")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(ApoliPowerCommand::suggestHeldPowers)
                    .executes(ApoliPowerCommand::sources))));

        root.then(Commands.literal("dump")
            .then(Commands.argument("power", ResourceLocationArgument.id())
                .suggests(LOADED_POWERS)
                .executes(ApoliPowerCommand::dump)));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);

        dispatcher.register(Commands.literal("power")
            .requires(ApoliPermissions.require("apoli.command.power", 2))
            .redirect(node));
    }

    private static CompletableFuture<Suggestions> suggestHeldPowers(CommandContext<CommandSourceStack> ctx,
                                                                    SuggestionsBuilder builder) {
        try {
            Set<ResourceLocation> held = new HashSet<>();
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) held.addAll(c.allPowers());
            }
            if (!held.isEmpty()) return SharedSuggestionProvider.suggestResource(held, builder);
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestSourcesOfPower(CommandContext<CommandSourceStack> ctx,
                                                                        SuggestionsBuilder builder) {
        try {
            ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
            Set<ResourceLocation> sources = new HashSet<>();
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) sources.addAll(c.sourcesOf(power));
            }
            if (!sources.isEmpty()) return SharedSuggestionProvider.suggestResource(sources, builder);
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestAllHeldSources(CommandContext<CommandSourceStack> ctx,
                                                                        SuggestionsBuilder builder) {
        try {
            Set<ResourceLocation> sources = new HashSet<>();
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) sources.addAll(c.allSources());
            }
            if (!sources.isEmpty()) return SharedSuggestionProvider.suggestResource(sources, builder);
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return grant(ctx, null);
    }

    private static int grant(CommandContext<CommandSourceStack> ctx, ResourceLocation explicitPower) throws CommandSyntaxException {
        ResourceLocation power = explicitPower != null ? explicitPower : ResourceLocationArgument.getId(ctx, "power");
        ResourceLocation source = hasArg(ctx, "source") ? ResourceLocationArgument.getId(ctx, "source") : power;
        if (ApoliPowers.get(power) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown power: " + power
                + " (no JSON found at data/" + power.getNamespace() + "/powers/" + power.getPath() + ".json)"));
            return 0;
        }
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainerAttachment.getOrCreate(e);
            if (c != null && c.addPower(power, source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Granted " + power + " to " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int grantAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation source = hasArg(ctx, "source")
            ? ResourceLocationArgument.getId(ctx, "source")
            : GrantAllPowersAction.DEFAULT_SOURCE;
        int granted = 0;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainerAttachment.getOrCreate(e);
            if (c == null) continue;
            for (ResourceLocation id : ApoliPowers.grantableIds()) {
                if (c.addPower(id, source)) granted++;
            }
            affected++;
        }
        final int ents = affected;
        final int pows = granted;
        ctx.getSource().sendSuccess(() -> Component.literal("Granted all powers (" + pows
            + " new, source " + source + ") to " + ents + " entit" + (ents == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return revoke(ctx, ResourceLocationArgument.getId(ctx, "source"));
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx, ResourceLocation source) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.removePower(power, source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + power + " (source " + source
            + ") from " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int revokeAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation source = ResourceLocationArgument.getId(ctx, "source");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.removeAllFromSource(source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked all powers from source " + source
            + " on " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.removePowerCompletely(power)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Removed " + power + " from " + result
            + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null) { c.clear(); affected++; }
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared all powers from " + result
            + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int suppress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        ResourceLocation source = hasArg(ctx, "source")
            ? ResourceLocationArgument.getId(ctx, "source")
            : SuppressPowerAction.DEFAULT_SOURCE;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.suppressPower(power, source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Suppressed " + power + " (source " + source
            + ") on " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static int unsuppress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        ResourceLocation source = hasArg(ctx, "source")
            ? ResourceLocationArgument.getId(ctx, "source")
            : SuppressPowerAction.DEFAULT_SOURCE;
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.unsuppressPower(power, source)) affected++;
        }
        final int result = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Unsuppressed " + power + " (source " + source
            + ") on " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return affected;
    }

    private static CompletableFuture<Suggestions> suggestSuppressedPowers(CommandContext<CommandSourceStack> ctx,
                                                                          SuggestionsBuilder builder) {
        try {
            Set<ResourceLocation> suppressed = new HashSet<>();
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c != null) suppressed.addAll(c.suppressedPowers());
            }
            if (!suppressed.isEmpty()) return SharedSuggestionProvider.suggestResource(suppressed, builder);
        } catch (Exception ignored) {}
        return SharedSuggestionProvider.suggestResource(ApoliPowers.view().keySet(), builder);
    }

    private static int has(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c != null && c.hasPower(power)) {
                ctx.getSource().sendSuccess(() -> Component.literal(e.getName().getString() + " has " + power), false);
                return 1;
            }
        }
        return 0;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int total = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            Set<ResourceLocation> powers = c.allPowers();
            ctx.getSource().sendSuccess(() ->
                Component.literal(e.getName().getString() + ": " + powers), false);
            total += powers.size();
        }
        return total;
    }

    private static int sources(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int total = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            Set<ResourceLocation> srcs = c.sourcesOf(power);
            ctx.getSource().sendSuccess(() ->
                Component.literal(e.getName().getString() + " sources for " + power + ": " + srcs), false);
            total += srcs.size();
        }
        return total;
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "power");
        Power p = ApoliPowers.get(id);
        if (p == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown power: " + id));
            return 0;
        }
        Power.CODEC.encodeStart(JsonOps.INSTANCE, p).result().ifPresent(json ->
            ctx.getSource().sendSuccess(() -> Component.literal(json.toString()), false));
        return 1;
    }

    private static boolean hasArg(CommandContext<CommandSourceStack> ctx, String name) {
        try { ctx.getArgument(name, Object.class); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }
}
