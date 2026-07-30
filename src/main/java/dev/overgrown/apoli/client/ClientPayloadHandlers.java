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

    public static void onSyncPowersChunk(dev.overgrown.apoli.network.payload.SyncPowersChunkS2C msg) {
        ClientPowerState.applyPowersChunk(msg);
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

    public static void onDisguiseUpdate(dev.overgrown.apoli.network.payload.DisguiseUpdateS2C msg) {
        msg.data().ifPresentOrElse(
            data -> dev.overgrown.apoli.client.disguise.ClientDisguiseManager.apply(msg.entityId(), data),
            () -> dev.overgrown.apoli.client.disguise.ClientDisguiseManager.remove(msg.entityId()));
    }

    public static void onTextDisplay(dev.overgrown.apoli.network.payload.TextDisplayS2C msg) {
        TextOverlayRenderer.apply(msg);
    }

    public static void onLabelUpdate(dev.overgrown.apoli.network.payload.LabelUpdateS2C msg) {
        ClientLabelState.apply(msg.entityId(), msg.texts());
    }

    public static void onForceKey(dev.overgrown.apoli.network.payload.ForceKeyS2C msg) {
        ForcedKeys.force(msg.key(), msg.duration(), msg.release());
    }

    public static void onSkillDefs(dev.overgrown.apoli.network.payload.SkillDefsSyncS2C msg) {
        dev.overgrown.apoli.client.skill.ClientSkillState.applyDefs(msg);
    }

    public static void onSkillState(dev.overgrown.apoli.network.payload.SkillStateSyncS2C msg) {
        dev.overgrown.apoli.client.skill.ClientSkillState.applyState(msg);
    }

    public static void onRadialMenuOpen(dev.overgrown.apoli.network.payload.RadialMenuOpenS2C msg) {
        Minecraft.getInstance().setScreen(new dev.overgrown.apoli.client.radial.RadialMenuScreen(msg));
    }

    public static void onRopeCreate(RopeCreateS2C msg) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) RopeClientManager.attach(msg, level);
    }

    public static void onRopeDelete(RopeDeleteS2C msg) {
        RopeClientManager.detach(msg.id());
    }

    public static void onRopeVerletLength(RopeVerletLengthS2C msg) {
        VerletRopeState rope = RopeClientManager.get(msg.id());
        if (rope != null) rope.targetLength = msg.length();
    }
}
