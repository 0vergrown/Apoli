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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ModifyFoodHandler {

    private ModifyFoodHandler() {}

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

    public static FoodProperties modify(LivingEntity entity, ItemStack stack, FoodProperties properties) {
        PowerContainer container = candidates(entity, stack);
        if (container == null) return properties;

        Accumulator acc = new Accumulator(properties);
        forEachApplicable(entity, stack, container, cfg -> {
            acc.nutrition = apply(cfg.foodModifiers(), acc.nutrition, entity, container);
            acc.saturation = apply(cfg.saturationModifiers(), acc.saturation, entity, container);
            if (cfg.alwaysEdible()) acc.canAlwaysEat = true;
            if (cfg.preventEffects()) acc.preventEffects = true;
            cfg.replaceStack().ifPresent(data -> acc.convertsTo = Optional.of(data.stack().copy()));
            acc.touched = true;
        });
        if (!acc.touched) return properties;

        return new FoodProperties(
            Math.max(0, (int) acc.nutrition),
            (float) acc.saturation,
            acc.canAlwaysEat,
            properties.eatSeconds(),
            acc.convertsTo,
            acc.preventEffects ? List.of() : properties.effects());
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
        Optional<ItemStack> convertsTo;
        boolean touched;

        Accumulator(FoodProperties properties) {
            this.nutrition = properties.nutrition();
            this.saturation = properties.saturation();
            this.canAlwaysEat = properties.canAlwaysEat();
            this.convertsTo = properties.usingConvertsTo();
        }
    }
}
