package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.EntityGlowPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        if (viewer == null || !(self instanceof LivingEntity glowing)) return null;
        EntityGlowPower.Config cfg = apoli$find(glowing, viewer, true);
        if (cfg != null) return cfg;
        return glowing == viewer ? null : apoli$find(glowing, viewer, false);
    }

    @Unique
    private static EntityGlowPower.Config apoli$find(LivingEntity glowing, LocalPlayer viewer, boolean selfGlow) {
        LivingEntity holder = selfGlow ? glowing : viewer;
        EntityGlowPower.Config[] match = {null};
        PowerLookup.forEach(holder, Apoli.id("entity_glow"), EntityGlowPower.Config.class, cfg -> {
            if (match[0] != null || cfg.selfGlowTarget() != selfGlow) return;
            if (cfg.entityCondition().isPresent()
                && !cfg.entityCondition().get().test(new EntityCtx(glowing, glowing.level()))) return;
            if (cfg.bientityCondition().isPresent()
                && !cfg.bientityCondition().get().test(new BiEntityCtx(viewer, glowing, glowing.level()))) return;
            match[0] = cfg;
        });
        return match[0];
    }
}
