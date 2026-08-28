package dev.overgrown.apoli.mixin.disguise;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor("speedOld")
    float apoli$getSpeedOld();

    @Accessor("speedOld")
    void apoli$setSpeedOld(float speedOld);

    @Accessor("position")
    void apoli$setPosition(float position);
}
