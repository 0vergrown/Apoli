package dev.overgrown.apoli.client.render.model;

import dev.overgrown.apoli.client.model.PerFaceCube;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerModelBones {
    public static final float SKIN_WIDTH = 64.0F;
    public static final float SKIN_HEIGHT = 64.0F;

    private PlayerModelBones() {}

    public static ModelPart bone(float x, float y, float z, float xRot, float yRot, float zRot,
                                 List<ModelPart.Cube> cubes, ModelPart... children) {
        Map<String, ModelPart> named = Map.of();
        if (children.length > 0) {
            named = new LinkedHashMap<>(children.length * 2);
            for (int i = 0; i < children.length; i++) {
                named.put("bone" + i, children[i]);
            }
        }
        ModelPart part = new ModelPart(cubes, named);
        part.setInitialPose(PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
        part.resetPose();
        return part;
    }

    public static ModelPart.Cube cube(float x, float y, float z,
                                      float sizeX, float sizeY, float sizeZ,
                                      float grow, boolean mirror, float[][] faces) {
        return PerFaceCube.of(faces, x, y, z, sizeX, sizeY, sizeZ, grow, mirror, SKIN_WIDTH, SKIN_HEIGHT);
    }
}
