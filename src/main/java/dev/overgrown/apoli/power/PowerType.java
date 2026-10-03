package dev.overgrown.apoli.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HudRender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

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

    public OptionalInt readResource(ResourceLocation powerId, C cfg, PowerContainer holder) {
        return OptionalInt.empty();
    }

    public OptionalInt writeResource(ResourceLocation powerId, C cfg, PowerContainer holder, int value) {
        return OptionalInt.empty();
    }

    public OptionalInt resourceBound(ResourceLocation powerId, C cfg, PowerContainer holder, boolean max) {
        return OptionalInt.empty();
    }

    public int resourceSize(ResourceLocation powerId, C cfg, PowerContainer holder) {
        return 0;
    }

    public OptionalInt readResourceAt(ResourceLocation powerId, C cfg, PowerContainer holder, int slot) {
        return slot == 0 ? readResource(powerId, cfg, holder) : OptionalInt.empty();
    }

    public OptionalInt writeResourceAt(ResourceLocation powerId, C cfg, PowerContainer holder, int slot, int value) {
        return slot == 0 ? writeResource(powerId, cfg, holder, value) : OptionalInt.empty();
    }

    public int resourceIndexOf(ResourceLocation powerId, C cfg, PowerContainer holder, int value) {
        OptionalInt only = readResource(powerId, cfg, holder);
        return only.isPresent() && only.getAsInt() == value ? 0 : -1;
    }
}
