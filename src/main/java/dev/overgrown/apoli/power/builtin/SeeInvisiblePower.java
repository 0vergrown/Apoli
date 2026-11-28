package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.mixin.flag.EntitySharedFlagInvoker;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// The holder sees invisible entities, drawn at the configured opacity. Only what the holder sees changes: the entity
// stays invisible to everyone else, and isInvisible() (so apoli:invisible) is untouched.
public final class SeeInvisiblePower extends PowerType<SeeInvisiblePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("see_invisible");
    private static final PowerTypeGate GATE = new PowerTypeGate(CANONICAL);

    // The shared entity flag set by the Invisibility effect and the Invisible data tag.
    private static final int FLAG_INVISIBLE = 5;

    // Why an entity is invisible: "any", "effect", a power ID or a #power tag, or the object forms.
    public record Source(Kind kind, @Nullable ResourceLocation id, @Nullable String tag, @Nullable MobEffect effect) {
        public enum Kind { ANY, EFFECT, POWER, TAG }

        static Source any() { return new Source(Kind.ANY, null, null, null); }
        static Source effect(MobEffect effect) { return new Source(Kind.EFFECT, null, null, effect); }
        static Source power(ResourceLocation id) { return new Source(Kind.POWER, id, null, null); }
        static Source tag(String tag) { return new Source(Kind.TAG, null, tag, null); }

        private static final Codec<Source> STRING = Codec.STRING.comapFlatMap(Source::read, Source::write);

        private record Obj(String type, Optional<ResourceLocation> effect, Optional<ResourceLocation> power, Optional<String> tag) {}

        private static final Codec<Source> OBJECT = RecordCodecBuilder.<Obj>create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(Obj::type),
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(Obj::effect),
            ResourceLocation.CODEC.optionalFieldOf("power").forGetter(Obj::power),
            Codec.STRING.optionalFieldOf("tag").forGetter(Obj::tag)
        ).apply(i, Obj::new)).comapFlatMap(Source::fromObj, Source::toObj);

        // A string, or else an object. Picked by shape so a bad string reports itself.
        public static final Codec<Source> CODEC = byShape(STRING, OBJECT, false);

        private static DataResult<Source> read(String raw) {
            if (raw.equals("any")) return DataResult.success(any());
            if (raw.equals("effect")) return DataResult.success(effect(MobEffects.INVISIBILITY));
            if (raw.startsWith("#")) return raw.length() > 1 ? DataResult.success(tag(raw.substring(1)))
                : DataResult.error(() -> "Empty power tag in see_invisible source");
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) return DataResult.error(() -> "Not a source: \"" + raw + "\". Use \"any\", \"effect\", a power ID or a #power tag.");
            return DataResult.success(power(id));
        }

        private static DataResult<Source> fromObj(Obj o) {
            return switch (o.type()) {
                case "any" -> DataResult.success(any());
                case "effect" -> {
                    if (o.effect().isEmpty()) yield DataResult.success(effect(MobEffects.INVISIBILITY));
                    ResourceLocation id = o.effect().get();
                    MobEffect found = BuiltInRegistries.MOB_EFFECT.containsKey(id) ? BuiltInRegistries.MOB_EFFECT.get(id) : null;
                    yield found != null ? DataResult.success(effect(found))
                        : DataResult.error(() -> "Unknown effect in see_invisible source: " + id);
                }
                case "power" -> o.power().map(p -> DataResult.success(power(p)))
                    .orElseGet(() -> DataResult.error(() -> "see_invisible source of type \"power\" needs \"power\""));
                case "power_tag", "tag" -> o.tag().map(t -> DataResult.success(tag(t.startsWith("#") ? t.substring(1) : t)))
                    .orElseGet(() -> DataResult.error(() -> "see_invisible source of type \"power_tag\" needs \"tag\""));
                default -> DataResult.error(() -> "Unknown see_invisible source type: " + o.type());
            };
        }

        private Obj toObj() {
            return switch (kind) {
                case ANY -> new Obj("any", Optional.empty(), Optional.empty(), Optional.empty());
                case EFFECT -> new Obj("effect", Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.getKey(effect)), Optional.empty(), Optional.empty());
                case POWER -> new Obj("power", Optional.empty(), Optional.of(id), Optional.empty());
                case TAG -> new Obj("power_tag", Optional.empty(), Optional.empty(), Optional.of(tag));
            };
        }

        private String write() {
            return switch (kind) {
                case ANY -> "any";
                case EFFECT -> effect == MobEffects.INVISIBILITY ? "effect" : String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(effect));
                case POWER -> id.toString();
                case TAG -> "#" + tag;
            };
        }

        boolean matches(Entity target) {
            return switch (kind) {
                case ANY -> true;
                case EFFECT -> effect == MobEffects.INVISIBILITY
                    ? ((EntitySharedFlagInvoker) target).apoli$getSharedFlag(FLAG_INVISIBLE)
                    : target instanceof LivingEntity living && living.hasEffect(effect);
                case POWER -> hasActive(target, id);
                case TAG -> {
                    List<ResourceLocation> tagged = ApoliPowers.withTag(tag);
                    for (int i = 0; i < tagged.size(); i++) {
                        if (hasActive(target, tagged.get(i))) yield true;
                    }
                    yield false;
                }
            };
        }
    }

    // One source or a list.
    private static final Codec<List<Source>> SOURCES = byShape(Source.CODEC.listOf(), Source.CODEC.xmap(List::of, l -> l.get(0)), true);

    // Decodes with first when the input is a list (list = true) or a string (list = false), else with second.
    // Encodes with first.
    private static <T> Codec<T> byShape(Codec<T> first, Codec<T> second, boolean list) {
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
                boolean match = list ? ops.getStream(input).result().isPresent() : ops.getStringValue(input).result().isPresent();
                return (match ? first : second).decode(ops, input);
            }

            @Override
            public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
                return first.encode(input, ops, prefix);
            }
        };
    }

    public record Config(float opacity, Optional<BiEntityCondition> bientityCondition, List<Source> sources) {
        boolean reveals(Entity observer, Entity target) {
            for (int i = 0; i < sources.size(); i++) {
                if (sources.get(i).matches(target)) {
                    return bientityCondition.isEmpty() || bientityCondition.get().test(new BiEntityCtx(observer, target, target.level()));
                }
            }
            return false;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("opacity", Codec.floatRange(0.0F, 1.0F), 0.5F).forGetter(Config::opacity),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            // strict, so a source with a typo doesn't quietly become "any"
            LoggedOptionalField.strict("source", SOURCES).forGetter(c -> Optional.of(c.sources()))
        ).apply(i, (opacity, bientity, sources) ->
            new Config(opacity, bientity, sources.filter(l -> !l.isEmpty()).orElse(List.of(Source.any())))));
    }

    // The power's condition is checked on the server and synced, so the renderer reads the server's answer.
    @Override
    public boolean resolvesForClient() {
        return true;
    }

    private static final ThreadLocal<Boolean> CHECKING = ThreadLocal.withInitial(() -> false);

    // How see-through target looks to observer: 0 solid to 1 not drawn, or NaN when none of the observer's powers
    // reveal it. With several matching powers the most visible one wins.
    public static float revealOpacity(@Nullable Entity observer, @Nullable Entity target) {
        if (observer == null || target == null || !GATE.inUse() || CHECKING.get()) return Float.NaN;
        boolean client = observer.level().isClientSide();
        // On the client only the local player's resolved powers are known.
        if (client && !(observer instanceof Player player && player.isLocalPlayer())) return Float.NaN;
        PowerContainer container = PowerContainer.of(observer);
        if (container == null || container.isEmpty()) return Float.NaN;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return Float.NaN;

        CHECKING.set(true);
        try {
            float best = Float.NaN;
            EntityCtx ctx = null;
            for (int i = 0; i < powers.size(); i++) {
                ResourceLocation id = powers.get(i);
                if (container.isSuppressed(id)) continue;
                Power power = ApoliPowers.get(id);
                if (power == null || !(power.config() instanceof Config cfg)) continue;
                if (!Float.isNaN(best) && cfg.opacity() >= best) continue;
                if (client) {
                    if (!ClientResolvedPowers.isActive(id)) continue;
                } else if (power.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(observer, observer.level());
                    if (!testSafely(power, ctx)) continue;
                }
                if (cfg.reveals(observer, target)) best = cfg.opacity();
            }
            return best;
        } finally {
            CHECKING.set(false);
        }
    }

    // The entity holds this power, it isn't suppressed and its condition passes.
    private static boolean hasActive(Entity entity, ResourceLocation powerId) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty() || !container.hasPower(powerId) || container.isSuppressed(powerId)) return false;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return false;
        return power.condition().isEmpty() || testSafely(power, EntityCtx.of(entity, entity.level()));
    }

    private static boolean testSafely(Power power, EntityCtx ctx) {
        try {
            return power.condition().get().test(ctx);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
