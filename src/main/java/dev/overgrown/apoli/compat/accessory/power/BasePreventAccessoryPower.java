package dev.overgrown.apoli.compat.accessory.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;


public abstract class BasePreventAccessoryPower extends PowerType<BasePreventAccessoryPower.Config> {
    public record Config(List<AccessorySlot> slots, Optional<ItemCondition> itemCondition, boolean allowInCreative) {}

    protected static MapCodec<Config> baseCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AccessorySlot.LIST.optionalFieldOf("slots", List.of()).forGetter(Config::slots),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            Codec.BOOL.optionalFieldOf("allow_in_creative", true).forGetter(Config::allowInCreative)
        ).apply(i, Config::new));
    }

    
    public static boolean isPrevented(LivingEntity entity, ResourceLocation canonical, AccessorySlotRef ref, ItemStack stack) {
        boolean creative = entity instanceof Player p && p.getAbilities().instabuild;
        ItemCtx itemCtx = new ItemCtx(stack, entity.level(), entity);
        boolean[] prevented = {false};
        PowerLookup.forEach(entity, canonical, Config.class, cfg -> {
            if (prevented[0]) return;
            if (creative && cfg.allowInCreative()) return;
            if (!AccessorySlot.matchesAny(cfg.slots(), ref)) return;
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(itemCtx)) return;
            prevented[0] = true;
        });
        return prevented[0];
    }
}
