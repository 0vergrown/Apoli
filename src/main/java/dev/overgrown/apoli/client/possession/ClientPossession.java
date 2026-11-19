package dev.overgrown.apoli.client.possession;

import dev.overgrown.apoli.entity.possession.Possession;
import dev.overgrown.apoli.mixin.disguise.WalkAnimationStateAccessor;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ClientPossession {
    private static final Int2IntOpenHashMap PAIRS = new Int2IntOpenHashMap();
    private static boolean active;

    static {
        PAIRS.defaultReturnValue(-1);
    }

    private ClientPossession() {}

    public static boolean active() {
        return active;
    }

    public static void apply(int possessorId, int hostId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        int previousHostId = PAIRS.remove(possessorId);
        Entity possessor = level.getEntity(possessorId);
        if (possessor != null && Possession.hostOf(possessor) != null) {
            Possession.unlink(possessor);
            possessor.refreshDimensions();
        }
        if (previousHostId >= 0) {
            Entity previousHost = level.getEntity(previousHostId);
            if (previousHost != null) Possession.unlinkHost(previousHost);
        }
        if (hostId >= 0) {
            PAIRS.put(possessorId, hostId);
            resolve(possessor, level.getEntity(hostId));
        }
        active = !PAIRS.isEmpty();
    }

    public static void tick(Minecraft mc) {
        if (!active || mc.level == null) return;
        ObjectIterator<Int2IntMap.Entry> it = PAIRS.int2IntEntrySet().fastIterator();
        while (it.hasNext()) {
            Int2IntMap.Entry pair = it.next();
            Entity possessor = mc.level.getEntity(pair.getIntKey());
            Entity host = mc.level.getEntity(pair.getIntValue());
            if (possessor == null) {
                if (host != null) Possession.unlinkHost(host);
                it.remove();
                continue;
            }
            if (host == null) continue;
            resolve(possessor, host);
            follow(possessor, host);
        }
        active = !PAIRS.isEmpty();
    }

    public static boolean isCameraHost(Entity entity) {
        Entity possessor = Possession.possessorOf(entity);
        return possessor != null && possessor == Minecraft.getInstance().getCameraEntity();
    }

    public static void clear() {
        PAIRS.clear();
        active = false;
    }

    private static void resolve(Entity possessor, Entity host) {
        if (possessor == null || host == null || Possession.hostOf(possessor) == host) return;
        Possession.link(possessor, host);
        possessor.refreshDimensions();
    }

    private static void follow(Entity possessor, Entity host) {
        host.setPos(possessor.getX(), possessor.getY(), possessor.getZ());
        host.xo = possessor.xo;
        host.yo = possessor.yo;
        host.zo = possessor.zo;
        host.xOld = possessor.xOld;
        host.yOld = possessor.yOld;
        host.zOld = possessor.zOld;
        host.setYRot(possessor.getYRot());
        host.yRotO = possessor.yRotO;
        host.setXRot(possessor.getXRot());
        host.xRotO = possessor.xRotO;
        host.setOnGround(possessor.onGround());
        host.setShiftKeyDown(possessor.isShiftKeyDown());
        if (host instanceof LivingEntity livingHost && possessor instanceof LivingEntity livingPossessor) {
            livingHost.yHeadRot = livingPossessor.yHeadRot;
            livingHost.yHeadRotO = livingPossessor.yHeadRotO;
            livingHost.yBodyRot = livingPossessor.yBodyRot;
            livingHost.yBodyRotO = livingPossessor.yBodyRotO;
            WalkAnimationStateAccessor to = (WalkAnimationStateAccessor) livingHost.walkAnimation;
            to.apoli$setSpeedOld(((WalkAnimationStateAccessor) livingPossessor.walkAnimation).apoli$getSpeedOld());
            livingHost.walkAnimation.setSpeed(livingPossessor.walkAnimation.speed());
            to.apoli$setPosition(livingPossessor.walkAnimation.position());
        }
    }
}
