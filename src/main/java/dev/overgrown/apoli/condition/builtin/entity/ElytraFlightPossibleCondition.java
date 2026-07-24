package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;

public final class ElytraFlightPossibleCondition implements ConditionType<EntityCtx, ElytraFlightPossibleCondition.Cfg> {
    public record Cfg(boolean checkState, boolean checkAbilities) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("check_state", false).forGetter(Cfg::checkState),
            Codec.BOOL.optionalFieldOf("check_abilities", true).forGetter(Cfg::checkAbilities)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null) return false;
        if (!cfg.checkState && !cfg.checkAbilities) return true;
        if (cfg.checkState) {
            if (e.onGround() || e.isFallFlying() || e.isInWater() || e.isPassenger()) return false;
        }
        if (cfg.checkAbilities) {
            ItemStack chest = e.getItemBySlot(EquipmentSlot.CHEST);
            boolean elytraItem = chest.getItem() instanceof ElytraItem && ElytraItem.isFlyEnabled(chest);
            boolean elytraPower = PowerLookup.hasActive(e, ApoliIds.ELYTRA_FLIGHT);
            if (!elytraItem && !elytraPower) return false;
        }
        return true;
    }
}
