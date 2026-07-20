package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModifyModelPartsPower extends PowerType<ModifyModelPartsPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_model_parts");

    public record Config(List<ModelPartTransformation> transformations) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ModelPartTransformation.CODEC.listOf().fieldOf("transformations").forGetter(Config::transformations)
        ).apply(instance, Config::new));
    }

    public static boolean has(@Nullable LivingEntity entity) {
        return PowerLookup.hasActive(entity, CANONICAL);
    }

    public static List<ModelPartTransformation> gather(@Nullable LivingEntity entity) {
        List<ModelPartTransformation> all = new ArrayList<>();
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> all.addAll(cfg.transformations()));
        return all;
    }
}
