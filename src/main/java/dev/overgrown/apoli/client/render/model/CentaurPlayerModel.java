package dev.overgrown.apoli.client.render.model;

import dev.overgrown.apoli.Apoli;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CentaurPlayerModel<T extends LivingEntity> extends PlayerModel<T>
    implements ExtraModelParts, ExtraTexturedModel {

    public static final ResourceLocation HORSE_TEXTURE = Apoli.id("textures/entity/centaur/horse.png");

    private static final float TORSO_Y = -7.0F;
    private static final float TORSO_Z = -2.0F;
    private static final float TAIL_PITCH = (float) Math.toRadians(32.5);
    private static final float FRONT_SWING = 0.8F;
    private static final float SPRINT_SWING = 0.2F;
    private static final float HIND_SWING = 0.5F;
    private static final float TAIL_SWING = 0.12F;
    private static final float MANE_SWING = 0.06F;
    private static final float GAIT = 0.6662F;
    private static final float HORSE_PIVOT_Y = 11.0F;
    private static final float HORSE_PIVOT_Z = 14.0F;
    private static final float WAIST_Y = 5.0F;
    private static final float WAIST_Z = -2.0F;
    private static final float RIDER_LEAN = 0.6F;
    private static final float LEAP_RISE_PITCH = -0.42F;
    private static final float LEAP_FALL_PITCH = 0.2F;
    private static final float LEAP_BLEND = 0.35F;
    private static final Map<Entity, Leap> LEAPS = new WeakHashMap<>();

    public final ModelPart horseBody;
    public final ModelPart horseMane;
    public final ModelPart horseTail;
    public final ModelPart frontLeftLeg;
    public final ModelPart frontRightLeg;
    public final ModelPart backLeftLeg;
    public final ModelPart backRightLeg;

    private final ModelPart cape;
    private final List<ModelPart> horseParts;
    private final List<ModelPart> riderParts;

    private static final class Leap {
        float air;
        float rise;
        float age = Float.NaN;
    }

    public CentaurPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.horseBody = root.getChild("horse_body");
        this.horseMane = root.getChild("horse_mane");
        this.horseTail = root.getChild("horse_tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.cape = root.getChild("cloak");
        this.horseParts = List.of(this.horseBody, this.horseMane, this.horseTail,
            this.frontLeftLeg, this.frontRightLeg, this.backLeftLeg, this.backRightLeg);
        this.riderParts = List.of(
            this.head, this.hat, this.body, this.jacket,
            this.rightArm, this.rightSleeve, this.leftArm, this.leftSleeve,
            this.rightLeg, this.rightPants, this.leftLeg, this.leftPants, this.cape);
    }

    public static LayerDefinition createLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("horse_body", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.05F)),
            PartPose.offset(0.0F, 11.0F, 14.0F));
        root.addOrReplaceChild("horse_mane", CubeListBuilder.create().texOffs(56, 36)
                .addBox(-1.0F, -4.0F, 0.01F, 2.0F, 9.0F, 2.0F),
            PartPose.offset(0.0F, 2.0F, -0.01F));
        root.addOrReplaceChild("horse_tail", CubeListBuilder.create().texOffs(42, 36)
                .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 6.0F, 16.0F, TAIL_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("front_left_leg", CubeListBuilder.create().texOffs(48, 21).mirror()
                .addBox(-3.0F, -1.0F, -1.9F, 4.0F, 11.0F, 4.0F),
            PartPose.offset(4.0F, 14.0F, -1.0F));
        root.addOrReplaceChild("front_right_leg", CubeListBuilder.create().texOffs(48, 21)
                .addBox(-1.0F, -1.0F, -1.9F, 4.0F, 11.0F, 4.0F),
            PartPose.offset(-4.0F, 14.0F, -1.0F));
        root.addOrReplaceChild("back_left_leg", CubeListBuilder.create().texOffs(48, 21).mirror()
                .addBox(-3.0F, -1.0F, -1.0F, 4.0F, 11.0F, 4.0F),
            PartPose.offset(4.0F, 14.0F, 16.0F));
        root.addOrReplaceChild("back_right_leg", CubeListBuilder.create().texOffs(48, 21)
                .addBox(-1.0F, -1.0F, -1.0F, 4.0F, 11.0F, 4.0F),
            PartPose.offset(-4.0F, 14.0F, 16.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (dev.overgrown.apoli.client.render.HandRenderPass.active()) {
            HandPassArms.reset(this);
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        for (int i = 0; i < this.riderParts.size(); i++) {
            this.riderParts.get(i).resetPose();
        }
        for (int i = 0; i < this.horseParts.size(); i++) {
            this.horseParts.get(i).resetPose();
        }

        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.rightPants.visible = false;
        this.leftPants.visible = false;

        Leap leap = leap(entity, ageInTicks);
        float air = leap.air;
        float ground = 1.0F - air;
        float rising = Math.max(leap.rise, 0.0F);
        float falling = Math.max(-leap.rise, 0.0F);
        float sprint = entity.isSprinting() ? 1.0F : 0.0F;
        float water = entity.isInWater() ? 0.2F : 1.0F;
        float stride = Mth.cos(water * limbSwing * GAIT + Mth.PI);
        float front = stride * (FRONT_SWING + SPRINT_SWING * sprint) * limbSwingAmount * ground;
        float hind = stride * HIND_SWING * limbSwingAmount * ground;
        float gallop = sprint * ground * limbSwingAmount;
        float pitch = air * (LEAP_RISE_PITCH * rising + LEAP_FALL_PITCH * falling)
            + gallop * Mth.cos(limbSwing * GAIT * 2.0F) * 0.05F;
        float bob = gallop * Mth.sin(limbSwing * GAIT * 2.0F) * 0.7F;
        float tuck = (-0.45F - 0.75F * rising + 0.1F * falling) * air;
        float kick = (0.3F + 0.55F * rising - 0.25F * falling) * air;

        this.frontLeftLeg.xRot = pitch + front + tuck;
        this.frontRightLeg.xRot = pitch - front + tuck + 0.12F * air;
        this.backLeftLeg.xRot = pitch - hind + kick;
        this.backRightLeg.xRot = pitch + hind + kick - 0.1F * air;
        this.horseBody.xRot = pitch;
        this.horseMane.xRot = pitch - Mth.abs(stride) * MANE_SWING * limbSwingAmount - 0.25F * air;
        this.horseTail.xRot = pitch + TAIL_PITCH + limbSwingAmount * 0.6F + 0.5F * air;
        this.horseTail.yRot = Mth.sin(ageInTicks * 0.06F) * TAIL_SWING * (1.0F - 0.5F * limbSwingAmount);

        float cos = Mth.cos(pitch);
        float sin = Mth.sin(pitch);
        for (int i = 0; i < this.horseParts.size(); i++) {
            ModelPart part = this.horseParts.get(i);
            if (part == this.horseBody) {
                part.y += bob;
                continue;
            }
            float dy = part.y - HORSE_PIVOT_Y;
            float dz = part.z - HORSE_PIVOT_Z;
            part.y = HORSE_PIVOT_Y + dy * cos - dz * sin + bob;
            part.z = HORSE_PIVOT_Z + dy * sin + dz * cos;
        }

        float wy = WAIST_Y - HORSE_PIVOT_Y;
        float wz = WAIST_Z - HORSE_PIVOT_Z;
        float waistY = HORSE_PIVOT_Y + wy * cos - wz * sin + bob;
        float waistZ = HORSE_PIVOT_Z + wy * sin + wz * cos;
        float lean = pitch * RIDER_LEAN;
        float leanCos = Mth.cos(lean);
        float leanSin = Mth.sin(lean);
        for (int i = 0; i < this.riderParts.size(); i++) {
            ModelPart part = this.riderParts.get(i);
            float dy = part.y + TORSO_Y - WAIST_Y;
            float dz = part.z + TORSO_Z - WAIST_Z;
            part.y = waistY + dy * leanCos - dz * leanSin;
            part.z = waistZ + dy * leanSin + dz * leanCos;
            part.xRot += lean;
        }
    }

    private static Leap leap(LivingEntity entity, float ageInTicks) {
        Leap leap = LEAPS.computeIfAbsent(entity, e -> new Leap());
        float dt = Float.isNaN(leap.age) ? 0.0F : Mth.clamp(ageInTicks - leap.age, 0.0F, 5.0F);
        leap.age = ageInTicks;
        boolean airborne = !entity.onGround() && !entity.isPassenger() && !entity.isInWater() && !entity.isFallFlying();
        float target = airborne ? 1.0F : 0.0F;
        float rise = Mth.clamp((float) (entity.getY() - entity.yo) / 0.35F, -1.0F, 1.0F);
        float blend = 1.0F - (float) Math.pow(1.0F - LEAP_BLEND, dt);
        leap.air += (target - leap.air) * blend;
        leap.rise += (rise - leap.rise) * blend;
        return leap;
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        for (int i = 0; i < this.horseParts.size(); i++) {
            this.horseParts.get(i).visible = visible;
        }
    }

    @Override
    public ResourceLocation defaultExtraTexture() {
        return HORSE_TEXTURE;
    }

    @Override
    public List<ModelPart> extraTexturedParts() {
        return this.horseParts;
    }

    @Override
    public void collectExtraParts(String normalized, List<ModelPart> parts) {
        switch (normalized) {
            case "horsebody" -> parts.add(this.horseBody);
            case "horsemane", "mane" -> parts.add(this.horseMane);
            case "horsetail", "tail" -> parts.add(this.horseTail);
            case "frontleftleg" -> parts.add(this.frontLeftLeg);
            case "frontrightleg" -> parts.add(this.frontRightLeg);
            case "backleftleg", "hindleftleg" -> parts.add(this.backLeftLeg);
            case "backrightleg", "hindrightleg" -> parts.add(this.backRightLeg);
            case "horselegs", "legs" -> {
                parts.add(this.frontLeftLeg);
                parts.add(this.frontRightLeg);
                parts.add(this.backLeftLeg);
                parts.add(this.backRightLeg);
            }
            case "horse", "lower" -> parts.addAll(this.horseParts);
            default -> {}
        }
    }

    @Override
    public void collectExtraParts(List<ModelPart> parts) {
        parts.addAll(this.horseParts);
    }
}
