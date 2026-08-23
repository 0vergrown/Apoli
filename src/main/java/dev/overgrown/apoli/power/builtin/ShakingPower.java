package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class ShakingPower extends PowerType<EmptyCfg> {
    public static final ResourceLocation CANONICAL = Apoli.id("shaking");

    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean isShaking(LivingEntity entity) {
        return ApoliPowers.anyOfType(CANONICAL) && PowerLookup.hasActive(entity, CANONICAL);
    }
}
