package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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

    public static final EntityType<MinionEntity> MINION =
        EntityType.Builder.of(MinionEntity::new, MobCategory.MISC)
            .sized(0.6F, 0.9F)
            .clientTrackingRange(10)
            .build("minion");

    public static final EntityType<CloneEntity> CLONE =
        EntityType.Builder.of(CloneEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .build("clone");

    private ApoliEntities() {}

    public static EntityType<CustomProjectileEntity> customProjectile() {
        return CUSTOM_PROJECTILE;
    }

    public static EntityType<MinionEntity> minionType() {
        return MINION;
    }

    public static EntityType<CloneEntity> cloneType() {
        return CLONE;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Apoli.id("custom_projectile"), CUSTOM_PROJECTILE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Apoli.id("minion"), MINION);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Apoli.id("clone"), CLONE);
        FabricDefaultAttributeRegistry.register(MINION, MinionEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CLONE, CloneEntity.createAttributes());
    }
}
