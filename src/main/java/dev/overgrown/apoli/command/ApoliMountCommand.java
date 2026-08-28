package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overgrown.apoli.mount.MountOffsets;
import dev.overgrown.apoli.mount.MountRotation;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public final class ApoliMountCommand {

    private ApoliMountCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("apoli:mount")
            .requires(ApoliPermissions.require("apoli.command.mount", 2))
            .then(Commands.literal("get")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ApoliMountCommand::get)))
            .then(Commands.literal("clear")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ApoliMountCommand::clear))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int found = 0;
        for (Entity target : targets) {
            MountOffsets.Offset offset = MountOffsets.get(target.getId());
            if (offset == null) {
                ctx.getSource().sendSuccess(() -> Component.literal(name(target) + ": no mount offset")
                    .withStyle(ChatFormatting.GRAY), false);
                continue;
            }
            found++;
            Entity vehicle = target.getVehicle();
            Vec3 resolved = vehicle == null ? Vec3.ZERO : MountOffsets.resolve(vehicle, target);
            MutableComponent line = Component.literal(name(target) + ": ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.format("x=%.2f y=%.2f z=%.2f ", offset.x(), offset.y(), offset.z()))
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(offset.space().getSerializedName()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" rotation=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(offset.rotation().getSerializedName())
                    .withStyle(offset.rotation() == MountRotation.BODY
                        ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            if (vehicle != null) {
                line.append(Component.literal(String.format(
                    " | riding %s (yaw=%.1f body=%.1f) -> %.2f/%.2f/%.2f",
                    name(vehicle), MountRotation.HEAD.yawOf(vehicle), MountRotation.BODY.yawOf(vehicle),
                    resolved.x, resolved.y, resolved.z)).withStyle(ChatFormatting.DARK_GRAY));
            }
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return found;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int cleared = 0;
        for (Entity target : EntityArgument.getEntities(ctx, "targets")) {
            if (MountOffsets.get(target.getId()) == null) continue;
            MountOffsets.clear(target.getId());
            cleared++;
        }
        int total = cleared;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + total + " mount offset(s)"), true);
        return cleared;
    }

    private static String name(Entity entity) {
        return entity.getName().getString() + "#" + entity.getId();
    }
}
