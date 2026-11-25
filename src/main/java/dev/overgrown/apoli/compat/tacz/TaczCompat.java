package dev.overgrown.apoli.compat.tacz;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunFirePostEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import dev.overgrown.apoli.compat.gun.GunHit;
import dev.overgrown.apoli.compat.gun.GunPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

public final class TaczCompat {
    private TaczCompat() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityHurtByGunEvent.Pre.class, TaczCompat::onHurt);
        NeoForge.EVENT_BUS.addListener(EntityHurtByGunEvent.Post.class, event -> {
            if (event.getLogicalSide().isServer()) GunHit.end();
        });
        NeoForge.EVENT_BUS.addListener(EntityKillByGunEvent.class, event -> {
            if (event.getLogicalSide().isServer()) GunHit.end();
        });
        NeoForge.EVENT_BUS.addListener(GunShootEvent.class, TaczCompat::onShoot);
        NeoForge.EVENT_BUS.addListener(GunFirePostEvent.class, TaczCompat::onFired);
        NeoForge.EVENT_BUS.addListener(AmmoHitBlockEvent.class, TaczCompat::onBlockHit);
    }

    private static void onHurt(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide().isClient()) return;
        LivingEntity attacker = event.getAttacker();
        Entity target = event.getHurtEntity();
        ResourceLocation gun = event.getGunId();
        boolean headshot = event.isHeadShot();
        event.setBaseAmount(GunPowers.modifyDamage(attacker, target, gun, headshot, event.getBaseAmount()));
        if (headshot) {
            event.setHeadshotMultiplier((float) GunPowers.modifyHeadshot(attacker, target, gun, event.getHeadshotMultiplier()));
        }
        GunHit.begin(gun, headshot, attacker, event.getBullet());
    }

    private static void onShoot(GunShootEvent event) {
        LivingEntity shooter = event.getShooter();
        if (GunPowers.prevents(shooter, GunPowers.PREVENT_GUN_FIRE, TaczGuns.gunId(event.getGunItemStack()))) event.setCanceled(true);
    }

    private static void onFired(GunFirePostEvent event) {
        if (!event.getLogicalSide().isServer() || event.getShooter() == null) return;
        GunPowers.onFire(event.getShooter(), event.getGunItemStack(), TaczGuns.gunId(event.getGunItemStack()));
    }

    private static void onBlockHit(AmmoHitBlockEvent event) {
        EntityKineticBullet ammo = event.getAmmo();
        if (ammo == null || event.getLevel().isClientSide() || !(ammo.getOwner() instanceof LivingEntity shooter)) return;
        GunPowers.onBlockHit(shooter, ammo.getGunId(), event.getLevel(), event.getHitResult().getBlockPos());
    }
}
