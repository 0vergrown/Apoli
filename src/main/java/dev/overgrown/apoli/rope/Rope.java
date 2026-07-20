package dev.overgrown.apoli.rope;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Rope {

    public final int id;
    public final RopeAnchor from;
    public final RopeAnchor to;
    public final @Nullable UUID fromEntity;
    public final @Nullable UUID toEntity;
    public final @Nullable String slot;
    public final @Nullable UUID owner;
    public final RopeParams params;
    public final ResourceLocation texture;
    public final ResourceKey<Level> dimension;

    public double length;
    public int flightTicks = 0;

    public Rope(int id, RopeAnchor from, RopeAnchor to,
                @Nullable UUID fromEntity, @Nullable UUID toEntity,
                @Nullable String slot, @Nullable UUID owner,
                RopeParams params, ResourceLocation texture, ResourceKey<Level> dimension, double length) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.fromEntity = fromEntity;
        this.toEntity = toEntity;
        this.slot = slot;
        this.owner = owner;
        this.params = params;
        this.texture = texture;
        this.dimension = dimension;
        this.length = length;
    }

    public boolean touches(UUID entity) {
        return entity.equals(fromEntity) || entity.equals(toEntity);
    }
}
