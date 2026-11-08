package dev.overgrown.apoli.compat.create;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

final class CreateBacktanks {

    private CreateBacktanks() {}

    private static final String BACKTANK_UTIL = "com.simibubi.create.content.equipment.armor.BacktankUtil";
    private static final int AIR_PER_SECOND = 1;
    private static final int TICKS_PER_AIR = 20;

    private static final MethodHandle ALL_WITH_AIR;
    private static final MethodHandle CONSUME_AIR;

    static {
        MethodHandle allWithAir = null;
        MethodHandle consumeAir = null;
        try {
            Class<?> util = Class.forName(BACKTANK_UTIL);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            allWithAir = lookup.findStatic(util, "getAllWithAir", MethodType.methodType(List.class, LivingEntity.class));
            for (Method method : util.getMethods()) {
                if (!method.getName().equals("consumeAir") || method.getParameterCount() != 3) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != LivingEntity.class || params[1] != ItemStack.class) continue;
                consumeAir = lookup.unreflect(method)
                    .asType(MethodType.methodType(void.class, LivingEntity.class, ItemStack.class, int.class));
                break;
            }
        } catch (Throwable t) {
            allWithAir = null;
            consumeAir = null;
        }
        ALL_WITH_AIR = allWithAir;
        CONSUME_AIR = consumeAir;
    }

    @SuppressWarnings("unchecked")
    static boolean breathe(LivingEntity entity) {
        if (ALL_WITH_AIR == null) return false;
        List<ItemStack> tanks;
        try {
            tanks = (List<ItemStack>) ALL_WITH_AIR.invokeExact(entity);
        } catch (Throwable t) {
            return false;
        }
        if (tanks == null || tanks.isEmpty()) return false;
        if (CONSUME_AIR != null && !entity.level().isClientSide()
            && entity.level().getGameTime() % TICKS_PER_AIR == 0) {
            try {
                CONSUME_AIR.invokeExact(entity, tanks.get(0), AIR_PER_SECOND);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
