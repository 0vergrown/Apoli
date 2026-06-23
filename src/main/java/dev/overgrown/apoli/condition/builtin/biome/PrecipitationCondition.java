package dev.overgrown.apoli.condition.builtin.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

public final class PrecipitationCondition implements ConditionType<BiomeCtx, PrecipitationCondition.Cfg> {
    public enum Type implements StringRepresentable {
        NONE("none", Biome.Precipitation.NONE),
        RAIN("rain", Biome.Precipitation.RAIN),
        SNOW("snow", Biome.Precipitation.SNOW);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private final String name;
        private final Biome.Precipitation vanilla;
        Type(String n, Biome.Precipitation v) {
            this.name = n; this.vanilla = v;

        }
        @Override
        public String getSerializedName() {
            return name;
        }

        public Biome.Precipitation vanilla() {
            return vanilla;
        }
    }

    public record Cfg(Type precipitation) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Type.CODEC.fieldOf("precipitation").forGetter(Cfg::precipitation)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiomeCtx ctx) {
        BlockPos anchor = new BlockPos(ctx.pos().getX(), 64, ctx.pos().getZ());
        return ctx.biome().value().getPrecipitationAt(anchor) == cfg.precipitation.vanilla();
    }
}
