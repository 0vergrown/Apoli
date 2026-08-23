package dev.overgrown.apoli.access;

import net.minecraft.world.entity.LivingEntity;

public interface DualSwingHolder {

    boolean apoli$isSwingingBothArms();

    void apoli$setSwingingBothArms(boolean value);

    static boolean of(LivingEntity entity) {
        return entity instanceof DualSwingHolder holder && holder.apoli$isSwingingBothArms();
    }
}
