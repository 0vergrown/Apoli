package dev.overgrown.apoli.client.summon;

import dev.overgrown.apoli.entity.summon.CloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CloneModel extends PlayerModel<CloneEntity> {
    public CloneModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(CloneEntity clone, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        ItemStack main = clone.getMainHandItem();
        ItemStack off = clone.getOffhandItem();
        HumanoidArm arm = clone.getMainArm();

        if (main.is(Items.BOW) && clone.isAggressive()) {
            setPose(arm, HumanoidModel.ArmPose.BOW_AND_ARROW);
        } else if (main.getItem() instanceof CrossbowItem) {
            setPose(arm, clone.isCharging() ? HumanoidModel.ArmPose.CROSSBOW_CHARGE : HumanoidModel.ArmPose.CROSSBOW_HOLD);
        } else {
            if (!main.isEmpty()) setPose(arm, HumanoidModel.ArmPose.ITEM);
            if (!off.isEmpty()) setPose(arm.getOpposite(), HumanoidModel.ArmPose.ITEM);
        }

        super.setupAnim(clone, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    private void setPose(HumanoidArm arm, HumanoidModel.ArmPose pose) {
        if (arm == HumanoidArm.RIGHT) {
            this.rightArmPose = pose;
        } else {
            this.leftArmPose = pose;
        }
    }
}