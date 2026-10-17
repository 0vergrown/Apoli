package dev.overgrown.apoli.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class OfflinePlayerSnapshot extends FakePlayer {
    private OfflinePlayerSnapshot(ServerLevel level, GameProfile profile) {
        super(level, profile);
    }

    public static @Nullable OfflinePlayerSnapshot load(MinecraftServer server, GameProfile profile) {
        OfflinePlayerSnapshot snapshot = new OfflinePlayerSnapshot(server.overworld(), profile);
        Optional<CompoundTag> data = server.getPlayerList().load(snapshot);
        if (data.isEmpty()) return null;
        ServerLevel level = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, data.get().get("Dimension")))
            .result().map(server::getLevel).orElse(null);
        if (level != null && level != snapshot.serverLevel()) snapshot.setServerLevel(level);
        return snapshot;
    }
}
