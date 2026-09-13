package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.util.GameVersion;

import java.util.List;
import java.util.Optional;

public final class ModLoadedCondition<CTX> implements ConditionType<CTX, ModLoadedCondition.Cfg> {

    public static final class Cfg {
        private final Optional<String> mod;
        private final List<String> mods;
        private final Optional<String> version;
        private final Comparison comparison;
        private final boolean result;

        public Cfg(Optional<String> mod, List<String> mods, Optional<String> version, Comparison comparison) {
            this.mod = mod;
            this.mods = List.copyOf(mods);
            this.version = version;
            this.comparison = comparison;
            this.result = evaluate(mod, this.mods, version, comparison);
        }

        public Optional<String> mod() {
            return mod;
        }

        public List<String> mods() {
            return mods;
        }

        public Optional<String> version() {
            return version;
        }

        public Comparison comparison() {
            return comparison;
        }

        private static boolean evaluate(Optional<String> mod, List<String> mods,
                                        Optional<String> version, Comparison comparison) {
            if (mod.isPresent() && !matches(mod.get(), version, comparison)) return false;
            for (int i = 0; i < mods.size(); i++) {
                if (!matches(mods.get(i), version, comparison)) return false;
            }
            return mod.isPresent() || !mods.isEmpty();
        }

        private static boolean matches(String id, Optional<String> version, Comparison comparison) {
            if (!ModCompat.isLoaded(id)) return false;
            if (version.isEmpty()) return true;
            String actual = ModCompat.versionOf(id);
            if (actual == null) return false;
            return comparison.compare(GameVersion.compare(actual, version.get()), 0);
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("mod").forGetter(Cfg::mod),
            Codec.STRING.listOf().optionalFieldOf("mods", List.of()).forGetter(Cfg::mods),
            Codec.STRING.optionalFieldOf("version").forGetter(Cfg::version),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        return cfg.result;
    }
}
