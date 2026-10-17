package dev.overgrown.apoli.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatusBarSprites {
    public static final ResourceLocation ICONS = vanilla("textures/gui/icons.png");
    public static final ResourceLocation WIDGETS = vanilla("textures/gui/widgets.png");

    public record Region(ResourceLocation sprite, ResourceLocation texture, int u, int v, int width, int height) {
        boolean covers(int pu, int pv) {
            return pu >= u && pu < u + width && pv >= v && pv < v + height;
        }
    }

    private static final List<Region> REGIONS = new ArrayList<>();
    private static final Map<ResourceLocation, Region> BY_SPRITE = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> SPRITE_TEXTURES = new HashMap<>();

    private StatusBarSprites() {}

    static {
        heartPair("hud/heart/container", "hud/heart/container_blinking", 16, 25, 0);
        heartPair("hud/heart/container_hardcore", "hud/heart/container_hardcore_blinking", 16, 25, 45);
        hearts("", 52, true);
        hearts("poisoned_", 88, true);
        hearts("withered_", 124, true);
        hearts("absorbing_", 160, false);
        hearts("frozen_", 178, false);
        icon("hud/heart/vehicle_container", 52, 9);
        icon("hud/heart/vehicle_full", 88, 9);
        icon("hud/heart/vehicle_half", 97, 9);
        icon("hud/armor_empty", 16, 9);
        icon("hud/armor_half", 25, 9);
        icon("hud/armor_full", 34, 9);
        icon("hud/air", 16, 18);
        icon("hud/air_bursting", 25, 18);
        icon("hud/food_empty", 16, 27);
        icon("hud/food_full", 52, 27);
        icon("hud/food_half", 61, 27);
        icon("hud/food_empty_hunger", 133, 27);
        icon("hud/food_full_hunger", 88, 27);
        icon("hud/food_half_hunger", 97, 27);
        region(ICONS, "hud/experience_bar_background", 0, 64, 182, 5);
        region(ICONS, "hud/experience_bar_progress", 0, 69, 182, 5);
        region(ICONS, "hud/jump_bar_cooldown", 0, 74, 182, 5);
        region(ICONS, "hud/jump_bar_background", 0, 84, 182, 5);
        region(ICONS, "hud/jump_bar_progress", 0, 89, 182, 5);
        region(ICONS, "hud/crosshair", 0, 0, 15, 15);
        region(ICONS, "hud/crosshair_attack_indicator_background", 36, 94, 16, 4);
        region(ICONS, "hud/crosshair_attack_indicator_progress", 52, 94, 16, 4);
        region(ICONS, "hud/crosshair_attack_indicator_full", 68, 94, 16, 16);
        region(ICONS, "hud/hotbar_attack_indicator_background", 0, 94, 18, 18);
        region(ICONS, "hud/hotbar_attack_indicator_progress", 18, 94, 18, 18);
        region(WIDGETS, "hud/hotbar", 0, 0, 182, 22);
        region(WIDGETS, "hud/hotbar_selection", 0, 22, 24, 22);
        region(WIDGETS, "hud/hotbar_offhand_left", 24, 22, 29, 24);
        region(WIDGETS, "hud/hotbar_offhand_right", 53, 22, 29, 24);
    }

    private static void hearts(String kind, int u, boolean blinks) {
        String base = "hud/heart/" + kind;
        String hardcore = base + "hardcore_";
        int blinkOffset = blinks ? 18 : 0;
        heartPair(base + "full", base + "full_blinking", u, u + blinkOffset, 0);
        heartPair(base + "half", base + "half_blinking", u + 9, u + 9 + blinkOffset, 0);
        heartPair(hardcore + "full", hardcore + "full_blinking", u, u + blinkOffset, 45);
        heartPair(hardcore + "half", hardcore + "half_blinking", u + 9, u + 9 + blinkOffset, 45);
    }

    private static void heartPair(String plain, String blinking, int u, int blinkingU, int v) {
        region(ICONS, plain, u, v, 9, 9);
        region(ICONS, blinking, blinkingU, v, 9, 9);
    }

    private static void icon(String sprite, int u, int v) {
        region(ICONS, sprite, u, v, 9, 9);
    }

    private static void region(ResourceLocation texture, String sprite, int u, int v, int width, int height) {
        Region region = new Region(vanilla(sprite), texture, u, v, width, height);
        REGIONS.add(region);
        BY_SPRITE.put(region.sprite(), region);
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.tryParse("minecraft:" + path);
    }

    public static @Nullable Region of(ResourceLocation sprite) {
        return BY_SPRITE.get(sprite);
    }

    public static @Nullable Region at(ResourceLocation texture, int u, int v) {
        boolean icons = ICONS.equals(texture);
        if (!icons && !WIDGETS.equals(texture)) return null;
        ResourceLocation sheet = icons ? ICONS : WIDGETS;
        for (int i = 0; i < REGIONS.size(); i++) {
            Region region = REGIONS.get(i);
            if (region.texture() == sheet && region.covers(u, v)) return region;
        }
        return null;
    }

    public static ResourceLocation spriteTexture(ResourceLocation sprite) {
        ResourceLocation texture = SPRITE_TEXTURES.get(sprite);
        if (texture == null) {
            texture = ResourceLocation.tryParse(sprite.getNamespace() + ":textures/gui/sprites/" + sprite.getPath() + ".png");
            if (texture == null) texture = sprite;
            SPRITE_TEXTURES.put(sprite, texture);
        }
        return texture;
    }
}
