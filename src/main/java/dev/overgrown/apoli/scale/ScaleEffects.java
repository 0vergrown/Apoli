package dev.overgrown.apoli.scale;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class ScaleEffects {
    private ScaleEffects() {}

    public static boolean untouched(@Nullable Entity entity) {
        return entity == null || Scales.untouched(entity);
    }

    public static float damage(@Nullable LivingEntity attacker, LivingEntity target, float amount) {
        if (amount <= 0.0F) return amount;
        float result = amount;
        if (!untouched(attacker)) {
            result *= Scales.applied(attacker, ScaleTypes.ATTACK);
        }
        if (!untouched(target)) {
            float defense = Scales.applied(target, ScaleTypes.DEFENSE);
            if (defense > 0.0F && defense != 1.0F) result /= defense;
        }
        return result;
    }

    public static float miningSpeed(Entity miner, float speed) {
        if (untouched(miner)) return speed;
        return speed * Scales.applied(miner, ScaleTypes.MINING_SPEED);
    }
}
