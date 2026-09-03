package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.StorageTarget;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class StoreDataBlockAction implements ActionType<BlockCtx, StorageTarget> {

    @Override
    public MapCodec<StorageTarget> codec() {
        return StorageTarget.CODEC;
    }

    @Override
    public void run(StorageTarget cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;

        BlockPos pos = ctx.pos();
        BlockState state = ctx.state();
        CompoundTag out = new CompoundTag();
        out.putString("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        out.putString("state", BlockStateParser.serialize(state));
        out.putInt("x", pos.getX());
        out.putInt("y", pos.getY());
        out.putInt("z", pos.getZ());
        out.putString("pos", pos.getX() + " " + pos.getY() + " " + pos.getZ());
        out.putString("dimension", level.dimension().location().toString());

        CompoundTag properties = new CompoundTag();
        for (Property<?> property : state.getProperties()) {
            properties.putString(property.getName(), name(state, property));
        }
        out.put("properties", properties);

        if (cfg.nbt()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) out.put("nbt", saveBlockEntity(level, blockEntity));
        }
        cfg.write(server, out);
    }

    private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static CompoundTag saveBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        return blockEntity.saveWithoutMetadata();
    }
}
