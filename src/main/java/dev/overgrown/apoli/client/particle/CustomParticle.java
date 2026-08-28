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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class CustomParticle extends SingleQuadParticle {

    private static final int FULL_BRIGHT = 0xF000F0;

    private final CustomParticleOptions options;
    private final ParticleRenderType renderType;
    private final float startSize;
    private final float endSize;
    private final float rollStep;
    private final int frames;
    private final int frameTime;

    protected CustomParticle(ClientLevel level, CustomParticleOptions options,
                             double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.options = options;
        this.renderType = ApoliParticleRenderTypes.of(options.texture(), options.blend());
        this.lifetime = Math.max(1, options.lifetime() + (options.lifetimeVariation() > 0
            ? this.random.nextInt(options.lifetimeVariation() + 1) : 0));
        this.gravity = options.gravity();
        this.friction = options.friction();
        this.hasPhysics = options.physics();
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.startSize = options.size();
        this.endSize = options.endSizeOr();
        this.quadSize = this.startSize;
        this.setSize(this.startSize, this.startSize);
        this.roll = options.roll() * Mth.DEG_TO_RAD;
        this.oRoll = this.roll;
        this.rollStep = options.rollSpeed() * Mth.DEG_TO_RAD;
        this.frames = Math.max(1, options.frames());
        this.frameTime = Math.max(0, options.frameTime());
        tint(0.0F);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) return;
        this.roll += this.rollStep;
        tint((float) this.age / (float) this.lifetime);
    }

    private void tint(float progress) {
        float eased = this.options.easing().apply(progress);
        int from = this.options.color();
        int to = this.options.endColorOr();
        this.rCol = Mth.lerp(eased, ColorCodecs.red(from), ColorCodecs.red(to));
        this.gCol = Mth.lerp(eased, ColorCodecs.green(from), ColorCodecs.green(to));
        this.bCol = Mth.lerp(eased, ColorCodecs.blue(from), ColorCodecs.blue(to));
        this.alpha = Mth.lerp(eased, ColorCodecs.alpha(from), ColorCodecs.alpha(to));
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
        return 0.0F;
    }

    @Override
    protected float getU1() {
        return 1.0F;
    }

    @Override
    protected float getV0() {
        return frame() / (float) this.frames;
    }

    @Override
    protected float getV1() {
        return (frame() + 1) / (float) this.frames;
    }

    private int frame() {
        if (this.frames <= 1) return 0;
        int index = this.frameTime > 0
            ? this.age / this.frameTime
            : (int) ((long) this.age * this.frames / this.lifetime);
        if (this.options.loopFrames()) return ((index % this.frames) + this.frames) % this.frames;
        return Mth.clamp(index, 0, this.frames - 1);
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
