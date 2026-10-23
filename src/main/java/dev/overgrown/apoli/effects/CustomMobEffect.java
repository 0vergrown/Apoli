package dev.overgrown.apoli.effects;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class CustomMobEffect extends MobEffect {
    public final ResourceLocation id;
    public final ResourceLocation source;
    public volatile Optional<String> name;
    public volatile Optional<ResourceLocation> icon;
    private volatile MobEffectCategory category;
    private volatile int color;
    private volatile List<ResourceLocation> powers;
    private volatile String descriptionId;

    CustomMobEffect(ResourceLocation id, MobEffectCategory category, int color, List<ResourceLocation> powers,
                    Optional<String> name, Optional<ResourceLocation> icon) {
        super(category, color);
        this.id = id;
        this.source = Apoli.id("effect/" + id.getNamespace() + "/" + id.getPath());
        this.category = category;
        this.color = color;
        this.powers = List.copyOf(powers);
        this.name = name;
        this.icon = icon;
        this.descriptionId = name.orElseGet(() -> Util.makeDescriptionId("effect", id));
    }

    public void copyFrom(CustomMobEffect source) {
        this.category = source.category;
        this.color = source.color;
        this.powers = source.powers;
        this.name = source.name;
        this.icon = source.icon;
        this.descriptionId = source.descriptionId;
    }

    public List<ResourceLocation> powers() {
        return powers;
    }

    @Override
    public @NotNull MobEffectCategory getCategory() {
        return category;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean isBeneficial() {
        return category == MobEffectCategory.BENEFICIAL;
    }

    public void grantPowers(LivingEntity entity) {
        List<ResourceLocation> granted = powers;
        if (granted.isEmpty() || entity.level().isClientSide()) return;
        PowerContainer holder = null;
        for (int i = 0; i < granted.size(); i++) {
            ResourceLocation power = granted.get(i);
            if (ApoliPowers.get(power) == null) continue;
            if (holder == null) {
                holder = PowerContainerAttachment.getOrCreate(entity);
                if (holder == null) return;
            }
            holder.addPower(power, source);
        }
    }

    public void revokePowers(PowerContainer holder) {
        holder.removeAllFromSource(source);
        List<ResourceLocation> granted = powers;
        for (int i = 0; i < granted.size(); i++) holder.removePower(granted.get(i), id);
    }

    @Override
    public @NotNull String getDescriptionId() {
        return descriptionId;
    }
}
