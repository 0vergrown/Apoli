package dev.overgrown.apoli.compat.create;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.ModCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public final class CreateCompat {

    private CreateCompat() {}

    public static final TagKey<Item> DIVING_HELMETS = TagKey.create(Registries.ITEM, Apoli.id("create/diving_helmets"));
    public static final TagKey<Item> DIVING_BOOTS = TagKey.create(Registries.ITEM, Apoli.id("create/diving_boots"));

    private static final String GOGGLES_ITEM = "com.simibubi.create.content.equipment.goggles.GogglesItem";

    public static void init() {
        try {
            Method add = Class.forName(GOGGLES_ITEM).getMethod("addIsWearingPredicate", Predicate.class);
            Predicate<Player> wearing = CreateGogglesPower::grants;
            add.invoke(null, wearing);
        } catch (Throwable t) {
            Apoli.LOGGER.warn("[Apoli] Create is loaded but its goggle API was not found, so apoli:create_goggles does nothing: {}", t.toString());
        }
    }

    public static boolean wearsDivingBoots(LivingEntity entity) {
        return ModCompat.CREATE && entity.getItemBySlot(EquipmentSlot.FEET).is(DIVING_BOOTS);
    }

    public static boolean breathesFromBacktank(LivingEntity entity) {
        if (!ModCompat.CREATE || !entity.getItemBySlot(EquipmentSlot.HEAD).is(DIVING_HELMETS)) return false;
        return CreateBacktanks.breathe(entity);
    }
}
