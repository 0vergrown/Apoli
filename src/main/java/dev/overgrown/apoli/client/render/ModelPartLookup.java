package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.client.render.model.ExtraModelParts;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.PoseMath;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.List;

public final class ModelPartLookup {
    private ModelPartLookup() {}

    private static final ThreadLocal<List<ModelPart>> SCRATCH = ThreadLocal.withInitial(() -> new ArrayList<>(8));

    public static void resolveInto(HumanoidModel<?> model, BodyPart part, List<ModelPart> parts) {
        if (part.isEverything()) {
            allPartsInto(model, parts);
            return;
        }
        int mask = part.models();
        if (mask != 0) {
            if ((mask & BodyPart.HEAD) != 0) parts.add(model.head);
            if ((mask & BodyPart.HAT) != 0) parts.add(model.hat);
            if ((mask & BodyPart.BODY) != 0) parts.add(model.body);
            if ((mask & BodyPart.RIGHT_ARM) != 0) parts.add(model.rightArm);
            if ((mask & BodyPart.LEFT_ARM) != 0) parts.add(model.leftArm);
            if ((mask & BodyPart.RIGHT_LEG) != 0) parts.add(model.rightLeg);
            if ((mask & BodyPart.LEFT_LEG) != 0) parts.add(model.leftLeg);
            if (model instanceof PlayerModel<?> player) {
                if ((mask & BodyPart.JACKET) != 0) parts.add(player.jacket);
                if ((mask & BodyPart.RIGHT_SLEEVE) != 0) parts.add(player.rightSleeve);
                if ((mask & BodyPart.LEFT_SLEEVE) != 0) parts.add(player.leftSleeve);
                if ((mask & BodyPart.RIGHT_PANTS) != 0) parts.add(player.rightPants);
                if ((mask & BodyPart.LEFT_PANTS) != 0) parts.add(player.leftPants);
            }
        }
        if ((part.isCustom() || part.isGroup()) && model instanceof ExtraModelParts extra) {
            extra.collectExtraParts(part.key(), parts);
        }
    }

    public static boolean affects(HumanoidModel<?> model, BodyPart part, ModelPart target) {
        List<ModelPart> parts = SCRATCH.get();
        parts.clear();
        resolveInto(model, part, parts);
        boolean found = false;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i) == target) {
                found = true;
                break;
            }
        }
        parts.clear();
        return found;
    }

    public static void allPartsInto(HumanoidModel<?> model, List<ModelPart> parts) {
        parts.add(model.head);
        parts.add(model.hat);
        parts.add(model.body);
        parts.add(model.rightArm);
        parts.add(model.leftArm);
        parts.add(model.rightLeg);
        parts.add(model.leftLeg);
        if (model instanceof PlayerModel<?> player) {
            parts.add(player.jacket);
            parts.add(player.rightSleeve);
            parts.add(player.leftSleeve);
            parts.add(player.rightPants);
            parts.add(player.leftPants);
        }
        if (model instanceof ExtraModelParts extra) {
            extra.collectExtraParts(parts);
        }
    }

    public static void limbPosesInto(HumanoidModel<?> model, float[] poses) {
        read(model.head, poses, 0);
        read(model.body, poses, PoseMath.STRIDE);
        read(model.rightArm, poses, PoseMath.STRIDE * 2);
        read(model.leftArm, poses, PoseMath.STRIDE * 3);
        read(model.rightLeg, poses, PoseMath.STRIDE * 4);
        read(model.leftLeg, poses, PoseMath.STRIDE * 5);
    }

    public static void read(ModelPart part, float[] pose, int at) {
        pose[at + PoseMath.X] = part.x;
        pose[at + PoseMath.Y] = part.y;
        pose[at + PoseMath.Z] = part.z;
        pose[at + PoseMath.X_ROT] = part.xRot;
        pose[at + PoseMath.Y_ROT] = part.yRot;
        pose[at + PoseMath.Z_ROT] = part.zRot;
        pose[at + PoseMath.X_SCALE] = part.xScale;
        pose[at + PoseMath.Y_SCALE] = part.yScale;
        pose[at + PoseMath.Z_SCALE] = part.zScale;
    }

    public static void write(float[] pose, int at, ModelPart part) {
        part.x = pose[at + PoseMath.X];
        part.y = pose[at + PoseMath.Y];
        part.z = pose[at + PoseMath.Z];
        part.xRot = pose[at + PoseMath.X_ROT];
        part.yRot = pose[at + PoseMath.Y_ROT];
        part.zRot = pose[at + PoseMath.Z_ROT];
        part.xScale = pose[at + PoseMath.X_SCALE];
        part.yScale = pose[at + PoseMath.Y_SCALE];
        part.zScale = pose[at + PoseMath.Z_SCALE];
    }
}
