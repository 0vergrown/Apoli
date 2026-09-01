package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.model.BedrockAnimation;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.data.ModelAnimation;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AnimationPlayer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final ThreadLocal<float[]> SCRATCH = ThreadLocal.withInitial(() -> new float[3]);
    private static final Map<ResourceLocation, Set<String>> WARNED = new HashMap<>();

    private AnimationPlayer() {}

    public static void clearWarnings() {
        WARNED.clear();
    }

    public static void apply(Entity entity, GeometryRender render, CustomModel model, float partialTick) {
        if (render.animations().isEmpty()) return;
        ModelAnimation.Entry entry = render.animations().get().select(entity);
        if (entry == null) {
            if (warnOnce(render.model(), "")) {
                Apoli.LOGGER.warn("[Apoli] No animation entry for the custom model '{}' matched, so it stays in its bind pose. "
                    + "Every entry has a condition and none of them passed — add a last entry with no condition as the fallback.",
                    render.model());
            }
            return;
        }
        BedrockAnimation animation = AnimationManager.get(entry.animation(), entry.name().orElse(null));
        if (animation == null) return;
        if (animation.length() <= 0.0F) {
            String still = entry.name().orElse("");
            if (warnOnce(entry.animation(), "static:" + still)) {
                Apoli.LOGGER.warn("[Apoli] The animation '{}'{} holds a single pose and never moves — every channel is one value with no timecode, "
                    + "and the file declares no 'animation_length'. Apoli is applying that pose. If you meant it to move, the clip needs keyframes at "
                    + "more than one time in Blockbench.",
                    entry.animation(), still.isEmpty() ? "" : " ('" + still + "')");
            }
        }
        float elapsed = AnimationPlayback.elapsed(entity.getId(), render.model(), entry, entity.tickCount, partialTick);
        float time = animation.timeFor(elapsed, entry.loop().orElse(null));
        if (time < 0.0F) {
            String clip = entry.name().orElse("");
            if (warnOnce(entry.animation(), clip)) {
                Apoli.LOGGER.warn("[Apoli] The animation '{}'{} has already finished and does not loop, so the model is back in its bind pose. "
                    + "Blockbench writes no loop flag for a 'Play Once' animation, and playback only restarts when the selected entry changes — "
                    + "set the clip to Loop in Blockbench, or put \"loop\": true on the animations entry.",
                    entry.animation(), clip.isEmpty() ? "" : " ('" + clip + "')");
            }
            return;
        }
        apply(model, animation, time);
    }

    private static boolean warnOnce(ResourceLocation id, String detail) {
        return WARNED.computeIfAbsent(id, key -> new HashSet<>(2)).add(detail);
    }

    public static void apply(CustomModel model, BedrockAnimation animation, float time) {
        BedrockAnimation.Bone[] bones = animation.bones();
        float[] scratch = SCRATCH.get();
        for (int i = 0; i < bones.length; i++) {
            BedrockAnimation.Bone bone = bones[i];
            CustomModel.Bone[] bound = model.bones(bone.name);
            if (bound.length == 0) continue;
            for (int j = 0; j < bound.length; j++) {
                ModelPart part = bound[j].part;
                if (bone.position != null) {
                    bone.position.sample(time, scratch);
                    part.x += scratch[0];
                    part.y -= scratch[1];
                    part.z += scratch[2];
                }
                if (bone.rotation != null) {
                    bone.rotation.sample(time, scratch);
                    part.xRot += scratch[0] * DEG_TO_RAD;
                    part.yRot += scratch[1] * DEG_TO_RAD;
                    part.zRot += scratch[2] * DEG_TO_RAD;
                }
                if (bone.scale != null) {
                    bone.scale.sample(time, scratch);
                    part.xScale *= scratch[0];
                    part.yScale *= scratch[1];
                    part.zScale *= scratch[2];
                }
            }
        }
    }
}
