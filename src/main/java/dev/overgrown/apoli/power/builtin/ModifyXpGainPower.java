package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifyXpGainPower extends PowerType<ModifyXpGainPower.Config> {
    public record Config(
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        Optional<BlockCondition> blockCondition,
        List<AttributeModifier> combined
    ) {
        public Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers,
                      Optional<BlockCondition> blockCondition) {
            this(modifier, modifiers, blockCondition, AttributeModifierHelper.flatten(modifier, modifiers));
        }
    }

    private static @Nullable ServerPlayer cook;
    private static @Nullable BlockCtx cookingBlock;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition)
        ).apply(i, (modifier, modifiers, condition) -> new Config(modifier, modifiers, condition)));
    }

    public static int modifyGain(Player player, int amount) {
        if (amount <= 0 || !PowerLookup.holdsType(player, ApoliIds.MODIFY_XP_GAIN)) return amount;
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(player, ApoliIds.MODIFY_XP_GAIN, Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty()) mods.addAll(cfg.combined());
        });
        if (mods.isEmpty()) return amount;
        double value = AttributeModifierHelper.apply((double) amount, AttributeModifierHelper.ensureSorted(mods), player);
        if (value <= 0.0D) return 0;
        int whole = (int) value;
        return player.getRandom().nextDouble() < value - whole ? whole + 1 : whole;
    }

    public static void beginCooking(ServerPlayer player, BlockEntity furnace) {
        if (furnace.getLevel() == null || !PowerLookup.holdsType(player, ApoliIds.MODIFY_XP_GAIN)) return;
        cook = player;
        cookingBlock = new BlockCtx(furnace.getBlockPos(), furnace.getBlockState(), furnace.getLevel(), player);
    }

    public static void endCooking() {
        cook = null;
        cookingBlock = null;
    }

    public static float modifyCookingXp(float xp) {
        ServerPlayer player = cook;
        BlockCtx block = cookingBlock;
        if (player == null || block == null) return xp;
        List<AttributeModifier> mods = new ArrayList<>(2);
        PowerLookup.forEach(player, ApoliIds.MODIFY_XP_GAIN, Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && cfg.blockCondition().get().test(block)) mods.addAll(cfg.combined());
        });
        if (mods.isEmpty()) return xp;
        return (float) Math.max(0.0D, AttributeModifierHelper.apply((double) xp, AttributeModifierHelper.ensureSorted(mods), player));
    }
}
