package dev.overgrown.apoli.data;

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FirstPersonFrame {
    private FirstPersonFrame() {}

    public static final int MATRIX_SIZE = 18;

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float SHOULDER_X = -5.0F;
    private static final float SHOULDER_Y = 2.0F;
    private static final int TRANSLATION = 9;
    private static final int PRODUCT = 9;

    private static final double[] RIGHT_TO_BODY = toBody(1.0F);
    private static final double[] LEFT_TO_BODY = toBody(-1.0F);

    public static Matrix4f arm(Matrix4f frame, float side, float equip, float swing) {
        float root = Mth.sqrt(swing);
        frame.translate(side * (-0.3F * Mth.sin(root * Mth.PI) + 0.64000005F),
            0.4F * Mth.sin(root * Mth.TWO_PI) + -0.6F + equip * -0.6F,
            -0.4F * Mth.sin(swing * Mth.PI) + -0.71999997F);
        frame.rotateY(side * 45.0F * Mth.DEG_TO_RAD);
        frame.rotateY(side * Mth.sin(root * Mth.PI) * 70.0F * Mth.DEG_TO_RAD);
        frame.rotateZ(side * Mth.sin(swing * swing * Mth.PI) * -20.0F * Mth.DEG_TO_RAD);
        frame.translate(side * -1.0F, 3.6F, 3.5F);
        frame.rotateZ(side * 120.0F * Mth.DEG_TO_RAD);
        frame.rotateX(200.0F * Mth.DEG_TO_RAD);
        frame.rotateY(side * -135.0F * Mth.DEG_TO_RAD);
        frame.translate(side * 5.6F, 0.0F, 0.0F);
        return frame;
    }

    public static void reach(boolean right, float[] pose, int at, float endX, float endY, float endZ,
                             float targetX, float targetY, float targetZ, float blend,
                             float[] body, double[] matrix) {
        if (blend <= 0.0F) return;
        double[] toBody = right ? RIGHT_TO_BODY : LEFT_TO_BODY;
        double x = pose[at + PoseMath.X];
        double y = pose[at + PoseMath.Y];
        double z = pose[at + PoseMath.Z];
        body[PoseMath.X] = (float) (toBody[0] * x + toBody[1] * y + toBody[2] * z + toBody[TRANSLATION]);
        body[PoseMath.Y] = (float) (toBody[3] * x + toBody[4] * y + toBody[5] * z + toBody[TRANSLATION + 1]);
        body[PoseMath.Z] = (float) (toBody[6] * x + toBody[7] * y + toBody[8] * z + toBody[TRANSLATION + 2]);
        body[PoseMath.X_SCALE] = pose[at + PoseMath.X_SCALE];
        body[PoseMath.Y_SCALE] = pose[at + PoseMath.Y_SCALE];
        body[PoseMath.Z_SCALE] = pose[at + PoseMath.Z_SCALE];
        PoseMath.rotation(pose[at + PoseMath.X_ROT], pose[at + PoseMath.Y_ROT], pose[at + PoseMath.Z_ROT], matrix, 0);
        multiply(toBody, false, matrix);
        PoseMath.euler(matrix, PRODUCT, body, 0);

        PoseMath.reach(body, 0, endX, endY, endZ, targetX, targetY, targetZ, blend);

        PoseMath.rotation(body[PoseMath.X_ROT], body[PoseMath.Y_ROT], body[PoseMath.Z_ROT], matrix, 0);
        multiply(toBody, true, matrix);
        PoseMath.euler(matrix, PRODUCT, pose, at);
        pose[at + PoseMath.Y_SCALE] = body[PoseMath.Y_SCALE];
    }

    private static void multiply(double[] toBody, boolean transposed, double[] matrix) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                double sum = 0.0;
                for (int k = 0; k < 3; k++) {
                    double left = transposed ? toBody[k * 3 + row] : toBody[row * 3 + k];
                    sum += left * matrix[k * 3 + col];
                }
                matrix[PRODUCT + row * 3 + col] = sum;
            }
        }
    }

    private static double[] toBody(float side) {
        Vector3f shoulder = arm(new Matrix4f(), 1.0F, 0.0F, 0.0F)
            .transformPosition(SHOULDER_X * PIXEL, SHOULDER_Y * PIXEL, 0.0F, new Vector3f());
        Matrix4f m = new Matrix4f()
            .scale(16.0F)
            .mul(arm(new Matrix4f(), side, 0.0F, 0.0F).invert())
            .translate(0.0F, shoulder.y + SHOULDER_Y * PIXEL, shoulder.z)
            .scale(-PIXEL, -PIXEL, PIXEL)
            .invert();
        return new double[]{
            m.m00(), m.m10(), m.m20(),
            m.m01(), m.m11(), m.m21(),
            m.m02(), m.m12(), m.m22(),
            m.m30(), m.m31(), m.m32()
        };
    }
}
