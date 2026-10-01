package dev.overgrown.apoli.macros;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record MacroUsage(String macro, Map<String, Dynamic<?>> arguments) {
    public static final Codec<MacroUsage> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("macro").forGetter(MacroUsage::macro),
        Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH).optionalFieldOf("arguments", Map.of()).forGetter(MacroUsage::arguments)
    ).apply(i, MacroUsage::new));
}
