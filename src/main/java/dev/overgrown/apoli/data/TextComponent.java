package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public final class TextComponent {
    public static final Codec<Component> CODEC = ExtraCodecs.COMPONENT;

    private TextComponent() {}

    public static String autoKey(ResourceLocation id, String suffix) {
        return "power." + id.getNamespace() + "." + id.getPath() + "." + suffix;
    }

    public static Component autoTranslatable(ResourceLocation id, String suffix) {
        return Component.translatable(autoKey(id, suffix));
    }
}
