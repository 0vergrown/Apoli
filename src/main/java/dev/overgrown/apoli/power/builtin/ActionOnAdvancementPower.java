package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ActionOnAdvancementPower extends PowerType<ActionOnAdvancementPower.Config> {

    public record Config (
            EntityAction entityAction,
            Optional<ResourceLocation> advancement,
            Optional<ResourceLocation> category
    ) {}

    @Override
    public MapCodec<ActionOnAdvancementPower.Config> configCodec() {
        return RecordCodecBuilder.mapCodec( i -> i.group(
                EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction),
                ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(Config::advancement),
                ResourceLocation.CODEC.optionalFieldOf("category_root").forGetter(Config::category)
        ).apply(i, Config::new));
    }
}
