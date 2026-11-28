package dev.overgrown.apoli.compat.spellengine;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.spellengine.power.ActionOnSpellCastPower;
import dev.overgrown.apoli.compat.spellengine.power.ActionOnSpellHitPower;
import dev.overgrown.apoli.compat.spellengine.power.ActionWhenHitBySpellPower;
import dev.overgrown.apoli.compat.spellengine.power.PreventSpellCastPower;
import dev.overgrown.apoli.compat.spellengine.power.SpellsPower;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.FunctionPower;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.spell_engine.api.entity.SpellEntityPredicates;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ReleaseFx;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.delivery.SpellDelivery;
import net.spell_engine.internals.target.SpellIntents;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellEngineCompat {
    private SpellEngineCompat() {}

    private static final SpellHandlers.ImpactResult HIT = new SpellHandlers.ImpactResult(true, false);
    private static final SpellHandlers.ImpactResult MISSED = new SpellHandlers.ImpactResult(false, false);

    private static final ThreadLocal<int[]> NOTIFY_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Map<String, SpellHandlers.CustomImpact> FUNCTION_IMPACTS = new HashMap<>();
    private static final Map<String, Optional<ResourceLocation>> POWER_IDS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<EntityCondition>> ENTITY_CONDITIONS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<BiEntityCondition>> BIENTITY_CONDITIONS = new ConcurrentHashMap<>();

    public static void init() {
        SpellEvents.SPELL_CAST.register(args -> notifyCast(args.caster(), args.spell(), args.targets()));
        SpellEvents.CASTING_ATTEMPT.PRE.register(args ->
            PreventSpellCastPower.prevents(args.caster(), args.spell()) ? SpellCast.Attempt.none() : null);
        SpellEntityPredicates.register(Apoli.id("power"), SpellEngineCompat::hasActivePower);
        SpellEntityPredicates.register(Apoli.id("condition"), SpellEngineCompat::matchesCondition);
        SpellEntityPredicates.register(Apoli.id("bientity_condition"), SpellEngineCompat::matchesBiEntityCondition);
    }

    public static @Nullable Holder<Spell> spell(Level level, ResourceLocation id) {
        return SpellRegistry.from(level).getHolder(net.minecraft.resources.ResourceKey.create(SpellRegistry.KEY, id)).orElse(null);
    }

    public static @Nullable ResourceLocation cooldownGroup(Holder<Spell> spell) {
        Spell value = spell.value();
        if (value.cost == null || value.cost.cooldown == null || value.cost.cooldown.group == null) return null;
        return ResourceLocation.tryBuild("group", value.cost.cooldown.group);
    }

    public static void warnUnknownSpell(ResourceLocation id) {
        warnOnce("spell:" + id, "[Apoli] apoli:cast_spell names the spell " + id + ", which is not loaded.");
    }

    public static void cast(LivingEntity caster, Holder<Spell> spellEntry) {
        cast(caster, spellEntry, null, false);
    }

    // forcedTarget: cast at that entity instead of searching. useCost: a normal cast that uses the spell's cost and starts
    // its cooldown (players only).
    public static void cast(LivingEntity caster, Holder<Spell> spellEntry, @Nullable Entity forcedTarget, boolean useCost) {
        Spell spell = spellEntry.value();
        if (spell.active == null) {
            warnOnce("passive:" + spellEntry.unwrapKey().map(k -> k.location().toString()).orElse("?"),
                "[Apoli] apoli:cast_spell can only cast active spells; " + spellEntry.unwrapKey().map(k -> k.location().toString()).orElse("?") + " is not one.");
            return;
        }
        Level level = caster.level();
        SpellTarget.SearchResult found = forcedTarget != null ? SpellTarget.SearchResult.of(forcedTarget)
            : SpellTarget.findTargets(caster, spellEntry, SpellTarget.SearchResult.empty(), true);
        if (useCost && caster instanceof Player player) {
            castWithCost(player, spellEntry, found);
            return;
        }
        List<Entity> targets = found.entities();
        if (spell.target != null && spell.target.cap > 0 && targets.size() > spell.target.cap) {
            List<Entity> sorted = new ArrayList<>(targets);
            sorted.sort(Comparator.comparingDouble(target -> target.distanceToSqr(caster)));
            targets = List.copyOf(sorted.subList(0, spell.target.cap));
            found = new SpellTarget.SearchResult(targets, found.location());
        }
        SpellExecution.ImpactContext context = new SpellExecution.ImpactContext()
            .power(SpellPower.getSpellPower(spell.school, caster))
            .target(SpellIntents.focusMode(spell));
        List<Entity> hit = targets;
        SpellDelivery.resolveAndDeliver(level, caster, spellEntry, found, context, completion -> {
            if (!completion.success()) return;
            ReleaseFx.send(level, caster, spellEntry, 1F);
            notifyCast(caster, spellEntry, hit);
        });
    }

    // Goes through Spell Engine's own release, which takes the cost and starts the cooldown. Spell Engine only performs
    // spells the player has from some source, so one is lent for the cast if needed.
    private static void castWithCost(Player player, Holder<Spell> spellEntry, SpellTarget.SearchResult targets) {
        ResourceLocation id = spellEntry.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
        if (id == null) return;
        SpellContainerSource.Owner owner = (SpellContainerSource.Owner) player;
        SpellContainerSource.Result before = owner.getSpellContainers();
        boolean lent = SpellContainerSource.getFirstSourceOfSpell(id, player) == null;
        if (lent) {
            SpellContainerSource.Result r = before == null ? SpellContainerSource.Result.EMPTY : before;
            List<SpellContainerSource.SourcedContainer> sources = new ArrayList<>(r.sources() == null ? List.of() : r.sources());
            sources.add(new SpellContainerSource.SourcedContainer("apoli:cast_spell", null,
                new SpellContainer(SpellContainer.ContentType.ANY, "", "", 1, List.of(id.toString()))));
            owner.setSpellContainers(new SpellContainerSource.Result(r.activeContainer(), r.actives(), r.passives(), r.modifiers(), sources));
        }
        try {
            SpellExecution.performSpell(player.level(), player, spellEntry, targets, SpellCast.Action.RELEASE, 1F);
        } finally {
            if (lent) owner.setSpellContainers(before);
        }
    }

    public static void notifyCast(LivingEntity caster, Holder<Spell> spell, List<Entity> targets) {
        int[] depth = NOTIFY_DEPTH.get();
        if (depth[0] > 0) return;
        depth[0]++;
        try {
            ActionOnSpellCastPower.handle(caster, spell, targets);
        } finally {
            depth[0]--;
        }
    }

    public static void onImpact(LivingEntity caster, Entity target, Holder<Spell> spell) {
        ActionOnSpellHitPower.handle(caster, target, spell);
        ActionWhenHitBySpellPower.handle(caster, target, spell);
    }

    public static void onPowersReloaded(@Nullable MinecraftServer server) {
        ENTITY_CONDITIONS.clear();
        BIENTITY_CONDITIONS.clear();
        for (Map.Entry<String, SpellHandlers.CustomImpact> entry : FUNCTION_IMPACTS.entrySet()) {
            SpellHandlers.customImpact.remove(entry.getKey(), entry.getValue());
        }
        FUNCTION_IMPACTS.clear();
        for (Map.Entry<ResourceLocation, Power> entry : ApoliPowers.view().entrySet()) {
            if (!(entry.getValue().config() instanceof FunctionPower.Cfg cfg) || !cfg.parameters().isEmpty()) continue;
            String key = entry.getKey().toString();
            if (SpellHandlers.customImpact.containsKey(key)) continue;
            ResourceLocation functionId = entry.getKey();
            SpellHandlers.CustomImpact impact = (spell, power, caster, target, context) -> runFunction(functionId, caster, target);
            SpellHandlers.customImpact.put(key, impact);
            FUNCTION_IMPACTS.put(key, impact);
        }
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) SpellsPower.sweep(player);
    }

    private static SpellHandlers.ImpactResult runFunction(ResourceLocation functionId, LivingEntity caster, @Nullable Entity target) {
        Power power = ApoliPowers.get(functionId);
        if (power == null || !(power.config() instanceof FunctionPower.Cfg cfg)) return MISSED;
        Entity subject = target != null ? target : caster;
        FunctionPower.run(functionId, cfg, Map.of(), EntityCtx.of(subject, subject.level()));
        return HIT;
    }

    private static boolean hasActivePower(SpellEntityPredicates.Input input) {
        String param = input.param();
        if (param == null || input.entity() == null) return false;
        ResourceLocation id = POWER_IDS.computeIfAbsent(param, raw -> Optional.ofNullable(ResourceLocation.tryParse(raw))).orElse(null);
        if (id == null) return false;
        Entity entity = input.entity();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || !container.hasPower(id) || container.isSuppressed(id)) return false;
        Power power = ApoliPowers.get(id);
        return power != null && (power.condition().isEmpty() || power.condition().get().test(EntityCtx.of(entity, entity.level())));
    }

    private static boolean matchesCondition(SpellEntityPredicates.Input input) {
        if (input.entity() == null) return false;
        EntityCondition condition = parse(ENTITY_CONDITIONS, input.param(), EntityCondition.CODEC);
        return condition != null && condition.test(EntityCtx.of(input.entity(), input.entity().level()));
    }

    private static boolean matchesBiEntityCondition(SpellEntityPredicates.Input input) {
        Entity actor = input.other();
        if (input.entity() == null || actor == null) return false;
        BiEntityCondition condition = parse(BIENTITY_CONDITIONS, input.param(), BiEntityCondition.CODEC);
        return condition != null && condition.test(BiEntityCtx.of(actor, input.entity(), input.entity().level()));
    }

    private static <C> @Nullable C parse(Map<String, Optional<C>> cache, @Nullable String param, Codec<C> codec) {
        if (param == null) return null;
        Optional<C> cached = cache.get(param);
        if (cached == null) {
            try {
                cached = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(param))
                    .resultOrPartial(error -> warnOnce("condition:" + param,
                        "[Apoli] The Spell Engine predicate parameter " + param + " is not a valid Apoli condition: " + error));
            } catch (JsonParseException e) {
                warnOnce("condition:" + param, "[Apoli] The Spell Engine predicate parameter " + param + " is not valid JSON: " + e.getMessage());
                cached = Optional.empty();
            }
            cache.put(param, cached);
        }
        return cached.orElse(null);
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) Apoli.LOGGER.warn(message);
    }
}
