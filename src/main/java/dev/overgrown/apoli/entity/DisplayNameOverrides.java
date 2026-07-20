package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class DisplayNameOverrides {
    private DisplayNameOverrides() {}

    @Nullable
    public static Component chatNameFor(@Nullable Entity entity) {
        if (entity == null || entity.level().isClientSide()) return null;
        Component label = LabelManager.chatName(entity.getUUID());
        if (label != null) return label;
        return disguiseName(entity);
    }

    @Nullable
    public static Component tabNameFor(Entity player) {
        if (player.level().isClientSide()) return null;
        Component label = LabelManager.tabName(player.getUUID());
        if (label != null) return label;
        return disguiseName(player);
    }

    @Nullable
    private static Component disguiseName(Entity entity) {
        DisguiseData data = DisguiseManager.get(entity.getUUID());
        if (data == null) return null;
        if (data.name().isPresent()) {
            String name = data.name().get();
            return name.isEmpty() ? null : Component.literal(name);
        }
        if (data.isPlayerDisguise()) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(data.entityTypeId());
        return type != null ? Component.translatable(type.getDescriptionId()) : null;
    }
}
