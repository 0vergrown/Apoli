package dev.overgrown.apoli.mixin.hud;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiStatusHelperAccessor {

    @Invoker("getCameraPlayer")
    @Nullable
    Player apoli$cameraPlayer();

    @Invoker("getPlayerVehicleWithHealth")
    @Nullable
    LivingEntity apoli$vehicleWithHealth();

    @Invoker("getVehicleMaxHearts")
    int apoli$vehicleMaxHearts(@Nullable LivingEntity vehicle);

    @Invoker("getVisibleVehicleHeartRows")
    int apoli$vehicleHeartRows(int hearts);
}
