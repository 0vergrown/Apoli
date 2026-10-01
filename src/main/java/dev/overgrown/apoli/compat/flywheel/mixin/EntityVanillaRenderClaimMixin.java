package dev.overgrown.apoli.compat.flywheel.mixin;

import dev.overgrown.apoli.compat.flywheel.VanillaRenderClaim;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityVanillaRenderClaimMixin implements VanillaRenderClaim {

    @Unique
    private volatile boolean apoli$vanillaClaimed;

    @Override
    public boolean apoli$vanillaClaimed() {
        return this.apoli$vanillaClaimed;
    }

    @Override
    public void apoli$setVanillaClaimed(boolean claimed) {
        this.apoli$vanillaClaimed = claimed;
    }
}
