package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class PosePower extends PowerType<PosePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("pose");
    private static final PowerTypeGate IN_USE = new PowerTypeGate(CANONICAL);

    public record Config(Optional<Pose> entityPose, Optional<ArmPoseReference> armPose, List<Hand> hands, int priority) {
        public boolean hasPose() {
            return entityPose.isPresent() || armPose.isPresent();
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityPoses.CODEC.optionalFieldOf("entity_pose").forGetter(Config::entityPose),
            ArmPoseReference.CODEC.optionalFieldOf("arm_pose").forGetter(Config::armPose),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    @Nullable
    public static Config chooseBest(Entity entity) {
        if (!IN_USE.inUse()) return null;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        Config best = null;
        EntityCtx ctx = null;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg) || !cfg.hasPose()) continue;
            if (best != null && cfg.priority() <= best.priority()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            best = cfg;
        }
        return best;
    }

    public static boolean hasEntityPose(Entity entity, Pose pose) {
        return entity instanceof ModifiedPoseHolder holder
            && pose.equals(holder.apoli$getModifiedEntityPose());
    }
}
