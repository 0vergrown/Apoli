package dev.overgrown.apoli.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ShaderPowerState {

    public interface GameRendererAccess {
        void apoli$loadEffect(ResourceLocation shader);

        void apoli$shutdownEffect();

        @Nullable PostChain apoli$postEffect();
    }

    private static final Set<ResourceLocation> BROKEN = new HashSet<>();

    private static @Nullable ResourceLocation desired;
    private static @Nullable ResourceLocation applied;
    private static @Nullable PostChain lastSeen;
    private static boolean desiredToggleable = true;

    private ShaderPowerState() {}

    public static void accept(@Nullable ResourceLocation shader, boolean toggleable) {
        desired = shader;
        desiredToggleable = toggleable;
    }

    public static void sync(GameRendererAccess renderer) {
        ResourceLocation want = desired;
        if (want != null && BROKEN.contains(want)) want = null;
        if (want == null) {
            PostChain current = renderer.apoli$postEffect();
            if (applied != null || (lastSeen != null && current == lastSeen)) {
                renderer.apoli$shutdownEffect();
                applied = null;
                lastSeen = null;
            }
            return;
        }
        PostChain current = renderer.apoli$postEffect();
        if (want.equals(applied) && current != null && current == lastSeen) return;

        renderer.apoli$loadEffect(want);
        PostChain loaded = renderer.apoli$postEffect();
        if (loaded == null) {
            BROKEN.add(want);
            applied = null;
            lastSeen = null;
            return;
        }
        applied = want;
        lastSeen = loaded;
    }

    public static boolean locksToggle() {
        return applied != null && !desiredToggleable;
    }

    public static void invalidate() {
        BROKEN.clear();
        applied = null;
        lastSeen = null;
    }

    public static void clear() {
        invalidate();
        desired = null;
        desiredToggleable = true;
    }
}
