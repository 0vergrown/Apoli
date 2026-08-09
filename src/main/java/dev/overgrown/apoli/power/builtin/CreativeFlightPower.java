package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

public final class CreativeFlightPower extends PowerType<CreativeFlightPower.Config> {
    public record Config() {
        public static final Config INSTANCE = new Config();
    }

    @Override
    public MapCodec<Config> configCodec() {
        return MapCodec.unit(Config.INSTANCE);
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        sync(holder.owner());
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        sync(holder.owner());
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(holder.owner());
    }

    @Override
    public void onUnsuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(holder.owner());
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        sync(holder.owner());
    }

    private static void sync(LivingEntity owner) {
        if (!(owner instanceof Player player)) return;
        Abilities abilities = player.getAbilities();
        if (PowerLookup.hasActive(player, ApoliIds.CREATIVE_FLIGHT)) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            return;
        }
        if (!abilities.mayfly || abilities.instabuild || player.isSpectator()) return;
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
    }
}
