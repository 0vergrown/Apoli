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
import dev.overgrown.apoli.power.builtin.ActionOnCollisionPower;
import dev.overgrown.apoli.power.builtin.ActionOnHitPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeySequencePower;
import dev.overgrown.apoli.power.builtin.ActionOnKillPower;
import dev.overgrown.apoli.power.builtin.ActionWhenHitPower;
import dev.overgrown.apoli.power.builtin.CooldownPower;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.GameEventListenerPower;
import dev.overgrown.apoli.power.builtin.ResourcePower;
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
import java.util.OptionalInt;

public final class PowerHudRenderer {
    private static final int BAR_WIDTH = 71;
    private static final int BAR_HEIGHT = 8;
    private static final int EMPTY_BAR_HEIGHT = 5;
    private static final int ICON_SIZE = 8;
    private static final int BAR_INDEX_OFFSET = BAR_HEIGHT + 2;
    private static final int ICON_INDEX_OFFSET = ICON_SIZE + 1;
    private static final int ICONS_U_OFFSET = BAR_WIDTH + 2;

    private static final List<Renderable> RENDERABLES = new ArrayList<>();
    private static final Comparator<Renderable> BY_ORDER = Comparator.comparingInt(Renderable::order);

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

        RENDERABLES.clear();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            collect(power, powerId, player, container, ctx);
        }
        if (RENDERABLES.isEmpty()) return;
        RENDERABLES.sort(BY_ORDER);

        for (int i = 0; i < RENDERABLES.size(); i++) {
            Renderable r = RENDERABLES.get(i);
            drawEntry(graphics, x, y, r.entry, r.fill);
            y -= 8;
        }
        RENDERABLES.clear();
    }

    private static void collect(Power power, ResourceLocation powerId, LocalPlayer player,
                                @Nullable PowerContainer container, EntityCtx ctx) {
        if (container == null) return;
        PowerType<?> type = PowerTypeRegistry.get(power.typeId());
        Object config = power.config();

        HudRender hud = hudRenderOf(type, config);
        if (hud == null) return;

        OptionalInt value = PowerResources.read(container, powerId);
        if (value.isEmpty()) return;
        int current = value.getAsInt();

        HudRender.Entry entry = hud.selectEntry(ctx);
        if (entry == null) return;

        float fill;
        if (type instanceof ResourcePower && !(type instanceof CooldownPower) && config instanceof ResourcePower.Cfg cfg) {
            fill = resourceFill(player, container, powerId, cfg, entry);
            if (fill < 0.0F) return;
        } else {
            if (current <= 0) return;
            int bound = entry.max().isPresent()
                ? entry.max().get().evalIntWith(player, container, current)
                : PowerResources.bound(container, powerId, true).orElse(0);
            fill = cooldownProgress(current, bound);
        }

        RENDERABLES.add(new Renderable(entry, fill, entry.order().orElse(0)));
    }

    private static @Nullable HudRender hudRenderOf(@Nullable PowerType<?> type, Object config) {
        if (type instanceof ActionOnKeyPressPower && config instanceof ActionOnKeyPressPower.Config cfg) return cfg.hudRender();
        if (type instanceof ActionOnKeySequencePower && config instanceof ActionOnKeySequencePower.Config cfg) return cfg.hudRender();
        if (type instanceof FireProjectilePower && config instanceof FireProjectilePower.Config cfg) return cfg.params().hudRender().orElse(null);
        if (type instanceof ResourcePower && config instanceof ResourcePower.Cfg cfg) return cfg.hudRender();
        if (type instanceof ActionOnHitPower && config instanceof ActionOnHitPower.Config cfg) return cfg.hudRender();
        if (type instanceof ActionWhenHitPower && config instanceof ActionWhenHitPower.Config cfg) return cfg.hudRender();
        if (type instanceof ActionOnCollisionPower && config instanceof ActionOnCollisionPower.Config cfg) return cfg.hudRender();
        if (type instanceof ActionOnKillPower && config instanceof ActionOnKillPower.Config cfg) return cfg.hudRender();
        if (type instanceof GameEventListenerPower && config instanceof GameEventListenerPower.Config cfg) return cfg.hudRender();
        return null;
    }

    private static float cooldownProgress(int remaining, int max) {
        if (max <= 0) return 1.0F;
        return 1.0F - (remaining / (float) max);
    }

    private static final java.util.Set<ResourceLocation> UNSCALED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static float resourceFill(LocalPlayer player, @Nullable PowerContainer container,
                                      ResourceLocation powerId, ResourcePower.Cfg cfg, HudRender.Entry entry) {
        OptionalInt cur = ClientPowerState.getAuxInt(powerId);
        if (cur.isEmpty()) return 0.0F;
        int value = cur.getAsInt();
        dev.overgrown.apoli.data.Expression scale = entry.max().or(cfg::max).orElse(null);
        if (scale == null) {
            if (UNSCALED.add(powerId)) {
                dev.overgrown.apoli.Apoli.LOGGER.warn(
                    "[Apoli] {} asks for a HUD bar but has no 'max', so there is no value that would fill it. "
                    + "Give the resource a 'max', or give its 'hud_render' one, or set 'should_render' to false.",
                    powerId);
            }
            return -1.0F;
        }
        int max = scale.evalIntWith(player, container, value);
        int min = cfg.min().isPresent() ? cfg.min().get().evalIntWith(player, container, value) : 0;
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
