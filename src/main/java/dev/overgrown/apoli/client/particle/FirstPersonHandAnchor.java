package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overgrown.apoli.client.render.ArmEdits;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.FirstPersonFrame;
import dev.overgrown.apoli.data.HumanoidPose;
import dev.overgrown.apoli.data.ModelPartAnchor;
import dev.overgrown.apoli.mixin.firstperson.GameRendererFovInvoker;
import dev.overgrown.apoli.mixin.firstperson.ItemInHandRendererAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class FirstPersonHandAnchor {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float PI = (float) Math.PI;

    private FirstPersonHandAnchor() {}

    @Nullable
    public static ModelPartAnchor.Frame frame(Minecraft mc, Entity entity, BodyPart part, Vec3 base,
                                             float partialTick) {
        LocalPlayer player = mc.player;
        if (player == null || entity != player || mc.getCameraEntity() != player) return null;
        if (!mc.options.getCameraType().isFirstPerson() || player.isSleeping() || player.isScoping()
            || player.isSpectator()) {
            return null;
        }
        BodyPart sided = part.sided(player);
        if (sided.isGroup() || !sided.hasPoint()) return null;
        HumanoidArm arm;
        int limb;
        if (sided.limbs() == BodyPart.LIMB_RIGHT_ARM) {
            arm = HumanoidArm.RIGHT;
            limb = HumanoidPose.RIGHT_ARM;
        } else if (sided.limbs() == BodyPart.LIMB_LEFT_ARM) {
            arm = HumanoidArm.LEFT;
            limb = HumanoidPose.LEFT_ARM;
        } else {
            return null;
        }

        boolean main = arm == player.getMainArm();
        InteractionHand hand = main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemInHandRendererAccessor renderer =
            (ItemInHandRendererAccessor) mc.getEntityRenderDispatcher().getItemInHandRenderer();
        ItemStack stack = main ? renderer.apoli$mainHandItem() : renderer.apoli$offHandItem();
        float height = main
            ? Mth.lerp(partialTick, renderer.apoli$oMainHandHeight(), renderer.apoli$mainHandHeight())
            : Mth.lerp(partialTick, renderer.apoli$oOffHandHeight(), renderer.apoli$offHandHeight());
        int edits = ArmEdits.of(player, true);
        boolean held = ArmEdits.held(edits, arm);
        float equip = held ? 0.0F : 1.0F - height;
        InteractionHand swinging = player.swingingArm == null ? InteractionHand.MAIN_HAND : player.swingingArm;
        float swing = held || swinging != hand ? 0.0F : player.getAttackAnim(partialTick);
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        PoseStack pose = new PoseStack();
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        pose.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));

        Vector4f at;
        Matrix3f linear;
        float xRot = 0.0F;
        float yRot = 0.0F;
        float zRot = 0.0F;
        if (stack.isEmpty() || (ArmEdits.edited(edits, arm) && !stack.is(Items.FILLED_MAP))) {
            FirstPersonFrame.arm(pose.last().pose(), side, equip, swing);
            HumanoidPose arms = HumanoidPose.firstPersonArms(player, ModelPartAnimator.timeline(),
                ModelPartAnimator.now(player));
            float[] point = new float[3];
            sided.pointInto(arms.poses(), new double[3], point);
            linear = new Matrix3f(pose.last().pose());
            at = pose.last().pose().transform(new Vector4f(point[0] * PIXEL, point[1] * PIXEL, point[2] * PIXEL, 1.0F));
            xRot = arms.xRot(limb);
            yRot = arms.yRot(limb);
            zRot = arms.zRot(limb);
        } else {
            itemTransform(pose, side, equip, swing);
            linear = new Matrix3f(pose.last().pose()).scale(-1.0F, -1.0F, 1.0F);
            at = pose.last().pose().transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        GameRendererFovInvoker fov = (GameRendererFovInvoker) mc.gameRenderer;
        double ratio = Math.tan(Math.toRadians(fov.apoli$getFov(camera, partialTick, true)) * 0.5)
            / Math.tan(Math.toRadians(fov.apoli$getFov(camera, partialTick, false)) * 0.5);
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vector3f look = camera.getLookVector();
        Matrix3f viewToWorld = new Matrix3f(-left.x, -left.y, -left.z, up.x, up.y, up.z, -look.x, -look.y, -look.z);
        Vector3f offset = viewToWorld.transform((float) (at.x * ratio), (float) (at.y * ratio), at.z, new Vector3f());
        Vec3 position = camera.getPosition().add(offset.x, offset.y, offset.z);
        return ModelPartAnchor.Frame.world(position.subtract(base), xRot, yRot, zRot,
            viewToWorld.mul(linear, new Matrix3f()));
    }

    private static void itemTransform(PoseStack pose, float side, float equip, float swing) {
        float root = Mth.sqrt(swing);
        pose.translate(side * -0.4F * Mth.sin(root * PI), 0.2F * Mth.sin(root * PI * 2.0F),
            -0.2F * Mth.sin(swing * PI));
        pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        float twist = Mth.sin(swing * swing * PI);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45.0F + twist * -20.0F)));
        float arc = Mth.sin(root * PI);
        pose.mulPose(Axis.ZP.rotationDegrees(side * arc * -20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(arc * -80.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }
}
