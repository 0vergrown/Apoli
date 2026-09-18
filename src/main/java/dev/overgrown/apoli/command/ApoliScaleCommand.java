package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.overgrown.apoli.scale.ScaleEasing;
import dev.overgrown.apoli.scale.ScaleOperation;
import dev.overgrown.apoli.scale.ScaleState;
import dev.overgrown.apoli.scale.ScaleType;
import dev.overgrown.apoli.scale.ScaleTypeCodec;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Locale;

public final class ApoliScaleCommand {

    private ApoliScaleCommand() {}

    private static final SuggestionProvider<CommandSourceStack> SCALE_TYPES = (ctx, builder) ->
        SharedSuggestionProvider.suggestResource(ScaleTypes.ids(), builder);

    private static final SuggestionProvider<CommandSourceStack> EASINGS = (ctx, builder) -> {
        for (ScaleEasing easing : ScaleEasing.values()) builder.suggest(easing.getSerializedName());
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:scale")
            .requires(ApoliPermissions.require("apoli.command.scale", 2));

        root.then(Commands.literal("get")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ctx -> get(ctx, ScaleTypes.BASE))
                .then(Commands.argument("scale_type", ResourceLocationArgument.id())
                    .suggests(SCALE_TYPES)
                    .executes(ctx -> get(ctx, type(ctx))))));

        for (ScaleOperation operation : ScaleOperation.values()) {
            root.then(operation(operation));
        }

        root.then(Commands.literal("reset")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ApoliScaleCommand::reset)));

        root.then(Commands.literal("list").executes(ApoliScaleCommand::list));

        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> operation(ScaleOperation operation) {
        RequiredArgumentBuilder<CommandSourceStack, Float> value =
            Commands.argument("value", FloatArgumentType.floatArg(ScaleState.MIN, ScaleState.MAX))
                .executes(ctx -> apply(ctx, operation, 0, ScaleEasing.LINEAR))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, Short.MAX_VALUE))
                    .executes(ctx -> apply(ctx, operation, IntegerArgumentType.getInteger(ctx, "ticks"),
                        ScaleEasing.LINEAR))
                    .then(Commands.argument("easing", ResourceLocationArgument.id())
                        .suggests(EASINGS)
                        .executes(ctx -> apply(ctx, operation, IntegerArgumentType.getInteger(ctx, "ticks"),
                            easing(ctx)))));

        return Commands.literal(operation.getSerializedName())
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("scale_type", ResourceLocationArgument.id())
                    .suggests(SCALE_TYPES)
                    .then(value)));
    }

    private static ScaleType type(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "scale_type");
        ScaleType type = ScaleTypeCodec.resolve(id);
        if (type == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                Component.literal("Unknown scale type: " + id)).create();
        }
        return type;
    }

    private static ScaleEasing easing(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "easing");
        for (ScaleEasing easing : ScaleEasing.values()) {
            if (easing.getSerializedName().equals(id.getPath())) return easing;
        }
        throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
            Component.literal("Unknown easing: " + id)).create();
    }

    private static int get(CommandContext<CommandSourceStack> ctx, ScaleType type) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int last = 0;
        for (Entity entity : targets) {
            float value = Scales.value(entity, type);
            last = (int) (value * 100.0F);
            ctx.getSource().sendSuccess(() -> Component.literal(
                entity.getName().getString() + " " + type.id() + " = "
                    + String.format(Locale.ROOT, "%.4f", value)), false);
        }
        return last;
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, ScaleOperation operation,
                             int ticks, ScaleEasing easing) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        ScaleType type = type(ctx);
        float argument = FloatArgumentType.getFloat(ctx, "value");
        for (Entity entity : targets) {
            ScaleState state = Scales.stateOf(entity);
            float current = state == null ? ScaleState.DEFAULT : state.target(type);
            Scales.set(entity, type, operation.apply(current, argument), ticks, easing);
        }
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Set " + type.id() + " on " + count + " entit" + (count == 1 ? "y" : "ies")), true);
        return count;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        for (Entity entity : targets) Scales.reset(entity);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Reset every scale on " + count + " entit" + (count == 1 ? "y" : "ies")), true);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        StringBuilder builder = new StringBuilder();
        for (ScaleType type : ScaleTypes.all()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(type.id());
        }
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return ScaleTypes.count();
    }
}
