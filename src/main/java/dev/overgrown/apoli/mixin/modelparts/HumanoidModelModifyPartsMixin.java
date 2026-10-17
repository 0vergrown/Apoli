package dev.overgrown.apoli.mixin.modelparts;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.access.PartVisibilityUndo;
import dev.overgrown.apoli.compat.entitymodelfeatures.EntityModelFeaturesCompat;
import dev.overgrown.apoli.client.render.AnimationPlayer;
import dev.overgrown.apoli.client.render.HandRenderPass;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.client.render.ModelPartEdits;
import dev.overgrown.apoli.data.ModelAnimation;
import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import dev.overgrown.apoli.data.ModelPartTimeline;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.FirstPersonFrame;
import dev.overgrown.apoli.data.HumanoidPose;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.data.PoseMath;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(HumanoidModel.class)
@OnlyIn(Dist.CLIENT)
public abstract class HumanoidModelModifyPartsMixin implements PartVisibilityUndo {

    private static final String SETUP_ANIM = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V";

    @Unique
    private final Map<ModelPart, float[]> apoli$originals = new IdentityHashMap<>();
    @Unique
    private final List<ModelPart> apoli$scratch = new ArrayList<>(2);
    @Unique
    private final float[] apoli$limbs = new float[HumanoidPose.PART_COUNT * PoseMath.STRIDE];
    @Unique
    private final float[] apoli$pose = new float[PoseMath.STRIDE];
    @Unique
    private final float[] apoli$pivot = new float[3];
    @Unique
    private final double[] apoli$vector = new double[3];
    @Unique
    private final float[] apoli$end = new float[3];
    @Unique
    private final float[] apoli$body = new float[PoseMath.STRIDE];
    @Unique
    private final double[] apoli$matrix = new double[FirstPersonFrame.MATRIX_SIZE];
    @Unique
    private ModelAnimation apoli$modelAnimation;
    @Unique
    private boolean apoli$hadPower;
    @Unique
    private boolean apoli$poseOverridden;
    @Unique
    private float apoli$savedSwimAmount;
    @Unique
    private boolean apoli$savedCrouching;
    @Unique
    private boolean apoli$savedRiding;
    @Unique
    private ModelPart[] apoli$visibilityParts = new ModelPart[4];
    @Unique
    private byte[] apoli$visibilityStates = new byte[4];
    @Unique
    private int apoli$visibilityCount;

    @Inject(method = SETUP_ANIM, at = @At("HEAD"))
    private void apoli$modifyPartsHead(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (apoli$visibilityCount > 0) apoli$undoPartVisibility();
        this.apoli$modelAnimation = (Object) this instanceof PlayerModel
            ? ModifyPlayerModelPower.firstActiveAnimations(entity)
            : null;
        boolean has = !ModelPartAnimator.update(entity).isEmpty() || this.apoli$modelAnimation != null;
        if (has) {
            if (apoli$originals.isEmpty()) apoli$snapshot();
            apoli$restore();
        } else if (apoli$hadPower) {
            apoli$restore();
            apoli$originals.clear();
        }
        apoli$hadPower = has;

        apoli$poseOverridden = ModelPartAnimator.overridesPose(entity);
        if (apoli$poseOverridden) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            apoli$savedSwimAmount = model.swimAmount;
            apoli$savedCrouching = model.crouching;
            apoli$savedRiding = model.riding;
            model.swimAmount = 0.0F;
            model.crouching = false;
            model.riding = false;
        }
    }

    @ModifyExpressionValue(method = SETUP_ANIM, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;getFallFlyingTicks()I"))
    private int apoli$neutralizeFallFlying(int original) {
        return apoli$poseOverridden ? 0 : original;
    }

    @Inject(method = SETUP_ANIM, at = @At("TAIL"))
    private void apoli$modifyPartsTail(LivingEntity entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        List<ModelPartTimeline.Slot> slots = ModelPartAnimator.update(entity);
        if (!slots.isEmpty()) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            boolean handPass = HandRenderPass.active();
            boolean handEdited = false;
            for (int s = 0; s < slots.size(); s++) {
                ModelPartTimeline.Slot slot = slots.get(s);
                if (slot.weight() <= 0.0F) continue;
                if (!slot.rendersIn(handPass)) continue;
                if (handPass && !handEdited) {
                    handEdited = true;
                    model.rightArm.xRot = 0.0F;
                    model.leftArm.xRot = 0.0F;
                    HandRenderPass.markEdited();
                }
                ModelPartTransformation transformation = slot.transformation();
                BodyPart part = transformation.bodyPart().sided(entity);
                apoli$scratch.clear();
                ModelPartLookup.resolveInto(model, part, apoli$scratch);
                if (transformation.type() == ModelPartTransformation.Type.REACH) {
                    apoli$reach(model, slot, handPass);
                    continue;
                }
                if (part.isGroup() && PoseMath.isSpatial(transformation.type())) {
                    apoli$applyGroup(model, part, slot);
                    continue;
                }
                boolean visibility = ModelPartEdits.changesVisibility(transformation.type());
                boolean hidesCustom = visibility && ModelPartEdits.hides(slot) && EntityModelFeaturesCompat.hasCustomParts();
                for (int p = 0; p < apoli$scratch.size(); p++) {
                    ModelPart member = apoli$scratch.get(p);
                    if (visibility) apoli$rememberVisibility(member);
                    ModelPartEdits.apply(member, slot, apoli$originals.get(member));
                    if (hidesCustom) apoli$hideCustomChildren(member);
                }
            }
            apoli$scratch.clear();
        }

        if (this.apoli$modelAnimation != null) {
            AnimationPlayer.apply((HumanoidModel<?>) (Object) this, entity, this.apoli$modelAnimation,
                ModifyPlayerModelPower.CANONICAL, Mth.clamp(h - entity.tickCount, 0.0F, 1.0F));
            this.apoli$modelAnimation = null;
        }

        if (apoli$poseOverridden) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            model.swimAmount = apoli$savedSwimAmount;
            model.crouching = apoli$savedCrouching;
            model.riding = apoli$savedRiding;
            apoli$poseOverridden = false;
        }
    }

    @Unique
    private void apoli$applyGroup(HumanoidModel<?> model, BodyPart part, ModelPartTimeline.Slot slot) {
        ModelPartTransformation transformation = slot.transformation();
        ModelPartLookup.limbPosesInto(model, apoli$limbs);
        HumanoidPose.groupPivot(part, transformation, apoli$limbs, apoli$vector, apoli$pivot);
        for (int p = 0; p < apoli$scratch.size(); p++) {
            ModelPart member = apoli$scratch.get(p);
            ModelPartLookup.read(member, apoli$pose, 0);
            PoseMath.applyGroup(transformation.type(), apoli$pose, 0, slot.value(), slot.weight(),
                transformation.overrideAnimation(), apoli$pivot[0], apoli$pivot[1], apoli$pivot[2]);
            ModelPartLookup.write(apoli$pose, 0, member);
        }
        apoli$scratch.clear();
    }

    @Unique
    private void apoli$reach(HumanoidModel<?> model, ModelPartTimeline.Slot slot, boolean handPass) {
        if (slot.hasTarget()) {
            float blend = Mth.clamp(slot.value(), 0.0F, 1.0F) * slot.weight();
            for (int p = 0; p < apoli$scratch.size(); p++) {
                ModelPart member = apoli$scratch.get(p);
                if (!ModelPartEdits.endPoint(member, apoli$end)) continue;
                ModelPartLookup.read(member, apoli$pose, 0);
                int side = handPass ? apoli$armSide(model, member) : 0;
                if (side != 0) {
                    FirstPersonFrame.reach(side > 0, apoli$pose, 0, apoli$end[0], apoli$end[1], apoli$end[2],
                        slot.targetX(), slot.targetY(), slot.targetZ(), blend, apoli$body, apoli$matrix);
                } else {
                    PoseMath.reach(apoli$pose, 0, apoli$end[0], apoli$end[1], apoli$end[2],
                        slot.targetX(), slot.targetY(), slot.targetZ(), blend);
                }
                ModelPartLookup.write(apoli$pose, 0, member);
            }
        }
        apoli$scratch.clear();
    }

    @Unique
    private static int apoli$armSide(HumanoidModel<?> model, ModelPart part) {
        if (part == model.rightArm) return 1;
        if (part == model.leftArm) return -1;
        if (model instanceof PlayerModel<?> player) {
            if (part == player.rightSleeve) return 1;
            if (part == player.leftSleeve) return -1;
        }
        return 0;
    }

    @Unique
    private void apoli$snapshot() {
        apoli$scratch.clear();
        ModelPartLookup.allPartsInto((HumanoidModel<?>) (Object) this, apoli$scratch);
        for (ModelPart part : apoli$scratch) {
            apoli$originals.put(part, ModelPartEdits.snapshot(part));
        }
        apoli$scratch.clear();
    }

    @Unique
    private void apoli$restore() {
        for (Map.Entry<ModelPart, float[]> entry : apoli$originals.entrySet()) {
            ModelPartEdits.restorePose(entry.getKey(), entry.getValue());
        }
    }

    @Unique
    private void apoli$rememberVisibility(ModelPart part) {
        for (int i = 0; i < apoli$visibilityCount; i++) {
            if (apoli$visibilityParts[i] == part) return;
        }
        if (apoli$visibilityCount == apoli$visibilityParts.length) {
            apoli$visibilityParts = Arrays.copyOf(apoli$visibilityParts, apoli$visibilityCount * 2);
            apoli$visibilityStates = Arrays.copyOf(apoli$visibilityStates, apoli$visibilityCount * 2);
        }
        apoli$visibilityParts[apoli$visibilityCount] = part;
        apoli$visibilityStates[apoli$visibilityCount] = (byte) ((part.visible ? 1 : 0) | (part.skipDraw ? 2 : 0));
        apoli$visibilityCount++;
    }

    @Unique
    private void apoli$hideCustomChildren(ModelPart part) {
        ModelPart[] custom = EntityModelFeaturesCompat.customChildren(part);
        for (int i = 0; i < custom.length; i++) {
            apoli$rememberVisibility(custom[i]);
            custom[i].visible = false;
        }
    }

    @Override
    public void apoli$undoPartVisibility() {
        for (int i = apoli$visibilityCount - 1; i >= 0; i--) {
            ModelPart part = apoli$visibilityParts[i];
            byte state = apoli$visibilityStates[i];
            part.visible = (state & 1) != 0;
            part.skipDraw = (state & 2) != 0;
            apoli$visibilityParts[i] = null;
        }
        apoli$visibilityCount = 0;
    }
}
