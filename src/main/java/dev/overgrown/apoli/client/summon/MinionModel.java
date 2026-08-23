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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class MinionModel extends EntityModel<MinionEntity> {
    public static final String MAIN = "main";
    public static final String FLAT_2 = "flat2";
    public static final String FLAT_3 = "flat3";
    public static final String[] BONES = {MAIN, FLAT_2, FLAT_3};

    private final ModelPart main;
    private final ModelPart flat2;
    private final ModelPart flat3;

    public MinionModel(ModelPart root) {
        this.main = root.getChild(MAIN);
        this.flat2 = this.main.getChild(FLAT_2);
        this.flat3 = this.main.getChild(FLAT_3);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition bone = parts.addOrReplaceChild(MAIN, CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(24, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(24, 28).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(0, 12).addBox(0.0F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offset(0.0F, 20.0F, 0.0F));
        bone.addOrReplaceChild(FLAT_3, CubeListBuilder.create()
                .texOffs(0, -12).addBox(0.0F, -6.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        bone.addOrReplaceChild(FLAT_2, CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, -10.0F, -6.0F, 0.0F, 12.0F, 12.0F),
            PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Nullable
    public ModelPart bone(String name) {
        return switch (name) {
            case MAIN -> this.main;
            case FLAT_2 -> this.flat2;
            case FLAT_3 -> this.flat3;
            default -> null;
        };
    }

    @Override
    public void setupAnim(MinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.main.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.main.xRot = headPitch * Mth.DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        this.main.render(pose, consumer, light, overlay, red, green, blue, alpha);
    }
}
