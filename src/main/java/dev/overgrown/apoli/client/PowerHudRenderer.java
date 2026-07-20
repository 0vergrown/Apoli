package dev.overgrown.apoli.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeySequencePower;
import dev.overgrown.apoli.power.builtin.CooldownPower;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class PowerHudRenderer {
    private static final int BAR_WIDTH = 71;
    private static final int BAR_HEIGHT = 8;
    private static final int EMPTY_BAR_HEIGHT = 5;
    private static final int ICON_SIZE = 8;
    private static final int BAR_INDEX_OFFSET = BAR_HEIGHT + 2;
    private static final int ICON_INDEX_OFFSET = ICON_SIZE + 1;
    private static final int ICONS_U_OFFSET = BAR_WIDTH + 2;

    private PowerHudRenderer() {}

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        int yOffset = 49;
        if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
            yOffset += 10;
        }
        if (player.getVehicle() instanceof LivingEntity vehicle) {
            int rows = Mth.clamp((int) Math.ceil(vehicle.getMaxHealth() / 20.0F), 1, 3) - 1;
            yOffset += rows * 10;
        }

        int x = (graphics.guiWidth() / 2) + 20;
        int y = graphics.guiHeight() - yOffset;

        EntityCtx ctx = new EntityCtx(player, player.level());
        PowerContainer container = PowerContainer.of(player);

        List<Renderable> renderables = new ArrayList<>();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(power.typeId());

            HudRender hud;
            float fill;

            if (type instanceof ActionOnKeyPressPower && power.config() instanceof ActionOnKeyPressPower.Config cfg) {
                if (ClientPowerState.getCooldown(powerId) <= 0) continue;
                hud = cfg.hudRender();
                fill = cooldownFill(powerId, cfg);
            } else if (type instanceof CooldownPower && power.config() instanceof ResourcePower.Cfg cfg) {
                if (!conditionHolds(power, ctx)) continue;
                OptionalInt remaining = ClientPowerState.getAuxInt(powerId);
                if (remaining.isEmpty() || remaining.getAsInt() <= 0) continue;
                hud = cfg.hudRender();
                fill = 1.0F - resourceFill(player, container, powerId, cfg);
            } else if (type instanceof ResourcePower && power.config() instanceof ResourcePower.Cfg cfg) {
                if (!conditionHolds(power, ctx)) continue;
                hud = cfg.hudRender();
                fill = resourceFill(player, container, powerId, cfg);
            } else if (type instanceof ActionOnKeySequencePower && power.config() instanceof ActionOnKeySequencePower.Config cfg) {
                if (ClientPowerState.getCooldown(powerId) <= 0) continue;
                hud = cfg.hudRender();
                int max = cfg.cooldown();
                fill = max <= 0 ? 1.0F : 1.0F - (ClientPowerState.getCooldown(powerId) / (float) max);
            } else {
                continue;
            }

            Optional<HudRender.Entry> selected = hud.select(ctx);
            if (selected.isEmpty()) continue;
            int order = selected.get().order().orElse(0);
            renderables.add(new Renderable(selected.get(), fill, order));
        }
        renderables.sort(Comparator.comparingInt(Renderable::order));

        for (Renderable r : renderables) {
            drawEntry(graphics, x, y, r.entry, r.fill);
            y -= 8;
        }
    }

    private static boolean conditionHolds(Power power, EntityCtx ctx) {
        return power.condition().isEmpty() || power.condition().get().test(ctx);
    }

    private static float cooldownFill(ResourceLocation powerId, ActionOnKeyPressPower.Config cfg) {
        int remaining = ClientPowerState.getCooldown(powerId);
        if (cfg.cooldown() <= 0) return 1.0F;
        return 1.0F - (remaining / (float) cfg.cooldown());
    }

    private static float resourceFill(LocalPlayer player, @Nullable PowerContainer container, ResourceLocation powerId, ResourcePower.Cfg cfg) {
        OptionalInt cur = ClientPowerState.getAuxInt(powerId);
        if (cur.isEmpty()) return 0.0F;
        int value = cur.getAsInt();
        int min = cfg.min().evalIntWith(player, container, value);
        int max = cfg.max().evalIntWith(player, container, value);
        if (max == min) return value >= max ? 1.0F : 0.0F;
        return Mth.clamp((value - min) / (float) (max - min), 0.0F, 1.0F);
    }

    private static void drawEntry(GuiGraphics graphics, int x, int y, HudRender.Entry entry, float fill) {
        ResourceLocation tex = entry.spriteLocation();
        int barV = BAR_HEIGHT + entry.barIndex() * BAR_INDEX_OFFSET;
        int iconU = ICONS_U_OFFSET + entry.iconIndex() * ICON_INDEX_OFFSET;

        RenderSystem.enableBlend();
        graphics.blit(tex, x, y, 0, 0, BAR_WIDTH, EMPTY_BAR_HEIGHT);
        float ratio = entry.inverted() ? 1.0F - fill : fill;
        int fillWidth = (int) (BAR_WIDTH * Mth.clamp(ratio, 0.0F, 1.0F));
        if (fillWidth > 0) {
            graphics.blit(tex, x, y - 2, 0, barV, fillWidth, BAR_HEIGHT);
        }
        graphics.blit(tex, x - ICON_SIZE - 2, y - 2, iconU, barV, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    private record Renderable(HudRender.Entry entry, float fill, int order) {}
}
