package dev.overgrown.apoli.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;

public abstract class PowerType<C> {
    public abstract MapCodec<C> configCodec();

    public void onAdded(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void onRemoved(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void tick(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public boolean isActive(ResourceLocation powerId, C cfg, EntityCtx ctx) {
        return true;
    }
}