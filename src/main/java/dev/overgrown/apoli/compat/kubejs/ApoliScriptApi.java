package dev.overgrown.apoli.compat.kubejs;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ApoliScriptApi {

    public boolean hasPower(@Nullable Entity entity, String power) {
        PowerContainer container = entity == null ? null : PowerContainer.of(entity);
        ResourceLocation id = ResourceLocation.tryParse(power);
        return container != null && id != null && container.hasPower(id);
    }

    public boolean grantPower(@Nullable Entity entity, String power, String source) {
        PowerContainer container = entity == null ? null : PowerContainer.of(entity);
        ResourceLocation id = ResourceLocation.tryParse(power);
        ResourceLocation src = ResourceLocation.tryParse(source);
        return container != null && id != null && src != null && container.addPower(id, src);
    }

    public boolean revokePower(@Nullable Entity entity, String power, String source) {
        PowerContainer container = entity == null ? null : PowerContainer.of(entity);
        ResourceLocation id = ResourceLocation.tryParse(power);
        ResourceLocation src = ResourceLocation.tryParse(source);
        return container != null && id != null && src != null && container.removePower(id, src);
    }

    public boolean isSuppressed(@Nullable Entity entity, String power) {
        PowerContainer container = entity == null ? null : PowerContainer.of(entity);
        ResourceLocation id = ResourceLocation.tryParse(power);
        return container != null && id != null && container.isSuppressed(id);
    }
}
