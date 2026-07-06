package dev.overgrown.apoli.compat.icarus;

import dev.cammiescorner.icarus.util.IcarusHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

public final class IcarusFieldHooks {
    private IcarusFieldHooks() {}

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) return;

        Predicate<LivingEntity> previousHasWings = IcarusHelper.hasWings;
        IcarusHelper.hasWings = entity -> WingsAccess.hasWings(entity) || previousHasWings.test(entity);

        Function<LivingEntity, ItemStack> previousEquippedWings = IcarusHelper.getEquippedWings;
        IcarusHelper.getEquippedWings = entity -> {
            WingsPower.Config cfg = WingsAccess.get(entity);
            return cfg != null ? cfg.wingsType().copy() : previousEquippedWings.apply(entity);
        };
    }
}
