package dev.overgrown.apoli.compat.flywheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.overgrown.apoli.client.CustomProjectileRenderer;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.compat.flywheel.mixin.ModelPartAccessor;
import dev.overgrown.apoli.data.RenderMode;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectileMeshes {
    private static final RendererReloadCache<MaterialKey, Material> MATERIALS =
        new RendererReloadCache<>(ProjectileMeshes::material);
    private static final RendererReloadCache<CustomModel, Map<ModelPart, Mesh>> PART_MESHES =
        new RendererReloadCache<>(ProjectileMeshes::compileParts);
    private static final RendererReloadCache<TreeKey, ModelTree> TREES =
        new RendererReloadCache<>(key -> tree(key.model().root(), PART_MESHES.get(key.model()), key.material()));
    private static final RendererReloadCache<ResourceLocation, Model> BILLBOARDS =
        new RendererReloadCache<>(ProjectileMeshes::billboard);

    private ProjectileMeshes() {}

    public static boolean supports(RenderMode mode) {
        return mode != RenderMode.ENERGY_SWIRL;
    }

    public static ModelTree geometry(CustomModel model, RenderMode mode, ResourceLocation texture) {
        return TREES.get(new TreeKey(model, MATERIALS.get(new MaterialKey(mode, texture))));
    }

    public static Model billboardModel(ResourceLocation texture) {
        return BILLBOARDS.get(texture);
    }

    public static Map<String, ModelPart> children(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).apoli$children();
    }

    public static ModelPart poseSkeleton(ModelPart part, Map<ModelPart, ModelPart> skeleton) {
        Map<String, ModelPart> children = children(part);
        Map<String, ModelPart> copies = new HashMap<>(children.size() * 2);
        for (Map.Entry<String, ModelPart> child : children.entrySet()) {
            copies.put(child.getKey(), poseSkeleton(child.getValue(), skeleton));
        }
        ModelPart copy = new ModelPart(List.of(), copies);
        copy.setInitialPose(part.getInitialPose());
        copy.resetPose();
        skeleton.put(part, copy);
        return copy;
    }

    private static Material material(MaterialKey key) {
        SimpleMaterial.Builder builder = SimpleMaterial.builder()
            .texture(key.texture())
            .mipmap(false)
            .useOverlay(false);
        switch (key.mode()) {
            case SOLID -> {
            }
            case CUTOUT -> builder.cutout(CutoutShaders.ONE_TENTH);
            case CUTOUT_NO_CULL -> builder.cutout(CutoutShaders.ONE_TENTH).backfaceCulling(false);
            case TRANSLUCENT -> builder.cutout(CutoutShaders.ONE_TENTH).transparency(Transparency.TRANSLUCENT)
                .backfaceCulling(false);
            case TRANSLUCENT_CULL -> builder.cutout(CutoutShaders.ONE_TENTH).transparency(Transparency.TRANSLUCENT);
            case EMISSIVE -> builder.cutout(CutoutShaders.ONE_TENTH).transparency(Transparency.TRANSLUCENT)
                .backfaceCulling(false).useLight(false);
            case EYES, ENERGY_SWIRL -> builder.transparency(Transparency.ADDITIVE).useLight(false).diffuse(false);
        }
        return builder.build();
    }

    private static Map<ModelPart, Mesh> compileParts(CustomModel model) {
        Map<ModelPart, Mesh> meshes = new IdentityHashMap<>();
        compile(model.root(), new PoseStack().last(), new MeshCollector(), meshes);
        return meshes;
    }

    private static void compile(ModelPart part, PoseStack.Pose identity, MeshCollector collector, Map<ModelPart, Mesh> meshes) {
        List<ModelPart.Cube> cubes = ((ModelPartAccessor) (Object) part).apoli$cubes();
        for (int i = 0; i < cubes.size(); i++) {
            cubes.get(i).compile(identity, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        }
        if (!collector.isEmpty()) {
            meshes.put(part, collector.build("apoli:custom_model"));
        }
        for (ModelPart child : children(part).values()) {
            compile(child, identity, collector, meshes);
        }
    }

    private static ModelTree tree(ModelPart part, Map<ModelPart, Mesh> meshes, Material material) {
        Map<String, ModelPart> children = children(part);
        Map<String, ModelTree> trees = new HashMap<>(children.size() * 2);
        for (Map.Entry<String, ModelPart> child : children.entrySet()) {
            trees.put(child.getKey(), tree(child.getValue(), meshes, material));
        }
        Mesh mesh = meshes.get(part);
        return new ModelTree(mesh == null ? null : new SingleMeshModel(mesh, material), part.getInitialPose(), trees);
    }

    private static Model billboard(ResourceLocation texture) {
        MeshCollector collector = new MeshCollector();
        CustomProjectileRenderer.billboardQuad(collector, new PoseStack().last(), LightTexture.FULL_BRIGHT);
        return new SingleMeshModel(collector.build("apoli:projectile_billboard"),
            MATERIALS.get(new MaterialKey(RenderMode.CUTOUT_NO_CULL, texture)));
    }

    private record MaterialKey(RenderMode mode, ResourceLocation texture) {}

    private record TreeKey(CustomModel model, Material material) {}
}
