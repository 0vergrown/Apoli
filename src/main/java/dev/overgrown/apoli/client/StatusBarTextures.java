package dev.overgrown.apoli.client;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeGate;
import dev.overgrown.apoli.power.builtin.StatusBarTexturePower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatusBarTextures {
    private static final PowerTypeGate GATE = new PowerTypeGate(ApoliIds.STATUS_BAR_TEXTURE);
    private static final Comparator<StatusBarTexturePower.Config> BY_PRIORITY =
        Comparator.comparingInt(StatusBarTexturePower.Config::priority).reversed();

    private static final List<StatusBarTexturePower.Config> ACTIVE = new ArrayList<>();
    private static final List<StatusBarTexturePower.Config> APPLIED = new ArrayList<>();

    private static Map<ResourceLocation, ResourceLocation> sprites = Map.of();
    private static @Nullable ResourceLocation sheet;
    private static boolean inHud;
    private static boolean replacing;
    private static long resolvedTime = Long.MIN_VALUE;
    private static int resolvedGeneration = -1;
    private static @Nullable LocalPlayer resolvedFor;

    private StatusBarTextures() {}

    public static void beginHud() {
        inHud = false;
        if (!GATE.inUse()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        long time = player.level().getGameTime();
        int generation = ApoliPowers.generation();
        if (time != resolvedTime || generation != resolvedGeneration || player != resolvedFor) {
            resolvedTime = time;
            resolvedGeneration = generation;
            resolvedFor = player;
            resolve(player);
        }
        inHud = sheet != null || !sprites.isEmpty();
    }

    public static void endHud() {
        inHud = false;
    }

    public static boolean active() {
        return inHud && !replacing;
    }

    public static @Nullable ResourceLocation sprite(ResourceLocation original) {
        return sprites.get(original);
    }

    public static @Nullable ResourceLocation sheet() {
        return sheet;
    }

    public static void beginReplacement() {
        replacing = true;
    }

    public static void endReplacement() {
        replacing = false;
    }

    public static void clear() {
        ACTIVE.clear();
        APPLIED.clear();
        sprites = Map.of();
        sheet = null;
        inHud = false;
        resolvedTime = Long.MIN_VALUE;
        resolvedFor = null;
    }

    private static void resolve(LocalPlayer player) {
        ACTIVE.clear();
        PowerContainer container = PowerContainer.of(player);
        if (container != null && !container.isEmpty()) {
            List<ResourceLocation> powers = container.powersOfType(ApoliIds.STATUS_BAR_TEXTURE);
            EntityCtx ctx = null;
            for (int i = 0; i < powers.size(); i++) {
                ResourceLocation powerId = powers.get(i);
                if (container.isSuppressed(powerId)) continue;
                Power power = ApoliPowers.get(powerId);
                if (power == null || !(power.config() instanceof StatusBarTexturePower.Config cfg)) continue;
                if (power.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(player, player.level());
                    if (!power.condition().get().test(ctx)) continue;
                }
                ACTIVE.add(cfg);
            }
        }
        if (sameAsApplied()) return;
        APPLIED.clear();
        APPLIED.addAll(ACTIVE);
        if (ACTIVE.isEmpty()) {
            sprites = Map.of();
            sheet = null;
        } else if (ACTIVE.size() == 1) {
            StatusBarTexturePower.Config only = ACTIVE.get(0);
            sprites = only.textureMap();
            sheet = only.texture().orElse(null);
        } else {
            merge();
        }
    }

    private static boolean sameAsApplied() {
        if (ACTIVE.size() != APPLIED.size()) return false;
        for (int i = 0; i < ACTIVE.size(); i++) {
            if (ACTIVE.get(i) != APPLIED.get(i)) return false;
        }
        return true;
    }

    private static void merge() {
        List<StatusBarTexturePower.Config> ordered = new ArrayList<>(ACTIVE);
        ordered.sort(BY_PRIORITY);
        Map<ResourceLocation, ResourceLocation> merged = new HashMap<>();
        ResourceLocation topSheet = null;
        for (StatusBarTexturePower.Config cfg : ordered) {
            cfg.textureMap().forEach(merged::putIfAbsent);
            if (topSheet == null && cfg.texture().isPresent()) topSheet = cfg.texture().get();
        }
        sprites = merged;
        sheet = topSheet;
    }
}
