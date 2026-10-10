package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.access.DualSwingHolder;
import dev.overgrown.apoli.power.builtin.ShowBothArmsPower;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class GhostArmTrails {
    private GhostArmTrails() {}

    private static final int X = 0;
    private static final int Z = 1;
    private static final int X_ROT = 2;
    private static final int Y_ROT = 3;
    private static final int Z_ROT = 4;

    private static final ShowBothArmsPower.Arms ARMS = new ShowBothArmsPower.Arms();
    private static final float[] RIGHT_ARM = new float[5];
    private static final float[] LEFT_ARM = new float[5];
    private static final float[] RIGHT_SLEEVE = new float[5];
    private static final float[] LEFT_SLEEVE = new float[5];

    public static void render(HumanoidModel<?> model, LivingEntity entity, PoseStack poseStack,
                              MultiBufferSource buffers, ResourceLocation texture, int light) {
        float attack = model.attackTime;
        if (attack <= 0.0F) return;
        if (!ShowBothArmsPower.resolve(entity, ARMS) || !ARMS.ghostsIn(false)) return;
        float spacing = ARMS.spacing;
        if (spacing <= 0.0F) return;

        HumanoidArm attacking = entity.swingingArm == InteractionHand.MAIN_HAND
            ? entity.getMainArm()
            : entity.getMainArm().getOpposite();
        boolean dual = DualSwingHolder.of(entity);
        int edits = ArmEdits.of(entity, false);
        boolean rightSwings = (dual || attacking == HumanoidArm.RIGHT) && !ArmEdits.held(edits, HumanoidArm.RIGHT);
        boolean leftSwings = (dual || attacking == HumanoidArm.LEFT) && !ArmEdits.held(edits, HumanoidArm.LEFT);
        if (!rightSwings && !leftSwings) return;
        boolean leftLead = attacking == HumanoidArm.LEFT;
        float headPitch = model.head.xRot;
        PlayerModel<?> player = model instanceof PlayerModel<?> playerModel ? playerModel : null;

        snapshot(model.rightArm, RIGHT_ARM);
        snapshot(model.leftArm, LEFT_ARM);
        if (player != null) {
            snapshot(player.rightSleeve, RIGHT_SLEEVE);
            snapshot(player.leftSleeve, LEFT_SLEEVE);
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        int color = FastColor.ARGB32.colorFromFloat(ARMS.alpha, 1.0F, 1.0F, 1.0F);
        float yawNow = bodyYaw(attack, leftLead);
        float liftNow = lift(attack, headPitch);
        float rollNow = roll(attack);
        try {
            for (int n = ARMS.ghosts; n >= 1; n--) {
                float lag = spacing * n;
                if (lag >= 1.0F) continue;
                float then = attack - lag;
                if (then <= 0.0F) then += 1.0F;
                float yawThen = bodyYaw(then, leftLead);
                float yaw = yawThen - yawNow;
                float lift = lift(then, headPitch) - liftNow;
                float roll = roll(then) - rollNow;
                float across = (Mth.cos(yawThen) - Mth.cos(yawNow)) * 5.0F;
                float forward = (Mth.sin(yawThen) - Mth.sin(yawNow)) * 5.0F;
                if (rightSwings) {
                    lag(model.rightArm, RIGHT_ARM, -across, forward, -lift, yaw * 3.0F, roll);
                    model.rightArm.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
                    if (player != null) {
                        lag(player.rightSleeve, RIGHT_SLEEVE, -across, forward, -lift, yaw * 3.0F, roll);
                        player.rightSleeve.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
                    }
                }
                if (leftSwings) {
                    lag(model.leftArm, LEFT_ARM, across, -forward, yaw - lift, yaw * 3.0F, roll);
                    model.leftArm.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
                    if (player != null) {
                        lag(player.leftSleeve, LEFT_SLEEVE, across, -forward, yaw - lift, yaw * 3.0F, roll);
                        player.leftSleeve.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, color);
                    }
                }
            }
        } finally {
            restore(model.rightArm, RIGHT_ARM);
            restore(model.leftArm, LEFT_ARM);
            if (player != null) {
                restore(player.rightSleeve, RIGHT_SLEEVE);
                restore(player.leftSleeve, LEFT_SLEEVE);
            }
        }
    }

    private static float bodyYaw(float attack, boolean leftLead) {
        float yaw = Mth.sin(Mth.sqrt(attack) * (float) (Math.PI * 2)) * 0.2F;
        return leftLead ? -yaw : yaw;
    }

    private static float lift(float attack, float headPitch) {
        float eased = 1.0F - attack;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        return Mth.sin(eased * (float) Math.PI) * 1.2F
            + Mth.sin(attack * (float) Math.PI) * -(headPitch - 0.7F) * 0.75F;
    }

    private static float roll(float attack) {
        return Mth.sin(attack * (float) Math.PI) * -0.4F;
    }

    private static void snapshot(ModelPart part, float[] out) {
        out[X] = part.x;
        out[Z] = part.z;
        out[X_ROT] = part.xRot;
        out[Y_ROT] = part.yRot;
        out[Z_ROT] = part.zRot;
    }

    private static void lag(ModelPart part, float[] now, float dx, float dz, float dxRot, float dyRot, float dzRot) {
        part.x = now[X] + dx;
        part.z = now[Z] + dz;
        part.xRot = now[X_ROT] + dxRot;
        part.yRot = now[Y_ROT] + dyRot;
        part.zRot = now[Z_ROT] + dzRot;
    }

    private static void restore(ModelPart part, float[] now) {
        part.x = now[X];
        part.z = now[Z];
        part.xRot = now[X_ROT];
        part.yRot = now[Y_ROT];
        part.zRot = now[Z_ROT];
    }
}
