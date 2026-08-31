package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.ScrollDirection;
import dev.overgrown.apoli.network.payload.ScrollWheelC2S;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ActionOnScrollWheelPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

@Environment(EnvType.CLIENT)
public final class ScrollWatcher {
    private ScrollWatcher() {}

    public static boolean onScroll(double delta) {
        if (delta == 0) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return false;
        if (BlockedKeys.blocksScroll()) return true;

        ScrollDirection notch = ScrollDirection.ofDelta(delta);
        if (!ActionOnScrollWheelPower.anyAccepting(mc.player, notch)) return false;

        boolean[] prevent = new boolean[]{false};
        PowerLookup.forEach(mc.player, ApoliIds.ACTION_ON_SCROLL_WHEEL, ActionOnScrollWheelPower.Config.class, cfg -> {
            if (cfg.preventHotbarChange() && cfg.direction().accepts(notch)) prevent[0] = true;
        });

        if (ClientPlayNetworking.canSend(ScrollWheelC2S.CHANNEL)) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new ScrollWheelC2S(notch == ScrollDirection.UP, 1).write(buf);
            ClientPlayNetworking.send(ScrollWheelC2S.CHANNEL, buf);
        }
        return prevent[0];
    }
}
