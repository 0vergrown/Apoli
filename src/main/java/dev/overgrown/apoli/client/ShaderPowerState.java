package dev.overgrown.apoli.client;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ShaderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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

    public static void clientTick(Minecraft mc) {
        desired = null;
        desiredToggleable = true;
        if (mc.level == null) return;

        Entity viewer = mc.getCameraEntity();
        if (viewer == null) viewer = mc.player;
        if (viewer == null) return;

        EntityCtx ctx = null;
        int bestPriority = Integer.MIN_VALUE;
        for (ResourceLocation powerId : ClientPowerState.powersFor(viewer.getId()).keySet()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof ShaderPower)) continue;
            if (!(power.config() instanceof ShaderPower.Config cfg)) continue;
            if (BROKEN.contains(cfg.shader())) continue;
            if (cfg.priority() < bestPriority) continue;
            if (ClientPowerState.suppressedFor(viewer.getId()).contains(powerId)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(viewer, viewer.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            bestPriority = cfg.priority();
            desired = cfg.shader();
            desiredToggleable = cfg.toggleable();
        }
    }

    public static void sync(GameRendererAccess renderer) {
        ResourceLocation want = desired;
        if (want == null) {
            if (applied != null) {
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
            desired = null;
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
