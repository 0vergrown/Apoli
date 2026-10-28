package dev.overgrown.apoli.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class Sounds {

    private Sounds() {}

    public static SoundEvent event(ResourceLocation id) {
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(id);
        return registered != null ? registered : SoundEvent.createVariableRangeEvent(id);
    }

    public static SoundEvent variant(ResourceLocation id, int index) {
        SoundEvent base = event(id);
        ResourceLocation variant = SoundVariants.encode(id, index);
        float range = base.getRange(1.0F);
        return range == base.getRange(2.0F)
            ? SoundEvent.createFixedRangeEvent(variant, range)
            : SoundEvent.createVariableRangeEvent(variant);
    }
}
