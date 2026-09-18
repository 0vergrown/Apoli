package dev.overgrown.apoli.data;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.dev.DevMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BodyHit {

    public static final int PROJECTILE = 0;
    public static final int MELEE = 1;
    public static final int POSITION = 2;
    public static final int TAGGED = 3;
    public static final int RANDOM = 4;

    private static final String[] METHODS = {"projectile", "melee", "position", "damage type tag", "random"};

    private static final double GRAZE = 16.0;
    private static final double GRAZE_BLOCKS = 0.0625;

    public static final TagKey<EntityType<?>> HUMANOID = TagKey.create(Registries.ENTITY_TYPE, Apoli.id("humanoid"));

    private record Route(TagKey<DamageType> tag, BodyPart part) {}

    private static final Route[] ROUTES = {
        route("head", BodyParts.HEAD),
        route("body", BodyParts.BODY),
        route("arms", BodyParts.ARMS),
        route("hands", BodyParts.HANDS),
        route("legs", BodyParts.LEGS),
        route("feet", BodyParts.FEET)
    };

    private static Route route(String name, BodyPart part) {
        return new Route(TagKey.create(Registries.DAMAGE_TYPE, Apoli.id("body_part/" + name)), part);
    }

    private static final class Cache {
        LivingEntity target;
        DamageSource source;
        long time = Long.MIN_VALUE;
        BodyHit hit;
    }

    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

    private final int limb;
    private final float x;
    private final float y;
    private final float z;
    private final double normalX;
    private final double normalY;
    private final double normalZ;
    private final int method;
    private final boolean precise;

    private BodyHit(int limb, double x, double y, double z, double normalX, double normalY, double normalZ, int method,
                    boolean precise) {
        this.limb = limb;
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.method = method;
        this.precise = precise && method <= POSITION;
    }

    public int limb() {
        return limb;
    }

    public int method() {
        return method;
    }

    public boolean precise() {
        return precise;
    }

    public boolean in(BodyPart part, LivingEntity target) {
        return part.sided(target).contains(limb, x, y, z);
    }

    public boolean within(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        return normalX >= minX && normalX <= maxX
            && normalY >= minY && normalY <= maxY
            && normalZ >= minZ && normalZ <= maxZ;
    }

    public static BodyHit resolve(LivingEntity target, DamageSource source) {
        Cache cache = CACHE.get();
        long time = target.level().getGameTime();
        if (cache.target == target && cache.source == source && cache.time == time) {
            return cache.hit;
        }
        BodyHit hit = compute(target, source);
        cache.target = target;
        cache.source = source;
        cache.time = time;
        cache.hit = hit;
        if (DevMode.any()) {
            DevMode.report(target, hit.describe(target));
        }
        return hit;
    }

    public static boolean isHumanoid(LivingEntity entity) {
        return entity instanceof Player || entity.getType().is(HUMANOID);
    }

    private static BodyHit compute(LivingEntity target, DamageSource source) {
        RandomSource random = target.getRandom();
        for (Route route : ROUTES) {
            if (source.is(route.tag())) return tagged(target, route.part(), TAGGED, random);
        }

        AABB box = target.getBoundingBox();
        Vec3 centre = box.getCenter();
        double reach = box.getXsize() + box.getYsize() + box.getZsize();
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != target) {
            if (direct instanceof Player player) {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getViewVector(1.0F);
                double length = eye.distanceTo(centre) + reach;
                return cast(target, eye.x, eye.y, eye.z, look.x * length, look.y * length, look.z * length, MELEE);
            }
            if (direct instanceof LivingEntity living) {
                double originY = living.getY() + living.getBbHeight() * 0.55;
                double aimY = Mth.clamp(originY, box.minY, box.maxY);
                return towards(target, living.getX(), originY, living.getZ(), centre.x, aimY, centre.z, reach, MELEE);
            }
            Vec3 origin = direct.position();
            Vec3 motion = direct.getDeltaMovement();
            double length = origin.distanceTo(centre) + reach;
            double speed = motion.length();
            if (speed > 1.0E-3) {
                double scale = length / speed;
                return cast(target, origin.x, origin.y, origin.z, motion.x * scale, motion.y * scale, motion.z * scale,
                    PROJECTILE);
            }
            return towards(target, origin.x, origin.y, origin.z, centre.x, centre.y, centre.z, reach, PROJECTILE);
        }

        Vec3 position = source.getSourcePosition();
        if (position != null) {
            return towards(target, position.x, position.y, position.z, centre.x, centre.y, centre.z, reach, POSITION);
        }
        return tagged(target, BodyParts.WHOLE, RANDOM, random);
    }

    private static BodyHit towards(LivingEntity target, double ox, double oy, double oz,
                                   double tx, double ty, double tz, double reach, int method) {
        double dx = tx - ox;
        double dy = ty - oy;
        double dz = tz - oz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6) {
            return cast(target, ox, oy + reach, oz, 0.0, -reach * 2.0, 0.0, method);
        }
        double scale = (length + reach) / length;
        return cast(target, ox, oy, oz, dx * scale, dy * scale, dz * scale, method);
    }

    private static BodyHit cast(LivingEntity target, double ox, double oy, double oz,
                                double dx, double dy, double dz, int method) {
        if (!isHumanoid(target)) {
            return castBox(target, ox, oy, oz, dx, dy, dz, method);
        }
        HumanoidPose pose = HumanoidPose.of(target);
        BodyFrame frame = BodyFrame.of(target);
        float[] poses = pose.poses();
        double[] scratch = new double[3];

        frame.worldToModel(ox - target.getX(), oy - target.getY(), oz - target.getZ(), scratch);
        double modelX = scratch[0];
        double modelY = scratch[1];
        double modelZ = scratch[2];
        frame.worldDirectionToModel(dx, dy, dz, scratch);
        double modelDX = scratch[0];
        double modelDY = scratch[1];
        double modelDZ = scratch[2];

        int hitLimb = -1;
        double hitT = Double.POSITIVE_INFINITY;
        double hitX = 0.0;
        double hitY = 0.0;
        double hitZ = 0.0;
        int nearLimb = HumanoidPose.BODY;
        double nearDistance = Double.POSITIVE_INFINITY;
        double nearX = 0.0;
        double nearY = 0.0;
        double nearZ = 0.0;

        for (int limb = 0; limb < HumanoidPose.PART_COUNT; limb++) {
            int at = limb * PoseMath.STRIDE;
            PoseMath.parentToLocal(poses, at, modelX, modelY, modelZ, scratch);
            double localX = scratch[0];
            double localY = scratch[1];
            double localZ = scratch[2];
            PoseMath.directionToLocal(poses, at, modelDX, modelDY, modelDZ, scratch);
            double localDX = scratch[0];
            double localDY = scratch[1];
            double localDZ = scratch[2];

            double t = slab(limb, localX, localY, localZ, localDX, localDY, localDZ);
            if (t >= 0.0 && t < hitT) {
                hitT = t;
                hitLimb = limb;
                hitX = localX + localDX * t;
                hitY = localY + localDY * t;
                hitZ = localZ + localDZ * t;
            }

            double centreX = (HumanoidPose.boxMin(limb, 0) + HumanoidPose.boxMax(limb, 0)) * 0.5;
            double centreY = (HumanoidPose.boxMin(limb, 1) + HumanoidPose.boxMax(limb, 1)) * 0.5;
            double centreZ = (HumanoidPose.boxMin(limb, 2) + HumanoidPose.boxMax(limb, 2)) * 0.5;
            double lengthSqr = localDX * localDX + localDY * localDY + localDZ * localDZ;
            double along = lengthSqr < 1.0E-12 ? 0.0 : Mth.clamp(
                ((centreX - localX) * localDX + (centreY - localY) * localDY + (centreZ - localZ) * localDZ) / lengthSqr,
                0.0, 1.0);
            double closestX = localX + localDX * along;
            double closestY = localY + localDY * along;
            double closestZ = localZ + localDZ * along;
            double clampedX = clamp(limb, 0, closestX);
            double clampedY = clamp(limb, 1, closestY);
            double clampedZ = clamp(limb, 2, closestZ);
            double distance = sqr(closestX - clampedX) + sqr(closestY - clampedY) + sqr(closestZ - clampedZ);
            if (distance < nearDistance) {
                nearDistance = distance;
                nearLimb = limb;
                nearX = clampedX;
                nearY = clampedY;
                nearZ = clampedZ;
            }
        }

        int limb = hitLimb >= 0 ? hitLimb : nearLimb;
        double localX = hitLimb >= 0 ? clamp(limb, 0, hitX) : nearX;
        double localY = hitLimb >= 0 ? clamp(limb, 1, hitY) : nearY;
        double localZ = hitLimb >= 0 ? clamp(limb, 2, hitZ) : nearZ;

        PoseMath.localToParent(poses, limb * PoseMath.STRIDE, localX, localY, localZ, scratch);
        frame.modelToWorld(scratch[0], scratch[1], scratch[2], scratch);
        return normalised(target, limb, localX, localY, localZ,
            target.getX() + scratch[0], target.getY() + scratch[1], target.getZ() + scratch[2], method,
            hitLimb >= 0 || nearDistance <= GRAZE);
    }

    private static BodyHit castBox(LivingEntity target, double ox, double oy, double oz,
                                   double dx, double dy, double dz, int method) {
        AABB box = target.getBoundingBox();
        Vec3 from = new Vec3(ox, oy, oz);
        Optional<Vec3> clip = box.clip(from, from.add(dx, dy, dz));
        double px;
        double py;
        double pz;
        boolean touched = true;
        if (clip.isPresent()) {
            px = clip.get().x;
            py = clip.get().y;
            pz = clip.get().z;
        } else if (box.contains(from)) {
            px = ox;
            py = oy;
            pz = oz;
        } else {
            Vec3 centre = box.getCenter();
            double lengthSqr = dx * dx + dy * dy + dz * dz;
            double along = lengthSqr < 1.0E-12 ? 0.0 : Mth.clamp(
                ((centre.x - ox) * dx + (centre.y - oy) * dy + (centre.z - oz) * dz) / lengthSqr, 0.0, 1.0);
            double rayX = ox + dx * along;
            double rayY = oy + dy * along;
            double rayZ = oz + dz * along;
            px = Mth.clamp(rayX, box.minX, box.maxX);
            py = Mth.clamp(rayY, box.minY, box.maxY);
            pz = Mth.clamp(rayZ, box.minZ, box.maxZ);
            touched = sqr(rayX - px) + sqr(rayY - py) + sqr(rayZ - pz) <= GRAZE_BLOCKS;
        }

        double[] normal = new double[3];
        normalise(target, px, py, pz, normal);
        double modelX = normal[0] * 5.0;
        double modelY = normal[1] <= 0.5 ? 24.0 - normal[1] * 24.0
            : normal[1] <= 0.88 ? 12.0 - (normal[1] - 0.5) / 0.38 * 12.0
            : -(normal[1] - 0.88) / 0.12 * 8.0;
        double modelZ = normal[2] * 2.0;

        float[] rest = HumanoidPose.REST.poses();
        double[] scratch = new double[3];
        int bestLimb = HumanoidPose.BODY;
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestX = 0.0;
        double bestY = 0.0;
        double bestZ = 0.0;
        for (int limb = 0; limb < HumanoidPose.PART_COUNT; limb++) {
            PoseMath.parentToLocal(rest, limb * PoseMath.STRIDE, modelX, modelY, modelZ, scratch);
            double clampedX = clamp(limb, 0, scratch[0]);
            double clampedY = clamp(limb, 1, scratch[1]);
            double clampedZ = clamp(limb, 2, scratch[2]);
            double distance = sqr(scratch[0] - clampedX) + sqr(scratch[1] - clampedY) + sqr(scratch[2] - clampedZ);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestLimb = limb;
                bestX = clampedX;
                bestY = clampedY;
                bestZ = clampedZ;
            }
        }
        return new BodyHit(bestLimb, bestX, bestY, bestZ, normal[0], normal[1], normal[2], method, touched);
    }

    private static BodyHit tagged(LivingEntity target, BodyPart part, int method, RandomSource random) {
        int limb = part.pickLimb(random);
        float[] local = new float[3];
        part.centreInto(limb, local);
        double[] scratch = new double[3];
        if (isHumanoid(target)) {
            HumanoidPose pose = HumanoidPose.of(target);
            PoseMath.localToParent(pose.poses(), limb * PoseMath.STRIDE, local[0], local[1], local[2], scratch);
            BodyFrame.of(target).modelToWorld(scratch[0], scratch[1], scratch[2], scratch);
            return normalised(target, limb, local[0], local[1], local[2],
                target.getX() + scratch[0], target.getY() + scratch[1], target.getZ() + scratch[2], method, false);
        }
        PoseMath.localToParent(HumanoidPose.REST.poses(), limb * PoseMath.STRIDE, local[0], local[1], local[2], scratch);
        double modelY = scratch[1];
        double normalY = modelY >= 12.0 ? (24.0 - modelY) / 24.0
            : modelY >= 0.0 ? 0.5 + (12.0 - modelY) / 12.0 * 0.38
            : 0.88 - modelY / 8.0 * 0.12;
        return new BodyHit(limb, local[0], local[1], local[2],
            Mth.clamp(scratch[0] / 5.0, -1.0, 1.0), Mth.clamp(normalY, 0.0, 1.0),
            Mth.clamp(scratch[2] / 2.0, -1.0, 1.0), method, false);
    }

    private static BodyHit normalised(LivingEntity target, int limb, double localX, double localY, double localZ,
                                      double worldX, double worldY, double worldZ, int method, boolean precise) {
        double[] normal = new double[3];
        normalise(target, worldX, worldY, worldZ, normal);
        return new BodyHit(limb, localX, localY, localZ, normal[0], normal[1], normal[2], method, precise);
    }

    private static void normalise(LivingEntity target, double px, double py, double pz, double[] out) {
        AABB box = target.getBoundingBox();
        double height = Math.max(box.maxY - box.minY, 1.0E-3);
        double halfWidth = Math.max(target.getBbWidth() * 0.5, 1.0E-3);
        double raw = Mth.clamp((Mth.clamp(py, box.minY, box.maxY) - box.minY) / height, 0.0, 1.0);
        double headStart = Mth.clamp((target.getEyeY() - box.minY) / height, 0.0, 0.99);
        double normalY = raw <= headStart
            ? (headStart > 1.0E-6 ? raw / headStart * 0.88 : 0.0)
            : 0.88 + (raw - headStart) / (1.0 - headStart) * 0.12;

        double yaw = Math.toRadians(target.yBodyRot);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double offsetX = px - (box.minX + box.maxX) * 0.5;
        double offsetZ = pz - (box.minZ + box.maxZ) * 0.5;
        out[0] = Mth.clamp((offsetX * forwardZ - offsetZ * forwardX) / halfWidth, -1.0, 1.0);
        out[1] = Mth.clamp(normalY, 0.0, 1.0);
        out[2] = Mth.clamp(-(offsetX * forwardX + offsetZ * forwardZ) / halfWidth, -1.0, 1.0);
    }

    private static double slab(int limb, double ox, double oy, double oz, double dx, double dy, double dz) {
        double enter = 0.0;
        double exit = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            double origin = axis == 0 ? ox : axis == 1 ? oy : oz;
            double direction = axis == 0 ? dx : axis == 1 ? dy : dz;
            double min = HumanoidPose.boxMin(limb, axis);
            double max = HumanoidPose.boxMax(limb, axis);
            if (Math.abs(direction) < 1.0E-9) {
                if (origin < min || origin > max) return -1.0;
                continue;
            }
            double near = (min - origin) / direction;
            double far = (max - origin) / direction;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            if (near > enter) enter = near;
            if (far < exit) exit = far;
            if (enter > exit) return -1.0;
        }
        return enter;
    }

    private static double clamp(int limb, int axis, double value) {
        return Mth.clamp(value, HumanoidPose.boxMin(limb, axis), HumanoidPose.boxMax(limb, axis));
    }

    private static double sqr(double value) {
        return value * value;
    }

    private String describe(LivingEntity target) {
        StringBuilder text = new StringBuilder("body part hit: ").append(HumanoidPose.nameOf(limb));
        List<BodyPart> parts = BodyParts.all();
        for (int i = 0; i < parts.size(); i++) {
            BodyPart part = parts.get(i);
            if (part.models() != 0 || part.isHanded() || part.isGroup()) continue;
            if (part.sided(target).contains(limb, x, y, z)) text.append(", ").append(part.name());
        }
        return text.append(" (").append(METHODS[method]).append(precise ? "" : ", imprecise").append(String.format(Locale.ROOT,
            ", x=%.2f y=%.2f z=%.2f)", normalX, normalY, normalZ)).toString();
    }
}
