package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public final class ExecuteCommandAction implements ActionType<EntityCtx, ExecuteCommandAction.Cfg> {
    public record Cfg(String command) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;
        CommandSourceStack source = ctx.raw().createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, cfg.command);
    }
}
