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

    public static final int MAX_GHOST_ARMS = 16;

    public record Config(boolean mainHand, boolean offHand, int ghostArms, float ghostSpacing, float ghostAlpha) {}

    public static final class Arms {
        public int mask;
        public int ghosts;
        public float spacing = 0.12F;
        public float alpha = 0.35F;

        private void clear() {
            this.mask = 0;
            this.ghosts = 0;
            this.spacing = 0.12F;
            this.alpha = 0.35F;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("main_hand", false).forGetter(Config::mainHand),
            Codec.BOOL.optionalFieldOf("off_hand", true).forGetter(Config::offHand),
            Codec.intRange(0, MAX_GHOST_ARMS).optionalFieldOf("ghost_arms", 0).forGetter(Config::ghostArms),
            Codec.FLOAT.optionalFieldOf("ghost_spacing", 0.12F).forGetter(Config::ghostSpacing),
            Codec.FLOAT.optionalFieldOf("ghost_alpha", 0.35F).forGetter(Config::ghostAlpha)
        ).apply(i, Config::new));
    }

    public static boolean resolve(@Nullable Entity entity, Arms out) {
        out.clear();
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.SHOW_BOTH_ARMS);
        if (powers.isEmpty()) return false;
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
            if (cfg.mainHand) out.mask |= MAIN;
            if (cfg.offHand) out.mask |= OFF;
            if (cfg.ghostArms > out.ghosts) {
                out.ghosts = cfg.ghostArms;
                out.spacing = cfg.ghostSpacing;
                out.alpha = cfg.ghostAlpha;
            }
        }
        return out.mask != 0 || out.ghosts > 0;
    }
}
