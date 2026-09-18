package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.StorageTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

public final class StoreDataAction implements ActionType<EntityCtx, StorageTarget> {

    @Override
    public MapCodec<StorageTarget> codec() {
        return StorageTarget.CODEC;
    }

    @Override
    public void run(StorageTarget cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return;
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;

        CompoundTag out = new CompoundTag();
        out.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        out.putString("uuid", entity.getStringUUID());
        out.putString("name", entity.getName().getString());
        out.putDouble("x", entity.getX());
        out.putDouble("y", entity.getY());
        out.putDouble("z", entity.getZ());
        out.putString("pos", entity.getX() + " " + entity.getY() + " " + entity.getZ());
        out.putString("dimension", entity.level().dimension().location().toString());
        if (cfg.nbt()) out.put("nbt", entity.saveWithoutId(new CompoundTag()));
        cfg.write(server, out);
    }
}
