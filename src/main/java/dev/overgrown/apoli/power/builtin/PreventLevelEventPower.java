package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PreventLevelEventPower extends PowerType<PreventLevelEventPower.Config> {
    public record Config(Optional<List<Integer>> events, int[] ids) {
        public Config(Optional<List<Integer>> events) {
            this(events, events.map(list -> list.stream().mapToInt(Integer::intValue).toArray()).orElse(null));
        }

        boolean matches(int event) {
            if (ids == null) return true;
            for (int id : ids) {
                if (id == event) return true;
            }
            return false;
        }
    }

    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.PREVENT_LEVEL_EVENT);

    @Override
    public MapCodec<Config> configCodec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.<Config>mapCodec(i -> i.group(
            SingleOrList.of(Codec.INT).optionalFieldOf("events").forGetter(Config::events)
        ).apply(i, Config::new)), Map.of("event", "events"));
    }

    public static boolean prevents(@Nullable Player player, int event) {
        if (player == null || !IN_USE.inUse() || !PowerLookup.holdsType(player, ApoliIds.PREVENT_LEVEL_EVENT)) return false;
        return PowerLookup.anyActive(player, ApoliIds.PREVENT_LEVEL_EVENT, Config.class, Config::matches, event);
    }
}
