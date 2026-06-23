package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class ActionOnLandPower extends PowerType<ActionOnLandPower.Config> {
    public record Config(EntityAction entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && impl.getAuxInt(powerId).isEmpty()) {
            impl.setAuxInt(powerId, 0);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(powerId)) impl.removeAux(powerId);
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;

        if (!owner.onGround()) {
            impl.setAuxInt(powerId, Math.max(1, Math.round(owner.fallDistance * 100.0F)));
            return;
        }

        int airborneFall = impl.getAuxInt(powerId).orElse(0);
        if (airborneFall <= 0) return;
        impl.setAuxInt(powerId, 0);

        float saved = owner.fallDistance;
        owner.fallDistance = Math.max(saved, airborneFall / 100.0F);
        try {
            Power loaded = ApoliPowers.get(powerId);
            if (loaded != null && loaded.condition().isPresent()
                && !loaded.condition().get().test(new EntityCtx(owner, level))) {
                return;
            }
            cfg.entityAction().run(new EntityCtx(owner, level));
        } finally {
            owner.fallDistance = saved;
        }
    }
}
