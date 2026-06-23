package dev.overgrown.apoli.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.OverlayPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class OverlayRenderer {
    private OverlayRenderer() {}

    public static void renderBelowHud(GuiGraphics graphics, float partialTick) {
        render(graphics, OverlayPower.DrawPhase.BELOW_HUD);
    }

    public static void renderAboveHud(GuiGraphics graphics, float partialTick) {
        render(graphics, OverlayPower.DrawPhase.ABOVE_HUD);
    }

    private static void render(GuiGraphics graphics, OverlayPower.DrawPhase phase) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean hudHidden = mc.options.hideGui;
        boolean thirdPerson = mc.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON;
        EntityCtx ctx = new EntityCtx(player, player.level());

        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(power.typeId());
            if (!(type instanceof OverlayPower)) continue;
            if (!(power.config() instanceof OverlayPower.Config cfg)) continue;
            if (cfg.drawPhase() != phase) continue;
            if (hudHidden && cfg.hideWithHud()) continue;
            if (thirdPerson && !cfg.visibleInThirdPerson()) continue;
            if (power.condition().isPresent() && !power.condition().get().test(ctx)) continue;

            switch (cfg.drawMode()) {
                case TEXTURE -> drawTexture(graphics, cfg);
                case NAUSEA -> drawNausea(graphics, cfg);
            }
        }
    }

    private static void drawTexture(GuiGraphics graphics, OverlayPower.Config cfg) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(cfg.red(), cfg.green(), cfg.blue(), cfg.strength());
        graphics.blit(cfg.texture(), 0, 0, -90, 0.0F, 0.0F, w, h, w, h);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawNausea(GuiGraphics graphics, OverlayPower.Config cfg) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float strength = Mth.clamp(cfg.strength(), 0.0F, 1.0F);
        float scale = Mth.lerp(strength, 2.0F, 1.0F);

        int quadW = Math.round(w * scale);
        int quadH = Math.round(h * scale);
        int x = (w - quadW) / 2;
        int y = (h - quadH) / 2;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(cfg.red() * strength, cfg.green() * strength, cfg.blue() * strength, 1.0F);
        graphics.blit(cfg.texture(), x, y, -90, 0.0F, 0.0F, quadW, quadH, quadW, quadH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
