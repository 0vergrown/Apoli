package dev.overgrown.apoli.compat.icarus;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class WingsPower extends PowerType<WingsPower.Config> {

    private static final ResourceLocation MAX_STAMINA = ResourceLocation.fromNamespaceAndPath("icarus", "max_stamina");

    private static final Codec<ItemStack> WINGS_TYPE_CODEC = Codec.either(
        BuiltInRegistries.ITEM.byNameCodec().xmap(ItemStack::new, ItemStack::getItem),
        ItemStackData.CODEC.xmap(ItemStackData::stack, ItemStackData::new)
    ).xmap(
        either -> either.map(stack -> stack, stack -> stack),
        stack -> Either.<ItemStack, ItemStack>right(stack)
    );

    public record Config(
        ItemStack wingsType,
        Optional<Boolean> armorSlows,
        OptionalDouble maxSlowedMultiplier,
        OptionalDouble wingsSpeed,
        OptionalDouble exhaustionAmount,
        OptionalInt maxHeightAboveWorld,
        OptionalDouble stamina
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            WINGS_TYPE_CODEC.fieldOf("wings_type").forGetter(Config::wingsType),
            Codec.BOOL.optionalFieldOf("armor_slows").forGetter(Config::armorSlows),
            Codec.DOUBLE.optionalFieldOf("max_slowed_multiplier")
                .xmap(WingsPower::toOptionalDouble, WingsPower::fromOptionalDouble)
                .forGetter(Config::maxSlowedMultiplier),
            Codec.DOUBLE.optionalFieldOf("wings_speed")
                .xmap(WingsPower::toOptionalDouble, WingsPower::fromOptionalDouble)
                .forGetter(Config::wingsSpeed),
            Codec.DOUBLE.optionalFieldOf("exhaustion_amount")
                .xmap(WingsPower::toOptionalDouble, WingsPower::fromOptionalDouble)
                .forGetter(Config::exhaustionAmount),
            Codec.INT.optionalFieldOf("max_height_above_world")
                .xmap(WingsPower::toOptionalInt, WingsPower::fromOptionalInt)
                .forGetter(Config::maxHeightAboveWorld),
            Codec.DOUBLE.optionalFieldOf("stamina")
                .xmap(WingsPower::toOptionalDouble, WingsPower::fromOptionalDouble)
                .forGetter(Config::stamina)
        ).apply(instance, Config::new));
    }

    private static OptionalDouble toOptionalDouble(Optional<Double> opt) {
        return opt.map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    private static Optional<Double> fromOptionalDouble(OptionalDouble od) {
        return od.isPresent() ? Optional.of(od.getAsDouble()) : Optional.empty();
    }

    private static OptionalInt toOptionalInt(Optional<Integer> opt) {
        return opt.map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    private static Optional<Integer> fromOptionalInt(OptionalInt oi) {
        return oi.isPresent() ? Optional.of(oi.getAsInt()) : Optional.empty();
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        IcarusFieldHooks.install();
        LivingEntity owner = holder.owner();
        applyStamina(owner, cfg);
        if (owner != null && conditionPasses(powerId, owner)) WingsAccess.refresh(owner, cfg);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity owner = holder.owner();
        if (owner != null) {
            resetStamina(owner, cfg);
            WingsAccess.invalidate(owner);
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        IcarusFieldHooks.install();
        LivingEntity owner = holder.owner();
        if (owner == null) return;
        if (conditionPasses(powerId, owner)) WingsAccess.refresh(owner, cfg);
        else WingsAccess.invalidate(owner);
    }

    private static boolean conditionPasses(ResourceLocation powerId, LivingEntity owner) {
        Power power = ApoliPowers.get(powerId);
        if (power == null || power.condition().isEmpty()) return true;
        return power.condition().get().test(EntityCtx.of(owner, owner.level()));
    }

    private static void applyStamina(@Nullable LivingEntity owner, Config cfg) {
        if (owner == null || cfg.stamina().isEmpty()) return;
        AttributeInstance instance = maxStaminaInstance(owner);
        if (instance != null) instance.setBaseValue(cfg.stamina().getAsDouble());
    }

    private static void resetStamina(@Nullable LivingEntity owner, Config cfg) {
        if (owner == null || cfg.stamina().isEmpty()) return;
        AttributeInstance instance = maxStaminaInstance(owner);
        if (instance != null) instance.setBaseValue(defaultMaxStamina(instance));
    }

    @Nullable
    private static AttributeInstance maxStaminaInstance(LivingEntity owner) {
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(MAX_STAMINA).orElse(null);
        return attribute == null ? null : owner.getAttribute(attribute);
    }

    private static double defaultMaxStamina(AttributeInstance instance) {
        return instance.getAttribute().value().getDefaultValue();
    }
}
