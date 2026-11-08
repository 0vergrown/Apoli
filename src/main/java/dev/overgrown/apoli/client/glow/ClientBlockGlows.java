package dev.overgrown.apoli.client.glow;

import dev.overgrown.apoli.data.BlockGlowStyle;
import dev.overgrown.apoli.network.payload.BlockGlowsS2C;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientBlockGlows {

    public record Glow(long pos, Block block, int color, boolean respectBounding, boolean xray, long expiresAt) {}

    private static final Long2ObjectOpenHashMap<Glow> GLOWS = new Long2ObjectOpenHashMap<>();
    private static @Nullable ClientLevel level;
    private static int ticks;

    private ClientBlockGlows() {}

    public static void apply(BlockGlowsS2C payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) return;
        if (current != level || payload.reset()) {
            GLOWS.clear();
            level = current;
        }
        for (long pos : payload.removed()) GLOWS.remove(pos);
        long now = current.getGameTime();
        List<BlockGlowsS2C.Entry> added = payload.added();
        for (int i = 0, n = added.size(); i < n; i++) {
            BlockGlowsS2C.Entry entry = added.get(i);
            Block block = BuiltInRegistries.BLOCK.byId(entry.block());
            long expiresAt = entry.remaining() < 0 ? Long.MAX_VALUE : now + entry.remaining();
            GLOWS.put(entry.pos(), new Glow(entry.pos(), block, entry.color(),
                (entry.flags() & BlockGlowStyle.FLAG_RESPECT_BOUNDING) != 0,
                (entry.flags() & BlockGlowStyle.FLAG_XRAY) != 0, expiresAt));
        }
    }

    public static void clientTick(Minecraft mc) {
        if (mc.level != level) {
            GLOWS.clear();
            level = mc.level;
            return;
        }
        if (GLOWS.isEmpty() || level == null || (++ticks & 15) != 0) return;
        long now = level.getGameTime();
        GLOWS.values().removeIf(glow -> glow.expiresAt() <= now);
    }

    public static boolean isEmpty() {
        return GLOWS.isEmpty();
    }

    public static ObjectCollection<Glow> glows() {
        return GLOWS.values();
    }

    public static void clear() {
        GLOWS.clear();
        level = null;
    }
}
