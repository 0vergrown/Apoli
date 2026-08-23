package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.client.model.BedrockAnimation;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ModelAnimation;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public final class AnimationPlayer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final ThreadLocal<float[]> SCRATCH = ThreadLocal.withInitial(() -> new float[3]);

    private AnimationPlayer() {}

    public static void apply(Entity entity, GeometryRender render, CustomModel model, float partialTick) {
        if (render.animations().isEmpty()) return;
        ModelAnimation.Entry entry = render.animations().get().select(new EntityCtx(entity, entity.level()));
        if (entry == null) return;
        BedrockAnimation animation = AnimationManager.get(entry.animation(), entry.name().orElse(null));
        if (animation == null) return;
        float elapsed = AnimationPlayback.elapsed(entity.getId(), render.model(), entry, entity.tickCount, partialTick);
        float time = animation.timeFor(elapsed, entry.loop().orElse(null));
        if (time < 0.0F) return;
        apply(model, animation, time);
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
