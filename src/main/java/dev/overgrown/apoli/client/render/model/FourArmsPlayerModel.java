package dev.overgrown.apoli.client.render.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class FourArmsPlayerModel<T extends LivingEntity> extends PlayerModel<T> implements ExtraModelParts {
    private static final float ARM_PITCH = (float) Math.toRadians(17.0);
    private static final float ARM_ROLL = (float) Math.toRadians(27.0);
    private static final float SECOND_ARM_ROLL = (float) Math.toRadians(17.0);
    private static final float SECOND_ARM_DROP = 3.0F;

    public final ModelPart rightSecondArm;
    public final ModelPart rightSecondSleeve;
    public final ModelPart leftSecondArm;
    public final ModelPart leftSecondSleeve;

    private final List<ModelPart> extraParts;
    private final List<ModelPart> bodyParts;

    public FourArmsPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.rightSecondArm = root.getChild("right_second_arm");
        this.rightSecondSleeve = root.getChild("right_second_sleeve");
        this.leftSecondArm = root.getChild("left_second_arm");
        this.leftSecondSleeve = root.getChild("left_second_sleeve");
        this.extraParts = List.of(this.rightSecondArm, this.leftSecondArm,
            this.rightSecondSleeve, this.leftSecondSleeve);
        List<ModelPart> parts = new ArrayList<>();
        super.bodyParts().forEach(parts::add);
        parts.addAll(this.extraParts);
        this.bodyParts = List.copyOf(parts);
    }

    public static LayerDefinition createLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;
        CubeDeformation sleeve = none.extend(0.25F);
        float width = slim ? 3.0F : 4.0F;
        float leftInset = -1.0F;
        float rightInset = slim ? -2.0F : -3.0F;
        float pivotY = slim ? 5.5F : 5.0F;
        root.addOrReplaceChild("left_second_arm", CubeListBuilder.create().texOffs(32, 48)
            .addBox(leftInset, -2.0F, -2.0F, width, 12.0F, 4.0F, none), PartPose.offset(5.0F, pivotY, 0.0F));
        root.addOrReplaceChild("right_second_arm", CubeListBuilder.create().texOffs(40, 16)
            .addBox(rightInset, -2.0F, -2.0F, width, 12.0F, 4.0F, none), PartPose.offset(-5.0F, pivotY, 0.0F));
        root.addOrReplaceChild("left_second_sleeve", CubeListBuilder.create().texOffs(48, 48)
            .addBox(leftInset, -2.0F, -2.0F, width, 12.0F, 4.0F, sleeve), PartPose.offset(5.0F, pivotY, 0.0F));
        root.addOrReplaceChild("right_second_sleeve", CubeListBuilder.create().texOffs(40, 32)
            .addBox(rightInset, -2.0F, -2.0F, width, 12.0F, 4.0F, sleeve), PartPose.offset(-5.0F, pivotY, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return this.bodyParts;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (dev.overgrown.apoli.client.render.HandRenderPass.active()) {
            HandPassArms.reset(this);
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        for (int i = 0; i < this.bodyParts.size(); i++) {
            this.bodyParts.get(i).resetPose();
        }

        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.rightArm.xRot -= ARM_PITCH;
        this.rightSleeve.xRot -= ARM_PITCH;
        this.rightArm.zRot += ARM_ROLL;
        this.rightSleeve.zRot += ARM_ROLL;
        this.leftArm.xRot -= ARM_PITCH;
        this.leftSleeve.xRot -= ARM_PITCH;
        this.leftArm.zRot -= ARM_ROLL;
        this.leftSleeve.zRot -= ARM_ROLL;

        this.rightSecondArm.copyFrom(this.rightArm);
        this.leftSecondArm.copyFrom(this.leftArm);
        this.rightSecondSleeve.copyFrom(this.rightSleeve);
        this.leftSecondSleeve.copyFrom(this.leftSleeve);

        this.rightSecondArm.zRot -= SECOND_ARM_ROLL;
        this.rightSecondSleeve.zRot -= SECOND_ARM_ROLL;
        this.leftSecondArm.zRot += SECOND_ARM_ROLL;
        this.leftSecondSleeve.zRot += SECOND_ARM_ROLL;

        this.rightSecondArm.y += SECOND_ARM_DROP;
        this.leftSecondArm.y += SECOND_ARM_DROP;
        this.rightSecondSleeve.y += SECOND_ARM_DROP;
        this.leftSecondSleeve.y += SECOND_ARM_DROP;
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        for (int i = 0; i < this.extraParts.size(); i++) {
            this.extraParts.get(i).visible = visible;
        }
    }

    @Override
    public void collectExtraParts(String normalized, List<ModelPart> parts) {
        switch (normalized) {
            case "rightsecondarm", "secondrightarm" -> {
                parts.add(this.rightSecondArm);
                parts.add(this.rightSecondSleeve);
            }
            case "leftsecondarm", "secondleftarm" -> {
                parts.add(this.leftSecondArm);
                parts.add(this.leftSecondSleeve);
            }
            case "rightsecondsleeve", "secondrightsleeve" -> parts.add(this.rightSecondSleeve);
            case "leftsecondsleeve", "secondleftsleeve" -> parts.add(this.leftSecondSleeve);
            default -> {}
        }
    }

    @Override
    public void collectExtraParts(List<ModelPart> parts) {
        parts.addAll(this.extraParts);
    }
}
