package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ModifyBlockRenderPower extends PowerType<ModifyBlockRenderPower.Config> {
    public record Config(Optional<BlockCondition> blockCondition, ResourceLocation block) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            IdCodecs.ID.fieldOf("block").forGetter(Config::block)
        ).apply(i, Config::new));
    }
}
