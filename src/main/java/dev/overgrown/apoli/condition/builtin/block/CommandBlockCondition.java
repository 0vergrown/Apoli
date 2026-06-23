package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class CommandBlockCondition implements ConditionType<BlockCtx, CommandBlockCondition.Cfg> {
    public record Cfg(String command, Comparison comparison, int compareTo) {}

    private static final CommandSource SILENT_SOURCE = new CommandSource() {
        @Override public void sendSystemMessage(Component component) {}
        @Override public boolean acceptsSuccess() { return false; }
        @Override public boolean acceptsFailure() { return false; }
        @Override public boolean shouldInformAdmins() { return false; }
    };

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;
        MinecraftServer server = serverLevel.getServer();
        if (server == null) return false;
        Vec3 pos = Vec3.atCenterOf(ctx.pos());
        CommandSourceStack source = new CommandSourceStack(
            SILENT_SOURCE,
            pos,
            Vec2.ZERO,
            serverLevel,
            4,
            "ApoliBlockCondition",
            Component.literal("ApoliBlockCondition"),
            server,
            null
        );
        int result = server.getCommands().performPrefixedCommand(source, cfg.command);
        return cfg.comparison.compare(result, cfg.compareTo);
    }
}
