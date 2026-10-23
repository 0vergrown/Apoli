package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
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
import java.util.Map;
import java.util.Optional;

public final class ModifyAnvilPower extends PowerType<ModifyAnvilPower.Config> {
    public record Config(
        Optional<ItemCondition> itemCondition,
        List<AttributeModifier> repairMaterialModifiers,
        List<AttributeModifier> combineBonusModifiers
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.<Config>mapCodec(i -> i.group(
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("repair_material_modifier", List.of())
                .forGetter(Config::repairMaterialModifiers),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("combine_bonus_modifier", List.of())
                .forGetter(Config::combineBonusModifiers)
        ).apply(i, Config::new)), Map.of(
            "repair_material_modifiers", "repair_material_modifier",
            "combine_bonus_modifiers", "combine_bonus_modifier"));
    }

    public static int repairMaterial(@Nullable Player player, ItemStack stack, int original) {
        List<AttributeModifier> mods = collect(player, stack, true);
        if (mods.isEmpty()) return original;
        return Math.max(1, (int) Math.round(AttributeModifierHelper.apply((double) original, mods, player)));
    }

    public static int combineBonus(@Nullable Player player, ItemStack stack, int original) {
        List<AttributeModifier> mods = collect(player, stack, false);
        if (mods.isEmpty()) return original;
        return Math.max(0, (int) Math.round(AttributeModifierHelper.apply((double) original, mods, player)));
    }

    private static List<AttributeModifier> collect(@Nullable Player player, ItemStack stack, boolean material) {
        if (player == null || stack.isEmpty() || !PowerLookup.holdsType(player, ApoliIds.MODIFY_ANVIL)) return List.of();
        ItemCtx ctx = new ItemCtx(stack, player.level(), player);
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(player, ApoliIds.MODIFY_ANVIL, Config.class, cfg -> {
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(ctx)) return;
            mods.addAll(material ? cfg.repairMaterialModifiers() : cfg.combineBonusModifiers());
        });
        return AttributeModifierHelper.ensureSorted(mods);
    }
}
