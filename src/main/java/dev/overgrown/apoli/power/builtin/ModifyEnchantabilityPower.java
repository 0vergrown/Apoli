package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifyEnchantabilityPower extends PowerType<ModifyEnchantabilityPower.Config> {
    public record Config(
        Optional<ItemCondition> itemCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        List<AttributeModifier> combined
    ) {
        public Config(Optional<ItemCondition> itemCondition, Optional<AttributeModifier> modifier,
                      Optional<List<AttributeModifier>> modifiers) {
            this(itemCondition, modifier, modifiers, AttributeModifierHelper.flatten(modifier, modifiers));
        }
    }

    private static @Nullable Player enchanter;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, (condition, modifier, modifiers) -> new Config(condition, modifier, modifiers)));
    }

    public static void begin(@Nullable Player player) {
        enchanter = player;
    }

    public static void end() {
        enchanter = null;
    }

    public static int modify(ItemStack stack, int enchantability) {
        Player player = enchanter;
        if (player == null || enchantability <= 0 || !PowerLookup.holdsType(player, ApoliIds.MODIFY_ENCHANTABILITY)) {
            return enchantability;
        }
        ItemCtx ctx = new ItemCtx(stack, player.level(), player);
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(player, ApoliIds.MODIFY_ENCHANTABILITY, Config.class, cfg -> {
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(ctx)) return;
            mods.addAll(cfg.combined());
        });
        if (mods.isEmpty()) return enchantability;
        return (int) Math.round(AttributeModifierHelper.apply((double) enchantability,
            AttributeModifierHelper.ensureSorted(mods), player));
    }
}
