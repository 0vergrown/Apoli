package dev.overgrown.apoli.client.particle;

import dev.overgrown.apoli.data.ColorCodecs;
import dev.overgrown.apoli.particle.CustomParticleOptions;
import dev.overgrown.apoli.particle.ParticleFacing;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;

@OnlyIn(Dist.CLIENT)
public class CustomParticle extends SingleQuadParticle {

    private static final int FULL_BRIGHT = 0xF000F0;

    private final CustomParticleOptions options;
    private final ParticleRenderType renderType;
    private final ParticleSheet sheet;
    private final boolean loopFrames;
    private final float startSize;
    private final float endSize;
    private final float rollStep;
    private int cell;

    protected CustomParticle(ClientLevel level, CustomParticleOptions options,
                             double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.options = options;
        net.minecraft.resources.ResourceLocation texture = ParticleTextures.resolve(options.texture());
        this.renderType = ApoliParticleRenderTypes.of(texture, options.blend());
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
        this.setSize(this.startSize, this.startSize);
        this.roll = options.roll() * Mth.DEG_TO_RAD;
        this.oRoll = this.roll;
        this.rollStep = options.rollSpeed() * Mth.DEG_TO_RAD;
        this.cell = this.sheet.cellAt(0, this.lifetime, this.loopFrames);
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
    public float getQuadSize(float partialTick) {
        if (this.startSize == this.endSize) return this.startSize;
        float progress = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        return Mth.lerp(this.options.easing().apply(progress), this.startSize, this.endSize);
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
