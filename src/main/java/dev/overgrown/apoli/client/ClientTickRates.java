package dev.overgrown.apoli.client;

import dev.overgrown.apoli.tick.TickRates;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientTickRates {

    private static final Int2IntOpenHashMap RATES = new Int2IntOpenHashMap();
    private static final Int2ObjectOpenHashMap<Lerp> LERPS = new Int2ObjectOpenHashMap<>();
    private static int baseRate = 20;
    private static long gateTick;

    private static final class Lerp {
        double x;
        double y;
        double z;
        float yRot;
        float xRot;
        int steps;
    }

    static {
        RATES.defaultReturnValue(-1);
    }

    private ClientTickRates() {}

    public static void clear() {
        RATES.clear();
        LERPS.clear();
        baseRate = 20;
        gateTick = 0L;
    }

    public static void set(int entityId, int rate, int base) {
        baseRate = Math.max(1, base);
        if (entityId < 0) {
            RATES.clear();
            LERPS.clear();
            return;
        }
        if (rate < 0 || rate >= baseRate) {
            RATES.remove(entityId);
            LERPS.remove(entityId);
        } else {
            RATES.put(entityId, Math.max(0, rate));
        }
    }

    public static void beginClientTick(long gameTime) {
        gateTick = gameTime;
    }

    public static boolean idle() {
        return RATES.isEmpty();
    }

    private static Entity root(Entity entity) {
        return entity.isPassenger() ? entity.getRootVehicle() : entity;
    }

    public static int rateOf(Entity entity) {
        if (RATES.isEmpty()) return -1;
        return RATES.get(root(entity).getId());
    }

    private static int rateOfRoot(Entity root) {
        return RATES.isEmpty() ? -1 : RATES.get(root.getId());
    }

    public static boolean frozen(Entity entity) {
        return rateOf(entity) == 0;
    }

    public static int baseRate() {
        return baseRate;
    }

    private static boolean gated(Entity root, int rate) {
        if (rate < 0) return false;
        LocalPlayer self = Minecraft.getInstance().player;
        return self == null || self == root || root(self) != root;
    }

    public static boolean isGated(Entity entity) {
        Entity root = root(entity);
        return gated(root, rateOfRoot(root));
    }

    public static boolean simulatedOnClient(Entity entity) {
        return root(entity) instanceof LivingEntity;
    }

    public static boolean shouldTick(Entity entity) {
        Entity root = root(entity);
        int rate = rateOfRoot(root);
        if (!gated(root, rate)) return true;
        if (rate == 0) return false;
        return TickRates.gate(rate, baseRate, entity.level().getGameTime());
    }

    public static boolean slotDriven(Entity entity) {
        if (RATES.isEmpty()) return false;
        Entity root = root(entity);
        return root == Minecraft.getInstance().player && rateOfRoot(root) >= 0;
    }

    public static float slotPartial(Entity entity, float partial) {
        if (RATES.isEmpty()) return partial;
        if (!(root(entity) instanceof LivingEntity)) return partial;
        int rate = rateOf(entity);
        if (rate < 0 || rate >= baseRate) return partial;
        if (rate == 0) return 0.0F;
        long slot = Math.floorDiv(gateTick * (long) rate, (long) baseRate);
        long start = ceilDiv(slot * (long) baseRate, rate);
        long span = ceilDiv((slot + 1L) * (long) baseRate, rate) - start;
        if (span <= 1L) return partial;
        return Mth.clamp(((float) (gateTick - start) + partial) / (float) span, 0.0F, 1.0F);
    }

    public static float cameraPartial(float partial) {
        if (RATES.isEmpty()) return partial;
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || !slotDriven(camera)) return partial;
        return slotPartial(camera, partial);
    }

    private static long ceilDiv(long value, int divisor) {
        return -Math.floorDiv(-value, (long) divisor);
    }

    public static boolean captureLerp(Entity entity, double x, double y, double z, float yRot, float xRot) {
        if (RATES.isEmpty()) return false;
        Entity root = root(entity);
        int rate = rateOfRoot(root);
        if (!gated(root, rate)) {
            LERPS.remove(entity.getId());
            return false;
        }
        Lerp lerp = LERPS.get(entity.getId());
        if (lerp == null) {
            lerp = new Lerp();
            LERPS.put(entity.getId(), lerp);
        }
        boolean rotationOnly = lerp.steps > 0
            && Math.abs(x - entity.getX()) < 1.0E-6
            && Math.abs(y - entity.getY()) < 1.0E-6
            && Math.abs(z - entity.getZ()) < 1.0E-6;
        if (!rotationOnly) {
            lerp.x = x;
            lerp.y = y;
            lerp.z = z;
        }
        lerp.yRot = yRot;
        lerp.xRot = xRot;
        lerp.steps = rate <= 0 ? 1 : Math.max(1, (baseRate + rate - 1) / rate);
        return true;
    }

    public static int headLerpSteps(Entity entity, int steps) {
        if (RATES.isEmpty()) return steps;
        Entity root = root(entity);
        return gated(root, rateOfRoot(root)) ? 1 : steps;
    }

    public static float walkAnimationInput(Entity entity, float distance) {
        if (RATES.isEmpty()) return distance;
        Entity root = root(entity);
        int rate = rateOfRoot(root);
        if (rate <= 0 || !gated(root, rate)) return distance;
        return distance * Math.max(1, (baseRate + rate - 1) / rate);
    }

    public static void advanceLerp(Entity entity) {
        if (LERPS.isEmpty()) return;
        int id = entity.getId();
        Lerp lerp = LERPS.get(id);
        if (lerp == null) return;
        if (lerp.steps <= 0) {
            LERPS.remove(id);
            return;
        }
        double step = 1.0 / lerp.steps;
        entity.setPos(
            Mth.lerp(step, entity.getX(), lerp.x),
            Mth.lerp(step, entity.getY(), lerp.y),
            Mth.lerp(step, entity.getZ(), lerp.z));
        entity.setYRot(Mth.rotLerp((float) step, entity.getYRot(), lerp.yRot));
        entity.setXRot(Mth.rotLerp((float) step, entity.getXRot(), lerp.xRot));
        lerp.steps--;
        if (lerp.steps <= 0) LERPS.remove(id);
    }

    public static void keepRenderPose(Entity entity) {
        if (frozen(entity) || !slotDriven(entity)) entity.setOldPosAndRot();
        keepViewBob(entity);
        List<Entity> passengers = entity.getPassengers();
        for (int i = 0, n = passengers.size(); i < n; i++) {
            keepRenderPose(passengers.get(i));
        }
    }

    public static void keepViewBob(Entity entity) {
        if (!(entity instanceof LocalPlayer player)) return;
        if (Minecraft.getInstance().getCameraEntity() != player) return;
        player.yBobO = player.yBob;
        player.xBobO = player.xBob;
        player.xBob += (player.getXRot() - player.xBob) * 0.5F;
        player.yBob += (player.getYRot() - player.yBob) * 0.5F;
    }
}
