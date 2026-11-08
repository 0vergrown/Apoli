package dev.overgrown.apoli.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

public abstract class PowerType<C> {
    public abstract MapCodec<C> configCodec();

    public void onAdded(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void onRemoved(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {}

    public void onSuppressed(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public void onUnsuppressed(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public void tick(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public void tickStored(ResourceLocation powerId, C cfg, PowerContainer holder) {}

    public boolean isActive(ResourceLocation powerId, C cfg, EntityCtx ctx) {
        return true;
    }

    public boolean ticksNonLivingEntities() {
        return false;
    }

    public boolean isCooldown() {
        return false;
    }

    public @Nullable HudRender hudRender(C cfg) {
        return cfg instanceof HudRendered rendered ? rendered.hudRender() : null;
    }

    public boolean resolvesForClient() {
        return false;
    }

    public int clientTarget(ResourceLocation powerId, C cfg, ServerPlayer player, int current) {
        return -1;
    }

    public OptionalDouble readResource(ResourceLocation powerId, C cfg, PowerContainer holder) {
        return OptionalDouble.empty();
    }

    public OptionalDouble writeResource(ResourceLocation powerId, C cfg, PowerContainer holder, double value) {
        return OptionalDouble.empty();
    }

    public OptionalDouble resourceBound(ResourceLocation powerId, C cfg, PowerContainer holder, boolean max) {
        return OptionalDouble.empty();
    }

    public int resourceSize(ResourceLocation powerId, C cfg, PowerContainer holder) {
        return 0;
    }

    public OptionalDouble readResourceAt(ResourceLocation powerId, C cfg, PowerContainer holder, int slot) {
        return slot == 0 ? readResource(powerId, cfg, holder) : OptionalDouble.empty();
    }

    public OptionalDouble writeResourceAt(ResourceLocation powerId, C cfg, PowerContainer holder, int slot, double value) {
        return slot == 0 ? writeResource(powerId, cfg, holder, value) : OptionalDouble.empty();
    }

    public int resourceIndexOf(ResourceLocation powerId, C cfg, PowerContainer holder, double value) {
        OptionalDouble only = readResource(powerId, cfg, holder);
        return only.isPresent() && only.getAsDouble() == value ? 0 : -1;
    }
}
