package dev.overgrown.apoli.client.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

public final class CustomModel {
    public static final Bone[] NONE = new Bone[0];

    private final ModelPart root;
    private final Map<String, Bone[]> bones;
    private final Bone[] all;

    public CustomModel(ModelPart root, Map<String, Bone[]> bones, Bone[] all) {
        this.root = root;
        this.bones = bones;
        this.all = all;
    }

    public ModelPart root() {
        return this.root;
    }

    public Bone[] bones() {
        return this.all;
    }

    public Bone[] bones(String normalizedName) {
        Bone[] found = this.bones.get(normalizedName);
        return found == null ? NONE : found;
    }

    public static final class Bone {
        public final ModelPart part;

        private final float x;
        private final float y;
        private final float z;
        private final float xRot;
        private final float yRot;
        private final float zRot;

        public Bone(ModelPart part) {
            this.part = part;
            this.x = part.x;
            this.y = part.y;
            this.z = part.z;
            this.xRot = part.xRot;
            this.yRot = part.yRot;
            this.zRot = part.zRot;
        }

        public void reset() {
            this.part.x = this.x;
            this.part.y = this.y;
            this.part.z = this.z;
            this.part.xRot = this.xRot;
            this.part.yRot = this.yRot;
            this.part.zRot = this.zRot;
            this.part.xScale = 1.0F;
            this.part.yScale = 1.0F;
            this.part.zScale = 1.0F;
        }
    }
}
