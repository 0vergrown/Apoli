package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ModifyUseSlowdownPower extends PowerType<ModifyUseSlowdownPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_use_slowdown");

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         Optional<ItemCondition> itemCondition) {
        public List<AttributeModifier> flattened() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition)
        ).apply(i, Config::new));
    }

    public static float compute(@Nullable LivingEntity entity, float base) {
        if (entity == null) return base;
        List<Config> configs = PowerLookup.active(entity, CANONICAL, Config.class);
        if (configs.isEmpty()) return base;

        ItemStack inUse = entity.getUseItem();
        ItemCtx itemCtx = null;
        double value = base;
        for (int i = 0, n = configs.size(); i < n; i++) {
            Config cfg = configs.get(i);
            if (cfg.itemCondition().isPresent()) {
                if (itemCtx == null) itemCtx = new ItemCtx(inUse, entity.level(), entity);
                if (!cfg.itemCondition().get().test(itemCtx)) continue;
            }
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            value = AttributeModifierHelper.apply(value, mods, entity);
        }
        return (float) Mth.clamp(value, 0.0, 1.0);
    }
}
