package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public final class MultiplePower extends PowerType<MultiplePower.Cfg> {
    public static final Set<String> RESERVED_FIELDS = Set.of(
        "type", "loading_priority", "name", "description", "hidden", "condition", "sub_powers"
    );

    public record Cfg(List<ResourceLocation> subPowerIds) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("sub_powers", List.of()).forGetter(Cfg::subPowerIds)
        ).apply(i, Cfg::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        for (ResourceLocation subId : cfg.subPowerIds) holder.addPower(subId, powerId);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        for (ResourceLocation subId : cfg.subPowerIds) holder.removePower(subId, powerId);
    }
}
