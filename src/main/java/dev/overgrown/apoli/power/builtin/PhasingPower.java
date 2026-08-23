package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class PhasingPower extends PowerType<PhasingPower.Config> {
    public record Config(
        boolean blacklist,
        Optional<BlockCondition> blockCondition,
        RenderType renderType,
        float viewDistance,
        Optional<EntityCondition> phaseDownCondition
    ) {}

    public enum RenderType implements StringRepresentable {
        BLINDNESS("blindness"),
        REMOVE_BLOCKS("remove_blocks"),
        NONE("none");

        public static final Codec<RenderType> CODEC = StringRepresentable.fromEnum(RenderType::values);

        private final String name;
        RenderType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("blacklist", false).forGetter(Config::blacklist),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            RenderType.CODEC.optionalFieldOf("render_type", RenderType.BLINDNESS).forGetter(Config::renderType),
            Codec.FLOAT.optionalFieldOf("view_distance", 10.0f).forGetter(Config::viewDistance),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("phase_down_condition", EntityCondition.CODEC).forGetter(Config::phaseDownCondition)
        ).apply(i, Config::new));
    }

    public static boolean allowsPhasing(Config cfg, Level level, BlockPos pos, BlockState state) {
        if (cfg.blockCondition.isEmpty()) return true;
        boolean matches = cfg.blockCondition.get().test(new BlockCtx(pos.immutable(), state, level));
        return cfg.blacklist ? !matches : matches;
    }
}
