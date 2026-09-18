package dev.overgrown.apoli.scale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ScaleState {

    public static final float DEFAULT = 1.0F;
    public static final float MIN = 1.0E-4F;
    public static final float MAX = 1.0E4F;

    private final Entity owner;

    private float[] value;
    private float[] prev;
    private float[] from;
    private float[] to;
    private short[] ticks;
    private short[] total;
    private byte[] easing;
    private int animating;
    private boolean prevStale;

    private float[] cache;
    private float[] own;
    private long cacheTick = Long.MIN_VALUE;
    private int cacheGeneration = Integer.MIN_VALUE;
    private float cachePartial = Float.NaN;
    private boolean cacheVolatile = true;

    private float appliedWidth = Float.NaN;
    private float appliedHeight = Float.NaN;

    public ScaleState(Entity owner) {
        this.owner = owner;
    }

    public Entity owner() {
        return owner;
    }

    public boolean isDefault() {
        if (value == null) return true;
        for (int i = 0; i < value.length; i++) {
            if (value[i] != DEFAULT) return false;
        }
        return animating == 0;
    }

    public boolean isAnimating() {
        return animating > 0;
    }

    public boolean needsTick() {
        return animating > 0 || prevStale;
    }

    private void allocate() {
        if (value != null) return;
        int n = ScaleTypes.count();
        value = new float[n];
        prev = new float[n];
        from = new float[n];
        to = new float[n];
        ticks = new short[n];
        total = new short[n];
        easing = new byte[n];
        java.util.Arrays.fill(value, DEFAULT);
        java.util.Arrays.fill(prev, DEFAULT);
        java.util.Arrays.fill(from, DEFAULT);
        java.util.Arrays.fill(to, DEFAULT);
    }

    public float base(ScaleType type) {
        return value == null ? DEFAULT : value[type.index()];
    }

    public float base(ScaleType type, float partial) {
        return baseAt(type.index(), partial);
    }

    public float baseAt(int i, float partial) {
        if (value == null) return DEFAULT;
        if (total[i] == 0 || partial >= 1.0F) return value[i];
        return prev[i] + (value[i] - prev[i]) * partial;
    }

    public float target(ScaleType type) {
        if (value == null) return DEFAULT;
        int i = type.index();
        return total[i] == 0 ? value[i] : to[i];
    }

    public void set(ScaleType type, float target, int overTicks, ScaleEasing ease) {
        allocate();
        int i = type.index();
        float clamped = clamp(target);
        boolean wasAnimating = total[i] != 0;
        if (overTicks <= 0) {
            if (wasAnimating) animating--;
            total[i] = 0;
            ticks[i] = 0;
            from[i] = clamped;
            to[i] = clamped;
            prev[i] = clamped;
            value[i] = clamped;
        } else {
            if (!wasAnimating) animating++;
            from[i] = value[i];
            to[i] = clamped;
            ticks[i] = 0;
            total[i] = (short) Math.min(overTicks, Short.MAX_VALUE);
            easing[i] = (byte) ease.ordinal();
        }
        invalidate();
    }

    public void reset() {
        if (value == null) return;
        java.util.Arrays.fill(value, DEFAULT);
        java.util.Arrays.fill(prev, DEFAULT);
        java.util.Arrays.fill(from, DEFAULT);
        java.util.Arrays.fill(to, DEFAULT);
        java.util.Arrays.fill(ticks, (short) 0);
        java.util.Arrays.fill(total, (short) 0);
        java.util.Arrays.fill(easing, (byte) 0);
        animating = 0;
        prevStale = false;
        invalidate();
    }

    public void tick() {
        if (animating == 0) {
            if (prevStale) {
                if (value != null) System.arraycopy(value, 0, prev, 0, value.length);
                prevStale = false;
                invalidate();
            }
            return;
        }
        for (int i = 0; i < value.length; i++) {
            prev[i] = value[i];
            short span = total[i];
            if (span == 0) continue;
            short elapsed = (short) (ticks[i] + 1);
            if (elapsed >= span) {
                value[i] = to[i];
                ticks[i] = 0;
                total[i] = 0;
                animating--;
            } else {
                ticks[i] = elapsed;
                float t = ScaleEasing.byOrdinal(easing[i]).apply((float) elapsed / span);
                value[i] = from[i] + (to[i] - from[i]) * t;
            }
        }
        prevStale = true;
        invalidate();
    }

    public void invalidate() {
        cacheGeneration = Integer.MIN_VALUE;
        cachePartial = Float.NaN;
    }

    float[] cached(long gameTime, int generation, float partial) {
        if (cache == null || cacheGeneration != generation || cachePartial != partial) return null;
        if (cacheVolatile && cacheTick != gameTime) return null;
        return cache;
    }

    float[] cacheSlot() {
        if (cache == null) cache = new float[ScaleTypes.count()];
        return cache;
    }

    float[] ownSlot() {
        if (own == null) own = new float[ScaleTypes.count()];
        return own;
    }

    public float ownAt(int index) {
        return own == null ? DEFAULT : own[index];
    }

    void storeCache(long gameTime, int generation, float partial, boolean perTick) {
        cacheTick = gameTime;
        cacheGeneration = generation;
        cachePartial = partial;
        cacheVolatile = perTick;
    }

    public boolean dimensionsChanged(float width, float height) {
        if (appliedWidth == width && appliedHeight == height) return false;
        appliedWidth = width;
        appliedHeight = height;
        return true;
    }

    public void forgetDimensions() {
        appliedWidth = Float.NaN;
        appliedHeight = Float.NaN;
    }

    public static float clamp(float raw) {
        if (Float.isNaN(raw)) return DEFAULT;
        if (raw < MIN) return MIN;
        if (raw > MAX) return MAX;
        return raw;
    }

    public void save(CompoundTag tag) {
        if (value == null) return;
        ListTag list = new ListTag();
        for (int i = 0; i < value.length; i++) {
            boolean animated = total[i] != 0;
            if (value[i] == DEFAULT && !animated) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("id", ScaleTypes.byIndex(i).id().toString());
            entry.putFloat("value", value[i]);
            if (animated) {
                entry.putFloat("from", from[i]);
                entry.putFloat("to", to[i]);
                entry.putShort("ticks", ticks[i]);
                entry.putShort("total", total[i]);
                entry.putByte("easing", easing[i]);
            }
            list.add(entry);
        }
        if (!list.isEmpty()) tag.put("scales", list);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("scales", 9)) return;
        ListTag list = tag.getList("scales", 10);
        if (list.isEmpty()) return;
        allocate();
        animating = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ScaleType type = ScaleTypes.get(ResourceLocation.tryParse(entry.getString("id")));
            if (type == null) continue;
            int index = type.index();
            value[index] = clamp(entry.getFloat("value"));
            prev[index] = value[index];
            if (entry.contains("total")) {
                from[index] = clamp(entry.getFloat("from"));
                to[index] = clamp(entry.getFloat("to"));
                ticks[index] = entry.getShort("ticks");
                total[index] = entry.getShort("total");
                easing[index] = entry.getByte("easing");
                if (total[index] != 0) animating++;
            }
        }
        prevStale = animating > 0;
        forgetDimensions();
        invalidate();
    }

    public void write(FriendlyByteBuf buf) {
        int count = 0;
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                if (value[i] != DEFAULT || total[i] != 0) count++;
            }
        }
        buf.writeVarInt(count);
        if (count == 0) return;
        for (int i = 0; i < value.length; i++) {
            if (value[i] == DEFAULT && total[i] == 0) continue;
            buf.writeVarInt(i);
            buf.writeFloat(value[i]);
            buf.writeFloat(prev[i]);
            buf.writeFloat(from[i]);
            buf.writeFloat(to[i]);
            buf.writeShort(ticks[i]);
            buf.writeShort(total[i]);
            buf.writeByte(easing[i]);
        }
    }

    public void read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count == 0) {
            reset();
            forgetDimensions();
            return;
        }
        allocate();
        java.util.Arrays.fill(value, DEFAULT);
        java.util.Arrays.fill(prev, DEFAULT);
        java.util.Arrays.fill(total, (short) 0);
        java.util.Arrays.fill(ticks, (short) 0);
        animating = 0;
        int types = ScaleTypes.count();
        for (int n = 0; n < count; n++) {
            int index = buf.readVarInt();
            float v = buf.readFloat();
            float p = buf.readFloat();
            float f = buf.readFloat();
            float t = buf.readFloat();
            short tick = buf.readShort();
            short span = buf.readShort();
            byte ease = buf.readByte();
            if (index < 0 || index >= types) continue;
            value[index] = clamp(v);
            prev[index] = clamp(p);
            from[index] = clamp(f);
            to[index] = clamp(t);
            ticks[index] = tick;
            total[index] = span;
            easing[index] = ease;
            if (span != 0) animating++;
        }
        prevStale = animating > 0;
        forgetDimensions();
        invalidate();
    }

    public void copyFrom(@Nullable ScaleState other) {
        if (other == null || other.value == null) return;
        allocate();
        System.arraycopy(other.value, 0, value, 0, value.length);
        System.arraycopy(other.value, 0, prev, 0, value.length);
        java.util.Arrays.fill(total, (short) 0);
        java.util.Arrays.fill(ticks, (short) 0);
        animating = 0;
        prevStale = false;
        forgetDimensions();
        invalidate();
    }
}
