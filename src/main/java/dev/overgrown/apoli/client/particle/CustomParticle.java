package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.particle.CustomParticleOptions;
import dev.overgrown.apoli.particle.ParticleFacing;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

@OnlyIn(Dist.CLIENT)
public class CustomParticle extends SingleQuadParticle {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float COLLISION_HALF_WIDTH = 0.1F;
    private static final double MAX_COLLISION_SPEED_SQR = 10000.0;
    private static float aimX;
    private static float aimY;
    private static float aimZ;
    private static boolean aiming;

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
    private final float stretch;
    private float dirX;
    private float dirY;
    private float dirZ;
    private boolean directed;

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
        this.stretch = Math.max(0.0F, options.stretch());
        double motion = xd * xd + yd * yd + zd * zd;
        if (motion > 1.0E-12) {
            double inverse = 1.0 / Math.sqrt(motion);
            this.dirX = (float) (xd * inverse);
            this.dirY = (float) (yd * inverse);
            this.dirZ = (float) (zd * inverse);
            this.directed = true;
        } else if (aiming) {
            this.dirX = aimX;
            this.dirY = aimY;
            this.dirZ = aimZ;
            this.directed = true;
        }
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

    public static void aimNext(double x, double y, double z) {
        double length = x * x + y * y + z * z;
        if (length < 1.0E-12) {
            aiming = false;
            return;
        }
        double inverse = 1.0 / Math.sqrt(length);
        aimX = (float) (x * inverse);
        aimY = (float) (y * inverse);
        aimZ = (float) (z * inverse);
        aiming = true;
    }

    public static void stopAiming() {
        aiming = false;
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) return;
        ParticleFacing facing = this.options.facing();
        if (facing == ParticleFacing.VELOCITY || facing == ParticleFacing.HORIZONTAL) {
            double dx = this.x - this.xo;
            double dy = this.y - this.yo;
            double dz = this.z - this.zo;
            double moved = dx * dx + dy * dy + dz * dz;
            if (moved > 1.0E-10) {
                double inverse = 1.0 / Math.sqrt(moved);
                this.dirX = (float) (dx * inverse);
                this.dirY = (float) (dy * inverse);
                this.dirZ = (float) (dz * inverse);
                this.directed = true;
            }
        }
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
    public float getQuadSize(float partialTick) {
        if (this.startSize == this.endSize) return this.startSize;
        float progress = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        return Mth.lerp(this.options.easing().apply(progress), this.startSize, this.endSize);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        ParticleFacing facing = this.options.facing();
        if (this.stretch == 1.0F && (facing == ParticleFacing.CAMERA || facing == ParticleFacing.VERTICAL)) {
            super.render(consumer, camera, partialTick);
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (Mth.lerp((double) partialTick, this.xo, this.x) - cameraPos.x());
        float cy = (float) (Mth.lerp((double) partialTick, this.yo, this.y) - cameraPos.y());
        float cz = (float) (Mth.lerp((double) partialTick, this.zo, this.z) - cameraPos.z());
        float roll = Mth.lerp(partialTick, this.oRoll, this.roll);
        float rx;
        float ry;
        float rz;
        float ux;
        float uy;
        float uz;
        if (facing == ParticleFacing.VELOCITY && this.directed) {
            ux = this.dirX;
            uy = this.dirY;
            uz = this.dirZ;
            rx = cy * uz - cz * uy;
            ry = cz * ux - cx * uz;
            rz = cx * uy - cy * ux;
            float cross = rx * rx + ry * ry + rz * rz;
            if (cross < 1.0E-8F) {
                Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.rotation());
                rx = right.x();
                ry = right.y();
                rz = right.z();
            } else {
                float inverse = 1.0F / (float) Math.sqrt(cross);
                rx *= inverse;
                ry *= inverse;
                rz *= inverse;
            }
        } else if (facing == ParticleFacing.HORIZONTAL) {
            float hx = 0.0F;
            float hz = -1.0F;
            float flat = this.dirX * this.dirX + this.dirZ * this.dirZ;
            if (this.directed && flat > 1.0E-6F) {
                float inverse = 1.0F / (float) Math.sqrt(flat);
                hx = this.dirX * inverse;
                hz = this.dirZ * inverse;
            }
            if (roll != 0.0F) {
                float cos = Mth.cos(roll);
                float sin = Mth.sin(roll);
                float turned = hx * cos - hz * sin;
                hz = hx * sin + hz * cos;
                hx = turned;
            }
            ux = hx;
            uy = 0.0F;
            uz = hz;
            rx = -hz;
            ry = 0.0F;
            rz = hx;
        } else {
            Quaternionf rotation = facing == ParticleFacing.VERTICAL
                ? new Quaternionf(0.0F, camera.rotation().y, 0.0F, camera.rotation().w).normalize()
                : new Quaternionf(camera.rotation());
            if (roll != 0.0F) rotation.rotateZ(roll);
            Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
            Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);
            rx = right.x();
            ry = right.y();
            rz = right.z();
            ux = up.x();
            uy = up.y();
            uz = up.z();
        }
        float nx = ry * uz - rz * uy;
        float ny = rz * ux - rx * uz;
        float nz = rx * uy - ry * ux;
        if (nx * cx + ny * cy + nz * cz > 0.0F) {
            rx = -rx;
            ry = -ry;
            rz = -rz;
        }
        float size = this.getQuadSize(partialTick);
        float halfLength = size * this.stretch;
        rx *= size;
        ry *= size;
        rz *= size;
        ux *= halfLength;
        uy *= halfLength;
        uz *= halfLength;
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTick);
        this.corner(consumer, cx + rx - ux, cy + ry - uy, cz + rz - uz, u1, v1, light);
        this.corner(consumer, cx + rx + ux, cy + ry + uy, cz + rz + uz, u1, v0, light);
        this.corner(consumer, cx - rx + ux, cy - ry + uy, cz - rz + uz, u0, v0, light);
        this.corner(consumer, cx - rx - ux, cy - ry - uy, cz - rz - uz, u0, v1, light);
    }

    private void corner(VertexConsumer consumer, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public FacingCameraMode getFacingCameraMode() {
        return this.options.facing() == ParticleFacing.VERTICAL ? FacingCameraMode.LOOKAT_Y : FacingCameraMode.LOOKAT_XYZ;
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

    @OnlyIn(Dist.CLIENT)
    public static final class Provider implements ParticleProvider<CustomParticleOptions> {
        @Override
        public Particle createParticle(CustomParticleOptions options, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new CustomParticle(level, options, x, y, z, xd, yd, zd);
        }
    }
}
