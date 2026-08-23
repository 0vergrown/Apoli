package dev.overgrown.apoli.client.model;

import com.mojang.serialization.Dynamic;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.ModelParts;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BedrockModelParser {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float BEDROCK_FLOOR = 24.0F;
    private static final int MAX_PARENT_DEPTH = 64;

    private BedrockModelParser() {}

    public static <T> CustomModel parse(ResourceLocation id, Dynamic<T> json) {
        Dynamic<T> geo = json.get("minecraft:geometry").asStreamOpt().result()
            .flatMap(stream -> stream.findFirst())
            .orElseThrow(() -> new IllegalArgumentException("minecraft:geometry is empty"));
        Dynamic<T> description = geo.get("description").result().orElseGet(geo::emptyMap);
        int texWidth = description.get("texture_width").asInt(64);
        int texHeight = description.get("texture_height").asInt(64);

        List<Bone> ordered = new ArrayList<>();
        Map<String, Bone> byName = new LinkedHashMap<>();
        for (Dynamic<T> element : geo.get("bones").asList(java.util.function.Function.identity())) {
            Bone bone = parseBone(element, id);
            if (byName.putIfAbsent(bone.name, bone) == null) {
                ordered.add(bone);
            } else {
                Apoli.LOGGER.warn("[Apoli] Custom model {} declares the bone '{}' more than once; ignoring the duplicate.", id, bone.name);
            }
        }
        for (Bone bone : ordered) {
            Bone parent = bone.parent == null ? null : byName.get(bone.parent);
            if (parent != null) {
                parent.children.add(bone);
                bone.attachment = parent;
            } else if (bone.parent != null) {
                Apoli.LOGGER.warn("[Apoli] Custom model {} bone '{}' has unknown parent '{}'; treating it as a root bone.", id, bone.name, bone.parent);
            }
        }
        for (Bone bone : ordered) {
            bone.hoist = bone.attachment != null && bone.slot != null;
            if (bone.hoist && hasRotatedAncestor(bone)) {
                Apoli.LOGGER.warn("[Apoli] Custom model {} bone '{}' tracks the vanilla '{}' part, so it is lifted out of its rotated parent '{}'; the parent's rotation no longer applies to it.", id, bone.name, bone.slot, bone.parent);
            }
        }
        for (Bone bone : ordered) {
            if (bone.hoist) {
                bone.attachment = null;
            }
        }

        Set<String> built = new HashSet<>();
        Map<String, ModelPart> children = new LinkedHashMap<>();
        Vector3f origin = new Vector3f(0.0F, BEDROCK_FLOOR, 0.0F);
        for (Bone bone : ordered) {
            if (bone.attachment == null) {
                children.put(unique(children, bone.name), build(bone, origin, built, texWidth, texHeight));
            }
        }
        for (Bone bone : ordered) {
            if (!built.contains(bone.name)) {
                Apoli.LOGGER.warn("[Apoli] Custom model {} bone '{}' is part of a parent cycle; treating it as a root bone.", id, bone.name);
                children.put(unique(children, bone.name), build(bone, origin, built, texWidth, texHeight));
            }
        }

        List<CustomModel.Bone> all = new ArrayList<>(ordered.size());
        Map<String, List<CustomModel.Bone>> grouped = new HashMap<>(ordered.size() * 2);
        for (Bone bone : ordered) {
            if (bone.part == null) {
                continue;
            }
            bone.handle = new CustomModel.Bone(bone.part);
            all.add(bone.handle);
            String normalized = ModelParts.normalize(bone.name);
            grouped.computeIfAbsent(normalized, key -> new ArrayList<>(1)).add(bone.handle);
            if (bone.slot != null && !bone.slot.equals(normalized)) {
                grouped.computeIfAbsent(bone.slot, key -> new ArrayList<>(1)).add(bone.handle);
            }
        }

        Map<String, CustomModel.Bone[]> lookup = new HashMap<>(grouped.size() * 2);
        for (Map.Entry<String, List<CustomModel.Bone>> entry : grouped.entrySet()) {
            lookup.put(entry.getKey(), entry.getValue().toArray(new CustomModel.Bone[0]));
        }
        return new CustomModel(new ModelPart(List.of(), children), lookup,
            all.toArray(new CustomModel.Bone[0]));
    }

    private static boolean hasRotatedAncestor(Bone bone) {
        Bone parent = bone.attachment;
        for (int depth = 0; parent != null && depth < MAX_PARENT_DEPTH; depth++) {
            if (parent.rotX != 0.0F || parent.rotY != 0.0F || parent.rotZ != 0.0F) {
                return true;
            }
            parent = parent.attachment;
        }
        return parent != null;
    }

    private static ModelPart build(Bone bone, Vector3f parentPivot, Set<String> built, int texWidth, int texHeight) {
        built.add(bone.name);
        List<ModelPart.Cube> cubes = new ArrayList<>();
        Map<String, ModelPart> children = new LinkedHashMap<>();
        int rotated = 0;
        for (Cube cube : bone.cubes) {
            if (cube.rotX == 0.0F && cube.rotY == 0.0F && cube.rotZ == 0.0F) {
                cubes.add(bake(cube, bone.pivot, texWidth, texHeight));
                continue;
            }
            String name = unique(children, bone.name + "_r" + rotated++);
            children.put(name, posed(
                new ModelPart(List.of(bake(cube, cube.pivot, texWidth, texHeight)), Map.of()),
                cube.pivot.x - bone.pivot.x, bone.pivot.y - cube.pivot.y, cube.pivot.z - bone.pivot.z,
                cube.rotX, cube.rotY, cube.rotZ));
        }
        for (Bone child : bone.children) {
            if (child.attachment != bone || built.contains(child.name)) {
                continue;
            }
            children.put(unique(children, child.name), build(child, bone.pivot, built, texWidth, texHeight));
        }
        bone.part = posed(new ModelPart(cubes, children),
            bone.pivot.x - parentPivot.x, parentPivot.y - bone.pivot.y, bone.pivot.z - parentPivot.z,
            bone.rotX, bone.rotY, bone.rotZ);
        return bone.part;
    }

    private static ModelPart posed(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot) {
        part.setPos(x, y, z);
        part.setRotation(xRot, yRot, zRot);
        return part;
    }

    private static ModelPart.Cube bake(Cube cube, Vector3f pivot, int texWidth, int texHeight) {
        float x = cube.origin.x - pivot.x;
        float y = pivot.y - cube.origin.y - cube.size.y;
        float z = cube.origin.z - pivot.z;
        if (cube.faces != null) {
            return new PerFaceCube(cube.faces, x, y, z, cube.size.x, cube.size.y, cube.size.z,
                cube.inflate, cube.mirror, texWidth, texHeight);
        }
        return new PerFaceCube(cube.u, cube.v, x, y, z, cube.size.x, cube.size.y, cube.size.z,
            cube.inflate, cube.mirror, texWidth, texHeight);
    }

    private static String unique(Map<String, ModelPart> taken, String name) {
        if (!taken.containsKey(name)) {
            return name;
        }
        int suffix = 2;
        while (taken.containsKey(name + "_" + suffix)) {
            suffix++;
        }
        return name + "_" + suffix;
    }

    private static <T> Bone parseBone(Dynamic<T> json, ResourceLocation id) {
        Bone bone = new Bone();
        bone.name = json.get("name").asString().result()
            .orElseThrow(() -> new IllegalArgumentException("a bone is missing its 'name'"));
        bone.slot = ModelParts.slot(ModelParts.normalize(bone.name));
        bone.parent = json.get("parent").asString().result().orElse(null);
        bone.pivot = vector(json, "pivot");
        Vector3f rotation = vector(json, "rotation");
        bone.rotX = rotation.x * DEG_TO_RAD;
        bone.rotY = rotation.y * DEG_TO_RAD;
        bone.rotZ = rotation.z * DEG_TO_RAD;
        boolean mirror = json.get("mirror").asBoolean(false);
        float inflate = json.get("inflate").asNumber().result().map(Number::floatValue).orElse(0.0F);
        for (Dynamic<T> element : json.get("cubes").asList(java.util.function.Function.identity())) {
            bone.cubes.add(parseCube(element, mirror, inflate, id, bone.name));
        }
        return bone;
    }

    private static <T> Cube parseCube(Dynamic<T> json, boolean boneMirror, float boneInflate, ResourceLocation id, String boneName) {
        Cube cube = new Cube();
        cube.origin = vector(json, "origin");
        cube.size = vector(json, "size");
        cube.pivot = vector(json, "pivot");
        Vector3f rotation = vector(json, "rotation");
        cube.rotX = rotation.x * DEG_TO_RAD;
        cube.rotY = rotation.y * DEG_TO_RAD;
        cube.rotZ = rotation.z * DEG_TO_RAD;
        cube.inflate = json.get("inflate").asNumber().result().map(Number::floatValue).orElse(boneInflate);
        cube.mirror = json.get("mirror").asBoolean(boneMirror);
        Dynamic<T> uv = json.get("uv").result().orElse(null);
        if (uv != null) {
            List<Float> array = numbers(uv);
            if (!array.isEmpty()) {
                cube.u = array.size() > 0 ? array.get(0) : 0.0F;
                cube.v = array.size() > 1 ? array.get(1) : 0.0F;
            } else if (uv.getMapValues().result().isPresent()) {
                cube.faces = parseFaces(uv, cube.size, id, boneName);
            }
        }
        return cube;
    }

    private static <T> Map<Direction, float[]> parseFaces(Dynamic<T> json, Vector3f size, ResourceLocation id, String boneName) {
        Map<Direction, float[]> faces = new EnumMap<>(Direction.class);
        Map<Dynamic<T>, Dynamic<T>> entries = json.getMapValues().result().orElse(Map.of());
        for (Map.Entry<Dynamic<T>, Dynamic<T>> entry : entries.entrySet()) {
            String name = entry.getKey().asString().result().orElse(null);
            if (name == null) continue;
            Direction bedrock = Direction.byName(name);
            Dynamic<T> face = entry.getValue();
            if (bedrock == null || face.getMapValues().result().isEmpty()) {
                continue;
            }
            List<Float> offset = numbers(face.get("uv").result().orElse(null));
            if (offset.isEmpty()) {
                continue;
            }
            if (face.get("uv_rotation").result().isPresent()) {
                Apoli.LOGGER.warn("[Apoli] Custom model {} bone '{}' uses uv_rotation on the {} face; rotated face UVs are not supported and will render unrotated.", id, boneName, name);
            }
            Direction java = bedrock.getAxis() == Direction.Axis.Z ? bedrock : bedrock.getOpposite();
            float[] uv = new float[]{
                offset.size() > 0 ? offset.get(0) : 0.0F,
                offset.size() > 1 ? offset.get(1) : 0.0F,
                naturalWidth(java, size),
                naturalHeight(java, size)
            };
            List<Float> extent = numbers(face.get("uv_size").result().orElse(null));
            if (extent.size() > 0) {
                uv[2] = extent.get(0);
            }
            if (extent.size() > 1) {
                uv[3] = extent.get(1);
            }
            faces.put(java, uv);
        }
        return faces;
    }

    private static <T> List<Float> numbers(Dynamic<T> data) {
        if (data == null) return List.of();
        return data.asStreamOpt().result()
            .map(stream -> stream.map(element -> element.asNumber().result().map(Number::floatValue).orElse(0.0F))
                .toList())
            .orElse(List.of());
    }

    private static float naturalWidth(Direction face, Vector3f size) {
        return face.getAxis() == Direction.Axis.X ? size.z : size.x;
    }

    private static float naturalHeight(Direction face, Vector3f size) {
        return face.getAxis() == Direction.Axis.Y ? size.z : size.y;
    }

    private static <T> Vector3f vector(Dynamic<T> json, String key) {
        Vector3f out = new Vector3f();
        List<Float> array = numbers(json.get(key).result().orElse(null));
        if (array.size() > 0) {
            out.x = array.get(0);
        }
        if (array.size() > 1) {
            out.y = array.get(1);
        }
        if (array.size() > 2) {
            out.z = array.get(2);
        }
        return out;
    }

    private static final class Bone {
        String name;
        String parent;
        @Nullable
        String slot;
        @Nullable
        Bone attachment;
        boolean hoist;
        @Nullable
        ModelPart part;
        @Nullable
        CustomModel.Bone handle;
        Vector3f pivot = new Vector3f();
        float rotX;
        float rotY;
        float rotZ;
        final List<Bone> children = new ArrayList<>();
        final List<Cube> cubes = new ArrayList<>();
    }

    private static final class Cube {
        Vector3f origin = new Vector3f();
        Vector3f size = new Vector3f();
        Vector3f pivot = new Vector3f();
        float rotX;
        float rotY;
        float rotZ;
        float inflate;
        boolean mirror;
        float u;
        float v;
        Map<Direction, float[]> faces;
    }
}
