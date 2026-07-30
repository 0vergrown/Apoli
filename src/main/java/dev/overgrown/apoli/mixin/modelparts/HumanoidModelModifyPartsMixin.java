package dev.overgrown.apoli.mixin.modelparts;

import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.data.ModelPartTransformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(HumanoidModel.class)
@Environment(EnvType.CLIENT)
public abstract class HumanoidModelModifyPartsMixin {

    private static final String SETUP_ANIM = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V";

    @Unique
    private final Map<ModelPart, float[]> apoli$originals = new IdentityHashMap<>();
    @Unique
    private final List<ModelPart> apoli$scratch = new ArrayList<>(2);
    @Unique
    private boolean apoli$hadPower;

    @Inject(method = SETUP_ANIM, at = @At("HEAD"))
    private void apoli$modifyPartsHead(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        boolean has = !ModelPartAnimator.update(entity).isEmpty();
        if (has) {
            if (apoli$originals.isEmpty()) apoli$snapshot();
            apoli$restore();
        } else if (apoli$hadPower) {
            apoli$restore();
            apoli$originals.clear();
        }
        apoli$hadPower = has;
    }

    @Inject(method = SETUP_ANIM, at = @At("TAIL"))
    private void apoli$modifyPartsTail(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        List<ModelPartAnimator.Slot> slots = ModelPartAnimator.update(entity);
        if (slots.isEmpty()) return;
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        for (int s = 0; s < slots.size(); s++) {
            ModelPartAnimator.Slot slot = slots.get(s);
            if (slot.weight() <= 0.0F) continue;
            apoli$scratch.clear();
            ModelPartLookup.resolveInto(model, slot.transformation().normalizedPart(), apoli$scratch);
            for (int p = 0; p < apoli$scratch.size(); p++) {
                apoli$apply(apoli$scratch.get(p), slot);
            }
        }
        apoli$scratch.clear();
    }

    @Unique
    private void apoli$snapshot() {
        for (ModelPart part : ModelPartLookup.allParts((HumanoidModel<?>) (Object) this)) {
            apoli$originals.put(part, new float[]{
                part.x, part.y, part.z,
                part.xRot, part.yRot, part.zRot,
                part.xScale, part.yScale, part.zScale,
                part.visible ? 1f : 0f, part.skipDraw ? 1f : 0f
            });
        }
    }

    @Unique
    private void apoli$restore() {
        for (Map.Entry<ModelPart, float[]> entry : apoli$originals.entrySet()) {
            ModelPart part = entry.getKey();
            float[] o = entry.getValue();
            part.x = o[0]; part.y = o[1]; part.z = o[2];
            part.xRot = o[3]; part.yRot = o[4]; part.zRot = o[5];
            part.xScale = o[6]; part.yScale = o[7]; part.zScale = o[8];
            part.visible = o[9] != 0f;
            part.skipDraw = o[10] != 0f;
        }
    }

    @Unique
    private void apoli$apply(ModelPart part, ModelPartAnimator.Slot slot) {
        ModelPartTransformation t = slot.transformation();
        float[] o = apoli$originals.get(part);
        float value = slot.value();
        float weight = slot.weight();
        boolean override = t.overrideAnimation();
        switch (t.type()) {
            case PITCH -> part.xRot = override ? part.xRot + (value - part.xRot) * weight : part.xRot + value * weight;
            case YAW -> part.yRot = override ? part.yRot + (value - part.yRot) * weight : part.yRot + value * weight;
            case ROLL -> part.zRot = override ? part.zRot + (value - part.zRot) * weight : part.zRot + value * weight;
            case VISIBLE -> { if (weight >= 0.5f) part.visible = value != 0f; }
            case HIDDEN -> { if (weight >= 0.5f) part.skipDraw = value != 0f; }
            case X_SCALE -> part.xScale = (o != null ? o[6] : 1f) + value * weight;
            case Y_SCALE -> part.yScale = (o != null ? o[7] : 1f) + value * weight;
            case Z_SCALE -> part.zScale = (o != null ? o[8] : 1f) + value * weight;
            case PIVOT_X -> part.x += value * weight;
            case PIVOT_Y -> part.y += value * weight;
            case PIVOT_Z -> part.z += value * weight;
        }
    }
}
