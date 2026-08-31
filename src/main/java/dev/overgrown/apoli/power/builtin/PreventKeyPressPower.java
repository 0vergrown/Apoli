package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PreventKeyPressPower extends PowerType<PreventKeyPressPower.Config> {

    private static final ThreadLocal<boolean[]> REENTRY = ThreadLocal.withInitial(() -> new boolean[1]);

    public record Config(
        Optional<List<Key>> keys,
        Optional<Boolean> scroll,
        boolean affectForced,
        boolean unpress
    ) {
        public boolean blocksEverything() {
            return keys.isEmpty();
        }

        public boolean blocksScroll() {
            return scroll.orElseGet(this::blocksEverything);
        }

        public boolean blocks(String name) {
            if (keys.isEmpty()) return true;
            List<Key> list = keys.get();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key().equals(name)) return true;
            }
            return false;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(Key.CODEC).optionalFieldOf("keys").forGetter(Config::keys),
            Codec.BOOL.optionalFieldOf("scroll").forGetter(Config::scroll),
            Codec.BOOL.optionalFieldOf("affect_forced", false).forGetter(Config::affectForced),
            Codec.BOOL.optionalFieldOf("unpress", true).forGetter(Config::unpress)
        ).apply(i, Config::new));
    }

    public static boolean any(@Nullable Entity entity) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        return !container.powersOfType(ApoliIds.PREVENT_KEY_PRESS).isEmpty();
    }

    public static void forEachActive(@Nullable Entity entity, Consumer<Config> consumer) {
        if (!any(entity)) return;
        boolean[] guard = REENTRY.get();
        if (guard[0]) return;
        guard[0] = true;
        try {
            PowerLookup.forEach(entity, ApoliIds.PREVENT_KEY_PRESS, Config.class, consumer);
        } finally {
            guard[0] = false;
        }
    }

    public static boolean blocks(@Nullable Entity entity, String key) {
        return scan(entity, cfg -> cfg.blocks(key));
    }

    public static boolean blocksForcedKeys(@Nullable Entity entity, String key) {
        return scan(entity, cfg -> cfg.affectForced() && cfg.blocks(key));
    }

    private static boolean scan(@Nullable Entity entity, Predicate<Config> test) {
        boolean[] hit = new boolean[]{false};
        forEachActive(entity, cfg -> {
            if (!hit[0] && test.test(cfg)) hit[0] = true;
        });
        return hit[0];
    }
}
