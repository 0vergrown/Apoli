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

public class StinkFlyPlayerModel<T extends LivingEntity> extends PlayerModel<T> implements ExtraModelParts {
    private static final float ABDOMEN_PITCH = -1.0036F;
    private static final float THORAX_PITCH = 0.4363F;
    private static final float SECOND_LEG_PITCH = -0.3491F;
    private static final float ARM_PITCH = -0.5236F;

    public final ModelPart rightSecondLeg;
    public final ModelPart rightSecondPants;
    public final ModelPart leftSecondLeg;
    public final ModelPart leftSecondPants;

    private final List<ModelPart> extraParts;
    private final List<ModelPart> bodyParts;

    public StinkFlyPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.rightSecondLeg = root.getChild("right_second_leg");
        this.rightSecondPants = root.getChild("right_second_pants");
        this.leftSecondLeg = root.getChild("left_second_leg");
        this.leftSecondPants = root.getChild("left_second_pants");
        this.extraParts = List.of(this.rightSecondLeg, this.rightSecondPants,
            this.leftSecondLeg, this.leftSecondPants);
        List<ModelPart> parts = new ArrayList<>();
        super.bodyParts().forEach(parts::add);
        parts.addAll(this.extraParts);
        this.bodyParts = List.copyOf(parts);
    }

    public static LayerDefinition createLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;
        CubeDeformation overlay = new CubeDeformation(0.25F);

        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, none), PartPose.offset(0.0F, 5.0F, -2.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 5.0F, -2.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 5.5F));
        body.addOrReplaceChild("lower_r1", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, none), PartPose.offsetAndRotation(0.0F, 1.0F, 4.5F, ABDOMEN_PITCH, 0.0F, 0.0F));
        body.addOrReplaceChild("upper_r1", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, none), PartPose.offsetAndRotation(0.0F, -1.0F, -4.5F, THORAX_PITCH, 0.0F, 0.0F));

        PartDefinition jacket = root.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 5.5F));
        jacket.addOrReplaceChild("lower_r2", CubeListBuilder.create().texOffs(16, 32)
            .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(0.0F, 1.0F, 4.5F, ABDOMEN_PITCH, 0.0F, 0.0F));
        jacket.addOrReplaceChild("upper_r2", CubeListBuilder.create().texOffs(16, 32)
            .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(0.0F, -1.0F, -4.5F, THORAX_PITCH, 0.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
            .addBox(-4.0F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-4.0F, 12.0F, 3.0F));
        root.addOrReplaceChild("right_pants", CubeListBuilder.create().texOffs(0, 32)
            .addBox(-4.0F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.offset(-4.0F, 12.0F, 3.0F));
        root.addOrReplaceChild("right_second_leg", CubeListBuilder.create().texOffs(0, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offsetAndRotation(-4.0F, 12.0F, 0.0F, SECOND_LEG_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("right_second_pants", CubeListBuilder.create().texOffs(0, 32)
            .addBox(-4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(-4.0F, 12.0F, 0.0F, SECOND_LEG_PITCH, 0.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
            .addBox(0.0F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(4.0F, 12.0F, 3.0F));
        root.addOrReplaceChild("left_pants", CubeListBuilder.create().texOffs(0, 48)
            .addBox(0.0F, 0.0F, -1.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.offset(4.0F, 12.0F, 3.0F));
        root.addOrReplaceChild("left_second_leg", CubeListBuilder.create().texOffs(16, 48)
            .addBox(0.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offsetAndRotation(4.0F, 12.0F, 0.0F, SECOND_LEG_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("left_second_pants", CubeListBuilder.create().texOffs(0, 48)
            .addBox(0.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(4.0F, 12.0F, 0.0F, SECOND_LEG_PITCH, 0.0F, 0.0F));

        float width = slim ? 3.0F : 4.0F;
        float rightInset = slim ? -3.0F : -4.0F;
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
            .addBox(slim ? 0.0F : -1.0F, -2.0F, -2.0F, width, 12.0F, 4.0F, none), PartPose.offsetAndRotation(4.0F, 6.0F, -1.5F, ARM_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48)
            .addBox(slim ? 0.0F : -1.0F, -2.0F, -2.0F, width, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(4.0F, 6.0F, -1.5F, ARM_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
            .addBox(rightInset, -2.0F, -2.0F, width, 12.0F, 4.0F, none), PartPose.offsetAndRotation(-4.0F, 6.0F, -2.0F, ARM_PITCH, 0.0F, 0.0F));
        root.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32)
            .addBox(rightInset, -2.0F, -2.0F, width, 12.0F, 4.0F, overlay), PartPose.offsetAndRotation(-4.0F, 6.0F, -2.0F, ARM_PITCH, 0.0F, 0.0F));

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
        this.head.resetPose();

        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        copyRotation(this.rightLeg, this.rightSecondLeg);
        copyRotation(this.leftLeg, this.leftSecondLeg);
        copyRotation(this.rightPants, this.rightSecondPants);
        copyRotation(this.leftPants, this.leftSecondPants);

        this.rightSecondPants.visible = this.rightPants.visible;
        this.leftSecondPants.visible = this.leftPants.visible;

        this.body.y += this.crouching ? 9.0F : 12.0F;
        this.jacket.y += this.crouching ? 9.0F : 12.0F;

        this.head.y += 5.0F;
        this.hat.y += 5.0F;

        this.rightLeg.z += 3.0F;
        this.rightLeg.y -= this.crouching ? 2.0F : 0.0F;
        this.rightPants.z += 3.0F;
        this.rightPants.y -= this.crouching ? 2.0F : 0.0F;
        this.rightSecondLeg.z += this.crouching ? 4.0F : 0.0F;
        this.rightSecondPants.z += this.crouching ? 4.0F : 0.0F;

        this.leftLeg.z += 3.0F;
        this.leftLeg.y -= this.crouching ? 2.0F : 0.0F;
        this.leftPants.z += 3.0F;
        this.leftPants.y -= this.crouching ? 2.0F : 0.0F;
        this.leftSecondLeg.z += this.crouching ? 4.0F : 0.0F;
        this.leftSecondPants.z += this.crouching ? 4.0F : 0.0F;

        this.rightArm.y += 4.0F;
        this.rightArm.x += 1.0F;
        this.rightSleeve.y += 4.0F;
        this.rightSleeve.x += 1.0F;
        this.leftArm.y += 4.0F;
        this.leftArm.x -= 1.0F;
        this.leftSleeve.y += 4.0F;
        this.leftSleeve.x -= 1.0F;

        for (int i = 0; i < this.bodyParts.size(); i++) {
            ModelPart part = this.bodyParts.get(i);
            PartPose pose = part.getInitialPose();
            part.xRot += pose.xRot;
            part.yRot += pose.yRot;
            part.zRot += pose.zRot;
        }
    }

    private static void copyRotation(ModelPart from, ModelPart to) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
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
            case "rightsecondleg", "secondrightleg" -> {
                parts.add(this.rightSecondLeg);
                parts.add(this.rightSecondPants);
            }
            case "leftsecondleg", "secondleftleg" -> {
                parts.add(this.leftSecondLeg);
                parts.add(this.leftSecondPants);
            }
            case "rightsecondpants", "secondrightpants" -> parts.add(this.rightSecondPants);
            case "leftsecondpants", "secondleftpants" -> parts.add(this.leftSecondPants);
            default -> {}
        }
    }

    @Override
    public void collectExtraParts(List<ModelPart> parts) {
        parts.addAll(this.extraParts);
    }
}
