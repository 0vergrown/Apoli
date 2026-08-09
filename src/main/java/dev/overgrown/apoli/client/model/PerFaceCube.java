package dev.overgrown.apoli.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PerFaceCube extends ModelPart.Cube {
    private static final Set<Direction> NO_POLYGONS = EnumSet.noneOf(Direction.class);
    private static final float BOX_UV_EPSILON = 1.0E-7F;

    static final Direction[] ORDER = {
        Direction.DOWN, Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };
    private static final int[][] CORNERS = {
        {5, 4, 0, 1}, {2, 3, 7, 6}, {0, 4, 7, 3}, {1, 0, 3, 2}, {5, 1, 2, 6}, {4, 5, 6, 7}
    };

    private final float[] positions;
    private final float[] uvs;
    private final float[] normals;
    private final int faceCount;

    public PerFaceCube(Map<Direction, float[]> faces,
                       float originX, float originY, float originZ,
                       float sizeX, float sizeY, float sizeZ,
                       float grow, boolean mirror, float texWidth, float texHeight) {
        this(ordered(faces), originX, originY, originZ, sizeX, sizeY, sizeZ, grow, mirror, texWidth, texHeight);
    }

    public PerFaceCube(float u, float v,
                       float originX, float originY, float originZ,
                       float sizeX, float sizeY, float sizeZ,
                       float grow, boolean mirror, float texWidth, float texHeight) {
        this(boxUv(u, v, sizeX, sizeY, sizeZ), originX, originY, originZ,
            sizeX, sizeY, sizeZ, grow, mirror, texWidth, texHeight);
    }

    private PerFaceCube(float[][] faces,
                        float originX, float originY, float originZ,
                        float sizeX, float sizeY, float sizeZ,
                        float grow, boolean mirror, float texWidth, float texHeight) {
        super(0, 0, originX, originY, originZ, sizeX, sizeY, sizeZ, grow, grow, grow, mirror,
            texWidth, texHeight, NO_POLYGONS);

        float maxX = originX + sizeX + grow;
        float maxY = originY + sizeY + grow;
        float maxZ = originZ + sizeZ + grow;
        float minX = originX - grow;
        float minY = originY - grow;
        float minZ = originZ - grow;
        if (mirror) {
            float swap = maxX;
            maxX = minX;
            minX = swap;
        }
        float[] corners = {
            minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
            minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ
        };

        int count = 0;
        for (int i = 0; i < faces.length; i++) {
            if (faces[i] != null) {
                count++;
            }
        }
        this.faceCount = count;
        this.positions = new float[count * 12];
        this.uvs = new float[count * 8];
        this.normals = new float[count * 3];

        int slot = 0;
        for (int i = 0; i < ORDER.length; i++) {
            if (faces[i] == null) {
                continue;
            }
            emit(slot++, corners, CORNERS[i], faces[i], ORDER[i], mirror, texWidth, texHeight);
        }
    }

    private static float[][] ordered(Map<Direction, float[]> faces) {
        float[][] out = new float[ORDER.length][];
        for (int i = 0; i < ORDER.length; i++) {
            out[i] = faces.get(ORDER[i]);
        }
        return out;
    }

    private static float[][] boxUv(float u, float v, float sizeX, float sizeY, float sizeZ) {
        float sx = (float) Math.floor(sizeX + BOX_UV_EPSILON);
        float sy = (float) Math.floor(sizeY + BOX_UV_EPSILON);
        float sz = (float) Math.floor(sizeZ + BOX_UV_EPSILON);
        return new float[][]{
            {u + sz, v, sx, sz},
            {u + sz + sx, v + sz, sx, -sz},
            {u, v + sz, sz, sy},
            {u + sz, v + sz, sx, sy},
            {u + sz + sx, v + sz, sz, sy},
            {u + sz + sz + sx, v + sz, sx, sy}
        };
    }

    private void emit(int slot, float[] corners, int[] indices, float[] uv, Direction face, boolean mirror,
                      float texWidth, float texHeight) {
        float u1 = uv[0] / texWidth;
        float v1 = uv[1] / texHeight;
        float u2 = (uv[0] + uv[2]) / texWidth;
        float v2 = (uv[1] + uv[3]) / texHeight;
        float[] u = {u2, u1, u1, u2};
        float[] v = {v1, v1, v2, v2};

        int position = slot * 12;
        int texture = slot * 8;
        for (int i = 0; i < 4; i++) {
            int source = mirror ? 3 - i : i;
            int corner = indices[source] * 3;
            this.positions[position + i * 3] = corners[corner] / 16.0F;
            this.positions[position + i * 3 + 1] = corners[corner + 1] / 16.0F;
            this.positions[position + i * 3 + 2] = corners[corner + 2] / 16.0F;
            this.uvs[texture + i * 2] = u[source];
            this.uvs[texture + i * 2 + 1] = v[source];
        }

        int normal = slot * 3;
        this.normals[normal] = mirror ? -face.getStepX() : face.getStepX();
        this.normals[normal + 1] = face.getStepY();
        this.normals[normal + 2] = face.getStepZ();
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
        Matrix4f matrix = pose.pose();
        Vector3f scratch = new Vector3f();
        for (int face = 0; face < this.faceCount; face++) {
            int normal = face * 3;
            Vector3f transformed = pose.transformNormal(
                this.normals[normal], this.normals[normal + 1], this.normals[normal + 2], scratch);
            float nx = transformed.x();
            float ny = transformed.y();
            float nz = transformed.z();
            int position = face * 12;
            int texture = face * 8;
            for (int i = 0; i < 4; i++) {
                Vector3f vertex = matrix.transformPosition(
                    this.positions[position + i * 3],
                    this.positions[position + i * 3 + 1],
                    this.positions[position + i * 3 + 2], scratch);
                consumer.addVertex(vertex.x(), vertex.y(), vertex.z(), color,
                    this.uvs[texture + i * 2], this.uvs[texture + i * 2 + 1], overlay, light, nx, ny, nz);
            }
        }
    }
}
