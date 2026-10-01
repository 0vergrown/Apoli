package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import dev.overgrown.apoli.client.CustomProjectileRenderer;
import dev.overgrown.apoli.client.model.BedrockAnimation;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.render.AnimationPlayback;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.CustomModelManager;
import dev.overgrown.apoli.client.render.DynamicTextures;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.data.ModelAnimation;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ProjectilePlan {
    enum Kind { VANILLA, GEOMETRY, ITEM, BILLBOARD }

    private static final CustomModel[] NO_MODELS = new CustomModel[0];
    private static final Layer[] NO_LAYERS = new Layer[0];

    static final ProjectilePlan VANILLA =
        new ProjectilePlan(Kind.VANILLA, List.of(), NO_MODELS, NO_LAYERS, null, ItemStack.EMPTY, null);

    final Kind kind;
    final List<GeometryRender> geometry;
    final CustomModel[] models;
    final Layer[] layers;
    @Nullable
    final Model model;
    final ItemStack stack;
    @Nullable
    final ResourceLocation texture;

    private ProjectilePlan(Kind kind, List<GeometryRender> geometry, CustomModel[] models, Layer[] layers,
                           @Nullable Model model, ItemStack stack, @Nullable ResourceLocation texture) {
        this.kind = kind;
        this.geometry = geometry;
        this.models = models;
        this.layers = layers;
        this.model = model;
        this.stack = stack;
        this.texture = texture;
    }

    static ProjectilePlan next(Minecraft mc, CustomProjectileEntity entity, @Nullable ProjectilePlan previous) {
        if (entity.displayFireAnimation() || entity.shouldShowName()
            || mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return VANILLA;
        }
        List<GeometryRender> geometry = CustomProjectileRenderer.resolveGeometry(entity);
        if (!geometry.isEmpty()) {
            if (previous != null && previous.kind == Kind.GEOMETRY && previous.sameGeometry(geometry, entity)) {
                return previous;
            }
            ProjectilePlan built = geometry(geometry, entity);
            if (built != null) return built;
        }
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            if (previous != null && previous.kind == Kind.ITEM && ItemStack.matches(previous.stack, stack)) {
                return previous;
            }
            Model model = ModCompat.VANILLIN ? VanillinItemModels.ground(entity.level(), stack) : null;
            return model == null ? VANILLA
                : new ProjectilePlan(Kind.ITEM, List.of(), NO_MODELS, NO_LAYERS, model, stack.copy(), null);
        }
        ResourceLocation texture = CustomProjectileRenderer.textureOf(entity);
        if (previous != null && previous.kind == Kind.BILLBOARD && texture.equals(previous.texture)) {
            return previous;
        }
        return new ProjectilePlan(Kind.BILLBOARD, List.of(), NO_MODELS, NO_LAYERS,
            ProjectileMeshes.billboardModel(texture), ItemStack.EMPTY, texture);
    }

    @Nullable
    private static ProjectilePlan geometry(List<GeometryRender> geometry, CustomProjectileEntity entity) {
        CustomModel[] models = new CustomModel[geometry.size()];
        List<Layer> layers = new ArrayList<>(geometry.size());
        for (int i = 0; i < geometry.size(); i++) {
            GeometryRender render = geometry.get(i);
            CustomModel model = CustomModelManager.get(render.model(), false);
            models[i] = model;
            if (model == null) continue;
            if (!ProjectileMeshes.supports(render.mode())) return VANILLA;
            ResourceLocation texture = DynamicTextures.resolve(render.texture(), entity);
            layers.add(new Layer(render, texture, ProjectileMeshes.geometry(model, render.mode(), texture), model));
        }
        if (layers.isEmpty()) return null;
        return new ProjectilePlan(Kind.GEOMETRY, List.copyOf(geometry), models, layers.toArray(NO_LAYERS),
            null, ItemStack.EMPTY, null);
    }

    private boolean sameGeometry(List<GeometryRender> current, CustomProjectileEntity entity) {
        if (!this.geometry.equals(current)) return false;
        for (int i = 0; i < current.size(); i++) {
            if (this.models[i] != CustomModelManager.get(current.get(i).model(), false)) return false;
        }
        for (Layer layer : this.layers) {
            if (!layer.texture.equals(DynamicTextures.resolve(layer.render.texture(), entity))) return false;
        }
        return true;
    }

    void tickAnimations(CustomProjectileEntity entity) {
        for (Layer layer : this.layers) {
            GeometryRender render = layer.render;
            ModelAnimation.Entry entry = AnimationPlayer.entry(entity, render);
            BedrockAnimation animation = entry == null ? null : AnimationPlayer.animation(entry);
            if (animation == null) {
                layer.clock = null;
                continue;
            }
            int start = AnimationPlayback.startTick(entity.getId(), render.model(), entry, entity.tickCount);
            AnimationPlayer.time(entry, animation,
                AnimationPlayback.elapsedSince(start, entity.tickCount, 0.0F, entry.speed()));
            AnimationClock clock = layer.clock;
            if (clock == null || clock.entry() != entry || clock.animation() != animation || clock.startTick() != start) {
                layer.clock = new AnimationClock(entry, animation, start);
            }
        }
    }

    record AnimationClock(ModelAnimation.Entry entry, BedrockAnimation animation, int startTick) {}

    static final class Layer {
        final GeometryRender render;
        final ResourceLocation texture;
        final ModelTree tree;
        final ModelPart skeleton;
        final CustomModel pose;
        volatile AnimationClock clock;

        Layer(GeometryRender render, ResourceLocation texture, ModelTree tree, CustomModel model) {
            this.render = render;
            this.texture = texture;
            this.tree = tree;
            Map<ModelPart, ModelPart> copies = new IdentityHashMap<>();
            this.skeleton = ProjectileMeshes.poseSkeleton(model.root(), copies);
            this.pose = model.remapped(this.skeleton, copies);
        }
    }
}
