package dev.overgrown.apoli.data;

public final class PoseMath {
    private PoseMath() {}

    public static final int STRIDE = 9;
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int X_ROT = 3;
    public static final int Y_ROT = 4;
    public static final int Z_ROT = 5;
    public static final int X_SCALE = 6;
    public static final int Y_SCALE = 7;
    public static final int Z_SCALE = 8;

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;

    private static final double GIMBAL = 0.9999999;
    private static final double MIN_SCALE = 1.0E-4;

    public static void applyGroup(ModelPartTransformation.Type type, float[] pose, int at, float value, float weight,
                                  boolean override, float pivotX, float pivotY, float pivotZ) {
        switch (type) {
            case PITCH -> rotateGroup(pose, at, AXIS_X, value, weight, override, pivotX, pivotY, pivotZ);
            case YAW -> rotateGroup(pose, at, AXIS_Y, value, weight, override, pivotX, pivotY, pivotZ);
            case ROLL -> rotateGroup(pose, at, AXIS_Z, value, weight, override, pivotX, pivotY, pivotZ);
            case X_SCALE -> scaleAbout(pose, at, pivotX, pivotY, pivotZ, AXIS_X, 1.0F + value * weight);
            case Y_SCALE -> scaleAbout(pose, at, pivotX, pivotY, pivotZ, AXIS_Y, 1.0F + value * weight);
            case Z_SCALE -> scaleAbout(pose, at, pivotX, pivotY, pivotZ, AXIS_Z, 1.0F + value * weight);
            case PIVOT_X -> pose[at + X] += value * weight;
            case PIVOT_Y -> pose[at + Y] += value * weight;
            case PIVOT_Z -> pose[at + Z] += value * weight;
            default -> { }
        }
    }

    public static boolean isSpatial(ModelPartTransformation.Type type) {
        return type != ModelPartTransformation.Type.VISIBLE && type != ModelPartTransformation.Type.HIDDEN;
    }

    private static void rotateGroup(float[] pose, int at, int axis, float value, float weight, boolean override,
                                    float pivotX, float pivotY, float pivotZ) {
        if (override) {
            int channel = at + X_ROT + axis;
            pose[channel] -= pose[channel] * weight;
        }
        rotateAbout(pose, at, pivotX, pivotY, pivotZ, axis, value * weight);
    }

    public static void rotateAbout(float[] pose, int at, float pivotX, float pivotY, float pivotZ, int axis, float angle) {
        if (angle == 0.0F) return;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = pose[at + X] - pivotX;
        double dy = pose[at + Y] - pivotY;
        double dz = pose[at + Z] - pivotZ;
        switch (axis) {
            case AXIS_X -> {
                double ny = dy * cos - dz * sin;
                dz = dy * sin + dz * cos;
                dy = ny;
            }
            case AXIS_Y -> {
                double nx = dx * cos + dz * sin;
                dz = -dx * sin + dz * cos;
                dx = nx;
            }
            default -> {
                double nx = dx * cos - dy * sin;
                dy = dx * sin + dy * cos;
                dx = nx;
            }
        }
        pose[at + X] = (float) (pivotX + dx);
        pose[at + Y] = (float) (pivotY + dy);
        pose[at + Z] = (float) (pivotZ + dz);
        turn(pose, at, axis, angle, cos, sin);
    }

    private static void turn(float[] pose, int at, int axis, float angle, double cos, double sin) {
        float xRot = pose[at + X_ROT];
        float yRot = pose[at + Y_ROT];
        float zRot = pose[at + Z_ROT];
        if (axis == AXIS_Z) {
            pose[at + Z_ROT] = zRot + angle;
            return;
        }
        if (axis == AXIS_Y && zRot == 0.0F) {
            pose[at + Y_ROT] = yRot + angle;
            return;
        }
        if (axis == AXIS_X && yRot == 0.0F && zRot == 0.0F) {
            pose[at + X_ROT] = xRot + angle;
            return;
        }

        double ca = Math.cos(xRot);
        double sa = Math.sin(xRot);
        double cb = Math.cos(yRot);
        double sb = Math.sin(yRot);
        double cg = Math.cos(zRot);
        double sg = Math.sin(zRot);

        double m00 = cg * cb;
        double m01 = cg * sb * sa - sg * ca;
        double m02 = cg * sb * ca + sg * sa;
        double m10 = sg * cb;
        double m11 = sg * sb * sa + cg * ca;
        double m12 = sg * sb * ca - cg * sa;
        double m20 = -sb;
        double m21 = cb * sa;
        double m22 = cb * ca;

        double r00;
        double r01;
        double r10;
        double r11;
        double r20;
        double r21;
        double r22;
        if (axis == AXIS_X) {
            r00 = m00;
            r01 = m01;
            r10 = cos * m10 - sin * m20;
            r11 = cos * m11 - sin * m21;
            r20 = sin * m10 + cos * m20;
            r21 = sin * m11 + cos * m21;
            r22 = sin * m12 + cos * m22;
        } else {
            r00 = cos * m00 + sin * m20;
            r01 = cos * m01 + sin * m21;
            r10 = m10;
            r11 = m11;
            r20 = -sin * m00 + cos * m20;
            r21 = -sin * m01 + cos * m21;
            r22 = -sin * m02 + cos * m22;
        }

        double sinY = -r20;
        if (sinY >= GIMBAL) {
            pose[at + X_ROT] = (float) Math.atan2(r01, r11);
            pose[at + Y_ROT] = (float) (Math.PI / 2.0);
            pose[at + Z_ROT] = 0.0F;
        } else if (sinY <= -GIMBAL) {
            pose[at + X_ROT] = (float) Math.atan2(-r01, r11);
            pose[at + Y_ROT] = (float) (-Math.PI / 2.0);
            pose[at + Z_ROT] = 0.0F;
        } else {
            pose[at + X_ROT] = (float) Math.atan2(r21, r22);
            pose[at + Y_ROT] = (float) Math.asin(sinY);
            pose[at + Z_ROT] = (float) Math.atan2(r10, r00);
        }
    }

    public static void scaleAbout(float[] pose, int at, float pivotX, float pivotY, float pivotZ, int axis, float factor) {
        switch (axis) {
            case AXIS_X -> {
                pose[at + X] = pivotX + (pose[at + X] - pivotX) * factor;
                pose[at + X_SCALE] *= factor;
            }
            case AXIS_Y -> {
                pose[at + Y] = pivotY + (pose[at + Y] - pivotY) * factor;
                pose[at + Y_SCALE] *= factor;
            }
            default -> {
                pose[at + Z] = pivotZ + (pose[at + Z] - pivotZ) * factor;
                pose[at + Z_SCALE] *= factor;
            }
        }
    }

    public static void localToParent(float[] pose, int at, double lx, double ly, double lz, double[] out) {
        double x = lx * pose[at + X_SCALE];
        double y = ly * pose[at + Y_SCALE];
        double z = lz * pose[at + Z_SCALE];
        rotateForward(pose, at, x, y, z, out);
        out[0] += pose[at + X];
        out[1] += pose[at + Y];
        out[2] += pose[at + Z];
    }

    public static void parentToLocal(float[] pose, int at, double px, double py, double pz, double[] out) {
        rotateBackward(pose, at, px - pose[at + X], py - pose[at + Y], pz - pose[at + Z], out);
        out[0] /= safeScale(pose[at + X_SCALE]);
        out[1] /= safeScale(pose[at + Y_SCALE]);
        out[2] /= safeScale(pose[at + Z_SCALE]);
    }

    public static void directionToLocal(float[] pose, int at, double dx, double dy, double dz, double[] out) {
        rotateBackward(pose, at, dx, dy, dz, out);
        out[0] /= safeScale(pose[at + X_SCALE]);
        out[1] /= safeScale(pose[at + Y_SCALE]);
        out[2] /= safeScale(pose[at + Z_SCALE]);
    }

    private static double safeScale(float scale) {
        return Math.abs(scale) < MIN_SCALE ? (scale < 0.0F ? -MIN_SCALE : MIN_SCALE) : scale;
    }

    private static void rotateForward(float[] pose, int at, double x, double y, double z, double[] out) {
        float xRot = pose[at + X_ROT];
        float yRot = pose[at + Y_ROT];
        float zRot = pose[at + Z_ROT];
        if (xRot != 0.0F) {
            double cos = Math.cos(xRot);
            double sin = Math.sin(xRot);
            double ny = y * cos - z * sin;
            z = y * sin + z * cos;
            y = ny;
        }
        if (yRot != 0.0F) {
            double cos = Math.cos(yRot);
            double sin = Math.sin(yRot);
            double nx = x * cos + z * sin;
            z = -x * sin + z * cos;
            x = nx;
        }
        if (zRot != 0.0F) {
            double cos = Math.cos(zRot);
            double sin = Math.sin(zRot);
            double nx = x * cos - y * sin;
            y = x * sin + y * cos;
            x = nx;
        }
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    private static void rotateBackward(float[] pose, int at, double x, double y, double z, double[] out) {
        float xRot = pose[at + X_ROT];
        float yRot = pose[at + Y_ROT];
        float zRot = pose[at + Z_ROT];
        if (zRot != 0.0F) {
            double cos = Math.cos(zRot);
            double sin = Math.sin(zRot);
            double nx = x * cos + y * sin;
            y = -x * sin + y * cos;
            x = nx;
        }
        if (yRot != 0.0F) {
            double cos = Math.cos(yRot);
            double sin = Math.sin(yRot);
            double nx = x * cos - z * sin;
            z = x * sin + z * cos;
            x = nx;
        }
        if (xRot != 0.0F) {
            double cos = Math.cos(xRot);
            double sin = Math.sin(xRot);
            double ny = y * cos + z * sin;
            z = -y * sin + z * cos;
            y = ny;
        }
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }
}
