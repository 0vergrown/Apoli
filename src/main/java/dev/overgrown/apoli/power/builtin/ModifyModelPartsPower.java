package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ModifyModelPartsPower extends PowerType<ModifyModelPartsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_model_parts");

    public record Config(List<ModelPartTransformation> transformations, List<Pose> overridePose, int overridePoseMask) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ModelPartTransformation.CODEC.listOf().fieldOf("transformations").forGetter(Config::transformations),
            EntityPoses.CODEC.listOf().optionalFieldOf("override_pose", List.of()).forGetter(Config::overridePose)
        ).apply(instance, (transformations, poses) -> new Config(transformations, poses, maskOf(poses))));
    }

    public static int maskOf(List<Pose> poses) {
        int mask = 0;
        for (int i = 0; i < poses.size(); i++) mask |= 1 << poses.get(i).ordinal();
        return mask;
    }

    public static boolean masked(int mask, Pose pose) {
        return (mask & (1 << pose.ordinal())) != 0;
    }

    public static boolean has(@Nullable LivingEntity entity) {
        return PowerLookup.hasActive(entity, CANONICAL);
    }
}
