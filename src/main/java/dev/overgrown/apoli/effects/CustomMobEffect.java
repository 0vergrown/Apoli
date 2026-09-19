package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CustomMobEffect extends MobEffect {
    private final List<ResourceLocation> powers;
    public final CustomEffect effect;

    CustomMobEffect(CustomEffect effect, int i) {
        super(effect.mobEffectCategory(), i);
        this.powers = effect.powers();
        this.effect = effect;
    }

    @Override
    public void onEffectStarted(@NotNull LivingEntity livingEntity, int i) {
        PowerContainer holder = PowerContainer.of(livingEntity);
        if (holder != null) powers.forEach(power -> holder.addPower(power, effect.id()));
    }

    @Override
    public @NotNull Component getDisplayName() {
        if (effect.name().isPresent()) {
            return Component.literal(effect.name().get());
        }

        return super.getDisplayName();
    }
}
