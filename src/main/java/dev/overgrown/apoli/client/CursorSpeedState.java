package dev.overgrown.apoli.client;

import dev.overgrown.apoli.power.builtin.ModifyCursorSpeedPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CursorSpeedState {
    private static ModifyCursorSpeedPower.Multipliers current = ModifyCursorSpeedPower.Multipliers.NONE;
    private static boolean active;

    private CursorSpeedState() {}

    public static boolean appliesTo(Entity entity) {
        return active && entity == Minecraft.getInstance().player;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            reset();
            return;
        }
        ModifyCursorSpeedPower.Multipliers next = ModifyCursorSpeedPower.compute(mc.player);
        current = next;
        active = !next.isIdentity();
    }

    public static void reset() {
        current = ModifyCursorSpeedPower.Multipliers.NONE;
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static double horizontal() {
        return current.x();
    }

    public static double vertical() {
        return current.y();
    }
}
