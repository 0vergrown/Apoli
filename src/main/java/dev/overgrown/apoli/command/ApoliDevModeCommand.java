package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overgrown.apoli.dev.DevMode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class ApoliDevModeCommand {

    private ApoliDevModeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("apoli:dev_mode")
            .requires(ApoliPermissions.require("apoli.command.dev_mode", 2))
            .executes(ctx -> toggle(ctx, List.of(ctx.getSource().getPlayerOrException())))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> toggle(ctx, EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
        throws CommandSyntaxException {
        int changed = 0;
        for (ServerPlayer player : targets) {
            boolean enabled = DevMode.toggle(player);
            player.sendSystemMessage(Component.literal("Apoli developer mode " + (enabled ? "on" : "off"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            changed++;
        }
        int total = changed;
        ctx.getSource().sendSuccess(() ->
            Component.literal("Toggled Apoli developer mode for " + total + " player(s)"), true);
        return changed;
    }
}
