package dev.overgrown.apoli.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.Apoli;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public final class TextureMeshCube extends ModelPart.Cube {
    public static final int MAX_QUADS = 65536;

    private static final Set<Direction> NO_POLYGONS = EnumSet.noneOf(Direction.class);
    private static final float STRIP_START = 0.4F;
    private static final float STRIP_END = 0.6F;

    private final float[] positions;
    private final float[] uvs;
    private final float[] normals;
    private final int quadCount;

    private TextureMeshCube(Builder built) {
        super(0, 0, built.minX, built.minY, built.minZ,
            built.maxX - built.minX, built.maxY - built.minY, built.maxZ - built.minZ,
            0.0F, 0.0F, 0.0F, false, 1.0F, 1.0F, NO_POLYGONS);
        this.quadCount = built.count;
        this.positions = Arrays.copyOf(built.positions, built.count * 12);
        this.uvs = Arrays.copyOf(built.uvs, built.count * 8);
        this.normals = Arrays.copyOf(built.normals, built.count * 3);
    }

    @Nullable
    public static TextureMeshCube of(TextureMask mask, float uvWidth, float uvHeight, Vector3f scale, Vector3f pivot) {
        if (mask.isEmpty()) {
            return null;
        }
        int width = mask.width();
        int height = mask.height();
        float stepX = uvWidth / width * scale.x;
        float stepZ = uvHeight / height * scale.z;
        float top = -pivot.y;
        float bottom = scale.y - pivot.y;
        Builder out = new Builder(scale);

        for (int y = 0; y < height && !out.full; y++) {
            float z0 = y * stepZ - pivot.z;
            float z1 = (y + 1) * stepZ - pivot.z;
            float v0 = y / (float) height;
            float v1 = (y + 1) / (float) height;
            int x = 0;
            while (x < width) {
                if (!mask.visible(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < width && mask.visible(x, y)) {
                    x++;
                }
                float x0 = start * stepX - pivot.x;
                float x1 = x * stepX - pivot.x;
                float u0 = start / (float) width;
                float u1 = x / (float) width;
                out.quad(0, -1, 0,
                    x0, top, z0, u0, v0, x1, top, z0, u1, v0, x1, top, z1, u1, v1, x0, top, z1, u0, v1);
                out.quad(0, 1, 0,
                    x0, bottom, z0, u0, v0, x1, bottom, z0, u1, v0, x1, bottom, z1, u1, v1, x0, bottom, z1, u0, v1);
            }
        }

        for (int x = 0; x <= width && !out.full; x++) {
            float edge = x * stepX - pivot.x;
            int y = 0;
            while (y < height) {
                int state = side(mask.solid(x - 1, y), mask.solid(x, y));
                if (state == 0) {
                    y++;
                    continue;
                }
                int start = y;
                while (y < height && side(mask.solid(x - 1, y), mask.solid(x, y)) == state) {
                    y++;
                }
                int column = state > 0 ? x - 1 : x;
                float u0 = (column + STRIP_START) / width;
                float u1 = (column + STRIP_END) / width;
                float z0 = start * stepZ - pivot.z;
                float z1 = y * stepZ - pivot.z;
                float v0 = start / (float) height;
                float v1 = y / (float) height;
                out.quad(state, 0, 0,
                    edge, top, z0, u0, v0, edge, top, z1, u0, v1, edge, bottom, z1, u1, v1, edge, bottom, z0, u1, v0);
            }
        }

        for (int y = 0; y <= height && !out.full; y++) {
            float edge = y * stepZ - pivot.z;
            int x = 0;
            while (x < width) {
                int state = side(mask.solid(x, y - 1), mask.solid(x, y));
                if (state == 0) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < width && side(mask.solid(x, y - 1), mask.solid(x, y)) == state) {
                    x++;
                }
                int row = state > 0 ? y - 1 : y;
                float v0 = (row + STRIP_START) / height;
                float v1 = (row + STRIP_END) / height;
                float x0 = start * stepX - pivot.x;
                float x1 = x * stepX - pivot.x;
                float u0 = start / (float) width;
                float u1 = x / (float) width;
                out.quad(0, 0, state,
                    x0, top, edge, u0, v0, x1, top, edge, u1, v0, x1, bottom, edge, u1, v1, x0, bottom, edge, u0, v1);
            }
        }

        if (out.full) {
            Apoli.LOGGER.warn("[Apoli] A texture mesh cut from a {}x{} texture needs more than {} faces, so it is left out. Use a smaller texture for it.", width, height, MAX_QUADS);
            return null;
        }
        if (out.count == 0) {
            return null;
        }
        return new TextureMeshCube(out);
    }

    private static int side(boolean before, boolean after) {
        if (before == after) {
            return 0;
        }
        return before ? 1 : -1;
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                        float red, float green, float blue, float alpha) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector3f scratch = new Vector3f();
        Vector4f corner = new Vector4f();
        for (int quad = 0; quad < this.quadCount; quad++) {
            int normal = quad * 3;
            Vector3f transformed = normalMatrix.transform(
                scratch.set(this.normals[normal], this.normals[normal + 1], this.normals[normal + 2]));
            float nx = transformed.x();
            float ny = transformed.y();
            float nz = transformed.z();
            int position = quad * 12;
            int texture = quad * 8;
            for (int i = 0; i < 4; i++) {
                Vector4f vertex = matrix.transform(corner.set(
                    this.positions[position + i * 3],
                    this.positions[position + i * 3 + 1],
                    this.positions[position + i * 3 + 2], 1.0F));
                consumer.vertex(vertex.x(), vertex.y(), vertex.z(), red, green, blue, alpha,
                    this.uvs[texture + i * 2], this.uvs[texture + i * 2 + 1], overlay, light, nx, ny, nz);
            }
        }
    }

    private static final class Builder {
        final float signX;
        final float signY;
        final float signZ;
        float[] positions = new float[12 * 64];
        float[] uvs = new float[8 * 64];
        float[] normals = new float[3 * 64];
        int count;
        boolean full;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        Builder(Vector3f scale) {
            this.signX = scale.x < 0.0F ? -1.0F : 1.0F;
            this.signY = scale.y < 0.0F ? -1.0F : 1.0F;
            this.signZ = scale.z < 0.0F ? -1.0F : 1.0F;
        }

        void quad(float nx, float ny, float nz,
                  float ax, float ay, float az, float au, float av,
                  float bx, float by, float bz, float bu, float bv,
                  float cx, float cy, float cz, float cu, float cv,
                  float dx, float dy, float dz, float du, float dv) {
            if (this.count >= MAX_QUADS) {
                this.full = true;
                return;
            }
            if (this.count * 12 >= this.positions.length) {
                int grown = Math.min(MAX_QUADS, this.count * 2);
                this.positions = Arrays.copyOf(this.positions, grown * 12);
                this.uvs = Arrays.copyOf(this.uvs, grown * 8);
                this.normals = Arrays.copyOf(this.normals, grown * 3);
            }
            float outX = nx * this.signX;
            float outY = ny * this.signY;
            float outZ = nz * this.signZ;
            float e1x = bx - ax;
            float e1y = by - ay;
            float e1z = bz - az;
            float e2x = cx - ax;
            float e2y = cy - ay;
            float e2z = cz - az;
            float crossX = e1y * e2z - e1z * e2y;
            float crossY = e1z * e2x - e1x * e2z;
            float crossZ = e1x * e2y - e1y * e2x;
            boolean flip = crossX * outX + crossY * outY + crossZ * outZ < 0.0F;
            int position = this.count * 12;
            int texture = this.count * 8;
            if (flip) {
                this.vertex(position, texture, 0, ax, ay, az, au, av);
                this.vertex(position, texture, 1, dx, dy, dz, du, dv);
                this.vertex(position, texture, 2, cx, cy, cz, cu, cv);
                this.vertex(position, texture, 3, bx, by, bz, bu, bv);
            } else {
                this.vertex(position, texture, 0, ax, ay, az, au, av);
                this.vertex(position, texture, 1, bx, by, bz, bu, bv);
                this.vertex(position, texture, 2, cx, cy, cz, cu, cv);
                this.vertex(position, texture, 3, dx, dy, dz, du, dv);
            }
            int normal = this.count * 3;
            this.normals[normal] = outX;
            this.normals[normal + 1] = outY;
            this.normals[normal + 2] = outZ;
            this.count++;
        }

        private void vertex(int position, int texture, int index, float x, float y, float z, float u, float v) {
            this.positions[position + index * 3] = x / 16.0F;
            this.positions[position + index * 3 + 1] = y / 16.0F;
            this.positions[position + index * 3 + 2] = z / 16.0F;
            this.uvs[texture + index * 2] = u;
            this.uvs[texture + index * 2 + 1] = v;
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }
    }
}
