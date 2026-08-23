package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

public final class ExecuteCommandBiEntityAction implements ActionType<BiEntityCtx, ExecuteCommandBiEntityAction.Cfg> {
    public record Cfg(String command, String actorSelector, String targetSelector) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command),
            Codec.STRING.optionalFieldOf("actor_selector", "%a").forGetter(Cfg::actorSelector),
            Codec.STRING.optionalFieldOf("target_selector", "%t").forGetter(Cfg::targetSelector)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null) return;
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;
        String command = cfg.command();
        if (!cfg.actorSelector().isEmpty()) {
            command = command.replace(cfg.actorSelector(), actor.getStringUUID());
        }
        if (!cfg.targetSelector().isEmpty()) {
            command = command.replace(cfg.targetSelector(), target.getStringUUID());
        }
        CommandSourceStack source = actor.createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }
}
