package dev.overgrown.apoli.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class CustomMobEffect extends MobEffect {
    public final Optional<String> name;
    public final ResourceLocation id;
    public final Optional<ResourceLocation> icon;

    CustomMobEffect(CustomEffect effect, int i, Optional<String> name, ResourceLocation id, Optional<ResourceLocation> icon) {
        super(effect.mobEffectCategory(), i);
        this.name = name;
        this.id = id;
        this.icon = icon;
    }

    @Override
    public @NotNull Component getDisplayName() {
        if (name.isPresent()) {
            return Component.literal(name.get());
        }

        return super.getDisplayName();
    }
}
