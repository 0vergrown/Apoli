package dev.overgrown.apoli.client.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface ExtraTexturedModel {
    ResourceLocation defaultExtraTexture();

    List<ModelPart> extraTexturedParts();
}
