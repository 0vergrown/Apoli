package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class ActionOnLandPower extends PowerType<ActionOnLandPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("action_on_land");

    public record Config(EntityAction entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    public static void onLand(LivingEntity entity) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        if (container.powersOfType(CANONICAL).isEmpty()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        EntityCtx ctx = new EntityCtx(entity, level);
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> cfg.entityAction().run(ctx));
    }
}
