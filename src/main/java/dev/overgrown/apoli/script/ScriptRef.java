package dev.overgrown.apoli.script;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record ScriptRef(ResourceLocation script, CompoundTag params) {
    public static final MapCodec<ScriptRef> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.fieldOf("script").forGetter(ScriptRef::script),
        Nbt.CODEC.optionalFieldOf("params").forGetter(r -> r.params.isEmpty() ? Optional.empty() : Optional.of(new Nbt(r.params)))
    ).apply(i, (script, params) -> new ScriptRef(script, params.map(Nbt::tag).orElseGet(CompoundTag::new))));
}
