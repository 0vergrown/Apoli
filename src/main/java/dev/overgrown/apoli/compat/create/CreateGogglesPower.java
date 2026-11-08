package dev.overgrown.apoli.compat.create;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class CreateGogglesPower extends PowerType<EmptyCfg> {
    public static final ResourceLocation CANONICAL = Apoli.id("create_goggles");
    private static final PowerTypeGate IN_USE = new PowerTypeGate(CANONICAL);

    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean grants(Player player) {
        return IN_USE.inUse() && PowerLookup.hasActive(player, CANONICAL);
    }
}
