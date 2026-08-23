package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.function.Function;

public final class ActionOnCallbackPower extends PowerType<ActionOnCallbackPower.Config> {
    public record Config(
        Optional<EntityAction> entityActionGained,
        Optional<EntityAction> entityActionLost,
        Optional<EntityAction> entityActionAdded,
        Optional<EntityAction> entityActionRemoved,
        Optional<EntityAction> entityActionRespawned,
        Optional<EntityAction> entityActionChosen,
        boolean executeChosenWhenOrb
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_gained", EntityAction.CODEC).forGetter(Config::entityActionGained),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_lost", EntityAction.CODEC).forGetter(Config::entityActionLost),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_added", EntityAction.CODEC).forGetter(Config::entityActionAdded),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_removed", EntityAction.CODEC).forGetter(Config::entityActionRemoved),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_respawned", EntityAction.CODEC).forGetter(Config::entityActionRespawned),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_chosen", EntityAction.CODEC).forGetter(Config::entityActionChosen),
            Codec.BOOL.optionalFieldOf("execute_chosen_when_orb", true).forGetter(Config::executeChosenWhenOrb)
        ).apply(i, Config::new));
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.rawOwner().level() instanceof ServerLevel level)) return;
        EntityCtx ctx = new EntityCtx(holder.rawOwner(), level);
        cfg.entityActionGained().ifPresent(a -> a.run(ctx));
        cfg.entityActionAdded().ifPresent(a -> a.run(ctx));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.rawOwner().level() instanceof ServerLevel level)) return;
        EntityCtx ctx = new EntityCtx(holder.rawOwner(), level);
        cfg.entityActionLost().ifPresent(a -> a.run(ctx));
        cfg.entityActionRemoved().ifPresent(a -> a.run(ctx));
    }

    public static void fireLifecycle(Entity entity, Function<Config, Optional<EntityAction>> selector) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null) return;
        EntityCtx ctx = EntityCtx.of(entity, level);
        ResourceLocation canonical = Apoli.id("action_on_callback");
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!canonical.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof Config cfg)) continue;
            selector.apply(cfg).ifPresent(a -> a.run(ctx));
        }
    }

    public static void fireRespawn(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) {
            fireRespawnNow(player);
            return;
        }
        server.execute(() -> fireRespawnNow(player));
    }

    private static void fireRespawnNow(ServerPlayer player) {
        if (player.isRemoved() || player.hasDisconnected()) return;
        fireLifecycle(player, Config::entityActionAdded);
        fireLifecycle(player, Config::entityActionRespawned);
    }

    public static void fireChosen(LivingEntity entity, boolean fromOrb) {
        if (entity == null) return;
        EntityCtx ctx = new EntityCtx(entity, entity.level());
        PowerLookup.forEach(entity, Apoli.id("action_on_callback"), Config.class, cfg -> {
            if (fromOrb && !cfg.executeChosenWhenOrb()) return;
            cfg.entityActionChosen().ifPresent(action -> action.run(ctx));
        });
    }
}
