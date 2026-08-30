package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PoweredEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ScareMobsPower extends PowerType<ScareMobsPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition, double radius, double speed) {}

    private static final List<LivingEntity> FOUND = new ArrayList<>(4);
    private static long scannedTick = Long.MIN_VALUE;

    public static List<LivingEntity> holders(Level level) {
        long now = level.getGameTime();
        if (now == scannedTick) return FOUND;
        scannedTick = now;
        FOUND.clear();
        PoweredEntities.forEach(ScareMobsPower::collect);
        return FOUND;
    }

    private static void collect(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        List<net.minecraft.resources.ResourceLocation> powers = container.powersOfType(ApoliIds.SCARE_MOBS);
        for (int i = 0; i < powers.size(); i++) {
            if (!container.isSuppressed(powers.get(i))) {
                FOUND.add(living);
                return;
            }
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Codec.DOUBLE.optionalFieldOf("radius", 6.0).forGetter(Config::radius),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(Config::speed)
        ).apply(i, Config::new));
    }
}
