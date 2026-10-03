package dev.overgrown.apoli.client.camera;

import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.data.CameraAnimation;
import dev.overgrown.apoli.data.CameraLook;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ModifyCameraPower;
import dev.overgrown.apoli.power.builtin.ZoomPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class CameraController {
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final double DETACH_DISTANCE_SQ = 0.0025;
    private static final float MIN_ZOOM = 0.05F;
    private static final float MAX_ZOOM = 100.0F;

    public static final class Result {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float roll;
        public boolean detached;
        public boolean ridesVanilla;
    }

    private record Snapshot(double x, double y, double z, float yaw, float pitch) {}

    private static final Result RESULT = new Result();
    private static final Map<ResourceLocation, Snapshot> SNAPSHOTS = new HashMap<>();

    private static float fovScale = 1.0F;
    private static float roll;
    private static @Nullable Entity hiddenAnchor;
    private static boolean detached;
    private static boolean cinematic;

    private static float zoom = 1.0F;
    private static float zoomO = 1.0F;
    private static float zoomTarget = 1.0F;
    private static float zoomSpeed = 0.5F;
    private static boolean zoomScalesSensitivity;
    private static boolean zoomCinematic;
    private static boolean zoomHidesHand;

    private CameraController() {}

    public static @Nullable Result compute(Camera camera, Entity entity, float partialTick) {
        fovScale = 1.0F;
        roll = 0.0F;
        hiddenAnchor = null;
        detached = false;
        cinematic = false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || entity != player || ClientResolvedPowers.isEmpty()) return null;
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ModifyCameraPower.CANONICAL);
        if (powers.isEmpty()) return null;

        ResourceLocation chosenId = null;
        ModifyCameraPower.Config chosen = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (!ClientResolvedPowers.isActive(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyCameraPower.Config cfg)) continue;
            if (chosen == null || cfg.priority() > chosen.priority()) {
                chosen = cfg;
                chosenId = powerId;
            }
        }
        if (chosen == null) return null;

        float time = ClientResolvedPowers.age(chosenId, partialTick);
        CameraAnimation animation = chosen.animation();
        ModifyCameraPower.Transform transform = chosen.transform();
        float ox = channel(animation, CameraAnimation.X, time, transform.x(), player);
        float oy = channel(animation, CameraAnimation.Y, time, transform.y(), player);
        float oz = channel(animation, CameraAnimation.Z, time, transform.z(), player);
        float pitchOffset = channel(animation, CameraAnimation.PITCH, time, transform.pitch(), player);
        float yawOffset = channel(animation, CameraAnimation.YAW, time, transform.yaw(), player);
        float rollDegrees = channel(animation, CameraAnimation.ROLL, time, transform.roll(), player);
        float fov = channel(animation, CameraAnimation.FOV, time, transform.fov(), player);

        Entity anchor = player;
        int targetId = ClientResolvedPowers.target(chosenId);
        if (chosen.targetsOther() && targetId >= 0 && mc.level != null) {
            Entity target = mc.level.getEntity(targetId);
            if (target != null) anchor = target;
        }

        CameraLook look = chosen.look();
        boolean ridesVanilla = anchor == player && chosen.follow()
            && (look == CameraLook.ANCHOR || look == CameraLook.HOLDER);
        double bx;
        double by;
        double bz;
        float baseYaw;
        float basePitch;
        if (ridesVanilla) {
            Vec3 position = camera.getPosition();
            bx = position.x;
            by = position.y;
            bz = position.z;
            baseYaw = camera.getYRot();
            basePitch = camera.getXRot();
        } else if (!chosen.follow()) {
            Snapshot snapshot = SNAPSHOTS.get(chosenId);
            if (snapshot == null) {
                snapshot = new Snapshot(eyeX(anchor, partialTick), eyeY(anchor, partialTick), eyeZ(anchor, partialTick),
                    anchor.getViewYRot(partialTick), anchor.getViewXRot(partialTick));
                SNAPSHOTS.put(chosenId, snapshot);
            }
            bx = snapshot.x();
            by = snapshot.y();
            bz = snapshot.z();
            baseYaw = snapshot.yaw();
            basePitch = snapshot.pitch();
        } else {
            bx = eyeX(anchor, partialTick);
            by = eyeY(anchor, partialTick);
            bz = eyeZ(anchor, partialTick);
            baseYaw = anchor.getViewYRot(partialTick);
            basePitch = anchor.getViewXRot(partialTick);
        }
        if (!ridesVanilla && look == CameraLook.HOLDER) {
            baseYaw = player.getViewYRot(partialTick);
            basePitch = player.getViewXRot(partialTick);
        }
        if (look == CameraLook.FIXED) {
            baseYaw = 0.0F;
            basePitch = 0.0F;
        }
        cinematic = !ridesVanilla;

        float frameYaw = baseYaw + yawOffset;
        float framePitch = basePitch + pitchOffset;
        double wx;
        double wy;
        double wz;
        Space space = transform.space();
        if (space == Space.WORLD) {
            wx = ox;
            wy = oy;
            wz = oz;
        } else if (space == Space.LOCAL || space == Space.LOCAL_HORIZONTAL || space == Space.LOCAL_HORIZONTAL_NORMALIZED) {
            float framePitchUsed = space == Space.LOCAL ? framePitch : 0.0F;
            Vec3 forward = Vec3.directionFromRotation(framePitchUsed, frameYaw);
            Vec3 up = Vec3.directionFromRotation(framePitchUsed - 90.0F, frameYaw);
            Vec3 left = up.cross(forward);
            wx = left.x * ox + up.x * oy + forward.x * oz;
            wy = left.y * ox + up.y * oy + forward.y * oz;
            wz = left.z * ox + up.z * oy + forward.z * oz;
        } else {
            Vec3 global = space.toGlobal(anchor, new Vec3(ox, oy, oz));
            wx = global.x;
            wy = global.y;
            wz = global.z;
        }

        if (chosen.collision() && (wx != 0.0 || wy != 0.0 || wz != 0.0) && mc.level != null) {
            double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
            double allowed = clip(mc, anchor, bx, by, bz, wx / length, wy / length, wz / length, length);
            if (allowed < length) {
                double scale = allowed / length;
                wx *= scale;
                wy *= scale;
                wz *= scale;
            }
        }

        Result result = RESULT;
        result.x = bx + wx;
        result.y = by + wy;
        result.z = bz + wz;
        result.yaw = frameYaw;
        result.pitch = framePitch;
        if (look.looksAt()) {
            Entity focus = look == CameraLook.AT_HOLDER ? player : anchor;
            double dx = eyeX(focus, partialTick) - result.x;
            double dy = eyeY(focus, partialTick) - result.y;
            double dz = eyeZ(focus, partialTick) - result.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 1.0E-4 || Math.abs(dy) > 1.0E-4) {
                result.yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                result.pitch = (float) -(Mth.atan2(dy, horizontal) * (180.0 / Math.PI));
            }
        }
        result.roll = rollDegrees;
        result.ridesVanilla = ridesVanilla;
        CameraController.roll = rollDegrees;
        double ex = eyeX(player, partialTick) - result.x;
        double ey = eyeY(player, partialTick) - result.y;
        double ez = eyeZ(player, partialTick) - result.z;
        result.detached = anchor != player || ex * ex + ey * ey + ez * ez > DETACH_DISTANCE_SQ;
        if (anchor != player && anchor.getBoundingBox().inflate(0.3).contains(result.x, result.y, result.z)) {
            hiddenAnchor = anchor;
        }
        detached = result.detached;
        fovScale = fov > 0.0F ? fov : 1.0F;
        return result;
    }

    public static void clientTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        zoomO = zoom;
        float target = 1.0F;
        float speed = 0.5F;
        boolean scales = false;
        boolean smooth = false;
        boolean hides = false;
        if (player != null && !ClientResolvedPowers.isEmpty()) {
            PowerContainer container = PowerContainer.of(player);
            List<ResourceLocation> zooms = container == null ? List.of() : container.powersOfType(ZoomPower.CANONICAL);
            for (int i = 0, n = zooms.size(); i < n; i++) {
                ResourceLocation powerId = zooms.get(i);
                if (!ClientResolvedPowers.isActive(powerId)) continue;
                Power power = ApoliPowers.get(powerId);
                if (power == null || !(power.config() instanceof ZoomPower.Config cfg)) continue;
                float amount = Mth.clamp((float) cfg.zoom().eval(player), MIN_ZOOM, MAX_ZOOM);
                if (target == 1.0F || amount > target) {
                    target = amount;
                    speed = cfg.speed();
                    scales = cfg.scaleSensitivity();
                }
                smooth |= cfg.cinematic();
                hides |= cfg.hideHand();
            }
            if (!SNAPSHOTS.isEmpty()) SNAPSHOTS.keySet().removeIf(id -> !ClientResolvedPowers.isActive(id));
        } else {
            SNAPSHOTS.clear();
        }
        zoomTarget = target;
        zoomSpeed = speed;
        zoomScalesSensitivity = scales;
        zoomCinematic = smooth;
        zoomHidesHand = hides;
        zoom += (zoomTarget - zoom) * zoomSpeed;
        if (Math.abs(zoom - zoomTarget) < 0.001F) zoom = zoomTarget;
    }

    public static double fov(double fov, float partialTick) {
        double result = fov * fovScale;
        float current = Mth.lerp(partialTick, zoomO, zoom);
        if (current != 1.0F) result /= current;
        return result;
    }

    public static float roll() {
        return roll;
    }

    public static boolean hides(Entity entity) {
        return entity == hiddenAnchor;
    }

    public static boolean isDetached() {
        return detached;
    }

    public static boolean hidesHand() {
        return detached || (zoomHidesHand && zoom > 1.001F);
    }

    public static boolean suppressesBob() {
        return cinematic;
    }

    public static boolean smoothsMouse() {
        return zoomCinematic;
    }

    public static double sensitivity() {
        return zoomScalesSensitivity && zoom > 0.0F ? 1.0 / zoom : 1.0;
    }

    public static void setPerspective(Minecraft mc, int perspective) {
        net.minecraft.client.CameraType next = switch (perspective) {
            case 1 -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
            case 2 -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
            default -> net.minecraft.client.CameraType.FIRST_PERSON;
        };
        net.minecraft.client.CameraType previous = mc.options.getCameraType();
        if (previous == next) return;
        mc.options.setCameraType(next);
        if (previous.isFirstPerson() != next.isFirstPerson()) {
            mc.gameRenderer.checkEntityPostEffect(next.isFirstPerson() ? mc.getCameraEntity() : null);
        }
        mc.levelRenderer.needsUpdate();
    }

    public static void reset() {
        SNAPSHOTS.clear();
        hiddenAnchor = null;
        fovScale = 1.0F;
        detached = false;
        cinematic = false;
        zoom = 1.0F;
        zoomO = 1.0F;
        zoomTarget = 1.0F;
        zoomScalesSensitivity = false;
        zoomCinematic = false;
        zoomHidesHand = false;
    }

    private static float channel(CameraAnimation animation, int channel, float time, Expression fallback, Entity holder) {
        if (animation.has(channel)) return animation.sample(channel, time);
        return (float) fallback.eval(holder);
    }

    private static double clip(Minecraft mc, Entity anchor, double bx, double by, double bz,
                               double dx, double dy, double dz, double length) {
        double allowed = length;
        Vec3 base = new Vec3(bx, by, bz);
        for (int i = 0; i < 8; i++) {
            double cx = ((i & 1) * 2 - 1) * 0.1;
            double cy = ((i >> 1 & 1) * 2 - 1) * 0.1;
            double cz = ((i >> 2 & 1) * 2 - 1) * 0.1;
            Vec3 from = new Vec3(bx + cx, by + cy, bz + cz);
            Vec3 to = new Vec3(from.x + dx * length, from.y + dy * length, from.z + dz * length);
            HitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, anchor));
            if (hit.getType() == HitResult.Type.MISS) continue;
            double distance = hit.getLocation().distanceTo(base);
            if (distance < allowed) allowed = distance;
        }
        return allowed;
    }

    private static double eyeX(Entity entity, float partialTick) {
        return Mth.lerp(partialTick, entity.xo, entity.getX());
    }

    private static double eyeY(Entity entity, float partialTick) {
        return Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight();
    }

    private static double eyeZ(Entity entity, float partialTick) {
        return Mth.lerp(partialTick, entity.zo, entity.getZ());
    }
}
