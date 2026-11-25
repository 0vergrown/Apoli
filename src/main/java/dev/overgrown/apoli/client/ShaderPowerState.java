package dev.overgrown.apoli.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ShaderPowerState {

    public interface GameRendererAccess {
        @Nullable PostChain apoli$loadDetachedEffect(ResourceLocation shader);
    }

    private static final Set<ResourceLocation> BROKEN = new HashSet<>();

    private static @Nullable ResourceLocation desired;
    private static boolean desiredToggleable = true;
    private static boolean toggledOff;
    private static @Nullable PostChain chain;
    private static @Nullable ResourceLocation chainId;

    private ShaderPowerState() {}

    public static void accept(@Nullable ResourceLocation shader, boolean toggleable) {
        if (!Objects.equals(shader, desired)) toggledOff = false;
        desired = shader;
        desiredToggleable = toggleable;
    }

    public static void process(GameRendererAccess renderer, float timeDelta) {
        ResourceLocation want = desired;
        if (want == null) {
            if (chain != null) close();
            return;
        }
        PostChain current = chain;
        if (current == null || !want.equals(chainId)) {
            close();
            if (BROKEN.contains(want)) return;
            current = load(renderer, want);
            if (current == null) return;
            chain = current;
            chainId = want;
        }
        if (toggledOff) return;
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        current.process(timeDelta);
    }

    private static @Nullable PostChain load(GameRendererAccess renderer, ResourceLocation shader) {
        PostChain loaded;
        try {
            loaded = renderer.apoli$loadDetachedEffect(shader);
        } catch (RuntimeException e) {
            dev.overgrown.apoli.Apoli.LOGGER.warn("[Apoli] apoli:shader could not build '{}'", shader, e);
            loaded = null;
        }
        if (loaded == null && BROKEN.add(shader)) warnBroken(shader);
        return loaded;
    }

    private static void warnBroken(ResourceLocation shader) {
        String path = shader.getPath();
        if (path.startsWith("shaders/post/") && path.endsWith(".json")) {
            dev.overgrown.apoli.Apoli.LOGGER.warn(
                "[Apoli] apoli:shader asked for '{}' but the game loaded no post effect for it. "
                + "Either assets/{}/{} is missing or broken, or another mod is "
                + "blocking post-processing shaders (Sodium Extra's \"Prevent Shaders\" does exactly that).",
                shader, shader.getNamespace(), path);
        } else {
            dev.overgrown.apoli.Apoli.LOGGER.warn(
                "[Apoli] apoli:shader asked for '{}' but the game loaded no post effect for it. "
                + "The shader field is the full path of a post chain file, like "
                + "minecraft:shaders/post/creeper.json, so the game looked for assets/{}/{}.",
                shader, shader.getNamespace(), path);
        }
    }

    public static void resize(int width, int height) {
        if (chain != null) chain.resize(width, height);
    }

    public static void toggle() {
        if (desired != null && desiredToggleable) toggledOff = !toggledOff;
    }

    public static void invalidate() {
        if (RenderSystem.isOnRenderThread()) {
            reset();
        } else {
            RenderSystem.recordRenderCall(ShaderPowerState::reset);
        }
    }

    public static void clear() {
        desired = null;
        desiredToggleable = true;
        toggledOff = false;
        invalidate();
    }

    private static void reset() {
        BROKEN.clear();
        close();
    }

    private static void close() {
        PostChain current = chain;
        chain = null;
        chainId = null;
        if (current != null) current.close();
    }
}
