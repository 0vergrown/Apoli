package dev.overgrown.apoli.client.summon;

import dev.overgrown.apoli.Apoli;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class SummonModelLayers {
    private SummonModelLayers() {}

    public static final ModelLayerLocation MINION = new ModelLayerLocation(Apoli.id("minion"), "main");
    public static final ModelLayerLocation CLONE = new ModelLayerLocation(Apoli.id("clone"), "main");
    public static final ModelLayerLocation CLONE_SLIM = new ModelLayerLocation(Apoli.id("clone_slim"), "main");
}
