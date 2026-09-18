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
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class CentaurPlayerModel<T extends LivingEntity> extends PlayerModel<T>
    implements ExtraModelParts, ExtraTexturedModel {

    public static final ResourceLocation HORSE_TEXTURE = Apoli.id("textures/entity/centaur/horse.png");

    private static final float TORSO_Y = -7.0F;
    private static final float TORSO_Z = -2.0F;
    private static final float TAIL_PITCH = (float) Math.toRadians(32.5);
    private static final float LEG_SWING = 0.55F;
    private static final float TAIL_SWING = 0.12F;
    private static final float MANE_SWING = 0.06F;
    private static final float GAIT = 0.6662F;

    public final ModelPart horseBody;
    public final ModelPart horseMane;
    public final ModelPart horseTail;
    public final ModelPart frontLeftLeg;
    public final ModelPart frontRightLeg;
    public final ModelPart backLeftLeg;
    public final ModelPart backRightLeg;

    private final List<ModelPart> horseParts;
    private final List<ModelPart> riderParts;

    public CentaurPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.horseBody = root.getChild("horse_body");
        this.horseMane = root.getChild("horse_mane");
        this.horseTail = root.getChild("horse_tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.horseParts = List.of(this.horseBody, this.horseMane, this.horseTail,
            this.frontLeftLeg, this.frontRightLeg, this.backLeftLeg, this.backRightLeg);
        this.riderParts = List.of(
            this.head, this.hat, this.body, this.jacket,
            this.rightArm, this.rightSleeve, this.leftArm, this.leftSleeve,
            this.rightLeg, this.rightPants, this.leftLeg, this.leftPants);
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

        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.rightPants.visible = false;
        this.leftPants.visible = false;

        for (int i = 0; i < this.riderParts.size(); i++) {
            ModelPart part = this.riderParts.get(i);
            part.y += TORSO_Y;
            part.z += TORSO_Z;
        }

        float swing = Mth.cos(limbSwing * GAIT) * LEG_SWING * limbSwingAmount;
        this.frontLeftLeg.xRot = -swing;
        this.backRightLeg.xRot = -swing;
        this.frontRightLeg.xRot = swing;
        this.backLeftLeg.xRot = swing;

        this.horseTail.xRot = TAIL_PITCH + Mth.abs(swing) * TAIL_SWING;
        this.horseTail.yRot = Mth.sin(ageInTicks * 0.06F) * TAIL_SWING;
        this.horseMane.xRot = -Mth.abs(swing) * MANE_SWING;
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
            case "horselegs" -> {
                parts.add(this.frontLeftLeg);
                parts.add(this.frontRightLeg);
                parts.add(this.backLeftLeg);
                parts.add(this.backRightLeg);
            }
            case "horse" -> parts.addAll(this.horseParts);
            default -> {}
        }
    }

    @Override
    public void collectExtraParts(List<ModelPart> parts) {
        parts.addAll(this.horseParts);
    }
}
