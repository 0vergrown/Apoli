package dev.overgrown.apoli.compat.flywheel;

import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.ClientTickRates;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.client.render.ClientRenderFlags;
import dev.overgrown.apoli.power.builtin.EmissivePower;
import dev.overgrown.apoli.power.builtin.PreventEntityRenderHandler;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ApoliRenderClaims {
    private ApoliRenderClaims() {}

    public static boolean needsVanillaRenderer(Minecraft mc, Entity entity) {
        LocalPlayer viewer = mc.player;
        if (viewer != null && ClientRenderFlags.has(ClientRenderFlags.PREVENT_ENTITY_RENDER)
            && PreventEntityRenderHandler.shouldHide(viewer, entity)) return true;
        if (ClientDisguiseManager.hasAny() && ClientDisguiseManager.get(entity.getId()) != null) return true;
        if (mc.shouldEntityAppearGlowing(entity)) return true;
        if (entity.getVehicle() instanceof Player) return true;
        if (!ClientTickRates.idle() && ClientTickRates.rateOf(entity) >= 0) return true;
        if (!Scales.untouched(entity) && (Scales.applied(entity, ScaleTypes.MODEL_WIDTH) != 1.0F
            || Scales.applied(entity, ScaleTypes.MODEL_HEIGHT) != 1.0F)) return true;
        if (ClientPowerState.powersFor(entity.getId()).isEmpty()) return false;
        return entity instanceof LivingEntity || EmissivePower.selfLitLuminanceOf(entity) > 0;
    }
}
