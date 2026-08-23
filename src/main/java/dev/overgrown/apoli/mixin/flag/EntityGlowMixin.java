package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.EntityGlowPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void apoli$entityGlow(CallbackInfoReturnable<Boolean> cir) {
        if (apoli$matchingGlow() != null) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void apoli$glowColor(CallbackInfoReturnable<Integer> cir) {
        EntityGlowPower.Config cfg = apoli$matchingGlow();
        if (cfg != null && !cfg.useTeams()) {
            int r = Mth.clamp((int) (cfg.red() * 255f), 0, 255);
            int g = Mth.clamp((int) (cfg.green() * 255f), 0, 255);
            int b = Mth.clamp((int) (cfg.blue() * 255f), 0, 255);
            cir.setReturnValue((r << 16) | (g << 8) | b);
        }
    }

    @Unique
    private EntityGlowPower.Config apoli$matchingGlow() {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide()) return null;
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer == null) return null;
        EntityGlowPower.Config cfg = apoli$find(self, viewer, true);
        if (cfg != null) return cfg;
        return self == viewer ? null : apoli$find(self, viewer, false);
    }

    @Unique
    private static EntityGlowPower.Config apoli$find(Entity glowing, LocalPlayer viewer, boolean selfGlow) {
        Entity holder = selfGlow ? glowing : viewer;
        Entity subject = selfGlow ? viewer : glowing;
        EntityGlowPower.Config[] match = {null};
        PowerLookup.forEach(holder, ApoliIds.ENTITY_GLOW, EntityGlowPower.Config.class, cfg -> {
            if (match[0] != null || cfg.selfGlowTarget() != selfGlow) return;
            if (cfg.entityCondition().isPresent()
                && !cfg.entityCondition().get().test(new EntityCtx(subject, subject.level()))) return;
            if (cfg.bientityCondition().isPresent()
                && !cfg.bientityCondition().get().test(new BiEntityCtx(holder, subject, glowing.level()))) return;
            match[0] = cfg;
        });
        return match[0];
    }
}
