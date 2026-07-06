package dev.overgrown.apoli.compat.accessory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;


public final class AccessoryCommand {
    private AccessoryCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:accessory")
            .requires(s -> s.hasPermission(2));

        root.then(Commands.literal("list")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ctx -> list(ctx, false))));
        root.then(Commands.literal("slots")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ctx -> list(ctx, true))));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
        dispatcher.register(Commands.literal("accessory")
            .requires(s -> s.hasPermission(2))
            .redirect(node));
    }

    private static int list(CommandContext<CommandSourceStack> ctx, boolean allSlots) throws CommandSyntaxException {
        int total = 0;
        for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
            if (!(entity instanceof LivingEntity living)) continue;
            List<AccessorySlotRef> refs = allSlots ? Accessories.slots(living) : Accessories.equipped(living);
            String header = entity.getName().getString() + " — " + (allSlots ? "slots" : "equipped") + ": " + refs.size();
            ctx.getSource().sendSuccess(() -> Component.literal(header), false);
            for (AccessorySlotRef ref : refs) {
                ItemStack stack = ref.getStack();
                String line = "  [" + ref.provider() + "] " + ref.slotId()
                    + (stack.isEmpty() ? "" : " = " + stack.getCount() + "x " + stack.getHoverName().getString());
                ctx.getSource().sendSuccess(() -> Component.literal(line), false);
            }
            total += refs.size();
        }
        return total;
    }
}
