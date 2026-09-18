package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PreventElytraFlightPower extends PowerType<PreventElytraFlightPower.Config> {
    public record Config(Optional<EntityAction> entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner instanceof Player player) || !player.isFallFlying()) return;
        if (!conditionHolds(player, powerId)) return;
        player.stopFallFlying();
        cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(player, player.level())));
    }

    public static boolean isPrevented(Entity entity) {
        return PowerLookup.hasActive(entity, ApoliIds.PREVENT_ELYTRA_FLIGHT);
    }

    public static boolean blockAndReact(Entity entity) {
        boolean[] blocked = new boolean[1];
        PowerLookup.forEach(entity, ApoliIds.PREVENT_ELYTRA_FLIGHT, Config.class, cfg -> {
            blocked[0] = true;
            cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(entity, entity.level())));
        });
        return blocked[0];
    }

    private static boolean conditionHolds(LivingEntity entity, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;
        return loaded.condition().get().test(EntityCtx.of(entity, level));
    }
}
