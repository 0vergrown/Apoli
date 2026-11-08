package dev.overgrown.apoli.client.glow;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.BlockGlowStyle;
import dev.overgrown.apoli.data.ColorCodecs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class BlockGlowRenderer {

    private static final int MAX_OUTLINES = 8192;
    private static final int MAX_XRAY = 2048;
    private static final float BEHIND = -2.0F;

    private static final float[][][] FACES = {
        {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
        {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
        {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
        {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
        {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
        {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}
    };
    private static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    private static final OutlinePass OUTLINE_PASS = new OutlinePass();
    private static final XrayPass XRAY_PASS = new XrayPass();

    private static boolean drawing;
    private static boolean entityOutlines;
    private static boolean drewOutlines;
    private static @Nullable TextureTarget depthStore;

    private BlockGlowRenderer() {}

    public static void beginFrame() {
        entityOutlines = false;
        drewOutlines = false;
    }

    public static void noteEntityOutline() {
        if (!drawing) entityOutlines = true;
    }

    public static boolean needsOutlineEffect() {
        return drewOutlines && !entityOutlines;
    }

    private static boolean anyGlows() {
        return !ClientBlockGlows.isEmpty() || BlockGlowScanner.hasHits();
    }

    public static boolean renderOutlines(boolean outlinesShown, Matrix4f base) {
        if (!outlinesShown || !anyGlows()) return false;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return false;
        OutlinePass pass = OUTLINE_PASS;
        pass.begin(mc, level);
        pass.pose.set(base);
        drawing = true;
        try {
            for (ClientBlockGlows.Glow glow : ClientBlockGlows.glows()) {
                if (!glow.xray()) pass.server(glow);
            }
            BlockGlowScanner.forEachHit(player, pass);
        } finally {
            drawing = false;
            pass.consumer = null;
        }
        if (pass.drawn == 0) return false;
        drewOutlines = true;
        return true;
    }

    public static void renderXray(PoseStack poseStack, float partialTick) {
        if (!anyGlows()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) return;
        XrayPass pass = XRAY_PASS;
        pass.begin(mc, level);
        for (ClientBlockGlows.Glow glow : ClientBlockGlows.glows()) {
            if (glow.xray()) pass.server(glow);
        }
        BlockGlowScanner.forEachHit(player, pass);
        if (pass.count == 0) return;

        RenderTarget main = mc.getMainRenderTarget();
        TextureTarget store = depthStore(main);
        store.copyDepthFrom(main);
        main.bindWrite(false);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        BlockEntityRenderDispatcher blockEntities = mc.getBlockEntityRenderDispatcher();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Vec3 camera = pass.camera;
        int done = 0;
        while (done < pass.count) {
            int color = 0;
            boolean picked = false;
            for (int i = 0; i < pass.count; i++) {
                if (pass.handled[i]) continue;
                if (!picked) {
                    color = pass.colors[i];
                    picked = true;
                }
                if (pass.colors[i] != color) continue;
                pass.handled[i] = true;
                done++;
                cursor.set(pass.positions[i]);
                BlockState state = level.getBlockState(cursor);
                poseStack.pushPose();
                poseStack.translate(cursor.getX() - camera.x, cursor.getY() - camera.y, cursor.getZ() - camera.z);
                if (pass.respect[i]) renderBlock(level, cursor, state, poseStack, buffers, partialTick, blocks, blockEntities);
                else renderCube(state, poseStack, buffers, blocks);
                poseStack.popPose();
            }
            RenderSystem.setShaderColor(ColorCodecs.red(color), ColorCodecs.green(color), ColorCodecs.blue(color), 1.0F);
            buffers.endBatch();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        main.copyDepthFrom(store);
        main.bindWrite(false);
    }

    private static void renderBlock(ClientLevel level, BlockPos pos, BlockState state, PoseStack poseStack,
                                    MultiBufferSource buffers, float partialTick, BlockRenderDispatcher blocks,
                                    BlockEntityRenderDispatcher blockEntities) {
        boolean drew = false;
        if (state.getRenderShape() == RenderShape.MODEL) {
            blocks.renderSingleBlock(state, poseStack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            drew = true;
        }
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (blockEntity != null) {
            BlockEntityRenderer<BlockEntity> renderer = blockEntities.getRenderer(blockEntity);
            if (renderer != null) {
                renderer.render(blockEntity, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                drew = true;
            }
        }
        if (!drew) renderCube(state, poseStack, buffers, blocks);
    }

    private static void renderCube(BlockState state, PoseStack poseStack, MultiBufferSource buffers,
                                   BlockRenderDispatcher blocks) {
        if (state.isAir()) return;
        TextureAtlasSprite sprite = blocks.getBlockModelShaper().getParticleIcon(state);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        for (int face = 0; face < 6; face++) {
            float[][] corners = FACES[face];
            float[] normal = NORMALS[face];
            GlowVertices.solid(consumer, pose, corners[0][0], corners[0][1], corners[0][2], u0, v0, normal[0], normal[1], normal[2]);
            GlowVertices.solid(consumer, pose, corners[1][0], corners[1][1], corners[1][2], u0, v1, normal[0], normal[1], normal[2]);
            GlowVertices.solid(consumer, pose, corners[2][0], corners[2][1], corners[2][2], u1, v1, normal[0], normal[1], normal[2]);
            GlowVertices.solid(consumer, pose, corners[3][0], corners[3][1], corners[3][2], u1, v0, normal[0], normal[1], normal[2]);
        }
    }

    private static TextureTarget depthStore(RenderTarget main) {
        TextureTarget store = depthStore;
        if (store == null) {
            store = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            depthStore = store;
        } else if (store.width != main.width || store.height != main.height) {
            store.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        return store;
    }

    public static void clear() {
        TextureTarget store = depthStore;
        depthStore = null;
        if (store != null) RenderSystem.recordRenderCall(store::destroyBuffers);
    }

    private abstract static class Pass implements BlockGlowScanner.HitVisitor {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        @Nullable ClientLevel level;
        Vec3 camera = Vec3.ZERO;
        final Vector3f look = new Vector3f();
        long now;

        void begin(Minecraft mc, ClientLevel level) {
            this.level = level;
            Camera view = mc.gameRenderer.getMainCamera();
            this.camera = view.getPosition();
            this.look.set(view.getLookVector());
            this.now = level.getGameTime();
        }

        void server(ClientBlockGlows.Glow glow) {
            if (glow.expiresAt() <= now) return;
            cursor.set(glow.pos());
            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() != glow.block()) return;
            accept(cursor, state, glow.color(), glow.respectBounding());
        }

        @Override
        public void visit(long pos, BlockGlowStyle style) {
            if ((style.mode() == BlockGlowStyle.Mode.GLOW) != xray()) return;
            cursor.set(pos);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) return;
            accept(cursor, state, style.color(), style.respectBounding());
        }

        boolean behind(double x, double y, double z) {
            return x * look.x() + y * look.y() + z * look.z() < BEHIND;
        }

        abstract boolean xray();

        abstract void accept(BlockPos pos, BlockState state, int color, boolean respectBounding);
    }

    private static final class OutlinePass extends Pass implements Shapes.DoubleLineConsumer {
        final Matrix4f pose = new Matrix4f();
        @Nullable OutlineBufferSource buffers;
        @Nullable VertexConsumer consumer;
        int color;
        int drawn;
        float u;
        float v;
        float originX;
        float originY;
        float originZ;

        @Override
        void begin(Minecraft mc, ClientLevel level) {
            super.begin(mc, level);
            this.buffers = mc.renderBuffers().outlineBufferSource();
            this.consumer = null;
            this.drawn = 0;
            TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
            this.u = (sprite.getU0() + sprite.getU1()) * 0.5F;
            this.v = (sprite.getV0() + sprite.getV1()) * 0.5F;
        }

        @Override
        boolean xray() {
            return false;
        }

        @Override
        void accept(BlockPos pos, BlockState state, int color, boolean respectBounding) {
            if (drawn >= MAX_OUTLINES) return;
            double x = pos.getX() - camera.x;
            double y = pos.getY() - camera.y;
            double z = pos.getZ() - camera.z;
            if (behind(x + 0.5, y + 0.5, z + 0.5)) return;
            if (consumer == null || color != this.color) {
                buffers.setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255);
                consumer = buffers.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS));
                this.color = color;
            }
            originX = (float) x;
            originY = (float) y;
            originZ = (float) z;
            if (respectBounding) {
                VoxelShape shape = state.getShape(level, pos);
                if (!shape.isEmpty()) {
                    shape.forAllBoxes(this);
                    drawn++;
                    return;
                }
                if (state.isAir()) return;
            }
            consume(0, 0, 0, 1, 1, 1);
            drawn++;
        }

        @Override
        public void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            float x0 = originX + (float) minX;
            float y0 = originY + (float) minY;
            float z0 = originZ + (float) minZ;
            float x1 = originX + (float) maxX;
            float y1 = originY + (float) maxY;
            float z1 = originZ + (float) maxZ;
            for (int face = 0; face < 6; face++) {
                float[][] corners = FACES[face];
                for (int corner = 0; corner < 4; corner++) {
                    float[] c = corners[corner];
                    GlowVertices.outline(consumer, pose, c[0] == 0 ? x0 : x1, c[1] == 0 ? y0 : y1, c[2] == 0 ? z0 : z1, u, v);
                }
            }
        }
    }

    private static final class XrayPass extends Pass {
        final long[] positions = new long[MAX_XRAY];
        final int[] colors = new int[MAX_XRAY];
        final boolean[] respect = new boolean[MAX_XRAY];
        final boolean[] handled = new boolean[MAX_XRAY];
        int count;

        @Override
        void begin(Minecraft mc, ClientLevel level) {
            super.begin(mc, level);
            count = 0;
        }

        @Override
        boolean xray() {
            return true;
        }

        @Override
        void accept(BlockPos pos, BlockState state, int color, boolean respectBounding) {
            if (count >= MAX_XRAY) return;
            if (behind(pos.getX() + 0.5 - camera.x, pos.getY() + 0.5 - camera.y, pos.getZ() + 0.5 - camera.z)) return;
            positions[count] = pos.asLong();
            colors[count] = color;
            respect[count] = respectBounding;
            handled[count] = false;
            count++;
        }
    }
}
