package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ApoliEntities {
    public static final EntityType<CustomProjectileEntity> CUSTOM_PROJECTILE =
        EntityType.Builder.<CustomProjectileEntity>of(CustomProjectileEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("custom_projectile");

    private ApoliEntities() {}

    public static EntityType<CustomProjectileEntity> customProjectile() {
        return CUSTOM_PROJECTILE;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Apoli.id("custom_projectile"), CUSTOM_PROJECTILE);
    }
}
