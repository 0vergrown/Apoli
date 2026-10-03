package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

public final class PhasingPower extends PowerType<PhasingPower.Config> {
    public static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(ApoliIds.PHASING);

    public record Config(
        boolean blacklist,
        Optional<BlockCondition> blockCondition,
        RenderType renderType,
        float viewDistance,
        Optional<EntityCondition> phaseDownCondition
    ) {}

    public enum RenderType implements StringRepresentable {
        BLINDNESS("blindness"),
        REMOVE_BLOCKS("remove_blocks"),
        NONE("none");

        public static final Codec<RenderType> CODEC = StringRepresentable.fromEnum(RenderType::values);

        private final String name;
        RenderType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("blacklist", false).forGetter(Config::blacklist),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            RenderType.CODEC.optionalFieldOf("render_type", RenderType.BLINDNESS).forGetter(Config::renderType),
            Codec.FLOAT.optionalFieldOf("view_distance", 10.0f).forGetter(Config::viewDistance),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("phase_down_condition", EntityCondition.CODEC).forGetter(Config::phaseDownCondition)
        ).apply(i, Config::new));
    }

    public static boolean allowsPhasing(Config cfg, Level level, BlockPos pos, BlockState state) {
        if (cfg.blockCondition.isEmpty()) return true;
        boolean matches = cfg.blockCondition.get().test(new BlockCtx(pos.immutable(), state, level));
        return cfg.blacklist ? !matches : matches;
    }

    public static boolean mayHold(LivingEntity living) {
        return living.level().isClientSide() || HELD.isHeld();
    }

    public static boolean phases(LivingEntity living, Level level, BlockPos pos, BlockState state, VoxelShape shape) {
        PowerContainer container = PowerContainer.of(living);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.PHASING);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        int standingOnTop = -1;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(living, level);
                if (!power.condition().get().test(ctx)) continue;
            }
            if (!allowsPhasing(cfg, level, pos, state)) continue;
            if (standingOnTop < 0) standingOnTop = isStandingOnTop(living, shape, pos) ? 1 : 0;
            if (standingOnTop == 1) {
                if (ctx == null) ctx = EntityCtx.of(living, level);
                boolean down = cfg.phaseDownCondition.isPresent()
                    ? cfg.phaseDownCondition.get().test(ctx)
                    : living.isShiftKeyDown();
                if (!down) continue;
            }
            return true;
        }
        return false;
    }

    public static boolean holdsActive(LivingEntity living) {
        PowerContainer container = PowerContainer.of(living);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.PHASING);
        if (powers.isEmpty()) return false;
        EntityCtx ctx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(living, living.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isStandingOnTop(LivingEntity entity, VoxelShape shape, BlockPos pos) {
        double margin = entity.onGround() ? 8.05 / 16.0 : 0.0015;
        return entity.getY() > pos.getY() + shape.max(Direction.Axis.Y) - margin;
    }
}
