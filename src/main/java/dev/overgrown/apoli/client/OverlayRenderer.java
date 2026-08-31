package dev.overgrown.apoli.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.OverlayPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

@OnlyIn(Dist.CLIENT)
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
            if (power.condition().isPresent() && !power.condition().get().test(ctx)) continue;

            List<OverlayPower.Entry> overlays = cfg.overlays();
            for (int i = 0; i < overlays.size(); i++) {
                OverlayPower.Entry entry = overlays.get(i);
                if (entry.drawPhase() != phase) continue;
                if (hudHidden && entry.hideWithHud()) continue;
                if (thirdPerson && !entry.visibleInThirdPerson()) continue;
                if (!entry.shouldRender(ctx)) continue;

                switch (entry.drawMode()) {
                    case TEXTURE -> drawTexture(graphics, player, entry);
                    case NAUSEA -> drawNausea(graphics, player, entry);
                }
            }
        }
    }

    private static void drawTexture(GuiGraphics graphics, LocalPlayer player, OverlayPower.Entry entry) {
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int w = entry.width().map(e -> e.evalInt(player)).orElse(screenW);
        int h = entry.height().map(e -> e.evalInt(player)).orElse(screenH);
        if (w <= 0 || h <= 0) return;
        int x = entry.anchor().originX(screenW, w) + entry.x().evalInt(player);
        int y = entry.anchor().originY(screenH, h) + entry.y().evalInt(player);
        int texW = entry.textureWidth().orElse(w);
        int texH = entry.textureHeight().orElse(h);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(entry.red(), entry.green(), entry.blue(), entry.strength());
        graphics.blit(entry.texture(), x, y, -90,
            entry.u().evalInt(player), entry.v().evalInt(player), w, h, texW, texH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawNausea(GuiGraphics graphics, LocalPlayer player, OverlayPower.Entry entry) {
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        float strength = Mth.clamp(entry.strength(), 0.0F, 1.0F);
        float scale = Mth.lerp(strength, 2.0F, 1.0F);

        int baseW = entry.width().map(e -> e.evalInt(player)).orElse(screenW);
        int baseH = entry.height().map(e -> e.evalInt(player)).orElse(screenH);
        int quadW = Math.round(baseW * scale);
        int quadH = Math.round(baseH * scale);
        if (quadW <= 0 || quadH <= 0) return;
        int x = entry.anchor().originX(screenW, quadW) + entry.x().evalInt(player);
        int y = entry.anchor().originY(screenH, quadH) + entry.y().evalInt(player);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(entry.red() * strength, entry.green() * strength, entry.blue() * strength, 1.0F);
        graphics.blit(entry.texture(), x, y, -90,
            entry.u().evalInt(player), entry.v().evalInt(player), quadW, quadH, quadW, quadH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
