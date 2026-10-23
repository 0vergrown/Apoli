package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.access.ModifierSummaryHolder;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AttributeModifyTransferPower extends PowerType<AttributeModifyTransferPower.Config> {
    public record Config(Target target, Holder<Attribute> attribute, double multiplier) {}

    private static final Map<ResourceLocation, Target> TARGETS = new LinkedHashMap<>();

    public static final Target AIR_SPEED = target(ApoliIds.MODIFY_AIR_SPEED);
    public static final Target SWIM_SPEED = target(ApoliIds.MODIFY_SWIM_SPEED);
    public static final Target JUMP = target(ApoliIds.MODIFY_JUMP);
    public static final Target SLIPPERINESS = target(ApoliIds.MODIFY_SLIPPERINESS);
    public static final Target VELOCITY = target(ApoliIds.MODIFY_VELOCITY);
    public static final Target FALLING = target(ApoliIds.MODIFY_FALLING);
    public static final Target BREAK_SPEED = target(ApoliIds.MODIFY_BREAK_SPEED);
    public static final Target EXHAUSTION = target(ApoliIds.MODIFY_EXHAUSTION);
    public static final Target HEALING = target(ApoliIds.MODIFY_HEALING);
    public static final Target DAMAGE_DEALT = target(Apoli.id("modify_damage_dealt"));
    public static final Target DAMAGE_TAKEN = target(Apoli.id("modify_damage_taken"));
    public static final Target PROJECTILE_DAMAGE = target(Apoli.id("modify_projectile_damage"));
    public static final Target STATUS_EFFECT_AMPLIFIER = target(ApoliIds.MODIFY_STATUS_EFFECT_AMPLIFIER);
    public static final Target STATUS_EFFECT_DURATION = target(ApoliIds.MODIFY_STATUS_EFFECT_DURATION);
    public static final Target FOV = target(Apoli.id("modify_fov"));

    private static final Codec<Target> TARGET_CODEC =
        Codec.STRING.comapFlatMap(AttributeModifyTransferPower::readTarget, target -> target.id().toString()).stable();

    private static final Codec<Holder<Attribute>> ATTRIBUTE_CODEC = IdCodecs.ID.comapFlatMap(
        AttributeModifyTransferPower::readAttribute,
        attribute -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value())).stable();

    public static final MapCodec<Config> CONFIG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        TARGET_CODEC.fieldOf("class").forGetter(Config::target),
        ATTRIBUTE_CODEC.fieldOf("attribute").forGetter(Config::attribute),
        Codec.DOUBLE.optionalFieldOf("multiplier", 1.0).forGetter(Config::multiplier)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CONFIG_CODEC;
    }

    private static Target target(ResourceLocation id) {
        Target target = new Target(id);
        TARGETS.put(id, target);
        return target;
    }

    private static DataResult<Target> readTarget(String raw) {
        return IdCodecs.readId(raw.indexOf(':') < 0 ? Apoli.MOD_ID + ":" + raw : raw).flatMap(id -> {
            ResourceLocation named = NamespaceAlias.hasAlias(id.getNamespace()) ? NamespaceAlias.resolve(id) : id;
            Target target = TARGETS.get(named);
            if (target == null) target = TARGETS.get(PowerTypeRegistry.resolveId(named));
            if (target != null) return DataResult.success(target);
            String known = String.join(", ", TARGETS.keySet().stream().map(ResourceLocation::toString).toList());
            if (PowerTypeRegistry.get(named) != null) {
                return DataResult.error(() -> "'" + raw + "' does not take transferred attribute modifiers. "
                    + "'class' must be one of: " + known);
            }
            return DataResult.error(() -> "Unknown power type '" + raw + "' in 'class'. It must be one of: " + known);
        });
    }

    private static DataResult<Holder<Attribute>> readAttribute(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id)
            .<DataResult<Holder<Attribute>>>map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown attribute '" + id + "'"));
    }

    private static boolean targetedByAnyPower(Target target) {
        if (!ApoliPowers.anyOfType(ApoliIds.ATTRIBUTE_MODIFY_TRANSFER)) return false;
        for (Power power : ApoliPowers.view().values()) {
            if (power.config() instanceof Config cfg && cfg.target() == target) return true;
        }
        return false;
    }

    private static List<AttributeModifier> transferred(LivingEntity entity, Config cfg) {
        AttributeInstance instance = entity.getAttributes().getInstance(cfg.attribute());
        if (!(instance instanceof ModifierSummaryHolder holder)) return List.of();
        return holder.apoli$modifierSummary().modifiers(cfg.multiplier());
    }

    public static final class Target {
        private final ResourceLocation id;
        private volatile int inUse = -2;

        private Target(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public boolean appliesTo(@Nullable PowerContainer container) {
            return container != null && inUse()
                && !container.powersOfType(ApoliIds.ATTRIBUTE_MODIFY_TRANSFER).isEmpty();
        }

        public boolean inUse() {
            int generation = ApoliPowers.generation();
            int cached = this.inUse;
            if (cached >> 1 == generation) return (cached & 1) != 0;
            boolean used = targetedByAnyPower(this);
            this.inUse = generation << 1 | (used ? 1 : 0);
            return used;
        }

        public List<AttributeModifier> merge(LivingEntity entity, PowerContainer container, List<AttributeModifier> own) {
            List<ResourceLocation> transfers = container.powersOfType(ApoliIds.ATTRIBUTE_MODIFY_TRANSFER);
            List<AttributeModifier> result = own;
            boolean copied = false;
            EntityCtx ctx = null;
            for (int i = 0, n = transfers.size(); i < n; i++) {
                ResourceLocation powerId = transfers.get(i);
                if (container.isSuppressed(powerId)) continue;
                Power power = ApoliPowers.get(powerId);
                if (power == null || !(power.config() instanceof Config cfg) || cfg.target() != this) continue;
                if (power.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                    if (!power.condition().get().test(ctx)) continue;
                }
                List<AttributeModifier> modifiers = transferred(entity, cfg);
                if (modifiers.isEmpty()) continue;
                if (result.isEmpty()) {
                    result = modifiers;
                    continue;
                }
                if (!copied) {
                    result = new ArrayList<>(result);
                    copied = true;
                }
                result.addAll(modifiers);
            }
            return result;
        }
    }
}
