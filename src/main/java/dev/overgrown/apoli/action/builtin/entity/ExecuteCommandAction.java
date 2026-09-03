package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.MacroArguments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public final class ExecuteCommandAction implements ActionType<EntityCtx, ExecuteCommandAction.Cfg> {
    public record Cfg(String command, Optional<MacroArguments> arguments) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command),
            MacroArguments.FIELD.forGetter(Cfg::arguments)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;
        String command = cfg.command;
        if (cfg.arguments.isPresent()) {
            command = MacroArguments.expand(command, cfg.arguments.get().resolve(server, ctx.raw(), null));
            if (command == null) return;
        }
        dev.overgrown.apoli.dev.DevMode.echoCommand(ctx.raw(), command);
        CommandSourceStack source = ctx.raw().createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }
}
