package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class ExecuteCommandBlockAction implements ActionType<BlockCtx, ExecuteCommandBlockAction.Cfg> {
    public record Cfg(String command) {}

    private static final CommandSource SILENT_SOURCE = new CommandSource() {
        @Override public void sendSystemMessage(Component component) {}
        @Override public boolean acceptsSuccess() { return false; }
        @Override public boolean acceptsFailure() { return false; }
        @Override public boolean shouldInformAdmins() { return false; }
    };

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();
        if (server == null) return;
        Vec3 pos = Vec3.atCenterOf(ctx.pos());
        CommandSourceStack source = new CommandSourceStack(
            SILENT_SOURCE, pos, Vec2.ZERO, serverLevel, 4,
            "ApoliBlockAction", Component.literal("ApoliBlockAction"), server, null
        );
        server.getCommands().performPrefixedCommand(source, cfg.command);
    }
}
