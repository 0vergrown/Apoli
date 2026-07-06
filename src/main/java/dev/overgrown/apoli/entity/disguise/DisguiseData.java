package dev.overgrown.apoli.entity.disguise;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;


public record DisguiseData(ResourceLocation entityTypeId, Optional<UUID> playerUuid, Optional<CompoundTag> nbt, Optional<String> name) {

    public boolean isPlayerDisguise() {
        return playerUuid.isPresent();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(entityTypeId);
        buf.writeOptional(playerUuid, (b, uuid) -> b.writeUUID(uuid));
        buf.writeOptional(nbt, (b, tag) -> b.writeNbt(tag));
        buf.writeOptional(name, (b, string) -> b.writeUtf(string));
    }

    public static DisguiseData read(FriendlyByteBuf buf) {
        return new DisguiseData(
            buf.readResourceLocation(),
            buf.readOptional(b -> b.readUUID()),
            buf.readOptional(b -> b.readNbt()),
            buf.readOptional(b -> b.readUtf()));
    }
}
