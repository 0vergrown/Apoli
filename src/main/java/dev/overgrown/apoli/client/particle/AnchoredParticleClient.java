package dev.overgrown.apoli.client.particle;

import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.BodyParts;
import dev.overgrown.apoli.data.ModelPartAnchor;
import dev.overgrown.apoli.data.ParticlePlacement;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.network.payload.AnchoredParticlesS2C;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class AnchoredParticleClient {
    private static final int MAX_PENDING = 256;
    private static final ArrayDeque<AnchoredParticlesS2C> PENDING = new ArrayDeque<>();

    private AnchoredParticleClient() {}

    public static void enqueue(AnchoredParticlesS2C payload) {
        if (PENDING.size() >= MAX_PENDING) PENDING.pollFirst();
        PENDING.addLast(payload);
    }

    public static void beginFrame() {
        RenderedPoses.nextFrame();
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            PENDING.clear();
            RenderedPoses.clear();
            return;
        }
        float partialTick = ModelPartAnimator.partialTick();
        AnchoredParticlesS2C payload;
        while ((payload = PENDING.pollFirst()) != null) {
            spawn(mc, level, payload, partialTick);
        }
    }

    private static void spawn(Minecraft mc, ClientLevel level, AnchoredParticlesS2C payload, float partialTick) {
        Entity entity = level.getEntity(payload.entityId());
        if (entity == null) return;
        RenderedPoses.watch(entity);
        BodyPart part = BodyParts.lookupOrCustom(payload.part());
        Optional<Space> space = payload.spaceValue();
        Vec3 base = entity.getPosition(partialTick);
        RenderedPoses.Pose rendered = RenderedPoses.recent(entity);
        ModelPartAnchor.Frame frame = rendered == null ? null : rendered.frame(part.sided(entity), base);
        if (frame == null) frame = FirstPersonHandAnchor.frame(mc, entity, part, base, partialTick);
        if (frame == null) {
            frame = ModelPartAnchor.frameOf(entity, part, ModelPartAnimator.timeline(),
                ModelPartAnimator.now(entity), partialTick);
        }
        Vec3 origin = ParticlePlacement.origin(base, entity, frame, space,
            payload.offsetX(), payload.offsetY(), payload.offsetZ());
        Vec3 velocity = ParticlePlacement.velocity(entity, frame, space,
            new Vec3(payload.velocityX(), payload.velocityY(), payload.velocityZ()));
        ParticlePlacement.spawn(level, level.random, payload.particle(), payload.force(), origin, velocity,
            payload.count(), new Vector(payload.spreadX(), payload.spreadY(), payload.spreadZ()), payload.speed(),
            entity, frame, space);
    }
}
