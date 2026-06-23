package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public final class CommandCondition implements ConditionType<EntityCtx, CommandCondition.Cfg> {
    public record Cfg(String command, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("command").forGetter(Cfg::command),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return false;
        int[] result = new int[]{0};
        CommandSourceStack source = ctx.entity().createCommandSourceStack()
            .withPermission(4)
            .withSuppressedOutput()
            .withCallback((success, value) -> result[0] = value);
        server.getCommands().performPrefixedCommand(source, cfg.command);
        return cfg.comparison.compare(result[0], cfg.compareTo);
    }
}
