package dev.overgrown.apoli.compat.gun;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class GunHit {
    private GunHit() {}

    public static final class Info {
        private @Nullable ResourceLocation gun;
        private boolean headshot;
        private @Nullable Entity attacker;
        private @Nullable Entity bullet;
        private boolean active;

        public @Nullable ResourceLocation gun() {
            return gun;
        }

        public boolean headshot() {
            return headshot;
        }
    }

    private static final ThreadLocal<Info> CURRENT = ThreadLocal.withInitial(Info::new);

    public static void begin(@Nullable ResourceLocation gun, boolean headshot, @Nullable Entity attacker, @Nullable Entity bullet) {
        Info info = CURRENT.get();
        info.gun = gun;
        info.headshot = headshot;
        info.attacker = attacker;
        info.bullet = bullet;
        info.active = true;
    }

    public static void end() {
        Info info = CURRENT.get();
        info.active = false;
        info.gun = null;
        info.attacker = null;
        info.bullet = null;
    }

    public static @Nullable Info matching(DamageSource source) {
        Info info = CURRENT.get();
        if (!info.active) return null;
        if (info.bullet != null) return source.getDirectEntity() == info.bullet ? info : null;
        Entity cause = source.getEntity();
        return cause == null || cause == info.attacker ? info : null;
    }
}
