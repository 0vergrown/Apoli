package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MultiplePower extends PowerType<MultiplePower.Cfg> {
    public static final Set<String> RESERVED_FIELDS = Set.of(
        "type", "loading_priority", "name", "description", "hidden", "condition", "sub_powers", "skill"
    );

    public record Cfg(List<ResourceLocation> subPowerIds) {}

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.listOf().optionalFieldOf("sub_powers", List.of()).forGetter(Cfg::subPowerIds)
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

    public static void reconcile(PowerContainer holder) {
        Set<ResourceLocation> current = holder.allPowers();
        if (current.isEmpty()) return;
        List<ResourceLocation> held = new ArrayList<>(current);
        for (int i = 0; i < held.size(); i++) {
            ResourceLocation superId = held.get(i);
            Power power = ApoliPowers.get(superId);
            if (power == null || !(power.config() instanceof Cfg cfg)) continue;
            List<ResourceLocation> wanted = cfg.subPowerIds();
            for (int j = 0; j < held.size(); j++) {
                ResourceLocation subId = held.get(j);
                if (subId.equals(superId) || wanted.contains(subId)) continue;
                if (!holder.sourcesOf(subId).contains(superId)) continue;
                holder.removePower(subId, superId);
            }
            for (int j = 0; j < wanted.size(); j++) holder.addPower(wanted.get(j), superId);
        }
    }
}
