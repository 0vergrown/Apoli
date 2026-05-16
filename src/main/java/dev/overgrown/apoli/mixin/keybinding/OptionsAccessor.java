package dev.overgrown.apoli.mixin.keybinding;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets {@link dev.overgrown.apoli.client.DynamicKeyMappingManager} append
 * data-driven {@link KeyMapping}s to {@code Options.keyMappings} after the
 * field's normal one-shot initialization in {@link Options}'s constructor.
 *
 * <p>Mojang declares the field {@code public final KeyMapping[]}. Mixin strips
 * the {@code final} flag when {@link Mutable} is present, allowing the runtime
 * setter to succeed.</p>
 */
@Mixin(Options.class)
public interface OptionsAccessor {
    @Mutable
    @Accessor("keyMappings")
    void apoli$setKeyMappings(KeyMapping[] keyMappings);
}
