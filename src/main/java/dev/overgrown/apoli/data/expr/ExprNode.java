package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface ExprNode {

    double eval(@Nullable LivingEntity entity, @Nullable PowerContainer container, @Nullable Level level, double value);

    default boolean isConstant() {
        return false;
    }
}
