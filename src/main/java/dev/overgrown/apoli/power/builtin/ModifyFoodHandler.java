package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ModifyFoodHandler {

    private ModifyFoodHandler() {}

    public record Values(int nutrition, float saturation, boolean canAlwaysEat, boolean preventEffects,
                         @Nullable ItemStack replaceStack) {}

    private static @Nullable PowerContainer candidates(@Nullable LivingEntity entity, ItemStack stack) {
        if (entity == null || stack.isEmpty()) return null;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return null;
        return container.powersOfType(ApoliIds.MODIFY_FOOD).isEmpty() ? null : container;
    }

    private static void forEachApplicable(LivingEntity entity, ItemStack stack, PowerContainer container,
                                          Consumer<ModifyFoodPower.Config> consumer) {
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_FOOD);
        Level level = entity.level();
        EntityCtx entityCtx = null;
        ItemCtx itemCtx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyFoodPower.Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (entityCtx == null) entityCtx = EntityCtx.of(entity, level);
                if (!power.condition().get().test(entityCtx)) continue;
            }
            if (cfg.itemCondition().isPresent()) {
                if (itemCtx == null) itemCtx = new ItemCtx(stack, level, entity);
                if (!cfg.itemCondition().get().test(itemCtx)) continue;
            }
            consumer.accept(cfg);
        }
    }

    public static @Nullable Values values(LivingEntity entity, ItemStack stack, int nutrition, float saturation) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return null;

        Accumulator acc = new Accumulator(nutrition, saturation);
        forEachApplicable(entity, stack, container, cfg -> {
            acc.nutrition = apply(cfg.foodModifiers(), acc.nutrition, entity, container);
            acc.saturation = apply(cfg.saturationModifiers(), acc.saturation, entity, container);
            if (cfg.alwaysEdible()) acc.canAlwaysEat = true;
            if (cfg.preventEffects()) acc.preventEffects = true;
            cfg.replaceStack().ifPresent(data -> acc.replaceStack = data.stack().copy());
            acc.touched = true;
        });
        if (!acc.touched) return null;
        return new Values(Math.max(0, (int) acc.nutrition), (float) acc.saturation,
            acc.canAlwaysEat, acc.preventEffects, acc.replaceStack);
    }

    public static double eatTicks(@Nullable LivingEntity entity, ItemStack stack, double base) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return base;
        double[] ticks = {base};
        forEachApplicable(entity, stack, container,
            cfg -> ticks[0] = apply(cfg.eatTicksModifiers(), ticks[0], entity, container));
        return ticks[0];
    }

    public static boolean alwaysEdible(@Nullable LivingEntity entity, ItemStack stack) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return false;
        boolean[] found = {false};
        forEachApplicable(entity, stack, container, cfg -> {
            if (cfg.alwaysEdible()) found[0] = true;
        });
        return found[0];
    }

    public static boolean preventsEffects(@Nullable LivingEntity entity, ItemStack stack) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return false;
        boolean[] found = {false};
        forEachApplicable(entity, stack, container, cfg -> {
            if (cfg.preventEffects()) found[0] = true;
        });
        return found[0];
    }

    public static @Nullable ItemStack replacement(@Nullable LivingEntity entity, ItemStack stack) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return null;
        ItemStack[] result = {null};
        forEachApplicable(entity, stack, container,
            cfg -> cfg.replaceStack().ifPresent(data -> result[0] = data.stack().copy()));
        return result[0];
    }

    public static void afterEat(LivingEntity entity, ItemStack stack) {
        Level level = entity.level();
        if (level.isClientSide()) return;
        PowerContainer container = candidates(entity, stack);
        if (container == null) return;
        forEachApplicable(entity, stack, container, cfg -> {
            cfg.itemAction().ifPresent(action -> action.run(new ItemCtx(stack, level, entity)));
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(entity, level)));
        });
    }

    private static double apply(Optional<List<AttributeModifier>> modifiers, double value,
                                @Nullable LivingEntity entity, @Nullable PowerContainer container) {
        if (modifiers.isEmpty()) return value;
        List<AttributeModifier> list = modifiers.get();
        for (int i = 0; i < list.size(); i++) {
            value = list.get(i).applyToValue(value, entity, container);
        }
        return value;
    }

    private static final class Accumulator {
        double nutrition;
        double saturation;
        boolean canAlwaysEat;
        boolean preventEffects;
        @Nullable ItemStack replaceStack;
        boolean touched;

        Accumulator(int nutrition, float saturation) {
            this.nutrition = nutrition;
            this.saturation = saturation;
        }
    }
}
