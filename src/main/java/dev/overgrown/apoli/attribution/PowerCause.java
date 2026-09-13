package dev.overgrown.apoli.attribution;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.ModCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PowerCause {

    @FunctionalInterface
    public interface Describer {
        @Nullable String describe(Entity holder, ResourceLocation powerId);
    }

    public static final boolean ACTIVE = ModCompat.LEDGER || ModCompat.INDEXOR;

    public static final int MAX_LABEL_LENGTH = 30;

    private static final int DEPTH_LIMIT = 16;

    private static final Entity[] HOLDERS = new Entity[DEPTH_LIMIT];

    private static final ResourceLocation[] POWERS = new ResourceLocation[DEPTH_LIMIT];

    private static final List<Describer> DESCRIBERS = new CopyOnWriteArrayList<>();

    private static int depth;

    private PowerCause() {}

    public static void bootstrap() {
        if (!ACTIVE) return;
        Apoli.LOGGER.info("[Apoli] Block-logger compatibility active ({}): world edits made by a power "
            + "are labelled with the power (and its origin) that caused them.",
            ModCompat.LEDGER ? "Ledger" : "Indexor");
    }

    public static void addDescriber(Describer describer) {
        DESCRIBERS.add(describer);
    }

    public static boolean push(@Nullable Entity holder, @Nullable ResourceLocation powerId) {
        if (!ACTIVE) return false;
        if (holder == null || powerId == null || depth >= DEPTH_LIMIT) return false;
        if (!(holder.level() instanceof ServerLevel)) return false;
        HOLDERS[depth] = holder;
        POWERS[depth] = powerId;
        depth++;
        return true;
    }

    public static void pop() {
        if (!ACTIVE || depth == 0) return;
        depth--;
        HOLDERS[depth] = null;
        POWERS[depth] = null;
    }

    public static void clear() {
        if (!ACTIVE) return;
        while (depth > 0) pop();
    }

    public static @Nullable Entity holder() {
        return depth == 0 ? null : HOLDERS[depth - 1];
    }

    public static @Nullable ResourceLocation powerId() {
        return depth == 0 ? null : POWERS[depth - 1];
    }

    public static @Nullable String label() {
        if (!ACTIVE || depth == 0) return null;
        Entity holder = HOLDERS[depth - 1];
        ResourceLocation powerId = POWERS[depth - 1];
        if (holder == null || powerId == null) return null;
        for (Describer describer : DESCRIBERS) {
            String described = describer.describe(holder, powerId);
            if (described != null && !described.isEmpty()) return fit(described);
        }
        return fit(powerId.getPath());
    }

    public static String fit(String raw) {
        int length = raw.length();
        return length <= MAX_LABEL_LENGTH ? raw : raw.substring(length - MAX_LABEL_LENGTH);
    }
}
