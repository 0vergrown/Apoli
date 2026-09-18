package dev.overgrown.apoli.client.render.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class HandPassArms {
    private static final float PIVOT_X = 5.0F;
    private static final float PIVOT_Y = 2.0F;

    private HandPassArms() {}

    public static void reset(PlayerModel<?> model) {
        reset(model.rightArm, -PIVOT_X);
        reset(model.rightSleeve, -PIVOT_X);
        reset(model.leftArm, PIVOT_X);
        reset(model.leftSleeve, PIVOT_X);
    }

    private static void reset(ModelPart part, float x) {
        part.x = x;
        part.y = PIVOT_Y;
        part.z = 0.0F;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }
}
