package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class PreventSleepPower extends PowerType<PreventSleepPower.Config> {
    public record Config(
        Optional<BlockCondition> blockCondition,
        Component message,
        boolean setSpawnPoint,
        int priority
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            TextComponent.CODEC.optionalFieldOf("message", Component.translatable("text.apoli.cannot_sleep")).forGetter(Config::message),
            Codec.BOOL.optionalFieldOf("set_spawn_point", false).forGetter(Config::setSpawnPoint),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
