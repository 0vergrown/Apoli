package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.ScrollDirection;
import dev.overgrown.apoli.network.payload.ScrollWheelC2S;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ActionOnScrollWheelPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class ScrollWatcher {
    private ScrollWatcher() {}

    public static boolean onScroll(double delta) {
        if (delta == 0) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return false;

        ScrollDirection notch = ScrollDirection.ofDelta(delta);
        if (!ActionOnScrollWheelPower.anyAccepting(mc.player, notch)) return false;

        boolean[] prevent = new boolean[]{false};
        PowerLookup.forEach(mc.player, ApoliIds.ACTION_ON_SCROLL_WHEEL, ActionOnScrollWheelPower.Config.class, cfg -> {
            if (cfg.preventHotbarChange() && cfg.direction().accepts(notch)) prevent[0] = true;
        });

        if (ClientPlayNetworking.canSend(ScrollWheelC2S.TYPE)) {
            ClientPlayNetworking.send(new ScrollWheelC2S(notch == ScrollDirection.UP, 1));
        }
        return prevent[0];
    }
}
