package dev.overgrown.apoli.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.NamespaceAlias;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LegacyPowerShapes {
    private LegacyPowerShapes() {}

    public interface Shape {
        <T> Dynamic<T> reshape(Dynamic<T> power);
    }

    private static final Map<ResourceLocation, Shape> SHAPES = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    public static void register(ResourceLocation legacyId, Shape shape) {
        SHAPES.put(legacyId, shape);
    }

    public static <T> Dynamic<T> apply(Dynamic<T> power) {
        if (SHAPES.isEmpty()) return power;
        String declared = power.get("type").asString().result().orElse(null);
        if (declared == null) return power;
        ResourceLocation id = ResourceLocation.tryParse(declared);
        if (id == null) return power;

        Shape shape = SHAPES.get(id);
        if (shape == null && NamespaceAlias.hasAlias(id.getNamespace())) {
            shape = SHAPES.get(NamespaceAlias.resolve(id));
        }
        return shape == null ? power : shape.reshape(power);
    }

    public static void registerBuiltin() {
        register(Apoli.id("damage_over_time"), new Shape() {
            @Override
            public <T> Dynamic<T> reshape(Dynamic<T> power) {
                return damageOverTime(power);
            }
        });
        register(Apoli.id("burn"), new Shape() {
            @Override
            public <T> Dynamic<T> reshape(Dynamic<T> power) {
                return burn(power);
            }
        });
        register(Apoli.id("exhaust"), new Shape() {
            @Override
            public <T> Dynamic<T> reshape(Dynamic<T> power) {
                return exhaust(power);
            }
        });
        register(Apoli.id("modify_attribute"), new Shape() {
            @Override
            public <T> Dynamic<T> reshape(Dynamic<T> power) {
                return modifyAttribute(power);
            }
        });
    }

    private static <T> Dynamic<T> damageOverTime(Dynamic<T> power) {
        if (power.get("entity_action").result().isPresent()) return power;

        String damageType = power.get("damage_type").asString().result()
            .orElseGet(() -> legacyDamageSource(power));
        float damage = power.get("damage").asNumber().result().map(Number::floatValue).orElse(1f);
        float damageEasy = power.get("damage_easy").asNumber().result().map(Number::floatValue).orElse(damage);
        int interval = Math.max(1, power.get("interval").asNumber().result().map(Number::intValue).orElse(20));
        int onsetDelay = power.get("onset_delay").asNumber().result().map(Number::intValue).orElse(interval);

        if (power.get("protection_enchantment").result().isPresent()
            || power.get("protection_effectiveness").result().isPresent()) {
            warnOnce("protection", "[Apoli] apoli:damage_over_time no longer scales its onset delay by an "
                + "enchantment. Track the protection level in an apoli:resource and write "
                + "\"onset_delay\": \"20 + mypack:protection * 26\" on the power instead.");
        }

        Dynamic<T> action = damageAction(power, damage, damageType);
        if (damageEasy != damage) {
            action = power.emptyMap()
                .set("type", power.createString("apoli:if_else"))
                .set("condition", power.emptyMap()
                    .set("type", power.createString("apoli:difficulty"))
                    .set("difficulty", power.createString("easy")))
                .set("if_action", damageAction(power, damageEasy, damageType))
                .set("else_action", action);
        }

        return power
            .remove("damage_type").remove("damage_source").remove("damage").remove("damage_easy")
            .remove("protection_enchantment").remove("protection_effectiveness")
            .set("interval", power.createInt(interval))
            .set("onset_delay", power.createInt(onsetDelay))
            .set("entity_action", action);
    }

    private static <T> Dynamic<T> damageAction(Dynamic<T> ctx, float amount, String damageType) {
        return ctx.emptyMap()
            .set("type", ctx.createString("apoli:damage"))
            .set("amount", ctx.createFloat(amount))
            .set("damage_type", ctx.createString(damageType));
    }

    private static <T> String legacyDamageSource(Dynamic<T> power) {
        String name = power.get("damage_source").get("name").asString().result().orElse("");
        if (name.isEmpty()) return "minecraft:generic";
        ResourceLocation parsed = ResourceLocation.tryParse(name.indexOf(':') < 0 ? "minecraft:" + name : name);
        return parsed == null ? "minecraft:generic" : parsed.toString();
    }

    private static <T> Dynamic<T> burn(Dynamic<T> power) {
        if (power.get("entity_action").result().isPresent()) return power;

        float duration = power.get("burn_duration").asNumber().result().map(Number::floatValue).orElse(1f);
        int interval = Math.max(1, power.get("interval").asNumber().result().map(Number::intValue).orElse(20));

        Dynamic<T> fire = power.emptyMap()
            .set("type", power.createString("apoli:set_on_fire"))
            .set("duration", power.createString(String.valueOf(duration)));

        return power.remove("burn_duration")
            .set("interval", power.createInt(interval))
            .set("entity_action", fire);
    }

    private static <T> Dynamic<T> exhaust(Dynamic<T> power) {
        if (power.get("entity_action").result().isPresent()) return power;

        float exhaustion = power.get("exhaustion").asNumber().result().map(Number::floatValue).orElse(0f);
        int interval = Math.max(1, power.get("interval").asNumber().result().map(Number::intValue).orElse(20));

        Dynamic<T> action = power.emptyMap()
            .set("type", power.createString("apoli:exhaust"))
            .set("amount", power.createFloat(exhaustion));

        return power.remove("exhaustion")
            .set("interval", power.createInt(interval))
            .set("entity_action", action);
    }

    private static <T> Dynamic<T> modifyAttribute(Dynamic<T> power) {
        Dynamic<T> attribute = power.get("attribute").result().orElse(null);
        if (attribute == null) return power;

        List<Dynamic<T>> merged = new ArrayList<>(2);
        collectModifiers(power.get("modifier").result().orElse(null), attribute, merged);
        collectModifiers(power.get("modifiers").result().orElse(null), attribute, merged);

        return power.remove("attribute").remove("modifier")
            .set("modifiers", power.createList(merged.stream()));
    }

    private static <T> void collectModifiers(Dynamic<T> source, Dynamic<T> attribute, List<Dynamic<T>> into) {
        if (source == null) return;
        var list = source.asStreamOpt().result().orElse(null);
        if (list != null) {
            list.forEach(element -> collectModifiers(element, attribute, into));
            return;
        }
        if (source.getMapValues().result().isEmpty()) return;
        into.add(source.get("attribute").result().isPresent() ? source : source.set("attribute", attribute));
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) Apoli.LOGGER.warn(message);
    }
}
