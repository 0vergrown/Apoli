package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.entity.summon.MinionEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ApoliEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Apoli.MOD_ID);

    public static final Supplier<EntityType<CustomProjectileEntity>> CUSTOM_PROJECTILE =
        ENTITY_TYPES.register("custom_projectile", () ->
            EntityType.Builder.<CustomProjectileEntity>of(CustomProjectileEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("custom_projectile"));

    public static final Supplier<EntityType<MinionEntity>> MINION =
        ENTITY_TYPES.register("minion", () ->
            EntityType.Builder.of(MinionEntity::new, MobCategory.MISC)
                .sized(0.6F, 0.9F)
                .clientTrackingRange(10)
                .build("minion"));

    public static final Supplier<EntityType<CloneEntity>> CLONE =
        ENTITY_TYPES.register("clone", () ->
            EntityType.Builder.of(CloneEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .build("clone"));

    private ApoliEntities() {}

    public static EntityType<CustomProjectileEntity> customProjectile() {
        return CUSTOM_PROJECTILE.get();
    }

    public static EntityType<MinionEntity> minionType() {
        return MINION.get();
    }

    public static EntityType<CloneEntity> cloneType() {
        return CLONE.get();
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MINION.get(), MinionEntity.createAttributes().build());
        event.put(CLONE.get(), CloneEntity.createAttributes().build());
    }
}
