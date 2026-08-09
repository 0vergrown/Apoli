package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class RestrictArmorPower extends PowerType<RestrictArmorPower.Config> {
    public record Config(
        Optional<ItemCondition> head,
        Optional<ItemCondition> chest,
        Optional<ItemCondition> legs,
        Optional<ItemCondition> feet
    ) {}

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final int FIRST_ARMOR_CONTAINER_INDEX = 36;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemCondition.CODEC.optionalFieldOf("head").forGetter(Config::head),
            ItemCondition.CODEC.optionalFieldOf("chest").forGetter(Config::chest),
            ItemCondition.CODEC.optionalFieldOf("legs").forGetter(Config::legs),
            ItemCondition.CODEC.optionalFieldOf("feet").forGetter(Config::feet)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        unequip(holder.owner(), cfg);
    }

    @Override
    public void onUnsuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        unequip(holder.owner(), cfg);
    }

    public static boolean blocks(Config cfg, EquipmentSlot slot, ItemCtx ctx) {
        Optional<ItemCondition> rule = switch (slot) {
            case HEAD -> cfg.head;
            case CHEST -> cfg.chest;
            case LEGS -> cfg.legs;
            case FEET -> cfg.feet;
            default -> Optional.empty();
        };
        return rule.isPresent() && rule.get().test(ctx);
    }

    public static boolean restricts(@Nullable LivingEntity entity, @Nullable EquipmentSlot slot, ItemStack stack) {
        if (entity == null || slot == null || stack.isEmpty()) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        if (container.powersOfType(ApoliIds.RESTRICT_ARMOR).isEmpty()) return false;

        ItemCtx ctx = new ItemCtx(stack, entity.level(), entity);
        boolean[] hit = {false};
        PowerLookup.forEach(entity, ApoliIds.RESTRICT_ARMOR, Config.class, cfg -> {
            if (hit[0]) return;
            if (blocks(cfg, slot, ctx)) hit[0] = true;
        });
        return hit[0];
    }

    public static @Nullable EquipmentSlot armorSlotOf(int containerIndex) {
        int offset = containerIndex - FIRST_ARMOR_CONTAINER_INDEX;
        return switch (offset) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> null;
        };
    }

    public static void unequipRestricted(@Nullable LivingEntity entity) {
        unequip(entity, null);
    }

    private static void unequip(@Nullable LivingEntity entity, @Nullable Config cfg) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            boolean restricted = cfg == null
                ? restricts(player, slot, stack)
                : blocks(cfg, slot, new ItemCtx(stack, player.level(), player));
            if (!restricted) continue;
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
