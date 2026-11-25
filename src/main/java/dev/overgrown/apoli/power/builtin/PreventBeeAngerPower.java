package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PreventBeeAngerPower extends PowerType<PreventBeeAngerPower.Config> {
    public record Config(
            Optional<BlockCondition> blockCondition,
            Optional<BiEntityCondition> biEntityCondition,
            Optional<BiEntityAction> biEntityAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec( i -> i.group (
                LoggedOptionalField.of("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
                LoggedOptionalField.of("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::biEntityCondition),
                LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::biEntityAction)
        ).apply(i, Config::new));
    }

    public static boolean keepsCalm(LivingEntity target, Bee bee, Level level, BlockPos hivePos,
                                    @Nullable BlockState hiveState, boolean runActions) {
        if (!PowerLookup.holdsType(target, ApoliIds.PREVENT_BEE_ANGER)) return false;
        List<Config> configs = PowerLookup.active(target, ApoliIds.PREVENT_BEE_ANGER, Config.class);
        boolean calm = false;
        BiEntityCtx bi = null;
        BlockCtx block = null;
        for (int i = 0, n = configs.size(); i < n; i++) {
            Config config = configs.get(i);
            if (config.biEntityCondition().isPresent()) {
                if (bi == null) bi = BiEntityCtx.of(target, bee, level);
                if (!config.biEntityCondition().get().test(bi)) continue;
            }
            if (config.blockCondition().isPresent()) {
                if (block == null) {
                    block = new BlockCtx(hivePos, hiveState != null ? hiveState : level.getBlockState(hivePos), level);
                }
                if (!config.blockCondition().get().test(block)) continue;
            }
            calm = true;
            if (runActions && config.biEntityAction().isPresent()) {
                if (bi == null) bi = BiEntityCtx.of(target, bee, level);
                config.biEntityAction().get().run(bi);
            }
        }
        return calm;
    }
}
