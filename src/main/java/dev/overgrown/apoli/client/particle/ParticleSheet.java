package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.particle.ParticleFrameLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class ParticleSheet {

    private static final int[] SINGLE_ORDER = {0};

    public static final ParticleSheet SINGLE = new ParticleSheet(1, 1, SINGLE_ORDER, 0, null, 0, false);

    private record Key(ResourceLocation texture, ParticleFrameLayout layout, int frames, int frameTime) {}

    private static final Map<Key, ParticleSheet> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Source> SOURCES = new HashMap<>();

    private final int columns;
    private final int rows;
    private final int[] order;
    private final int uniformTime;
    private final int @Nullable [] starts;
    private final int totalTicks;
    private final boolean metadataDriven;

    private ParticleSheet(int columns, int rows, int[] order, int uniformTime,
                          int @Nullable [] starts, int totalTicks, boolean metadataDriven) {
        this.columns = Math.max(1, columns);
        this.rows = Math.max(1, rows);
        this.order = order;
        this.uniformTime = uniformTime;
        this.starts = starts;
        this.totalTicks = totalTicks;
        this.metadataDriven = metadataDriven;
    }

    public static synchronized ParticleSheet of(ResourceLocation texture, ParticleFrameLayout layout,
                                                int frames, int frameTime) {
        if (frames == 1) return SINGLE;
        Key key = new Key(texture, layout, frames, frameTime);
        ParticleSheet cached = CACHE.get(key);
        if (cached != null) return cached;
        ParticleSheet built = build(texture, layout, frames, frameTime);
        CACHE.put(key, built);
        return built;
    }

    public static synchronized void clearCache() {
        CACHE.clear();
        SOURCES.clear();
    }

    public boolean animated() {
        return this.order.length > 1;
    }

    public boolean loopsByDefault() {
        return this.metadataDriven;
    }

    public int stepCount() {
        return this.order.length;
    }

    public int cellAt(int age, int lifetime, boolean loop) {
        int steps = this.order.length;
        if (steps <= 1) return this.order[0];
        int index;
        if (this.starts != null) {
            int time = loop ? Math.floorMod(age, this.totalTicks) : Math.min(age, this.totalTicks - 1);
            index = stepOf(time);
        } else if (this.uniformTime > 0) {
            index = age / this.uniformTime;
        } else {
            index = (int) ((long) age * steps / Math.max(1, lifetime));
        }
        return this.order[loop ? Math.floorMod(index, steps) : Mth.clamp(index, 0, steps - 1)];
    }

    private int stepOf(int time) {
        int low = 0;
        int high = this.starts.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (this.starts[mid] <= time) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    public float u0(int cell) {
        return (cell % this.columns) / (float) this.columns;
    }

    public float u1(int cell) {
        return (cell % this.columns + 1) / (float) this.columns;
    }

    public float v0(int cell) {
        return (cell / this.columns) / (float) this.rows;
    }

    public float v1(int cell) {
        return (cell / this.columns + 1) / (float) this.rows;
    }

    private record Source(int width, int height, @Nullable AnimationMetadataSection animation) {}

    private static ParticleSheet build(ResourceLocation texture, ParticleFrameLayout layout,
                                       int frames, int frameTime) {
        Source source = sourceOf(texture);
        if (source == null) return declaredOnly(layout, frames, frameTime);

        AnimationMetadataSection animation = source.animation();
        int columns;
        int rows;
        if (animation != null) {
            FrameSize size = animation.calculateFrameSize(source.width(), source.height());
            columns = Math.max(1, source.width() / Math.max(1, size.width()));
            rows = Math.max(1, source.height() / Math.max(1, size.height()));
        } else {
            int[] grid = declaredGrid(layout, frames, source.width(), source.height());
            columns = grid[0];
            rows = grid[1];
        }

        int cells = columns * rows;
        if (cells <= 1) return SINGLE;

        if (animation != null) {
            List<int[]> steps = new ArrayList<>();
            animation.forEachFrame((index, time) -> {
                if (index >= 0 && index < cells && time > 0) steps.add(new int[]{index, time});
            });
            if (steps.isEmpty()) {
                int[] order = sequence(cells);
                int time = frameTime > 0 ? frameTime : Math.max(1, animation.getDefaultFrameTime());
                return new ParticleSheet(columns, rows, order, time, null, 0, true);
            }
            int[] order = new int[steps.size()];
            for (int i = 0; i < order.length; i++) order[i] = steps.get(i)[0];
            if (frameTime > 0) return new ParticleSheet(columns, rows, order, frameTime, null, 0, true);
            int[] starts = new int[steps.size()];
            int total = 0;
            boolean uniform = true;
            int first = steps.get(0)[1];
            for (int i = 0; i < steps.size(); i++) {
                starts[i] = total;
                int time = steps.get(i)[1];
                if (time != first) uniform = false;
                total += time;
            }
            if (uniform) return new ParticleSheet(columns, rows, order, first, null, 0, true);
            return new ParticleSheet(columns, rows, order, 0, starts, Math.max(1, total), true);
        }

        int steps = frames > 0 ? Math.min(frames, cells) : cells;
        return new ParticleSheet(columns, rows, sequence(steps), Math.max(0, frameTime), null, 0, false);
    }

    private static ParticleSheet declaredOnly(ParticleFrameLayout layout, int frames, int frameTime) {
        if (frames <= 1) return SINGLE;
        boolean row = layout == ParticleFrameLayout.HORIZONTAL;
        return new ParticleSheet(row ? frames : 1, row ? 1 : frames, sequence(frames),
            Math.max(0, frameTime), null, 0, false);
    }

    private static int[] declaredGrid(ParticleFrameLayout layout, int frames, int width, int height) {
        if (frames <= 1 && layout == ParticleFrameLayout.AUTO) return new int[]{1, 1};
        return switch (layout) {
            case VERTICAL -> new int[]{1, frames > 0 ? frames : stripCount(height, width)};
            case HORIZONTAL -> new int[]{frames > 0 ? frames : stripCount(width, height), 1};
            case GRID -> {
                int cell = Math.max(1, Math.min(width, height));
                yield new int[]{Math.max(1, width / cell), Math.max(1, height / cell)};
            }
            case AUTO -> height >= width ? new int[]{1, frames} : new int[]{frames, 1};
        };
    }

    private static int stripCount(int along, int across) {
        if (across <= 0 || along <= across || along % across != 0) return 1;
        return along / across;
    }

    private static int[] sequence(int count) {
        int[] order = new int[Math.max(1, count)];
        for (int i = 0; i < order.length; i++) order[i] = i;
        return order;
    }

    private static @Nullable Source sourceOf(ResourceLocation texture) {
        Source cached = SOURCES.get(texture);
        if (cached != null) return cached;
        Source built = readSource(texture);
        if (built != null) SOURCES.put(texture, built);
        return built;
    }

    private static @Nullable Source readSource(ResourceLocation texture) {
        Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (found.isEmpty()) return null;
        Resource resource = found.get();

        AnimationMetadataSection animation = null;
        try {
            animation = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not read the animation metadata of particle texture {}: {}",
                texture, e.toString());
        }

        int[] size = readSize(resource, texture);
        if (size == null) return null;
        return new Source(size[0], size[1], animation);
    }

    private static int @Nullable [] readSize(Resource resource, ResourceLocation texture) {
        try (InputStream stream = resource.open()) {
            byte[] header = stream.readNBytes(24);
            if (header.length == 24 && (header[0] & 0xFF) == 0x89 && header[1] == 'P'
                && header[2] == 'N' && header[3] == 'G'
                && header[12] == 'I' && header[13] == 'H' && header[14] == 'D' && header[15] == 'R') {
                int width = readInt(header, 16);
                int height = readInt(header, 20);
                if (width > 0 && height > 0) return new int[]{width, height};
            }
        } catch (Exception ignored) {
        }
        try (InputStream stream = resource.open(); NativeImage image = NativeImage.read(stream)) {
            return new int[]{Math.max(1, image.getWidth()), Math.max(1, image.getHeight())};
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not read the size of particle texture {}: {}", texture, e.toString());
            return null;
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }
}
