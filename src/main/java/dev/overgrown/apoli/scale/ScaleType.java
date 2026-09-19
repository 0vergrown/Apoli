package dev.overgrown.apoli.scale;

import net.minecraft.resources.ResourceLocation;

public final class ScaleType {

    private static final ScaleType[] NO_PARENTS = new ScaleType[0];

    private final ResourceLocation id;
    private final int index;
    private final boolean affectsDimensions;

    private ScaleType[] multipliers = NO_PARENTS;
    private ScaleType[] divisors = NO_PARENTS;

    ScaleType(ResourceLocation id, int index, boolean affectsDimensions) {
        this.id = id;
        this.index = index;
        this.affectsDimensions = affectsDimensions;
    }

    public ResourceLocation id() {
        return id;
    }

    public int index() {
        return index;
    }

    public boolean affectsDimensions() {
        return affectsDimensions;
    }

    public ScaleType[] multipliers() {
        return multipliers;
    }

    public ScaleType[] divisors() {
        return divisors;
    }

    ScaleType multipliedBy(ScaleType... parents) {
        this.multipliers = parents;
        return this;
    }

    ScaleType dividedBy(ScaleType... parents) {
        this.divisors = parents;
        return this;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
