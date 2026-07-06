package dev.overgrown.apoli.entity.disguise;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;


public final class DisguiseManager {
    private DisguiseManager() {}

    private static final Map<UUID, DisguiseData> DISGUISES = new ConcurrentHashMap<>();

    
    @Nullable
    private static BiConsumer<Entity, Optional<DisguiseData>> broadcaster;

    public interface ClientView {
        boolean isDisguised(int netId);

        boolean isDisguisedAs(int actorNetId, Entity target);
    }

    @Nullable
    private static ClientView clientView;

    public static void setBroadcaster(BiConsumer<Entity, Optional<DisguiseData>> sender) {
        broadcaster = sender;
    }

    public static void setClientView(ClientView view) {
        clientView = view;
    }

    

    public static void apply(LivingEntity actor, DisguiseData data, boolean overwrite) {
        if (actor.level().isClientSide) return;
        if (!overwrite && DISGUISES.containsKey(actor.getUUID())) return;
        DISGUISES.put(actor.getUUID(), data);
        if (broadcaster != null) broadcaster.accept(actor, Optional.of(data));
    }

    
    public static void applyAs(LivingEntity actor, Entity target, boolean overwrite) {
        if (actor.level().isClientSide) return;
        if (!overwrite && DISGUISES.containsKey(actor.getUUID())) return;
        Optional<UUID> playerUuid = target instanceof Player ? Optional.of(target.getUUID()) : Optional.empty();
        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);
        String name = target.getName().getString();
        apply(actor, new DisguiseData(
            BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()),
            playerUuid,
            Optional.of(nbt),
            Optional.ofNullable(name.isEmpty() ? null : name)
        ), true);
    }

    public static void remove(LivingEntity actor) {
        if (actor.level().isClientSide) return;
        if (DISGUISES.remove(actor.getUUID()) != null && broadcaster != null) {
            broadcaster.accept(actor, Optional.empty());
        }
    }

    public static void onPlayerLeave(UUID uuid) {
        DISGUISES.remove(uuid);
    }

    

    public static boolean isDisguised(@Nullable Entity entity) {
        if (entity == null) return false;
        if (entity.level().isClientSide) return clientView != null && clientView.isDisguised(entity.getId());
        return DISGUISES.containsKey(entity.getUUID());
    }

    public static boolean isDisguisedAs(@Nullable Entity actor, @Nullable Entity target) {
        if (actor == null || target == null) return false;
        if (actor.level().isClientSide) return clientView != null && clientView.isDisguisedAs(actor.getId(), target);
        return matches(DISGUISES.get(actor.getUUID()), target);
    }

    public static boolean matches(@Nullable DisguiseData data, Entity target) {
        if (data == null) return false;
        if (target instanceof Player && data.playerUuid().isPresent()) {
            return data.playerUuid().get().equals(target.getUUID());
        }
        return data.entityTypeId().equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
    }

    
    public static Map<UUID, DisguiseData> all() {
        return Collections.unmodifiableMap(DISGUISES);
    }

    @Nullable
    public static DisguiseData get(UUID uuid) {
        return DISGUISES.get(uuid);
    }
}
