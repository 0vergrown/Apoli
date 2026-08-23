package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ScriptPower extends PowerType<ScriptPower.Config> {
    public record Config(
        Optional<ResourceLocation> onAdded,
        Optional<ResourceLocation> onRemoved,
        CompoundTag params
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("on_added").forGetter(Config::onAdded),
            IdCodecs.ID.optionalFieldOf("on_removed").forGetter(Config::onRemoved),
            Nbt.CODEC.optionalFieldOf("params").forGetter(c -> c.params.isEmpty() ? Optional.empty() : Optional.of(new Nbt(c.params)))
        ).apply(i, (added, removed, params) ->
            new Config(added, removed, params.map(Nbt::tag).orElseGet(CompoundTag::new))));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        cfg.onAdded.ifPresent(id -> fire(ApoliScripts.Kind.POWER_ADDED, id, powerId, cfg, holder));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        cfg.onRemoved.ifPresent(id -> fire(ApoliScripts.Kind.POWER_REMOVED, id, powerId, cfg, holder));
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    private static void fire(ApoliScripts.Kind kind, ResourceLocation scriptId,
                             ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (holder.rawOwner() == null || holder.rawOwner().level().isClientSide()) return;
        ApoliScripts.run(kind, scriptId, ScriptCtx.ofPower(holder, powerId, cfg.params));
    }
}
