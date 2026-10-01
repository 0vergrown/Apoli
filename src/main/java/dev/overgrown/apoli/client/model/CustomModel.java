package dev.overgrown.apoli.client.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CustomModel {
    public static final Bone[] NONE = new Bone[0];
    public static final ModelPart[] NO_PARTS = new ModelPart[0];

    private final ModelPart root;
    private final Map<String, Bone[]> bones;
    private final Bone[] all;
    private final boolean hoisted;

    public CustomModel(ModelPart root, Map<String, Bone[]> bones, Bone[] all, boolean hoisted) {
        this.root = root;
        this.bones = bones;
        this.all = all;
        this.hoisted = hoisted;
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

    public boolean hoisted() {
        return this.hoisted;
    }

    public CustomModel remapped(ModelPart root, Map<ModelPart, ModelPart> parts) {
        Map<ModelPart, Bone> made = new IdentityHashMap<>(this.all.length * 2);
        Bone[] remappedAll = new Bone[this.all.length];
        for (int i = 0; i < remappedAll.length; i++) {
            remappedAll[i] = remap(this.all[i], parts, made);
        }
        Map<String, Bone[]> remappedBones = new HashMap<>(this.bones.size() * 2);
        for (Map.Entry<String, Bone[]> entry : this.bones.entrySet()) {
            Bone[] source = entry.getValue();
            Bone[] target = new Bone[source.length];
            for (int i = 0; i < source.length; i++) {
                target[i] = remap(source[i], parts, made);
            }
            remappedBones.put(entry.getKey(), target);
        }
        return new CustomModel(root, remappedBones, remappedAll, this.hoisted);
    }

    private static Bone remap(Bone source, Map<ModelPart, ModelPart> parts, Map<ModelPart, Bone> made) {
        ModelPart part = parts.get(source.part);
        Bone existing = made.get(part);
        if (existing != null) {
            return existing;
        }
        ModelPart[] ancestors = new ModelPart[source.ancestors.length];
        int kept = 0;
        for (ModelPart ancestor : source.ancestors) {
            ModelPart copy = parts.get(ancestor);
            if (copy != null) {
                ancestors[kept++] = copy;
            }
        }
        Bone bone = new Bone(part, kept == ancestors.length ? ancestors : java.util.Arrays.copyOf(ancestors, kept));
        made.put(part, bone);
        return bone;
    }

    public static final class Bone {
        public final ModelPart part;

        private final ModelPart[] ancestors;
        private final float x;
        private final float y;
        private final float z;
        private final float xRot;
        private final float yRot;
        private final float zRot;

        public Bone(ModelPart part, ModelPart[] ancestors) {
            this.part = part;
            this.ancestors = ancestors;
            this.x = part.x;
            this.y = part.y;
            this.z = part.z;
            this.xRot = part.xRot;
            this.yRot = part.yRot;
            this.zRot = part.zRot;
        }

        public ModelPart[] ancestors() {
            return this.ancestors;
        }

        public float restXRot() {
            return this.xRot;
        }

        public float restYRot() {
            return this.yRot;
        }

        public float restZRot() {
            return this.zRot;
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
