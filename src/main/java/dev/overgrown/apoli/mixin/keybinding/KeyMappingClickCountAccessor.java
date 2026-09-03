package dev.overgrown.apoli.mixin.keybinding;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingClickCountAccessor {
    @Accessor("clickCount")
    int apoli$getClickCount();
}
