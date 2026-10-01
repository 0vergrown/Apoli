package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.render.AnimationPlayback;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.GeometryRenderer;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Map;

public final class CustomProjectileVisual extends AbstractEntityVisual<CustomProjectileEntity> implements SimpleDynamicVisual {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final LayerInstances[] NONE = new LayerInstances[0];

    private final Matrix4f matrix = new Matrix4f();
    private volatile ProjectilePlan plan;
    @Nullable
    private ProjectilePlan built;
    private LayerInstances[] layers = NONE;
    @Nullable
    private TransformedInstance single;
    private boolean shown = true;
    private int light = -1;

    public CustomProjectileVisual(VisualizationContext ctx, CustomProjectileEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
    }

    void prepare(Minecraft mc) {
        ProjectilePlan next = ProjectilePlan.next(mc, this.entity, this.plan);
        if (next != this.plan) this.plan = next;
        if (next.kind == ProjectilePlan.Kind.GEOMETRY) next.tickAnimations(this.entity);
    }

    boolean drawn() {
        ProjectilePlan current = this.plan;
        return current != null && current.kind != ProjectilePlan.Kind.VANILLA;
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        try {
            draw(ctx);
        } catch (RuntimeException | LinkageError e) {
            deleteInstances();
            CustomProjectileVisualizer.disable(e);
        }
    }

    private void draw(DynamicVisual.Context ctx) {
        if (CustomProjectileVisualizer.disabled()) {
            deleteInstances();
            return;
        }
        ProjectilePlan current = this.plan;
        if (current != this.built) rebuild(current);
        if (current == null || current.kind == ProjectilePlan.Kind.VANILLA) return;
        boolean visible = isVisible(ctx.frustum());
        if (visible != this.shown) show(visible);
        if (!visible) return;
        float partialTick = ctx.partialTick();
        int packed = computePackedLight(partialTick);
        boolean relight = packed != this.light;
        this.light = packed;
        Vec3i origin = renderOrigin();
        float x = (float) (Mth.lerp(partialTick, this.entity.xOld, this.entity.getX()) - origin.getX());
        float y = (float) (Mth.lerp(partialTick, this.entity.yOld, this.entity.getY()) - origin.getY());
        float z = (float) (Mth.lerp(partialTick, this.entity.zOld, this.entity.getZ()) - origin.getZ());
        TransformedInstance instance = this.single;
        if (instance != null) {
            instance.setIdentityTransform().translate(x, y, z).rotate(ctx.camera().rotation()).rotateY((float) Math.PI);
            if (relight) instance.light(packed);
            instance.setChanged();
            return;
        }
        float yaw = (180.0F - Mth.lerp(partialTick, this.entity.yRotO, this.entity.getYRot())) * DEG_TO_RAD;
        float pitch = Mth.lerp(partialTick, this.entity.xRotO, this.entity.getXRot()) * DEG_TO_RAD;
        for (LayerInstances layer : this.layers) {
            this.matrix.translation(x, y, z).rotateY(yaw).rotateX(pitch).scale(-1.0F, -1.0F, 1.0F).translate(0.0F, -1.5F, 0.0F);
            layer.draw(this.entity, partialTick, packed, relight, this.matrix);
        }
    }

    private void rebuild(@Nullable ProjectilePlan current) {
        deleteInstances();
        this.built = current;
        this.shown = true;
        this.light = -1;
        if (current == null || current.kind == ProjectilePlan.Kind.VANILLA) return;
        if (current.model != null) {
            this.single = instancerProvider().instancer(InstanceTypes.TRANSFORMED, current.model).createInstance();
            return;
        }
        LayerInstances[] made = new LayerInstances[current.layers.length];
        for (int i = 0; i < made.length; i++) {
            ProjectilePlan.Layer layer = current.layers[i];
            made[i] = new LayerInstances(layer, InstanceTree.create(instancerProvider(), layer.tree));
        }
        this.layers = made;
    }

    private void show(boolean visible) {
        this.shown = visible;
        if (this.single != null) this.single.setVisible(visible);
        for (LayerInstances layer : this.layers) {
            layer.tree.visible(visible);
        }
    }

    private void deleteInstances() {
        if (this.single != null) {
            this.single.delete();
            this.single = null;
        }
        for (LayerInstances layer : this.layers) {
            layer.tree.delete();
        }
        this.layers = NONE;
    }

    @Override
    protected void _delete() {
        deleteInstances();
        CustomProjectileVisualizer.forget(this.entity, this);
    }

    private static final class LayerInstances {
        private final ProjectilePlan.Layer layer;
        private final InstanceTree tree;
        private final Link root;

        private LayerInstances(ProjectilePlan.Layer layer, InstanceTree tree) {
            this.layer = layer;
            this.tree = tree;
            this.root = Link.of(layer.skeleton, tree);
            GeometryRender render = layer.render;
            tree.traverse(instance -> instance.color(render.red(), render.green(), render.blue(), render.alpha()));
        }

        private void draw(CustomProjectileEntity entity, float partialTick, int light, boolean relight, Matrix4f base) {
            CustomModel pose = this.layer.pose;
            GeometryRenderer.resetAll(pose);
            ProjectilePlan.AnimationClock clock = this.layer.clock;
            if (clock != null) {
                float time = clock.animation().timeFor(
                    AnimationPlayback.elapsedSince(clock.startTick(), entity.tickCount, partialTick, clock.entry().speed()),
                    clock.entry().loop().orElse(null));
                if (time >= 0.0F) AnimationPlayer.apply(pose, clock.animation(), time);
            }
            GeometryRenderer.applyVisibility(pose, this.layer.render.bodyParts());
            this.root.sync();
            if (relight) this.tree.traverse(instance -> instance.light(light));
            dev.overgrown.apoli.data.Vector offset = this.layer.render.offset();
            if (offset.x() != 0.0F || offset.y() != 0.0F || offset.z() != 0.0F) {
                base.translate(offset.x(), -offset.y(), -offset.z());
            }
            float scale = this.layer.render.scale();
            if (scale != 1.0F) base.scale(scale);
            this.tree.updateInstances(base);
        }
    }

    private static final class Link {
        private final ModelPart part;
        private final InstanceTree node;
        private final Link[] children;

        private Link(ModelPart part, InstanceTree node, Link[] children) {
            this.part = part;
            this.node = node;
            this.children = children;
        }

        private static Link of(ModelPart part, InstanceTree node) {
            Map<String, ModelPart> parts = ProjectileMeshes.children(part);
            Link[] links = new Link[parts.size()];
            int i = 0;
            for (Map.Entry<String, ModelPart> child : parts.entrySet()) {
                links[i++] = of(child.getValue(), node.childOrThrow(child.getKey()));
            }
            return new Link(part, node, links);
        }

        private void sync() {
            if (!this.part.visible) {
                if (this.node.visible()) this.node.visible(false);
                return;
            }
            if (!this.node.visible()) this.node.visible(true);
            this.node.pos(this.part.x, this.part.y, this.part.z);
            this.node.rotation(this.part.xRot, this.part.yRot, this.part.zRot);
            this.node.scale(this.part.xScale, this.part.yScale, this.part.zScale);
            for (Link child : this.children) {
                child.sync();
            }
        }
    }
}
