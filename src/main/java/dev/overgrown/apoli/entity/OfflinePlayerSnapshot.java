package dev.overgrown.apoli.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

public final class OfflinePlayerSnapshot extends FakePlayer {
    private OfflinePlayerSnapshot(ServerLevel level, GameProfile profile) {
        super(level, profile);
    }

    public static @Nullable OfflinePlayerSnapshot load(MinecraftServer server, GameProfile profile) {
        OfflinePlayerSnapshot snapshot = new OfflinePlayerSnapshot(server.overworld(), profile);
        CompoundTag data = server.getPlayerList().load(snapshot);
        if (data == null) return null;
        ServerLevel level = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, data.get("Dimension")))
            .result().map(server::getLevel).orElse(null);
        if (level != null && level != snapshot.serverLevel()) snapshot.setServerLevel(level);
        return snapshot;
    }
}
