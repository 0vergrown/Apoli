package dev.overgrown.apoli.compat.flywheel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import dev.engine_room.flywheel.lib.vertex.PosTexNormalVertexView;

import java.util.Arrays;

final class MeshCollector implements VertexConsumer {
    private static final int FLOATS = 8;

    private float[] data = new float[FLOATS * 64];
    private int vertices;

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        int at = this.vertices * FLOATS;
        if (at + FLOATS > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
        }
        this.data[at] = (float) x;
        this.data[at + 1] = (float) y;
        this.data[at + 2] = (float) z;
        this.data[at + 3] = 0.0F;
        this.data[at + 4] = 0.0F;
        this.data[at + 5] = 0.0F;
        this.data[at + 6] = 1.0F;
        this.data[at + 7] = 0.0F;
        this.vertices++;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        int at = (this.vertices - 1) * FLOATS;
        this.data[at + 3] = u;
        this.data[at + 4] = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        int at = (this.vertices - 1) * FLOATS;
        this.data[at + 5] = x;
        this.data[at + 6] = y;
        this.data[at + 7] = z;
        return this;
    }

    @Override
    public void endVertex() {
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
    }

    @Override
    public void unsetDefaultColor() {
    }

    boolean isEmpty() {
        return this.vertices == 0;
    }

    Mesh build(String name) {
        PosTexNormalVertexView view = new PosTexNormalVertexView();
        view.load(MemoryBlock.mallocTracked((long) this.vertices * PosTexNormalVertexView.STRIDE));
        for (int i = 0; i < this.vertices; i++) {
            int at = i * FLOATS;
            view.x(i, this.data[at]);
            view.y(i, this.data[at + 1]);
            view.z(i, this.data[at + 2]);
            view.u(i, this.data[at + 3]);
            view.v(i, this.data[at + 4]);
            view.normalX(i, this.data[at + 5]);
            view.normalY(i, this.data[at + 6]);
            view.normalZ(i, this.data[at + 7]);
        }
        this.vertices = 0;
        return new SimpleQuadMesh(view, name);
    }
}
