package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class SameEquippedItemCondition implements ConditionType<BiEntityCtx, SameEquippedItemCondition.Cfg> {

    private static final List<EquipmentSlot> MAINHAND = List.of(EquipmentSlot.MAINHAND);

    public record Cfg(List<EquipmentSlot> equipmentSlot, Optional<List<EquipmentSlot>> targetEquipmentSlot,
                      boolean compareId, boolean compareCount, boolean compareTag,
                      boolean allowEmpty, Optional<ItemCondition> itemCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SingleOrList.of(EquipmentSlot.CODEC).optionalFieldOf("equipment_slot", MAINHAND).forGetter(Cfg::equipmentSlot),
            LoggedOptionalField.strict("target_equipment_slot", SingleOrList.of(EquipmentSlot.CODEC)).forGetter(Cfg::targetEquipmentSlot),
            Codec.BOOL.optionalFieldOf("compare_id", true).forGetter(Cfg::compareId),
            Codec.BOOL.optionalFieldOf("compare_count", false).forGetter(Cfg::compareCount),
            Codec.BOOL.optionalFieldOf("compare_tag", false).forGetter(Cfg::compareTag),
            Codec.BOOL.optionalFieldOf("allow_empty", false).forGetter(Cfg::allowEmpty),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Cfg::itemCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity actor = ctx.livingActor();
        LivingEntity target = ctx.livingTarget();
        if (actor == null || target == null) return false;

        List<EquipmentSlot> mineSlots = cfg.equipmentSlot();
        List<EquipmentSlot> theirSlots = cfg.targetEquipmentSlot().orElse(mineSlots);

        for (int a = 0; a < mineSlots.size(); a++) {
            ItemStack mine = actor.getItemBySlot(mineSlots.get(a).vanilla());
            if (mine.isEmpty() && !cfg.allowEmpty()) continue;
            for (int b = 0; b < theirSlots.size(); b++) {
                ItemStack theirs = target.getItemBySlot(theirSlots.get(b).vanilla());
                if (theirs.isEmpty() && !cfg.allowEmpty()) continue;
                if (!same(cfg, mine, theirs)) continue;
                if (cfg.itemCondition().isPresent()
                    && !cfg.itemCondition().get().test(new ItemCtx(mine, ctx.level(), actor))) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean same(Cfg cfg, ItemStack mine, ItemStack theirs) {
        if (mine.isEmpty() != theirs.isEmpty()) return false;
        if (cfg.compareId() && !ItemStack.isSameItem(mine, theirs)) return false;
        if (cfg.compareCount() && mine.getCount() != theirs.getCount()) return false;
        return !cfg.compareTag() || sameData(mine, theirs);
    }

    private static boolean sameData(ItemStack mine, ItemStack theirs) {
        return java.util.Objects.equals(mine.getComponentsPatch(), theirs.getComponentsPatch());
    }
}
