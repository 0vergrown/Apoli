package dev.overgrown.apoli.client;

import dev.overgrown.apoli.client.rope.RopeClientManager;
import dev.overgrown.apoli.client.rope.VerletRopeState;
import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void onSyncPowers(SyncPowersS2C msg) {
        ClientPowerState.applyPowersSync(msg);
    }

    public static void onSyncEntityPowers(SyncEntityPowersS2C msg) {
        ClientPowerState.applyEntityPowersSync(msg);
    }

    public static void onPowerActivated(PowerActivatedS2C msg) {
        ClientPowerState.setCooldown(msg.power(), msg.cooldown());
    }

    public static void onSyncKeybinds(SyncKeybindsS2C msg) {
        DynamicKeyMappingManager.applyKeybinds(msg.keybinds());
    }

    public static void onApplyVelocity(ApplyVelocityS2C msg) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        net.minecraft.world.entity.Entity e = level.getEntity(msg.entityId());
        if (e == null) return;
        net.minecraft.world.phys.Vec3 delta = new net.minecraft.world.phys.Vec3(msg.x(), msg.y(), msg.z());
        e.setDeltaMovement(msg.set() ? delta : e.getDeltaMovement().add(delta));
    }

    public static void onRopeCreate(RopeCreateS2C msg) {
        RopeClientManager.attach(msg.owner(), msg.anchor(), msg.length(), msg.maxLength(), msg.texture());
    }

    public static void onRopeDelete(RopeDeleteS2C msg) {
        RopeClientManager.detach(msg.owner());
    }

    public static void onRopeVerletLength(RopeVerletLengthS2C msg) {
        VerletRopeState rope = RopeClientManager.get(msg.owner());
        if (rope != null) rope.targetLength = msg.length();
    }
}
