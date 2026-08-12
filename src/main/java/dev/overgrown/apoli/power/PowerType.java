package dev.overgrown.apoli.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalInt;

public abstract class PowerType<C> {
    public abstract MapCodec<C> configCodec();

    public void onAdded(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void onRemoved(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void onSuppressed(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public void onUnsuppressed(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public void tick(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public boolean isActive(ResourceLocation powerId, C cfg, EntityCtx ctx) {
        return true;
    }

    public boolean ticksNonLivingEntities() {
        return false;
    }

    public OptionalInt readResource(ResourceLocation powerId, C cfg, PowerContainer holder) {
        return OptionalInt.empty();
    }

    public OptionalInt writeResource(ResourceLocation powerId, C cfg, PowerContainer holder, int value) {
        return OptionalInt.empty();
    }

    public OptionalInt resourceBound(ResourceLocation powerId, C cfg, PowerContainer holder, boolean max) {
        return OptionalInt.empty();
    }
}
