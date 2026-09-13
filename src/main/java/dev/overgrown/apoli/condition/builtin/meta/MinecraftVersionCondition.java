package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.util.GameVersion;

import java.util.Optional;
import java.util.OptionalInt;

public final class MinecraftVersionCondition<CTX> implements ConditionType<CTX, MinecraftVersionCondition.Cfg> {

    public static final class Cfg {
        private final Optional<String> version;
        private final OptionalInt dataVersion;
        private final Comparison comparison;
        private final boolean result;

        public Cfg(Optional<String> version, Optional<Integer> dataVersion, Comparison comparison) {
            this.version = version;
            this.dataVersion = dataVersion.isPresent() ? OptionalInt.of(dataVersion.get()) : OptionalInt.empty();
            this.comparison = comparison;
            this.result = evaluate(version, this.dataVersion, comparison);
        }

        public Optional<String> version() {
            return version;
        }

        public Optional<Integer> dataVersion() {
            return dataVersion.isPresent() ? Optional.of(dataVersion.getAsInt()) : Optional.empty();
        }

        public Comparison comparison() {
            return comparison;
        }

        private static boolean evaluate(Optional<String> version, OptionalInt dataVersion, Comparison comparison) {
            if (version.isPresent()
                && !comparison.compare(GameVersion.compare(GameVersion.name(), version.get()), 0)) {
                return false;
            }
            if (dataVersion.isPresent()
                && !comparison.compare(GameVersion.dataVersion(), dataVersion.getAsInt())) {
                return false;
            }
            return version.isPresent() || dataVersion.isPresent();
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("version").forGetter(Cfg::version),
            Codec.INT.optionalFieldOf("data_version").forGetter(Cfg::dataVersion),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        return cfg.result;
    }
}
