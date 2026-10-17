package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.ClientRenderFlags;
import dev.overgrown.apoli.power.builtin.EntityGlowPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @ModifyReturnValue(method = "isCurrentlyGlowing", at = @At("RETURN"))
    private boolean apoli$entityGlow(boolean original) {
        return original || apoli$matchingGlow() != null;
    }

    @ModifyReturnValue(method = "getTeamColor", at = @At("RETURN"))
    private int apoli$glowColor(int original) {
        EntityGlowPower.Config cfg = apoli$matchingGlow();
        if (cfg == null || cfg.useTeams()) return original;
        int r = Mth.clamp((int) (cfg.red() * 255f), 0, 255);
        int g = Mth.clamp((int) (cfg.green() * 255f), 0, 255);
        int b = Mth.clamp((int) (cfg.blue() * 255f), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    @Unique
    private EntityGlowPower.Config apoli$matchingGlow() {
        if (!EntityGlowPower.inUse()) return null;
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide()) return null;
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer == null) return null;
        EntityGlowPower.Config cfg = apoli$find(self, viewer, true);
        if (cfg != null) return cfg;
        if (self == viewer || !ClientRenderFlags.has(ClientRenderFlags.ENTITY_GLOW)) return null;
        return apoli$find(self, viewer, false);
    }

    @Unique
    private static EntityGlowPower.Config apoli$find(Entity glowing, LocalPlayer viewer, boolean selfGlow) {
        return selfGlow
            ? EntityGlowPower.match(glowing, viewer, glowing, true)
            : EntityGlowPower.match(viewer, glowing, glowing, false);
    }
}
