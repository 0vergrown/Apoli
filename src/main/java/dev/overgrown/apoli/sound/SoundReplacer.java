package dev.overgrown.apoli.sound;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.sound.WeightedSound;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ReplaceSoundPower;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SoundReplacer {

    @FunctionalInterface
    public interface Emitter {
        void play(SoundEvent sound, float volume, float pitch);
    }

    private SoundReplacer() {}

    public static boolean hasEmission(@Nullable Entity entity) {
        return has(entity, ApoliIds.REPLACE_SOUND_EMISSION);
    }

    public static boolean hasReception(@Nullable Entity entity) {
        return has(entity, ApoliIds.REPLACE_SOUND_RECEPTION);
    }

    private static boolean has(@Nullable Entity entity, ResourceLocation typeId) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        return container != null && !container.isEmpty() && !container.powersOfType(typeId).isEmpty();
    }

    public static void emit(@Nullable Entity source, SoundEvent sound, float volume, float pitch, Emitter emitter) {
        apply(ApoliIds.REPLACE_SOUND_EMISSION, source, sound, volume, pitch, emitter);
    }

    public static void receive(@Nullable Entity listener, SoundEvent sound, float volume, float pitch, Emitter emitter) {
        apply(ApoliIds.REPLACE_SOUND_RECEPTION, listener, sound, volume, pitch, emitter);
    }

    private static void apply(ResourceLocation typeId, @Nullable Entity holder, SoundEvent sound,
                              float volume, float pitch, Emitter emitter) {
        List<Entry> entries = collect(typeId, holder);
        if (entries.isEmpty()) {
            emitter.play(sound, volume, pitch);
            return;
        }

        RandomSource random = holder.level().random;
        EntityCtx ctx = null;
        boolean handled = false;

        for (int i = 0; i < entries.size(); i++) {
            ReplaceSoundPower.Config cfg = entries.get(i).config();
            WeightedSound replacement = cfg.sounds().find(sound.getLocation(), random);
            if (replacement == null) continue;

            if (cfg.entityAction().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(holder, holder.level());
                cfg.entityAction().get().run(ctx);
            }

            if (!replacement.mutes()) {
                SoundEvent replaced = resolve(replacement.id(), sound);
                if (replaced != null) {
                    emitter.play(replaced, replacement.volumeOr(volume), replacement.pitchOr(pitch));
                }
            }
            if (cfg.replace()) return;
            handled = true;
        }

        if (!handled) emitter.play(sound, volume, pitch);
    }

    private record Entry(ResourceLocation powerId, ReplaceSoundPower.Config config) {}

    private static List<Entry> collect(ResourceLocation typeId, @Nullable Entity holder) {
        if (holder == null) return List.of();
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return List.of();
        List<ResourceLocation> powers = container.powersOfType(typeId);
        if (powers.isEmpty()) return List.of();

        List<Entry> entries = null;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof ReplaceSoundPower.Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(holder, holder.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (entries == null) entries = new ArrayList<>(2);
            entries.add(new Entry(powerId, cfg));
        }
        if (entries == null) return List.of();
        if (entries.size() > 1) {
            entries.sort(Comparator.comparingInt((Entry e) -> e.config().priority()).reversed());
        }
        return entries;
    }

    private static @Nullable SoundEvent resolve(String id, SoundEvent original) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return null;
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(location);
        if (registered != null) return registered;
        return SoundEvent.createVariableRangeEvent(location);
    }
}
