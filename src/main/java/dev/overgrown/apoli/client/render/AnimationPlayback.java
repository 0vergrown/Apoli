package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.client.model.BedrockAnimation;
import dev.overgrown.apoli.data.ModelAnimation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

public final class AnimationPlayback {

    private static final int SWEEP_INTERVAL = 200;

    private static final Int2ObjectOpenHashMap<Tracks> ENTITIES = new Int2ObjectOpenHashMap<>();
    private static WeakReference<ClientLevel> level = new WeakReference<>(null);
    private static long nextSweep;

    private AnimationPlayback() {}

    public static void beginRender(Entity entity) {
        if (ENTITIES.isEmpty()) return;
        Tracks tracks = ENTITIES.get(entity.getId());
        if (tracks != null) tracks.renders++;
    }

    public static void release(Entity entity, ResourceLocation model) {
        if (ENTITIES.isEmpty()) return;
        Tracks tracks = ENTITIES.get(entity.getId());
        if (tracks == null) return;
        tracks.dropAll(model);
    }

    @Nullable
    public static Track resolve(Entity entity, ResourceLocation model, ModelAnimation animations) {
        maintain();
        int chosen = animations.selectIndex(entity);
        int tick = entity.tickCount;
        Tracks tracks = ENTITIES.get(entity.getId());
        Track track = tracks == null ? null : tracks.find(model, animations);
        if (track == null) {
            if (chosen < 0) return null;
            if (tracks == null) {
                tracks = new Tracks();
                ENTITIES.put(entity.getId(), tracks);
            }
            track = tracks.claim(model, animations);
            track.start(animations.entries(), chosen, tick);
            track.seen = tracks.renders;
            return track;
        }
        boolean dropped = track.dropped || tick < track.startTick || track.seen < tracks.renders - 1;
        track.seen = tracks.renders;
        List<ModelAnimation.Entry> entries = animations.entries();
        if (chosen >= 0 && chosen == track.index && entries.get(chosen) == track.entry) {
            if (dropped || track.unchosen) track.start(entries, chosen, tick);
            return track;
        }
        if (!dropped && track.finishingOnce(entries, chosen, tick)) {
            track.unchosen = true;
            return track;
        }
        if (chosen < 0) {
            track.dropped = true;
            return null;
        }
        track.start(entries, chosen, tick);
        return track;
    }

    public static float elapsedSince(int startTick, int tickCount, float partialTick, float speed) {
        return ((tickCount - startTick) + partialTick) / 20.0F * speed;
    }

    public static void clear() {
        ENTITIES.clear();
    }

    private static void maintain() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level.get()) {
            ENTITIES.clear();
            level = new WeakReference<>(current);
            nextSweep = 0L;
            return;
        }
        if (current == null || ENTITIES.isEmpty()) return;
        long now = current.getGameTime();
        if (now < nextSweep && nextSweep - now <= SWEEP_INTERVAL) return;
        nextSweep = now + SWEEP_INTERVAL;
        ObjectIterator<Int2ObjectMap.Entry<Tracks>> iterator = ENTITIES.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            if (current.getEntity(iterator.next().getIntKey()) == null) iterator.remove();
        }
    }

    public static final class Track {
        private final ResourceLocation model;
        private ModelAnimation animations;
        private int index = -1;
        @Nullable
        private ModelAnimation.Entry entry;
        private int startTick;
        private int seen;
        private boolean unchosen;
        private boolean dropped;

        private Track(ResourceLocation model, ModelAnimation animations) {
            this.model = model;
            this.animations = animations;
        }

        public ModelAnimation.Entry entry() {
            return this.entry;
        }

        public int startTick() {
            return this.startTick;
        }

        public boolean chosen() {
            return !this.unchosen;
        }

        private void start(List<ModelAnimation.Entry> entries, int index, int tick) {
            this.index = index;
            this.entry = entries.get(index);
            this.startTick = tick;
            this.unchosen = false;
            this.dropped = false;
        }

        private boolean finishingOnce(List<ModelAnimation.Entry> entries, int chosen, int tick) {
            if (this.entry == null || this.index >= entries.size() || entries.get(this.index) != this.entry) return false;
            if (chosen >= 0 && chosen < this.index) return false;
            BedrockAnimation animation = AnimationManager.get(this.entry.animation(), this.entry.name().orElse(null));
            if (animation == null || !animation.playsOnce(this.entry.loop().orElse(null))) return false;
            return elapsedSince(this.startTick, tick, 0.0F, this.entry.speed()) <= animation.length();
        }
    }

    private static final class Tracks {
        private int renders;
        private Track[] tracks = new Track[2];
        private int size;

        @Nullable
        private Track find(ResourceLocation model, ModelAnimation animations) {
            for (int i = 0; i < this.size; i++) {
                Track track = this.tracks[i];
                if (track.animations == animations && (track.model == model || track.model.equals(model))) return track;
            }
            return null;
        }

        private void dropAll(ResourceLocation model) {
            for (int i = 0; i < this.size; i++) {
                Track track = this.tracks[i];
                if (track.model == model || track.model.equals(model)) track.dropped = true;
            }
        }

        private Track claim(ResourceLocation model, ModelAnimation animations) {
            for (int i = 0; i < this.size; i++) {
                Track track = this.tracks[i];
                if (track.seen < this.renders - 1 && (track.model == model || track.model.equals(model))) {
                    track.animations = animations;
                    track.index = -1;
                    track.entry = null;
                    return track;
                }
            }
            if (this.size == this.tracks.length) {
                Track[] grown = new Track[this.size * 2];
                System.arraycopy(this.tracks, 0, grown, 0, this.size);
                this.tracks = grown;
            }
            Track track = new Track(model, animations);
            this.tracks[this.size++] = track;
            return track;
        }
    }
}
