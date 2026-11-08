package dev.overgrown.apoli.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

@Environment(EnvType.CLIENT)
public final class PowerHudRenderer {
    private static final int BAR_WIDTH = 71;
    private static final int BAR_HEIGHT = 8;
    private static final int EMPTY_BAR_HEIGHT = 5;
    private static final int ICON_SIZE = 8;
    private static final int BAR_INDEX_OFFSET = BAR_HEIGHT + 2;
    private static final int ICON_INDEX_OFFSET = ICON_SIZE + 1;
    private static final int ICONS_U_OFFSET = BAR_WIDTH + 2;
    private static final int BAR_STEP = 8;

    private static final List<Renderable> RENDERABLES = new ArrayList<>();
    private static final Comparator<Renderable> BY_ORDER = Comparator.comparingInt(Renderable::order);

    private PowerHudRenderer() {}

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || ClientDevMode.enabled()) return;

        ApoliClientConfig config = ApoliClientConfig.get();
        int x = (graphics.guiWidth() / 2) + 20 + config.hudOffsetX();
        int y = graphics.guiHeight() - HudStatusStack.rightHeight() + config.hudOffsetY();

        EntityCtx ctx = new EntityCtx(player, player.level());
        PowerContainer container = PowerContainer.of(player);

        RENDERABLES.clear();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            collect(power, powerId, player, container, ctx);
        }
        if (RENDERABLES.isEmpty()) return;
        RENDERABLES.sort(BY_ORDER);

        int bars = RENDERABLES.size();
        HudStatusStack.pause();
        for (int i = 0; i < bars; i++) {
            Renderable r = RENDERABLES.get(i);
            drawEntry(graphics, x, y, r.entry, r.fill);
            y -= BAR_STEP;
        }
        HudStatusStack.resume();
        RENDERABLES.clear();
        HudStatusStack.reserveRight(reservedHeight(bars));
    }

    private static int reservedHeight(int bars) {
        int used = (bars - 1) * BAR_STEP + HudStatusStack.ROW_HEIGHT;
        return ((used + HudStatusStack.ROW_HEIGHT - 1) / HudStatusStack.ROW_HEIGHT) * HudStatusStack.ROW_HEIGHT;
    }

    private static void collect(Power power, ResourceLocation powerId, LocalPlayer player,
                                @Nullable PowerContainer container, EntityCtx ctx) {
        if (container == null) return;
        PowerType<?> type = PowerTypeRegistry.get(power.typeId());
        if (type == null) return;

        HudRender hud = hudRenderOf(type, power.config());
        if (hud == null) return;

        HudRender.Entry entry = hud.selectEntry(ctx);
        if (entry == null) return;

        OptionalDouble value = PowerResources.read(container, powerId);
        if (value.isEmpty()) return;
        double current = value.getAsDouble();

        double fill;
        if (type.isCooldown()) {
            if (current <= 0) return;
            double bound = entry.max().isPresent()
                ? entry.max().get().evalWith(player, container, current)
                : PowerResources.bound(container, powerId, true).orElse(0);
            fill = cooldownProgress(current, bound);
        } else {
            fill = resourceFill(player, container, powerId, current, entry);
            if (fill < 0.0) return;
        }

        RENDERABLES.add(new Renderable(entry, fill, entry.order().orElse(0)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable HudRender hudRenderOf(PowerType type, Object config) {
        return type.hudRender(config);
    }

    private static double cooldownProgress(double remaining, double max) {
        if (max <= 0) return 1.0;
        return Mth.clamp(1.0 - remaining / max, 0.0, 1.0);
    }

    private static final java.util.Set<ResourceLocation> UNSCALED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static double resourceFill(LocalPlayer player, PowerContainer container, ResourceLocation powerId,
                                       double value, HudRender.Entry entry) {
        double max;
        if (entry.max().isPresent()) {
            max = PowerResources.canonical(entry.max().get().evalWith(player, container, value));
        } else {
            OptionalDouble upper = PowerResources.bound(container, powerId, true);
            if (upper.isEmpty() || upper.getAsDouble() >= ResourcePower.UNBOUNDED_MAX) {
                if (UNSCALED.add(powerId)) {
                    dev.overgrown.apoli.Apoli.LOGGER.warn(
                        "[Apoli] {} asks for a HUD bar but has no 'max', so there is no value that would fill it. "
                        + "Give the resource a 'max', or give its 'hud_render' one, or set 'should_render' to false.",
                        powerId);
                }
                return -1.0;
            }
            max = upper.getAsDouble();
        }
        OptionalDouble lower = PowerResources.bound(container, powerId, false);
        double min = lower.isPresent() && lower.getAsDouble() > ResourcePower.UNBOUNDED_MIN ? lower.getAsDouble() : 0.0;
        if (max == min) return value >= max ? 1.0 : 0.0;
        if (max < min) {
            double swap = max;
            max = min;
            min = swap;
        }
        if (value <= min) return 0.0;
        if (value >= max) return 1.0;
        return (value - min) / (max - min);
    }

    static int fillWidth(double ratio) {
        if (!(ratio > 0.0)) return 0;
        if (ratio >= 1.0) return BAR_WIDTH;
        int inner = BAR_WIDTH - 2;
        return 1 + Mth.clamp((int) Math.round(inner * ratio), 1, inner - 1);
    }

    private static void drawEntry(GuiGraphics graphics, int x, int y, HudRender.Entry entry, double fill) {
        ResourceLocation tex = entry.spriteLocation();
        int barIndex = entry.barIndex();
        int iconIndex = entry.iconIndex();
        List<HudRender.Look> fallback = entry.fallback();
        if (!fallback.isEmpty() && !TextureFallbacks.exists(tex)) {
            for (int i = 0, n = fallback.size(); i < n; i++) {
                HudRender.Look look = fallback.get(i);
                if (!TextureFallbacks.exists(look.spriteLocation())) continue;
                tex = look.spriteLocation();
                if (look.barIndex().isPresent()) barIndex = look.barIndex().get();
                if (look.iconIndex().isPresent()) iconIndex = look.iconIndex().get();
                break;
            }
        }
        int barV = BAR_HEIGHT + barIndex * BAR_INDEX_OFFSET;
        int iconU = ICONS_U_OFFSET + iconIndex * ICON_INDEX_OFFSET;

        RenderSystem.enableBlend();
        graphics.blit(tex, x, y, 0, 0, BAR_WIDTH, EMPTY_BAR_HEIGHT);
        int fillWidth = fillWidth(entry.inverted() ? 1.0 - fill : fill);
        if (fillWidth > 0) {
            graphics.blit(tex, x, y - 2, 0, barV, fillWidth, BAR_HEIGHT);
        }
        graphics.blit(tex, x - ICON_SIZE - 2, y - 2, iconU, barV, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    private record Renderable(HudRender.Entry entry, double fill, int order) {}
}
