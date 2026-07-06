package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class MinionModel extends EntityModel<MinionEntity> {
    private final ModelPart bone;

    public MinionModel(ModelPart root) {
        this.bone = root.getChild("main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition bone = parts.addOrReplaceChild("main", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(24, 16).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(24, 28).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(0, 12).addBox(0.0F, -10.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        bone.addOrReplaceChild("flat3", CubeListBuilder.create()
                .texOffs(0, -12).addBox(0.0F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        bone.addOrReplaceChild("flat2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, -10.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        this.bone.render(pose, consumer, light, overlay, color);
    }
}
