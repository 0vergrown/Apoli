package dev.overgrown.apoli.data;

import dev.overgrown.apoli.condition.builtin.entity.EntityNbtSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class NbtSources {
    private NbtSources() {}

    private static final class TickCache {
        long tick = Long.MIN_VALUE;
        final Map<ItemStack, Tag> stacks = new IdentityHashMap<>();
        final Map<BlockEntity, Tag> blocks = new IdentityHashMap<>();

        TickCache at(long now) {
            if (tick != now) {
                stacks.clear();
                blocks.clear();
                tick = now;
            }
            return this;
        }
    }

    private static final ThreadLocal<TickCache> CACHE = ThreadLocal.withInitial(TickCache::new);

    @Nullable
    public static Tag ofEntity(@Nullable Entity entity) {
        return entity == null ? null : EntityNbtSnapshot.cached(entity);
    }

    @Nullable
    public static Tag ofStack(ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty() || level == null) return null;
        TickCache cache = CACHE.get().at(level.getGameTime());
        Tag tag = cache.stacks.get(stack);
        if (tag == null) {
            tag = stack.save(level.registryAccess());
            cache.stacks.put(stack, tag);
        }
        return tag;
    }

    @Nullable
    public static Tag ofBlock(@Nullable Level level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        TickCache cache = CACHE.get().at(level.getGameTime());
        Tag tag = cache.blocks.get(blockEntity);
        if (tag == null) {
            tag = blockEntity.saveWithoutMetadata(level.registryAccess());
            cache.blocks.put(blockEntity, tag);
        }
        return tag;
    }

    @Nullable
    public static CompoundTag ofStorage(@Nullable Level level, ResourceLocation id) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getCommandStorage().get(id);
    }
}
