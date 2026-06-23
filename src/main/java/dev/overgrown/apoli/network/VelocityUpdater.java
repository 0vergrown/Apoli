package dev.overgrown.apoli.network;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class VelocityUpdater {
    private VelocityUpdater() {}

    public static void apply(Entity recipient, Vec3 worldDelta, boolean set) {
        if (!(recipient instanceof Player)) {
            Vec3 current = recipient.getDeltaMovement();
            recipient.setDeltaMovement(set ? worldDelta : current.add(worldDelta));
        }
        ApoliNetwork.sendApplyVelocityToTrackers(recipient,
            new ApplyVelocityS2C(recipient.getId(), worldDelta.x, worldDelta.y, worldDelta.z, set));
    }
}
