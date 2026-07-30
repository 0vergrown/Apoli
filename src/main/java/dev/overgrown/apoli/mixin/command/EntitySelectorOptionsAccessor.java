package dev.overgrown.apoli.mixin.command;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(EntitySelectorOptions.class)
public interface EntitySelectorOptionsAccessor {
    @Invoker("register")
    static void apoli$register(String id, EntitySelectorOptions.Modifier handler,
                               Predicate<EntitySelectorParser> condition, Component tooltip) {
        throw new AssertionError();
    }
}
