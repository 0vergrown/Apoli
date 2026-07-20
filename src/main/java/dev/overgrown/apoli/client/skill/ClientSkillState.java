package dev.overgrown.apoli.client.skill;

import dev.overgrown.apoli.network.payload.SkillDefsSyncS2C;
import dev.overgrown.apoli.network.payload.SkillStateSyncS2C;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientSkillState {
    private ClientSkillState() {}

    private static final Map<ResourceLocation, SkillDefsSyncS2C.Entry> DEFS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> CHILDREN = new HashMap<>();
    private static final List<ResourceLocation> ROOTS = new ArrayList<>();
    private static final Map<ResourceLocation, ResourceLocation> ROOT_OF = new HashMap<>();
    private static final Map<ResourceLocation, Integer> POINTS = new HashMap<>();
    private static final Set<ResourceLocation> PURCHASED = new HashSet<>();
    private static final Set<ResourceLocation> HIDDEN = new HashSet<>();
    private static final Set<ResourceLocation> LOCKED = new HashSet<>();
    private static final Set<ResourceLocation> NON_REFUNDABLE = new HashSet<>();

    public static void applyDefs(SkillDefsSyncS2C payload) {
        DEFS.clear();
        CHILDREN.clear();
        ROOTS.clear();
        ROOT_OF.clear();
        for (SkillDefsSyncS2C.Entry e : payload.entries()) DEFS.put(e.id(), e);
        for (SkillDefsSyncS2C.Entry e : DEFS.values()) {
            if (e.parent().isPresent() && DEFS.containsKey(e.parent().get())) {
                CHILDREN.computeIfAbsent(e.parent().get(), k -> new ArrayList<>()).add(e.id());
            } else {
                ROOTS.add(e.id());
            }
        }
        java.util.Comparator<ResourceLocation> byOrder = java.util.Comparator.comparingInt(id -> DEFS.get(id).order());
        for (List<ResourceLocation> siblings : CHILDREN.values()) siblings.sort(byOrder);
        ROOTS.sort(byOrder);
        for (ResourceLocation id : DEFS.keySet()) {
            ResourceLocation cur = id;
            int guard = 0;
            while (true) {
                SkillDefsSyncS2C.Entry e = DEFS.get(cur);
                if (e == null || e.parent().isEmpty() || !DEFS.containsKey(e.parent().get()) || guard++ > 256) break;
                cur = e.parent().get();
            }
            ROOT_OF.put(id, cur);
        }
    }

    public static void applyState(SkillStateSyncS2C payload) {
        POINTS.clear();
        POINTS.putAll(payload.points());
        PURCHASED.clear();
        PURCHASED.addAll(payload.purchased());
        HIDDEN.clear();
        HIDDEN.addAll(payload.hidden());
        LOCKED.clear();
        LOCKED.addAll(payload.locked());
        NON_REFUNDABLE.clear();
        NON_REFUNDABLE.addAll(payload.nonRefundable());
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof SkillTreeScreen screen) {
            screen.refreshFromState();
        }
    }

    public static void clear() {
        DEFS.clear();
        CHILDREN.clear();
        ROOTS.clear();
        ROOT_OF.clear();
        POINTS.clear();
        PURCHASED.clear();
        HIDDEN.clear();
        LOCKED.clear();
    }

    public static boolean isHidden(ResourceLocation id) {
        return HIDDEN.contains(id);
    }

    public static boolean canRefund(ResourceLocation id) {
        if (!isPurchased(id) || isHidden(id)) return false;
        if (NON_REFUNDABLE.contains(rootOf(id))) return false;
        for (ResourceLocation child : childrenOf(id)) {
            if (isPurchased(child)) return false;
        }
        return true;
    }

    public static boolean isLocked(ResourceLocation id) {
        return LOCKED.contains(id);
    }

    public static boolean hasAnyTree() {
        return !ROOTS.isEmpty();
    }

    public static List<ResourceLocation> roots() {
        return ROOTS;
    }

    public static List<ResourceLocation> childrenOf(ResourceLocation id) {
        return CHILDREN.getOrDefault(id, List.of());
    }

    @Nullable
    public static SkillDefsSyncS2C.Entry def(ResourceLocation id) {
        return DEFS.get(id);
    }

    public static ResourceLocation rootOf(ResourceLocation id) {
        return ROOT_OF.getOrDefault(id, id);
    }

    public static int points(ResourceLocation root) {
        return POINTS.getOrDefault(root, 0);
    }

    public static boolean isPurchased(ResourceLocation id) {
        return PURCHASED.contains(id);
    }

    public static boolean isObtained(ResourceLocation id) {
        if (isPurchased(id)) return true;
        SkillDefsSyncS2C.Entry e = DEFS.get(id);
        return e != null && e.powers().isEmpty();
    }

    public static boolean parentSatisfied(SkillDefsSyncS2C.Entry e) {
        return e.parent().isEmpty() || isObtained(e.parent().get());
    }

    public static boolean canBuy(SkillDefsSyncS2C.Entry e) {
        return !e.powers().isEmpty()
            && !isPurchased(e.id())
            && !isHidden(e.id())
            && !isLocked(e.id())
            && parentSatisfied(e)
            && points(rootOf(e.id())) >= e.cost();
    }
}
