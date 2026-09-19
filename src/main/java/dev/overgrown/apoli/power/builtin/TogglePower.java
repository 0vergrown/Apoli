package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.OptionalInt;

public final class TogglePower extends PowerType<TogglePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("toggle");

    public record Config(boolean activeByDefault, Key key, boolean retainState,
                         Optional<EntityCondition> turnOnCondition,
                         Optional<EntityCondition> turnOffCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("active_by_default", true).forGetter(Config::activeByDefault),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key),
            Codec.BOOL.optionalFieldOf("retain_state", true).forGetter(Config::retainState),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("turn_on_condition", EntityCondition.CODEC)
                .forGetter(Config::turnOnCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("turn_off_condition", EntityCondition.CODEC)
                .forGetter(Config::turnOffCondition)
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

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (cfg.retainState) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxIntOr(powerId, 0) == 0) return;
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return;
        if (loaded.condition().get().test(new EntityCtx(owner, level))) return;
        impl.setAuxInt(powerId, 0);
    }

    public static void toggle(PowerContainer holder, ResourceLocation powerId) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        int cur = impl.getAuxInt(powerId).orElse(0);
        impl.setAuxInt(powerId, cur == 0 ? 1 : 0);
    }

    public static boolean toggleByKey(PowerContainer holder, ResourceLocation powerId, Config cfg) {
        if (!(holder instanceof PowerContainerImpl impl)) return false;
        int cur = impl.getAuxInt(powerId).orElse(0);
        Optional<EntityCondition> gate = cur == 0 ? cfg.turnOnCondition() : cfg.turnOffCondition();
        if (gate.isPresent()) {
            Entity owner = holder.rawOwner();
            if (owner == null || !(owner.level() instanceof ServerLevel level)) return false;
            if (!gate.get().test(EntityCtx.of(owner, level))) return false;
        }
        impl.setAuxInt(powerId, cur == 0 ? 1 : 0);
        return true;
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
