package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScareMobsPower extends PowerType<ScareMobsPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition, double radius, double speed) {}

    private static final Set<UUID> HOLDERS = ConcurrentHashMap.newKeySet();

    public static boolean anyHolders() {
        return !HOLDERS.isEmpty();
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        Entity owner = holder.rawOwner();
        if (owner != null) HOLDERS.add(owner.getUUID());
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        Entity owner = holder.rawOwner();
        if (owner == null) return;
        if (holder.powersOfType(dev.overgrown.apoli.power.ApoliIds.SCARE_MOBS).isEmpty()) {
            HOLDERS.remove(owner.getUUID());
        }
    }

    public static void onEntityGone(UUID uuid) {
        HOLDERS.remove(uuid);
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            Codec.DOUBLE.optionalFieldOf("radius", 6.0).forGetter(Config::radius),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(Config::speed)
        ).apply(i, Config::new));
    }
}
