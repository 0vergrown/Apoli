package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.particle.CustomParticleOptions;
import dev.overgrown.apoli.particle.ParticleFacing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CustomParticle extends SingleQuadParticle {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float COLLISION_HALF_WIDTH = 0.1F;
    private static final double MAX_COLLISION_SPEED_SQR = 10000.0;

    private final CustomParticleOptions options;
    private final ParticleRenderType renderType;
    private final ParticleSheet sheet;
    private final boolean loopFrames;
    private final float startSize;
    private final float endSize;
    private final float halfHeight;
    private final float rollStep;
    private final float startRed;
    private final float startGreen;
    private final float startBlue;
    private final float startAlpha;
    private final float endRed;
    private final float endGreen;
    private final float endBlue;
    private final float endAlpha;
    private int cell;
    private boolean stopped;

    protected CustomParticle(ClientLevel level, CustomParticleOptions options,
                             double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.options = options;
        net.minecraft.resources.ResourceLocation texture = ParticleTextures.resolve(options.texture());
        this.renderType = ApoliParticleRenderTypes.of(ParticleTextures.bound(texture, options.alphaBleed()), options.blend());
        this.sheet = ParticleSheet.of(texture, options.frameLayout(), options.frames(), options.frameTime());
        this.loopFrames = options.loopFrames().orElseGet(this.sheet::loopsByDefault);
        this.lifetime = Math.max(1, options.lifetime() + (options.lifetimeVariation() > 0
            ? this.random.nextInt(options.lifetimeVariation() + 1) : 0));
        this.gravity = options.gravity();
        this.friction = options.friction();
        this.hasPhysics = options.physics();
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        float variation = options.sizeVariation() > 0
            ? this.random.nextFloat() * options.sizeVariation() : 0.0F;
        this.startSize = options.size() + variation;
        this.endSize = options.size() > 0
            ? options.endSizeOr() * (this.startSize / options.size())
            : options.endSizeOr() + variation;
        this.quadSize = this.startSize;
        this.halfHeight = Math.max(this.startSize, this.endSize);
        this.setBoundingBox(new AABB(x - COLLISION_HALF_WIDTH, y - this.halfHeight, z - COLLISION_HALF_WIDTH,
            x + COLLISION_HALF_WIDTH, y + this.halfHeight, z + COLLISION_HALF_WIDTH));
        this.roll = (float) options.roll().eval((net.minecraft.world.entity.Entity) null) * Mth.DEG_TO_RAD;
        this.oRoll = this.roll;
        this.rollStep = (float) options.rollSpeed().eval((net.minecraft.world.entity.Entity) null) * Mth.DEG_TO_RAD;
        this.cell = this.sheet.cellAt(0, this.lifetime, this.loopFrames);

        int from = options.color();
        int to = options.endColorOr();
        if (options.hueVariation() != 0.0F) {
            float degrees = (this.random.nextFloat() * 2.0F - 1.0F) * options.hueVariation();
            from = ColorCodecs.rotateHue(from, degrees);
            to = ColorCodecs.rotateHue(to, degrees);
        }
        if (options.colorVariation() != 0.0F) {
            float spread = options.colorVariation();
            float dRed = (this.random.nextFloat() * 2.0F - 1.0F) * spread;
            float dGreen = (this.random.nextFloat() * 2.0F - 1.0F) * spread;
            float dBlue = (this.random.nextFloat() * 2.0F - 1.0F) * spread;
            from = ColorCodecs.offsetRgb(from, dRed, dGreen, dBlue);
            to = ColorCodecs.offsetRgb(to, dRed, dGreen, dBlue);
        }
        this.startRed = ColorCodecs.red(from);
        this.startGreen = ColorCodecs.green(from);
        this.startBlue = ColorCodecs.blue(from);
        this.startAlpha = ColorCodecs.alpha(from);
        this.endRed = ColorCodecs.red(to);
        this.endGreen = ColorCodecs.green(to);
        this.endBlue = ColorCodecs.blue(to);
        this.endAlpha = ColorCodecs.alpha(to);
        tint(0.0F);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) return;
        this.roll += this.rollStep;
        if (this.sheet.animated()) this.cell = this.sheet.cellAt(this.age, this.lifetime, this.loopFrames);
        tint((float) this.age / (float) this.lifetime);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        if (this.stopped) return;
        double wantedX = dx;
        double wantedY = dy;
        double wantedZ = dz;
        if (this.hasPhysics && (dx != 0.0 || dy != 0.0 || dz != 0.0)
            && dx * dx + dy * dy + dz * dz < MAX_COLLISION_SPEED_SQR) {
            Vec3 allowed = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), this.getBoundingBox(), this.level, List.of());
            dx = allowed.x;
            dy = allowed.y;
            dz = allowed.z;
        }
        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(dx, dy, dz));
            this.x += dx;
            this.y += dy;
            this.z += dz;
        }
        if (Math.abs(wantedY) >= 1.0E-5F && Math.abs(dy) < 1.0E-5F) this.stopped = true;
        this.onGround = wantedY != dy && wantedY < 0.0;
        if (wantedX != dx) this.xd = 0.0;
        if (wantedZ != dz) this.zd = 0.0;
    }

    private void tint(float progress) {
        float eased = this.options.easing().apply(progress);
        this.rCol = Mth.lerp(eased, this.startRed, this.endRed);
        this.gCol = Mth.lerp(eased, this.startGreen, this.endGreen);
        this.bCol = Mth.lerp(eased, this.startBlue, this.endBlue);
        this.alpha = Mth.lerp(eased, this.startAlpha, this.endAlpha);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp((double) partialTick, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp((double) partialTick, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp((double) partialTick, this.zo, this.z) - cameraPos.z());
        Quaternionf rotation = this.options.facing() == ParticleFacing.VERTICAL
            ? new Quaternionf(0.0F, camera.rotation().y, 0.0F, camera.rotation().w)
            : new Quaternionf(camera.rotation());
        if (this.roll != 0.0F) rotation.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));

        Vector3f[] corners = new Vector3f[]{
            new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float size = this.getQuadSize(partialTick);
        for (int i = 0; i < 4; i++) corners[i].rotate(rotation).mul(size).add(x, y, z);

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTick);
        vertex(consumer, corners[0], u1, v1, light);
        vertex(consumer, corners[1], u1, v0, light);
        vertex(consumer, corners[2], u0, v0, light);
        vertex(consumer, corners[3], u0, v1, light);
    }

    private void vertex(VertexConsumer consumer, Vector3f corner, float u, float v, int light) {
        consumer.vertex(corner.x(), corner.y(), corner.z())
            .uv(u, v)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(light)
            .endVertex();
    }

    @Override
    public float getQuadSize(float partialTick) {
        if (this.startSize == this.endSize) return this.startSize;
        float progress = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        return Mth.lerp(this.options.easing().apply(progress), this.startSize, this.endSize);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.options.emissive() ? FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.renderType;
    }

    @Override
    protected float getU0() {
        return this.sheet.u0(this.cell);
    }

    @Override
    protected float getU1() {
        return this.sheet.u1(this.cell);
    }

    @Override
    protected float getV0() {
        return this.sheet.v0(this.cell);
    }

    @Override
    protected float getV1() {
        return this.sheet.v1(this.cell);
    }

    @Environment(EnvType.CLIENT)
    public static final class Provider implements ParticleProvider<CustomParticleOptions> {
        @Override
        public Particle createParticle(CustomParticleOptions options, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new CustomParticle(level, options, x, y, z, xd, yd, zd);
        }
    }
}
