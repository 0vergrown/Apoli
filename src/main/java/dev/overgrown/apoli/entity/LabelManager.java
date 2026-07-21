package dev.overgrown.apoli.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class LabelManager {
    private LabelManager() {}

    public static final class EntityLabels {
        final Map<ResourceLocation, Component> texts = new LinkedHashMap<>(4);
        @Nullable Component chatName;
        @Nullable Component tabName;
        int chatPriority = Integer.MIN_VALUE;
        int tabPriority = Integer.MIN_VALUE;
    }

    private static final Map<UUID, EntityLabels> LABELS = new ConcurrentHashMap<>();

    @Nullable
    private static BiConsumer<Entity, Map<ResourceLocation, Component>> broadcaster;

    public static void setBroadcaster(BiConsumer<Entity, Map<ResourceLocation, Component>> sender) {
        broadcaster = sender;
    }

    public static boolean setText(Entity entity, ResourceLocation powerId, @Nullable Component text) {
        if (text == null) {
            EntityLabels labels = LABELS.get(entity.getUUID());
            if (labels == null) return false;
            boolean removed = labels.texts.remove(powerId) != null;
            if (removed && labels.texts.isEmpty() && labels.chatName == null && labels.tabName == null) {
                LABELS.remove(entity.getUUID());
            }
            return removed;
        }
        EntityLabels labels = LABELS.computeIfAbsent(entity.getUUID(), k -> new EntityLabels());
        Component previous = labels.texts.put(powerId, text);
        return previous == null || !previous.equals(text);
    }

    public static boolean setNameOverrides(Entity entity, @Nullable Component chatName, int chatPriority,
                                           @Nullable Component tabName, int tabPriority) {
        EntityLabels labels = LABELS.get(entity.getUUID());
        if (labels == null) {
            if (chatName == null && tabName == null) return false;
            labels = LABELS.computeIfAbsent(entity.getUUID(), k -> new EntityLabels());
        }
        boolean changed = !java.util.Objects.equals(labels.chatName, chatName)
            || !java.util.Objects.equals(labels.tabName, tabName);
        labels.chatName = chatName;
        labels.chatPriority = chatPriority;
        labels.tabName = tabName;
        labels.tabPriority = tabPriority;
        if (labels.texts.isEmpty() && chatName == null && tabName == null) {
            LABELS.remove(entity.getUUID());
        }
        return changed;
    }

    public static void broadcast(Entity entity) {
        if (broadcaster == null) return;
        EntityLabels labels = LABELS.get(entity.getUUID());
        broadcaster.accept(entity, labels == null ? Map.of() : labels.texts);
    }

    public static Map<ResourceLocation, Component> textsFor(UUID entity) {
        EntityLabels labels = LABELS.get(entity);
        return labels == null ? Map.of() : Collections.unmodifiableMap(labels.texts);
    }

    @Nullable
    public static Component chatName(UUID entity) {
        EntityLabels labels = LABELS.get(entity);
        return labels == null ? null : labels.chatName;
    }

    @Nullable
    public static Component tabName(UUID entity) {
        EntityLabels labels = LABELS.get(entity);
        return labels == null ? null : labels.tabName;
    }

    public static boolean isEmptyFor(UUID entity) {
        return !LABELS.containsKey(entity);
    }

    public static void onEntityGone(UUID entity) {
        LABELS.remove(entity);
    }
}
