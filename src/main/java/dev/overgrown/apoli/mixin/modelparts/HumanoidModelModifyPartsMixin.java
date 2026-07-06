package dev.overgrown.apoli.mixin.modelparts;

import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.builtin.ModifyModelPartsPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;


@Mixin(HumanoidModel.class)
@OnlyIn(Dist.CLIENT)
public abstract class HumanoidModelModifyPartsMixin {

    private static final String SETUP_ANIM = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V";

    @Unique
    private final Map<ModelPart, float[]> apoli$originals = new IdentityHashMap<>();
    @Unique
    private boolean apoli$hadPower;

    @Inject(method = SETUP_ANIM, at = @At("HEAD"))
    private void apoli$modifyPartsHead(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        boolean has = ModifyModelPartsPower.has(entity);
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
        if (!ModifyModelPartsPower.has(entity)) return;
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        for (ModelPartTransformation t : ModifyModelPartsPower.gather(entity)) {
            for (ModelPart part : ModelPartLookup.resolve(model, ModelParts.normalize(t.part()))) {
                apoli$apply(part, t);
            }
        }
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
    private void apoli$apply(ModelPart part, ModelPartTransformation t) {
        float[] o = apoli$originals.get(part);
        float value = t.value();
        boolean override = t.overrideAnimation();
        switch (t.type()) {
            case PITCH -> part.xRot = override ? value : part.xRot + value;
            case YAW -> part.yRot = override ? value : part.yRot + value;
            case ROLL -> part.zRot = override ? value : part.zRot + value;
            case VISIBLE -> part.visible = value != 0f;
            case HIDDEN -> part.skipDraw = value != 0f;
            case X_SCALE -> part.xScale = (o != null ? o[6] : 1f) + value;
            case Y_SCALE -> part.yScale = (o != null ? o[7] : 1f) + value;
            case Z_SCALE -> part.zScale = (o != null ? o[8] : 1f) + value;
            case PIVOT_X -> part.x += value;
            case PIVOT_Y -> part.y += value;
            case PIVOT_Z -> part.z += value;
        }
    }
}
