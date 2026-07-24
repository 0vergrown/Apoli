package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.OptionalInt;

public final class TogglePower extends PowerType<TogglePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("toggle");

    public record Config(boolean activeByDefault, Key key, boolean retainState) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("active_by_default", true).forGetter(Config::activeByDefault),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key),
            Codec.BOOL.optionalFieldOf("retain_state", true).forGetter(Config::retainState)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxInt(powerId).isPresent()) return;
        impl.setAuxInt(powerId, cfg.activeByDefault ? 1 : 0);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (!holder.hasPower(powerId)) impl.removeAux(powerId);
    }

    public static void toggle(PowerContainer holder, ResourceLocation powerId) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        int cur = impl.getAuxInt(powerId).orElse(0);
        impl.setAuxInt(powerId, cur == 0 ? 1 : 0);
    }

    public static boolean isActive(Entity entity, ResourceLocation powerId) {
        PowerContainer holder = PowerContainer.of(entity);
        if (holder == null) return false;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return false;
        if (!CANONICAL.equals(PowerTypeRegistry.resolveId(power.typeId()))) return false;
        OptionalInt v = holder.getAuxInt(powerId);
        return v.isPresent() && v.getAsInt() != 0;
    }
}
