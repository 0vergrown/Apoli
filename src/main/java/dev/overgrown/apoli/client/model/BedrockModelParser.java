package dev.overgrown.apoli.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockModelParser {
    private BedrockModelParser() {}

    public static LayerDefinition parse(JsonObject json) {
        JsonArray geometry = GsonHelper.getAsJsonArray(json, "minecraft:geometry");
        if (geometry.isEmpty()) {
            throw new IllegalArgumentException("minecraft:geometry is empty");
        }
        JsonObject geo = GsonHelper.convertToJsonObject(geometry.get(0), "minecraft:geometry[0]");
        JsonObject description = GsonHelper.getAsJsonObject(geo, "description", new JsonObject());
        int texWidth = GsonHelper.getAsInt(description, "texture_width", 64);
        int texHeight = GsonHelper.getAsInt(description, "texture_height", 64);

        Map<String, Bone> bones = new LinkedHashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(geo, "bones", new JsonArray())) {
            Bone bone = parseBone(GsonHelper.convertToJsonObject(element, "bone"));
            bones.put(bone.name, bone);
        }

        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> built = new LinkedHashMap<>();
        List<Bone> remaining = new ArrayList<>(bones.values());
        boolean progress = true;
        while (!remaining.isEmpty() && progress) {
            progress = false;
            List<Bone> next = new ArrayList<>();
            for (Bone bone : remaining) {
                PartDefinition parent = bone.parent == null ? mesh.getRoot() : built.get(bone.parent);
                if (parent == null && bone.parent != null && bones.containsKey(bone.parent)) {
                    next.add(bone);
                    continue;
                }
                if (parent == null) {
                    parent = mesh.getRoot();
                }
                built.put(bone.name, addBone(parent, bone, bones));
                progress = true;
            }
            remaining = next;
        }
        for (Bone bone : remaining) {
            built.put(bone.name, addBone(mesh.getRoot(), bone, bones));
        }
        return LayerDefinition.create(mesh, texWidth, texHeight);
    }

    private static PartDefinition addBone(PartDefinition parent, Bone bone, Map<String, Bone> bones) {
        Vector3f abs = javaPivot(bone);
        Vector3f parentAbs = bone.parent != null && bones.containsKey(bone.parent)
            ? javaPivot(bones.get(bone.parent))
            : new Vector3f();
        CubeListBuilder builder = CubeListBuilder.create();
        for (Cube cube : bone.cubes) {
            builder.mirror(cube.mirror).texOffs(cube.u, cube.v).addBox(
                cube.origin.x - bone.pivot.x,
                bone.pivot.y - cube.origin.y - cube.size.y,
                cube.origin.z - bone.pivot.z,
                cube.size.x, cube.size.y, cube.size.z,
                new CubeDeformation(cube.inflate));
        }
        builder.mirror(false);
        return parent.addOrReplaceChild(bone.name, builder,
            PartPose.offsetAndRotation(abs.x - parentAbs.x, abs.y - parentAbs.y, abs.z - parentAbs.z,
                bone.rotX, bone.rotY, bone.rotZ));
    }

    private static Vector3f javaPivot(Bone bone) {
        return new Vector3f(bone.pivot.x, 24.0F - bone.pivot.y, bone.pivot.z);
    }

    private static Bone parseBone(JsonObject json) {
        Bone bone = new Bone();
        bone.name = GsonHelper.getAsString(json, "name");
        bone.parent = GsonHelper.getAsString(json, "parent", null);
        float[] pivot = floats(json, "pivot");
        bone.pivot = new Vector3f(pivot[0], pivot[1], pivot[2]);
        float[] rotation = floats(json, "rotation");
        bone.rotX = (float) Math.toRadians(rotation[0]);
        bone.rotY = (float) Math.toRadians(rotation[1]);
        bone.rotZ = (float) Math.toRadians(rotation[2]);
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "cubes", new JsonArray())) {
            bone.cubes.add(parseCube(GsonHelper.convertToJsonObject(element, "cube")));
        }
        return bone;
    }

    private static Cube parseCube(JsonObject json) {
        Cube cube = new Cube();
        float[] origin = floats(json, "origin");
        cube.origin = new Vector3f(origin[0], origin[1], origin[2]);
        float[] size = floats(json, "size");
        cube.size = new Vector3f(size[0], size[1], size[2]);
        cube.inflate = GsonHelper.getAsFloat(json, "inflate", 0.0F);
        cube.mirror = GsonHelper.getAsBoolean(json, "mirror", false);
        JsonElement uv = json.get("uv");
        if (uv != null && uv.isJsonArray()) {
            JsonArray array = uv.getAsJsonArray();
            cube.u = array.get(0).getAsInt();
            cube.v = array.get(1).getAsInt();
        } else if (uv != null && uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            JsonObject face = faces.has("north") ? GsonHelper.getAsJsonObject(faces, "north", null) : firstFace(faces);
            if (face != null && face.has("uv")) {
                JsonArray array = GsonHelper.getAsJsonArray(face, "uv");
                cube.u = array.get(0).getAsInt();
                cube.v = array.get(1).getAsInt();
            }
        }
        return cube;
    }

    private static JsonObject firstFace(JsonObject faces) {
        for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                return entry.getValue().getAsJsonObject();
            }
        }
        return null;
    }

    private static float[] floats(JsonObject json, String key) {
        float[] out = new float[]{0.0F, 0.0F, 0.0F};
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < Math.min(3, array.size()); i++) {
                out[i] = array.get(i).getAsFloat();
            }
        }
        return out;
    }

    private static final class Bone {
        String name;
        String parent;
        Vector3f pivot = new Vector3f();
        float rotX;
        float rotY;
        float rotZ;
        final List<Cube> cubes = new ArrayList<>();
    }

    private static final class Cube {
        Vector3f origin = new Vector3f();
        Vector3f size = new Vector3f();
        float inflate;
        boolean mirror;
        int u;
        int v;
    }
}
