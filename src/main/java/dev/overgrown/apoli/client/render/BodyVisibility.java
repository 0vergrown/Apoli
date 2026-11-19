package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class BodyVisibility {
    public static final float SEE_THROUGH = 0.15F;

    private BodyVisibility() {}

    public static float alpha(Entity entity) {
        boolean invisible = entity.isInvisible();
        if (!invisible && !InvisibilityPower.inUse()) return 1.0F;
        Player viewer = Minecraft.getInstance().player;
        if (viewer == null) return invisible ? 0.0F : 1.0F;
        if (entity.isInvisibleTo(viewer)) return 0.0F;
        return invisible ? SEE_THROUGH : 1.0F;
    }
}
