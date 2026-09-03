package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.StorageTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

public final class StoreDataItemAction implements ActionType<ItemCtx, StorageTarget> {

    @Override
    public MapCodec<StorageTarget> codec() {
        return StorageTarget.CODEC;
    }

    @Override
    public void run(StorageTarget cfg, ItemCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;
        ItemStack stack = ctx.stack();

        CompoundTag out = new CompoundTag();
        out.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.putInt("count", stack.getCount());
        out.putString("name", stack.getHoverName().getString());
        if (cfg.nbt()) out.put("nbt", saveStack(ctx, stack));
        cfg.write(server, out);
    }

    private static CompoundTag saveStack(ItemCtx ctx, ItemStack stack) {
        return stack.save(new CompoundTag());
    }
}
