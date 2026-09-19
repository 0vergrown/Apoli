package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class CustomMobEffect extends MobEffect {
    private final List<ResourceLocation> powers;
    public final Optional<String> name;
    public final ResourceLocation id;
    public final Optional<ResourceLocation> icon;

    CustomMobEffect(CustomEffect effect, int i, Optional<String> name, ResourceLocation id, Optional<ResourceLocation> icon) {
        super(effect.mobEffectCategory(), i);
        this.powers = effect.powers();
        this.name = name;
        this.id = id;
        this.icon = icon;
    }

    @Override
    public void onEffectStarted(@NotNull LivingEntity livingEntity, int i) {
        PowerContainer holder = PowerContainer.of(livingEntity);
        if (holder != null) powers.forEach(power -> holder.addPower(power, id));
    }

    @Override
    public @NotNull Component getDisplayName() {
        if (name.isPresent()) {
            return Component.literal(name.get());
        }

        return super.getDisplayName();
    }
}
