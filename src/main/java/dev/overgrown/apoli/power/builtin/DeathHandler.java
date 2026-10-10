package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class DeathHandler {
    private DeathHandler() {}

    public static void onDeath(LivingEntity dead, DamageSource source) {
        if (!(dead.level() instanceof ServerLevel level)) return;
        @Nullable LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;
        if (PowerContainer.of(dead) instanceof PowerContainerImpl impl) fireDeathPowers(dead, killer, source, level, impl);
        if (killer != null) fireActionOnKill(killer, dead, source, level);
    }

    private static void fireDeathPowers(LivingEntity dead, @Nullable LivingEntity killer, DamageSource source,
                                        ServerLevel level, PowerContainerImpl impl) {
        for (ResourceLocation powerId : impl.allPowers()) {
            Power loaded = ApoliPowers.get(powerId);
            if (loaded == null) continue;
            if (impl.isSuppressed(powerId)) continue;
            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());

            if (type instanceof ActionOnDeathPower aod
                && loaded.config() instanceof ActionOnDeathPower.Config cfg) {
                if (loaded.condition().isPresent()
                    && !loaded.condition().get().test(new EntityCtx(dead, level))) continue;
                aod.onDeath(cfg, dead, killer, source, level);
            } else if (type instanceof PowerStoragePower
                && loaded.config() instanceof PowerStoragePower.Config cfg
                && cfg.dropOnDeath()) {
                PowerStoragePower.clear(impl, powerId);
            }
        }
    }

    private static void fireActionOnKill(LivingEntity killer, LivingEntity victim, DamageSource source, ServerLevel level) {
        if (!(PowerContainer.of(killer) instanceof PowerContainerImpl impl)) return;
        List<ResourceLocation> powers = impl.powersOfType(ApoliIds.ACTION_ON_KILL);
        if (powers.isEmpty()) return;
        EntityCtx killerCtx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (impl.isSuppressed(powerId)) continue;
            Power loaded = ApoliPowers.get(powerId);
            if (loaded == null || !(loaded.config() instanceof ActionOnKillPower.Config cfg)) continue;
            if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof ActionOnKillPower aok)) continue;
            if (loaded.condition().isPresent()) {
                if (killerCtx == null) killerCtx = new EntityCtx(killer, level);
                if (!loaded.condition().get().test(killerCtx)) continue;
            }
            aok.onKill(powerId, cfg, killer, victim, source, level, impl);
        }
    }

    public static void dropPowerInventories(LivingEntity dead, Collection<ItemEntity> drops, boolean dropsCancelled) {
        List<InventoryPower.DeathDrop> taken = InventoryPower.deathDrops(dead, true);
        for (int i = 0; i < taken.size(); i++) {
            ItemStack stack = taken.get(i).stack();
            if (dropsCancelled) {
                dead.spawnAtLocation(stack);
                continue;
            }
            ItemEntity item = new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), stack);
            item.setDefaultPickUpDelay();
            drops.add(item);
        }
    }
}
