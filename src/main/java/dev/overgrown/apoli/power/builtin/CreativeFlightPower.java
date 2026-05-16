package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerContainer;
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
        applyFlight(holder.owner(), true);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.owner() instanceof Player p && !p.getAbilities().instabuild && !p.isSpectator()) {
            applyFlight(p, false);
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner instanceof Player p) {
            Abilities abilities = p.getAbilities();
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                p.onUpdateAbilities();
            }
        }
    }

    private static void applyFlight(LivingEntity entity, boolean enabled) {
        if (!(entity instanceof Player p)) return;
        Abilities abilities = p.getAbilities();
        abilities.mayfly = enabled;
        if (!enabled) abilities.flying = false;
        p.onUpdateAbilities();
    }
}
