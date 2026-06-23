package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ModifyBlockRenderPower extends PowerType<ModifyBlockRenderPower.Config> {
    public record Config(Optional<BlockCondition> blockCondition, ResourceLocation block) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            ResourceLocation.CODEC.fieldOf("block").forGetter(Config::block)
        ).apply(i, Config::new));
    }
}
