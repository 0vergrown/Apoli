package dev.overgrown.apoli.compat.walkers;

import dev.overgrown.apoli.Apoli;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class WalkersBridge {
    private WalkersBridge() {}

    private static final String PLAYER_SHAPE = "tocraft.walkers.api.PlayerShape";
    private static final String PLAYER_ABILITIES = "tocraft.walkers.api.PlayerAbilities";
    private static final String ABILITY_REGISTRY = "tocraft.walkers.ability.AbilityRegistry";

    private static final MethodHandle GET_CURRENT_SHAPE;
    private static final MethodHandle UPDATE_SHAPES;
    private static final MethodHandle GET_COOLDOWN;
    private static final MethodHandle CAN_USE_ABILITY;
    private static final MethodHandle SET_COOLDOWN;
    private static final MethodHandle SYNC_ABILITY;
    private static final MethodHandle HAS_ABILITY;
    private static final boolean PRESENT;

    static {
        MethodHandle getCurrentShape = null;
        MethodHandle updateShapes = null;
        MethodHandle getCooldown = null;
        MethodHandle canUseAbility = null;
        MethodHandle setCooldown = null;
        MethodHandle syncAbility = null;
        MethodHandle hasAbility = null;
        boolean present = false;
        if (classExists(PLAYER_SHAPE)) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> shapeClass = Class.forName(PLAYER_SHAPE);
                Class<?> abilitiesClass = Class.forName(PLAYER_ABILITIES);
                Class<?> registryClass = Class.forName(ABILITY_REGISTRY);
                getCurrentShape = lookup.findStatic(shapeClass, "getCurrentShape",
                    MethodType.methodType(LivingEntity.class, Player.class));
                updateShapes = lookup.findStatic(shapeClass, "updateShapes",
                    MethodType.methodType(boolean.class, ServerPlayer.class, LivingEntity.class));
                getCooldown = lookup.findStatic(abilitiesClass, "getCooldown",
                    MethodType.methodType(int.class, Player.class));
                canUseAbility = lookup.findStatic(abilitiesClass, "canUseAbility",
                    MethodType.methodType(boolean.class, Player.class));
                setCooldown = lookup.findStatic(abilitiesClass, "setCooldown",
                    MethodType.methodType(void.class, Player.class, int.class));
                syncAbility = lookup.findStatic(abilitiesClass, "sync",
                    MethodType.methodType(void.class, ServerPlayer.class));
                hasAbility = lookup.findStatic(registryClass, "has",
                    MethodType.methodType(boolean.class, LivingEntity.class));
                present = true;
            } catch (Throwable t) {
                Apoli.LOGGER.warn("[Apoli] Walkers is installed but its API did not match; shape compat is disabled", t);
                present = false;
            }
        }
        GET_CURRENT_SHAPE = getCurrentShape;
        UPDATE_SHAPES = updateShapes;
        GET_COOLDOWN = getCooldown;
        CAN_USE_ABILITY = canUseAbility;
        SET_COOLDOWN = setCooldown;
        SYNC_ABILITY = syncAbility;
        HAS_ABILITY = hasAbility;
        PRESENT = present;
    }

    private static boolean classExists(String name) {
        return WalkersBridge.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }

    public static boolean present() {
        return PRESENT;
    }

    @Nullable
    public static LivingEntity currentShape(@Nullable Entity entity) {
        if (!PRESENT || !(entity instanceof Player player)) return null;
        try {
            return (LivingEntity) GET_CURRENT_SHAPE.invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Entity shapeOrSelf(Entity entity) {
        LivingEntity shape = currentShape(entity);
        return shape == null ? entity : shape;
    }

    public static boolean hasShapeAbility(@Nullable Entity entity) {
        if (!PRESENT || !(entity instanceof LivingEntity living)) return false;
        Entity shape = shapeOrSelf(living);
        if (!(shape instanceof LivingEntity shapeLiving)) return false;
        try {
            return (boolean) HAS_ABILITY.invoke(shapeLiving);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean canUseAbility(@Nullable Entity entity) {
        if (!PRESENT || !(entity instanceof Player player)) return false;
        try {
            return (boolean) CAN_USE_ABILITY.invoke(player);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int abilityCooldown(@Nullable Entity entity) {
        if (!PRESENT || !(entity instanceof Player player)) return 0;
        try {
            return (int) GET_COOLDOWN.invoke(player);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void setAbilityCooldown(Entity entity, int cooldown) {
        if (!PRESENT || !(entity instanceof Player player)) return;
        try {
            SET_COOLDOWN.invoke(player, cooldown);
            if (player instanceof ServerPlayer serverPlayer) SYNC_ABILITY.invoke(serverPlayer);
        } catch (Throwable ignored) {
        }
    }

    public static boolean switchShape(Entity entity, @Nullable ResourceLocation shapeId, @Nullable CompoundTag nbt) {
        if (!PRESENT || !(entity instanceof ServerPlayer player)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        LivingEntity shape = null;
        if (shapeId != null) {
            CompoundTag tag = nbt == null ? new CompoundTag() : nbt.copy();
            tag.putString("id", shapeId.toString());
            Entity loaded = EntityType.loadEntityRecursive(tag, level, e -> e);
            if (!(loaded instanceof LivingEntity living)) return false;
            shape = living;
        }
        try {
            return (boolean) UPDATE_SHAPES.invoke(player, shape);
        } catch (Throwable t) {
            return false;
        }
    }
}
