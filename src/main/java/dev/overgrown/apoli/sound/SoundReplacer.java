package dev.overgrown.apoli.sound;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.sound.WeightedSound;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ReplaceSoundPower;
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

        default void layer(SoundEvent sound, float volume, float pitch) {
            play(sound, volume, pitch);
        }
    }

    private static final Comparator<ReplaceSoundPower.Config> BY_PRIORITY =
        Comparator.comparingInt(ReplaceSoundPower.Config::priority).reversed();

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
        apply(ApoliIds.REPLACE_SOUND_EMISSION, source, sound, volume, pitch, true, emitter);
    }

    public static void emitLocally(@Nullable Entity source, SoundEvent sound, float volume, float pitch, Emitter emitter) {
        apply(ApoliIds.REPLACE_SOUND_EMISSION, source, sound, volume, pitch, false, emitter);
    }

    public static void receive(@Nullable Entity listener, SoundEvent sound, float volume, float pitch, Emitter emitter) {
        apply(ApoliIds.REPLACE_SOUND_RECEPTION, listener, sound, volume, pitch, true, emitter);
    }

    private static void apply(ResourceLocation typeId, @Nullable Entity holder, SoundEvent sound,
                              float volume, float pitch, boolean runActions, Emitter emitter) {
        List<ReplaceSoundPower.Config> entries = collect(typeId, holder);
        if (entries.isEmpty()) {
            emitter.play(sound, volume, pitch);
            return;
        }

        RandomSource random = holder.level().random;
        ResourceLocation soundId = sound.getLocation();
        EntityCtx ctx = null;
        WeightedSound replacement = null;
        WeightedSound[] layers = null;
        int layerCount = 0;

        for (int i = 0; i < entries.size(); i++) {
            ReplaceSoundPower.Config cfg = entries.get(i);
            WeightedSound picked = cfg.sounds().find(soundId, random);
            if (picked == null) continue;

            if (runActions && cfg.entityAction().isPresent()) {
                if (ctx == null) ctx = new EntityCtx(holder, holder.level());
                cfg.entityAction().get().run(ctx);
            }

            if (cfg.replace()) {
                replacement = picked;
                break;
            }
            if (layers == null) layers = new WeightedSound[entries.size() - i];
            layers[layerCount++] = picked;
        }

        if (replacement == null) emitter.play(sound, volume, pitch);
        else emit(emitter, replacement, volume, pitch, false);
        for (int i = 0; i < layerCount; i++) {
            emit(emitter, layers[i], volume, pitch, true);
        }
    }

    private static void emit(Emitter emitter, WeightedSound picked, float volume, float pitch, boolean layered) {
        if (picked.mutes()) return;
        SoundEvent resolved = resolve(picked.id());
        if (resolved == null) return;
        if (layered) emitter.layer(resolved, picked.volumeOr(volume), picked.pitchOr(pitch));
        else emitter.play(resolved, picked.volumeOr(volume), picked.pitchOr(pitch));
    }

    private static List<ReplaceSoundPower.Config> collect(ResourceLocation typeId, @Nullable Entity holder) {
        if (holder == null) return List.of();
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return List.of();
        List<ResourceLocation> powers = container.powersOfType(typeId);
        if (powers.isEmpty()) return List.of();

        List<ReplaceSoundPower.Config> entries = null;
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
            entries.add(cfg);
        }
        if (entries == null) return List.of();
        if (entries.size() > 1) entries.sort(BY_PRIORITY);
        return entries;
    }

    private static @Nullable SoundEvent resolve(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return null;
        return Sounds.event(location);
    }
}
