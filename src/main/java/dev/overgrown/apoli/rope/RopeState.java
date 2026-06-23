package dev.overgrown.apoli.rope;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class RopeState {

    public final UUID owner;
    public final Vec3 anchor;
    public float maxLength;
    public double length;
    public ResourceLocation texture;
    public int playerFlightTicks = 0;

    public RopeState(Vec3 anchor, Player player, float maxLength) {
        this(anchor, player, maxLength, Apoli.id("textures/rope/rope.png"));
    }

    public RopeState(Vec3 anchor, Player player, float maxLength, ResourceLocation texture) {
        this.anchor = anchor;
        this.owner = player.getUUID();
        this.maxLength = maxLength;
        this.length = player.getBoundingBox().getCenter().distanceTo(anchor);
        this.texture = texture;
    }
}
