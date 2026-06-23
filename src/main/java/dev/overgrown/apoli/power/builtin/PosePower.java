package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class PosePower extends PowerType<PosePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("pose");

    public record Config(Optional<Pose> entityPose, Optional<ArmPoseReference> armPose, int priority) {
        public boolean hasPose() {
            return entityPose.isPresent() || armPose.isPresent();
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityPoses.CODEC.optionalFieldOf("entity_pose").forGetter(Config::entityPose),
            ArmPoseReference.CODEC.optionalFieldOf("arm_pose").forGetter(Config::armPose),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    @Nullable
    public static Config chooseBest(Entity entity) {
        Config[] best = new Config[1];
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (!cfg.hasPose()) return;
            if (best[0] == null || cfg.priority() > best[0].priority()) {
                best[0] = cfg;
            }
        });
        return best[0];
    }

    public static boolean hasEntityPose(Entity entity, Pose pose) {
        return entity instanceof ModifiedPoseHolder holder
            && pose.equals(holder.apoli$getModifiedEntityPose());
    }
}
