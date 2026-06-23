package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.level.LightLayer;

import java.util.Optional;

public final class LightLevelBlockCondition implements ConditionType<BlockCtx, LightLevelBlockCondition.Cfg> {
    public record Cfg(Optional<String> lightType, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("light_type").forGetter(Cfg::lightType),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        int value;
        if (cfg.lightType.isEmpty()) {
            int block = ctx.level().getBrightness(LightLayer.BLOCK, ctx.pos());
            int sky = ctx.level().getBrightness(LightLayer.SKY, ctx.pos());
            value = Math.max(block, sky);
        } else {
            value = switch (cfg.lightType.get()) {
                case "block" -> ctx.level().getBrightness(LightLayer.BLOCK, ctx.pos());
                case "sky" -> ctx.level().getBrightness(LightLayer.SKY, ctx.pos());
                default -> Math.max(
                    ctx.level().getBrightness(LightLayer.BLOCK, ctx.pos()),
                    ctx.level().getBrightness(LightLayer.SKY, ctx.pos()));
            };
        }
        return cfg.comparison.compare(value, cfg.compareTo);
    }
}
