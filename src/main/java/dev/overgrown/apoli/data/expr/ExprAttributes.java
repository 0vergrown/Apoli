package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.List;
import java.util.Set;

public final class ExprAttributes {
    private ExprAttributes() {}

    private static final String QUERIES = "get, base, default, min, max, has, modifier, has_modifier";
    private static final Set<String> PLAIN_QUERIES = Set.of("get", "base", "default", "min", "max", "has");

    public static ExprVars.ResolvedVar query(List<String> args, String name, int at) throws ExprParseException {
        if (args.isEmpty() || args.get(0).isEmpty()) {
            throw new ExprParseException(name + "[id, query] expects an attribute id", at);
        }
        String rawId = unquote(args.get(0));
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) throw new ExprParseException("'" + rawId + "' is not a valid attribute id", at);
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);

        String query = args.size() > 1 ? unquote(args.get(1)) : "get";
        boolean modifierQuery = query.equals("modifier") || query.equals("has_modifier");
        int allowed = modifierQuery ? 3 : 2;
        if (args.size() > allowed) {
            throw new ExprParseException(name + "[id, " + query + "] takes at most " + allowed
                + " arguments, but got " + args.size(), at);
        }
        if (modifierQuery) {
            if (args.size() < 3 || args.get(2).isEmpty()) {
                throw new ExprParseException(name + "[id, " + query + ", <modifier id>] is missing the modifier id", at);
            }
            String rawModifier = unquote(args.get(2));
            ResourceLocation modifier = ResourceLocation.tryParse(rawModifier);
            if (modifier == null) throw new ExprParseException("'" + rawModifier + "' is not a valid modifier id", at);
            if (attribute == null) return unknown(name, id);
            return query.equals("modifier") ? modifierAmount(attribute, modifier) : hasModifier(attribute, modifier);
        }

        if (!PLAIN_QUERIES.contains(query)) {
            throw new ExprParseException("'" + query + "' is not an attribute query (" + QUERIES + ")", at);
        }
        if (attribute == null) return unknown(name, id);
        return switch (query) {
            case "get" -> new ExprVars.ResolvedVar((e, c, l, v) -> {
                if (!(e instanceof LivingEntity living)) return 0;
                AttributeMap map = living.getAttributes();
                return map.hasAttribute(attribute) ? map.getValue(attribute) : 0;
            }, false);
            case "base" -> new ExprVars.ResolvedVar((e, c, l, v) -> {
                if (!(e instanceof LivingEntity living)) return 0;
                AttributeMap map = living.getAttributes();
                return map.hasAttribute(attribute) ? map.getBaseValue(attribute) : 0;
            }, false);
            case "has" -> new ExprVars.ResolvedVar((e, c, l, v) ->
                e instanceof LivingEntity living && living.getAttributes().hasAttribute(attribute) ? 1 : 0, false);
            case "default" -> constant(attribute.value().getDefaultValue());
            case "min" -> constant(attribute.value() instanceof RangedAttribute ranged ? ranged.getMinValue() : -Double.MAX_VALUE);
            default -> constant(attribute.value() instanceof RangedAttribute ranged ? ranged.getMaxValue() : Double.MAX_VALUE);
        };
    }

    private static ExprVars.ResolvedVar unknown(String name, ResourceLocation id) {
        Apoli.LOGGER.warn("[Apoli] {}[{}] names an attribute that isn't registered, so it reads 0.", name, id);
        return constant(0);
    }

    private static ExprVars.ResolvedVar modifierAmount(Holder<Attribute> attribute, ResourceLocation modifier) {
        return new ExprVars.ResolvedVar((e, c, l, v) -> {
            if (!(e instanceof LivingEntity living)) return 0;
            AttributeMap map = living.getAttributes();
            return map.hasModifier(attribute, modifier) ? map.getModifierValue(attribute, modifier) : 0;
        }, false);
    }

    private static ExprVars.ResolvedVar hasModifier(Holder<Attribute> attribute, ResourceLocation modifier) {
        return new ExprVars.ResolvedVar((e, c, l, v) ->
            e instanceof LivingEntity living && living.getAttributes().hasModifier(attribute, modifier) ? 1 : 0, false);
    }

    private static ExprVars.ResolvedVar constant(double value) {
        return new ExprVars.ResolvedVar((e, c, l, v) -> value, false);
    }

    private static String unquote(String arg) {
        int last = arg.length() - 1;
        if (last > 0) {
            char first = arg.charAt(0);
            if ((first == '\'' || first == '"') && arg.charAt(last) == first) return arg.substring(1, last).trim();
        }
        return arg;
    }
}
