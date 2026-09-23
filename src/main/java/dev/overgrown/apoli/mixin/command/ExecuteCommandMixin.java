package dev.overgrown.apoli.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.command.ApoliResourceCommand;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static dev.overgrown.apoli.command.ApoliResourceCommand.writeAll;

@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin {
    @Unique
    private static CommandSourceStack storeValue(CommandSourceStack commandSourceStack, List<LivingEntity> targets, ResourceLocation power, boolean successOnly, int position) {
        return  commandSourceStack.withCallback((ctx, success, result) -> {
            int affected = 0;

            for (var target: targets) {
                PowerContainer c = PowerContainer.of(target);
                if (c == null) continue;
                var value = successOnly ? (success ? 1 : 0) : result;

                if ((position < 0 ? writeAll(c, power, value) : PowerResources.writeAt(c, power, position, value)).isEmpty()) continue;

                affected++;
            }

            if (affected == 0) {
                commandSourceStack.sendFailure(Component.literal("No target holds the resource power " + power));
            }
        });
    }

    @Inject(method = "register", at = @At(value = "TAIL"))
    private static void apoli$registerExecuteStoreResource(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, CallbackInfo ci) {
        LiteralCommandNode<CommandSourceStack> executeNode = (LiteralCommandNode<CommandSourceStack>) commandDispatcher.getRoot().getChild("execute");
        CommandNode<CommandSourceStack> resultNode = executeNode.getChild("store").getChild("result");
        CommandNode<CommandSourceStack> successNode = executeNode.getChild("store").getChild("success");


        resultNode.addChild(storeResource(executeNode, false, false));
        successNode.addChild(storeResource(executeNode, true, false));
        resultNode.addChild(storeResource(executeNode, false, true));
        successNode.addChild(storeResource(executeNode, true, true));


    }

    @Unique
    private static CommandNode<CommandSourceStack> storeResource(LiteralCommandNode<CommandSourceStack> executeNode, boolean successOnly, boolean isPosition) {
        if (isPosition) {
            return Commands.literal("resource")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("power", ResourceLocationArgument.id()).suggests(ApoliResourceCommand.RESOURCE_POWERS)
                        .then(Commands.argument("position", IntegerArgumentType.integer(0))
                            .redirect(executeNode, ctx -> storeValue(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets").stream().filter(Entity::isAlive).map(entity -> (LivingEntity) entity).toList(),
                                ResourceLocationArgument.getId(ctx, "power"),
                                successOnly,
                                IntegerArgumentType.getInteger(ctx, "position")
                            ))
                        )
                    )

                ).build();
        }
        return Commands.literal("resource")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("power", ResourceLocationArgument.id()).suggests(ApoliResourceCommand.RESOURCE_POWERS)
                        .redirect(executeNode, ctx -> storeValue(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets").stream().filter(Entity::isAlive).map(entity -> (LivingEntity) entity).toList(),
                                ResourceLocationArgument.getId(ctx, "power"),
                                successOnly,
                                -1
                        ))
                )
            ).build();
    }
}
