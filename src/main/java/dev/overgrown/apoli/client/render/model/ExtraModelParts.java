package dev.overgrown.apoli.client.render.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

public interface ExtraModelParts {
    void collectExtraParts(String normalized, List<ModelPart> parts);

    void collectExtraParts(List<ModelPart> parts);
}
