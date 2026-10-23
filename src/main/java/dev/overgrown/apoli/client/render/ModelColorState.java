package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ModelColorState {
    private ModelColorState() {}

    private static final int MAX_DEPTH = 8;
    private static final ClientPowerState.TypeHolders HOLDERS = ClientPowerState.holders(ModelColorPower.CANONICAL);
    private static final ThreadLocal<Frames> FRAMES = ThreadLocal.withInitial(Frames::new);
    private static final ThreadLocal<List<ModelColorPower.PartColor>> SCRATCH =
        ThreadLocal.withInitial(() -> new ArrayList<>(4));
    private static volatile boolean active;

    public static void begin(Entity rendered, EntityModel<?> model) {
        Frame frame = FRAMES.get().push();
        Entity source = ClientDisguiseManager.powerSource(rendered);
        frame.rendered = rendered;
        if (frame.resolve(source) && !frame.partColors.isEmpty() && model instanceof HumanoidModel<?> humanoid) {
            frame.map.fill(source, humanoid, frame.partColors, null, null);
        }
        active = frame.map.map() != null;
    }

    public static void beginHand(Entity player, HumanoidModel<?> model, ModelPart arm, ModelPart sleeve) {
        Frame frame = FRAMES.get().push();
        frame.rendered = player;
        if (frame.resolve(player)) {
            if (!frame.partColors.isEmpty()) frame.map.fill(player, model, frame.partColors, arm, sleeve);
            if (frame.tinted) {
                frame.map.multiply(arm, frame.whole);
                frame.map.multiply(sleeve, frame.whole);
            }
        }
        active = frame.map.map() != null;
    }

    public static void end() {
        Frame top = FRAMES.get().pop();
        active = top != null && top.map.map() != null;
    }

    public static boolean isActive() {
        return active;
    }

    @Nullable
    public static float[] colorFor(ModelPart part) {
        Frame top = FRAMES.get().top();
        if (top == null) return null;
        Map<ModelPart, float[]> map = top.map.map();
        return map == null ? null : map.get(part);
    }

    public static float alphaFor(@Nullable ModelPart part) {
        if (!active) return 1f;
        float[] color = colorFor(part);
        return color == null ? 1f : color[3];
    }

    @Nullable
    public static float[] tint() {
        Frame top = FRAMES.get().top();
        return top == null || !top.tinted ? null : top.whole;
    }

    public static boolean whitens() {
        Frame top = FRAMES.get().top();
        return top != null && top.tinted && top.whole[4] >= 1f;
    }

    public static float minAlpha(Entity rendered) {
        Frame top = FRAMES.get().top();
        if (top != null && top.rendered == rendered) return top.minAlpha;
        Entity source = ClientDisguiseManager.powerSource(rendered);
        return held(source) ? ModelColorPower.minAlpha(source) : 1f;
    }

    public static float[] whole(@Nullable Entity source) {
        Frame top = FRAMES.get().top();
        if (top != null && top.source == source) return top.tinted ? top.whole : ModelColorPower.IDENTITY;
        return held(source) ? ModelColorPower.colorFor(source) : ModelColorPower.IDENTITY;
    }

    public static List<ModelColorPower.PartColor> partColors(@Nullable Entity source) {
        Frame top = FRAMES.get().top();
        if (top != null && top.source == source) return top.partColors;
        List<ModelColorPower.PartColor> scratch = SCRATCH.get();
        scratch.clear();
        if (held(source)) ModelColorPower.collectPartColors(source, scratch);
        return scratch;
    }

    public static void attach(ModelPart part, float[] colours, int at) {
        Frame top = FRAMES.get().top();
        if (top == null) return;
        top.map.attach(part, colours, at);
        active = true;
    }

    private static boolean held(@Nullable Entity source) {
        return source != null && HOLDERS.contains(source.getId());
    }

    private static final class Frame {
        private final float[] whole = new float[5];
        private final List<ModelColorPower.PartColor> partColors = new ArrayList<>(4);
        private final PartColorMap map = new PartColorMap();
        @Nullable
        private Entity rendered;
        @Nullable
        private Entity source;
        private boolean tinted;
        private float minAlpha = 1f;

        private boolean resolve(@Nullable Entity source) {
            this.source = source;
            this.tinted = false;
            this.minAlpha = 1f;
            this.partColors.clear();
            this.map.reset();
            if (!held(source) || !ModelColorPower.resolve(source, this.whole, this.partColors)) return false;
            this.tinted = this.whole[0] != 1f || this.whole[1] != 1f || this.whole[2] != 1f
                || this.whole[3] != 1f || this.whole[4] != 0f;
            float min = this.whole[3];
            for (int i = 0; i < this.partColors.size(); i++) {
                min = Math.min(min, this.partColors.get(i).tint().alpha(source));
            }
            this.minAlpha = min;
            return true;
        }
    }

    private static final class Frames {
        private Frame[] frames = new Frame[2];
        private int depth;

        private Frame push() {
            if (this.depth >= MAX_DEPTH) this.depth = 0;
            if (this.depth == this.frames.length) this.frames = Arrays.copyOf(this.frames, this.depth * 2);
            Frame frame = this.frames[this.depth];
            if (frame == null) {
                frame = new Frame();
                this.frames[this.depth] = frame;
            }
            this.depth++;
            return frame;
        }

        @Nullable
        private Frame pop() {
            if (this.depth > 0) this.depth--;
            return top();
        }

        @Nullable
        private Frame top() {
            return this.depth == 0 ? null : this.frames[this.depth - 1];
        }
    }
}
