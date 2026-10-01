package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.vanillin.item.ItemModels;
import dev.overgrown.apoli.Apoli;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class VanillinItemModels {
    private static volatile boolean unavailable;

    private VanillinItemModels() {}

    @Nullable
    static Model ground(Level level, ItemStack stack) {
        if (unavailable) return null;
        try {
            return ItemModels.isSupported(stack) ? ItemModels.get(level, stack, ItemDisplayContext.GROUND) : null;
        } catch (LinkageError e) {
            unavailable = true;
            Apoli.LOGGER.warn("[Apoli] This Vanillin version's item models don't match the ones Apoli was built against, "
                + "so custom projectiles shaped like items keep drawing the vanilla way.", e);
            return null;
        }
    }
}
