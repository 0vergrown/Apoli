package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
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

    private ApoliEntities() {}

    public static EntityType<CustomProjectileEntity> customProjectile() {
        return CUSTOM_PROJECTILE.get();
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
