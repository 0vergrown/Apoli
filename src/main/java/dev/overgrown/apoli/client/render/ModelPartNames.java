package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.mixin.custom_model.AgeableListModelPartsInvoker;
import dev.overgrown.apoli.mixin.custom_model.ModelPartChildrenAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class ModelPartNames {
    private static final ModelPart[] NO_CHAIN = new ModelPart[0];
    private static final Named EMPTY = new Named(Map.of(), Map.of());
    private static final Map<ModelPart, String> BAKED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EntityModel<?>, Named> BY_MODEL = Collections.synchronizedMap(new WeakHashMap<>());

    public record Named(Map<String, ModelPart> parts, Map<ModelPart, ModelPart[]> chains) {
        public ModelPart[] chain(ModelPart part) {
            ModelPart[] chain = chains.get(part);
            return chain == null ? NO_CHAIN : chain;
        }
    }

    private ModelPartNames() {}

    public static void recordBaked(ModelPart parent) {
        Map<String, ModelPart> children = ((ModelPartChildrenAccessor) (Object) parent).apoli$children();
        if (children.isEmpty()) return;
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            BAKED.put(entry.getValue(), entry.getKey());
        }
    }

    public static Map<String, ModelPart> of(EntityModel<?> model) {
        return named(model).parts();
    }

    public static Named named(EntityModel<?> model) {
        Named known = BY_MODEL.get(model);
        if (known != null) return known;
        Named built = build(model);
        BY_MODEL.put(model, built);
        return built;
    }

    private static Named build(EntityModel<?> model) {
        Map<String, ModelPart> parts = new LinkedHashMap<>();
        Map<ModelPart, ModelPart[]> chains = new IdentityHashMap<>();
        List<ModelPart> path = new ArrayList<>(8);
        if (model instanceof HierarchicalModel<?> hierarchical) {
            ModelPart root = hierarchical.root();
            path.add(root);
            walk(root, path, parts, chains);
        } else if (model instanceof AgeableListModel<?> ageable) {
            AgeableListModelPartsInvoker invoker = (AgeableListModelPartsInvoker) ageable;
            addTopLevel(invoker.apoli$headParts(), path, parts, chains);
            addTopLevel(invoker.apoli$bodyParts(), path, parts, chains);
        } else if (model instanceof ListModel<?> list) {
            addTopLevel(list.parts(), path, parts, chains);
        }
        if (parts.isEmpty()) return EMPTY;
        return new Named(Collections.unmodifiableMap(parts), chains);
    }

    private static void addTopLevel(Iterable<ModelPart> top, List<ModelPart> path, Map<String, ModelPart> parts,
                                    Map<ModelPart, ModelPart[]> chains) {
        for (ModelPart part : top) {
            String name = BAKED.get(part);
            if (name != null && parts.putIfAbsent(ModelParts.normalize(name), part) == null) {
                chains.put(part, NO_CHAIN);
            }
            path.add(part);
            walk(part, path, parts, chains);
            path.remove(path.size() - 1);
        }
    }

    private static void walk(ModelPart parent, List<ModelPart> path, Map<String, ModelPart> parts,
                             Map<ModelPart, ModelPart[]> chains) {
        Map<String, ModelPart> children = ((ModelPartChildrenAccessor) (Object) parent).apoli$children();
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            ModelPart child = entry.getValue();
            if (parts.putIfAbsent(ModelParts.normalize(entry.getKey()), child) == null) {
                chains.put(child, path.toArray(new ModelPart[0]));
            }
            path.add(child);
            walk(child, path, parts, chains);
            path.remove(path.size() - 1);
        }
    }
}
