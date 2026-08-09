package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.FoodComponent;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EdibleItemHandler {

    private EdibleItemHandler() {}

    private static final class Slot {
        private @Nullable Item item;
        private @Nullable EdibleItemPower.Config config;
    }

    private static final ThreadLocal<Slot> ANIMATION_SLOT = ThreadLocal.withInitial(Slot::new);

    public static @Nullable EdibleItemPower.Config find(@Nullable LivingEntity entity, ItemStack stack) {
        if (entity == null || stack.isEmpty()) return null;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.EDIBLE_ITEM);
        if (powers.isEmpty()) return null;
        Level level = entity.level();
        EntityCtx entityCtx = null;
        ItemCtx itemCtx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof EdibleItemPower.Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (entityCtx == null) entityCtx = EntityCtx.of(entity, level);
                if (!power.condition().get().test(entityCtx)) continue;
            }
            if (cfg.itemCondition().isPresent()) {
                if (itemCtx == null) itemCtx = new ItemCtx(stack, level, entity);
                if (!cfg.itemCondition().get().test(itemCtx)) continue;
            }
            arm(stack, cfg);
            return cfg;
        }
        return null;
    }

    public static boolean canConsume(Player player, EdibleItemPower.Config cfg) {
        return player.canEat(cfg.foodComponent().alwaysEdible());
    }

    public static int useDuration(LivingEntity entity, EdibleItemPower.Config cfg) {
        double ticks = cfg.foodComponent().snack() ? 16.0 : 32.0;
        if (cfg.consumingTimeModifier().isEmpty() && cfg.consumingTimeModifiers().isEmpty()) {
            return (int) ticks;
        }
        PowerContainer container = PowerContainer.of(entity);
        if (cfg.consumingTimeModifier().isPresent()) {
            ticks = cfg.consumingTimeModifier().get().applyToValue(ticks, entity, container);
        }
        if (cfg.consumingTimeModifiers().isPresent()) {
            List<AttributeModifier> modifiers = cfg.consumingTimeModifiers().get();
            for (int i = 0; i < modifiers.size(); i++) {
                ticks = modifiers.get(i).applyToValue(ticks, entity, container);
            }
        }
        return Math.max(1, (int) Math.round(ticks));
    }

    public static @Nullable UseAnim animationFor(ItemStack stack) {
        Slot slot = ANIMATION_SLOT.get();
        if (slot.config == null || slot.item != stack.getItem()) return null;
        return slot.config.consumeAnimation().vanilla();
    }

    public static ItemStack finish(Level level, LivingEntity entity, ItemStack stack, EdibleItemPower.Config cfg) {
        disarm(stack);
        boolean server = !level.isClientSide();
        FoodComponent food = cfg.foodComponent();

        playConsumeSound(level, entity, cfg);

        if (entity instanceof Player player) {
            FoodProperties properties = food.build(entity);
            player.getFoodData().eat(properties);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
        }

        if (server) {
            List<Pair<MobEffectInstance, Float>> effects = food.allEffectsWithProbability(entity);
            for (int i = 0; i < effects.size(); i++) {
                Pair<MobEffectInstance, Float> effect = effects.get(i);
                if (level.getRandom().nextFloat() < effect.getSecond()) {
                    entity.addEffect(new MobEffectInstance(effect.getFirst()));
                }
            }
            cfg.itemAction().ifPresent(action -> action.run(new ItemCtx(stack, level, entity)));
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(entity, level)));
        }

        stack.consume(1, entity);
        entity.gameEvent(GameEvent.EAT);

        if (cfg.resultStack().isEmpty()) return stack;
        ItemStack result = cfg.resultStack().get().stack().copy();
        if (result.isEmpty()) return stack;
        if (server) {
            cfg.resultItemAction().ifPresent(action -> action.run(new ItemCtx(result, level, entity)));
        }
        if (stack.isEmpty()) return result;
        if (server) giveResult(entity, result);
        return stack;
    }

    private static void giveResult(LivingEntity entity, ItemStack result) {
        if (entity instanceof Player player) {
            if (!player.getInventory().add(result)) player.drop(result, false);
        } else {
            entity.spawnAtLocation(result);
        }
    }

    private static void playConsumeSound(Level level, LivingEntity entity, EdibleItemPower.Config cfg) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(cfg.consumeSound());
        if (sound == null) return;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.NEUTRAL,
            1.0F, 1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F);
    }

    private static void arm(ItemStack stack, EdibleItemPower.Config cfg) {
        Slot slot = ANIMATION_SLOT.get();
        slot.item = stack.getItem();
        slot.config = cfg;
    }

    private static void disarm(ItemStack stack) {
        Slot slot = ANIMATION_SLOT.get();
        if (slot.item != stack.getItem()) return;
        slot.item = null;
        slot.config = null;
    }
}
