package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class ApoliResourceCommand {

    private ApoliResourceCommand() {}

    private static final SuggestionProvider<CommandSourceStack> RESOURCE_POWERS = (ctx, builder) -> {
        List<ResourceLocation> held = heldResourcePowers(ctx);
        return SharedSuggestionProvider.suggestResource(held.isEmpty() ? loadedResourcePowers() : held, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:resource")
            .requires(ApoliPermissions.require("apoli.command.resource", 2));

        root.then(Commands.literal("get")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(RESOURCE_POWERS)
                    .executes(ApoliResourceCommand::get))));

        root.then(Commands.literal("set")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(RESOURCE_POWERS)
                    .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ApoliResourceCommand::set)))));

        root.then(Commands.literal("change")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(RESOURCE_POWERS)
                    .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ApoliResourceCommand::change)))));

        root.then(Commands.literal("has")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id())
                    .suggests(RESOURCE_POWERS)
                    .executes(ApoliResourceCommand::has))));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);

        dispatcher.register(Commands.literal("resource")
            .requires(ApoliPermissions.require("apoli.command.resource", 2))
            .redirect(node));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        OptionalInt first = OptionalInt.empty();
        int reported = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            OptionalInt val = PowerResources.read(c, power);
            if (val.isEmpty()) continue;
            int v = val.getAsInt();
            ctx.getSource().sendSuccess(() -> Component.literal(
                e.getName().getString() + " — " + power + ": " + v), false);
            if (first.isEmpty()) first = OptionalInt.of(v);
            reported++;
        }
        if (reported == 0) {
            ctx.getSource().sendFailure(Component.literal("No target has a resource value for " + power));
            return 0;
        }
        return first.orElse(0);
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            OptionalInt written = PowerResources.write(c, power, value);
            if (written.isEmpty()) continue;
            int w = written.getAsInt();
            ctx.getSource().sendSuccess(() -> Component.literal(
                e.getName().getString() + " — " + power + " set to " + w), true);
            affected++;
        }
        if (affected == 0) {
            ctx.getSource().sendFailure(Component.literal("No target holds the resource power " + power));
        }
        return affected;
    }

    private static int change(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int delta = IntegerArgumentType.getInteger(ctx, "value");
        int affected = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            OptionalInt cur = PowerResources.read(c, power);
            if (cur.isEmpty()) continue;
            OptionalInt written = PowerResources.write(c, power, cur.getAsInt() + delta);
            if (written.isEmpty()) continue;
            int w = written.getAsInt();
            ctx.getSource().sendSuccess(() -> Component.literal(
                e.getName().getString() + " — " + power + " now " + w), true);
            affected++;
        }
        if (affected == 0) {
            ctx.getSource().sendFailure(Component.literal("No target holds the resource power " + power));
        }
        return affected;
    }

    private static int has(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation power = ResourceLocationArgument.getId(ctx, "power");
        int count = 0;
        for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
            PowerContainer c = PowerContainer.of(e);
            if (c == null) continue;
            if (PowerResources.read(c, power).isPresent()) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    e.getName().getString() + " has resource " + power), false);
                count++;
            }
        }
        if (count == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("No target holds the resource power " + power), false);
        }
        return count;
    }

    private static List<ResourceLocation> heldResourcePowers(CommandContext<CommandSourceStack> ctx) {
        List<ResourceLocation> out = new ArrayList<>();
        try {
            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                PowerContainer c = PowerContainer.of(e);
                if (c == null) continue;
                for (ResourceLocation id : c.allPowers()) {
                    Power power = ApoliPowers.get(id);
                    if (power != null && PowerResources.isResource(id) && !out.contains(id)) out.add(id);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static List<ResourceLocation> loadedResourcePowers() {
        List<ResourceLocation> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            if (PowerResources.isResource(e.getKey())) out.add(e.getKey());
        }
        return out;
    }
}
