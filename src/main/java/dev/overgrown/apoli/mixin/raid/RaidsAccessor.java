package dev.overgrown.apoli.mixin.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(Raids.class)
public interface RaidsAccessor {
    @Accessor("raidMap")
    Map<Integer, Raid> apoli$raidMap();

    @Invoker("getOrCreateRaid")
    Raid apoli$getOrCreateRaid(ServerLevel level, BlockPos pos);
}
