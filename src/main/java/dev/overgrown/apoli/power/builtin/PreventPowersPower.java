package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class PreventPowersPower extends PowerType<PreventPowersPower.Cfg> {
    public record Cfg(List<ResourceLocation> powers, int updateRate) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.listOf().fieldOf("powers").forGetter(Cfg::powers),
            Codec.INT.optionalFieldOf("update_rate", 5).forGetter(Cfg::updateRate)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner == null) return;
        int rate = Math.max(1, cfg.updateRate());
        if (rate > 1 && Math.floorMod(owner.tickCount, rate)
                != Math.floorMod(powerId.hashCode() + owner.getId(), rate)) {
            return;
        }
        boolean active = conditionHolds(owner, powerId);
        for (ResourceLocation target : cfg.powers()) {
            if (active) holder.suppressPower(target, powerId);
            else holder.unsuppressPower(target, powerId);
        }
    }

    private static boolean conditionHolds(Entity owner, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        return loaded.condition().get().test(EntityCtx.of(owner, owner.level()));
    }
}
