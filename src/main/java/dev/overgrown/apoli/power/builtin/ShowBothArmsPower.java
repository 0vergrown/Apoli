package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ShowBothArmsPower extends PowerType<ShowBothArmsPower.Config> {

    public static final int MAIN = 1;
    public static final int OFF = 2;

    public record Config(boolean mainHand, boolean offHand) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("main_hand", false).forGetter(Config::mainHand),
            Codec.BOOL.optionalFieldOf("off_hand", true).forGetter(Config::offHand)
        ).apply(i, Config::new));
    }

    public static int armMask(@Nullable Entity entity) {
        if (entity == null) return 0;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.SHOW_BOTH_ARMS);
        if (powers.isEmpty()) return 0;
        int mask = 0;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (cfg.mainHand) mask |= MAIN;
            if (cfg.offHand) mask |= OFF;
            if (mask == (MAIN | OFF)) break;
        }
        return mask;
    }
}
