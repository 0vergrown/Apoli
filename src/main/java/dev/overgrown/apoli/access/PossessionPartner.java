package dev.overgrown.apoli.access;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface PossessionPartner {

    @Nullable
    Entity apoli$possessionHost();

    @Nullable
    Entity apoli$possessor();

    void apoli$setPossessionHost(@Nullable Entity host);

    void apoli$setPossessor(@Nullable Entity possessor);
}
