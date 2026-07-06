package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ModifyPlayerModelPower extends PowerType<ModifyPlayerModelPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_player_model");

    public record Config(ResourceLocation model) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(Config::model)
        ).apply(i, Config::new));
    }

    @Nullable
    public static ResourceLocation firstActiveModel(@Nullable Entity entity) {
        ResourceLocation[] out = new ResourceLocation[1];
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (out[0] == null) out[0] = cfg.model();
        });
        return out[0];
    }
}
