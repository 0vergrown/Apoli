package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ExprVar {

    double get(@Nullable LivingEntity entity, @Nullable PowerContainer container, @Nullable Level level, double value);
}
