package dev.overgrown.apoli.loader;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.alias.AliasDefault;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.condition.StaticCondition;
import dev.overgrown.apoli.dev.DevMode;
import dev.overgrown.apoli.macros.MacroExpander;
import dev.overgrown.apoli.macros.MacroReport;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.LegacyPowerShapes;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApoliReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "powers";
    private static final ResourceLocation MULTIPLE = Apoli.id("multiple");
    private static final String KIND = "power";

    private MinecraftServer server;
    private volatile Map<ResourceLocation, JsonDataFiles.Source> sources = Map.of();

    private record Origin(ResourceLocation file, String rootPath) {}

    public ApoliReloadListener() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, JsonDataFiles.Source> found = new HashMap<>();
        Map<ResourceLocation, JsonElement> loaded = JsonDataFiles.load(resourceManager, DIR, LOG, found);
        sources = found;
        return loaded;
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        MacroExpander<JsonElement> macros = MacroExpander.collect(JsonOps.INSTANCE, data);
        Map<ResourceLocation, JsonDataFiles.Source> files = sources;
        Map<ResourceLocation, Dynamic<JsonElement>> expanded = new LinkedHashMap<>(data.size());
        Map<ResourceLocation, Origin> origins = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            try {
                Dynamic<JsonElement> power = macros.expand(id, new Dynamic<>(JsonOps.INSTANCE, e.getValue()));
                if (power == null || !loadConditionPasses(power, id, files.get(id))) continue;
                expandMultiples(id, power, expanded, origins, files.get(id));
            } catch (Exception ex) {
                LoadProblems.error(KIND, id, files.get(id), "Couldn't expand the macros in this power: " + ex.getMessage());
            }
        }
        MacroReport macroReport = macros.finish();

        Map<ResourceLocation, Power> loaded = new HashMap<>(expanded.size());
        int failed = 0;
        for (Map.Entry<ResourceLocation, Dynamic<JsonElement>> e : expanded.entrySet()) {
            ResourceLocation id = e.getKey();
            Dynamic<JsonElement> power = prepare(e.getValue(), id);
            Origin origin = origins.get(id);
            JsonDataFiles.Source source = files.get(origin == null ? id : origin.file());
            Power parsed = LoadProblems.parse(KIND, id, source, origin == null ? "$" : origin.rootPath(),
                Power.CODEC, dev.overgrown.apoli.codec.ApoliOps.attach(power)).orElse(null);
            if (parsed == null) {
                failed++;
                continue;
            }
            loaded.put(id, parsed);
        }
        ApoliPowers.replaceAll(loaded);
        dev.overgrown.apoli.data.MacroArguments.resetWarnings();
        int warnings = LoadProblems.count(KIND, false);
        if (failed == 0 && warnings == 0) {
            LOG.info("[Apoli] Loaded {} power{}.", loaded.size(), loaded.size() == 1 ? "" : "s");
        } else {
            LOG.warn("[Apoli] Loaded {} power{}. {} failed to load and there {} {} warning{}; each one is logged above with its file and line.",
                loaded.size(), loaded.size() == 1 ? "" : "s", failed, warnings == 1 ? "was" : "were", warnings, warnings == 1 ? "" : "s");
        }

        Map<ResourceLocation, dev.overgrown.apoli.skill.Skill> powerSkills = new HashMap<>();
        for (Map.Entry<ResourceLocation, Power> e : loaded.entrySet()) {
            e.getValue().toSkill(e.getKey()).ifPresent(skill -> powerSkills.put(e.getKey(), skill));
        }
        dev.overgrown.apoli.skill.SkillRegistry.setPowerSkills(powerSkills);

        if (server != null) {
            ApoliNetwork.broadcastPowers(server);
            DevMode.announce(server, macroReport.lines());
        }
    }

    public static <T> Dynamic<T> prepare(Dynamic<T> power, ResourceLocation id) {
        Dynamic<T> out = IdWildcards.apply(power, id);
        out = LegacyPowerShapes.apply(out);
        out = applyAliasFieldRenames(out);
        return applyAliasDefaults(out);
    }

    private static <T> void expandMultiples(ResourceLocation id, Dynamic<T> power,
                                            Map<ResourceLocation, Dynamic<T>> out,
                                            Map<ResourceLocation, Origin> origins,
                                            JsonDataFiles.Source source) {
        Map<Dynamic<T>, Dynamic<T>> fields = power.getMapValues().result().orElse(null);
        if (fields == null || !isMultiple(power)) {
            out.put(id, power);
            return;
        }
        List<ResourceLocation> subIds = new ArrayList<>();
        Dynamic<T> superPower = power.emptyMap();
        for (Map.Entry<Dynamic<T>, Dynamic<T>> field : fields.entrySet()) {
            String key = field.getKey().asString().result().orElse(null);
            if (key == null) continue;
            Dynamic<T> value = field.getValue();
            if (MultiplePower.RESERVED_FIELDS.contains(key)) {
                superPower = superPower.set(key, value);
                continue;
            }
            if (value.getMapValues().result().isEmpty()) {
                LoadProblems.warn(KIND, id, source, "'" + key + "' is not a field of apoli:multiple and is not an object, "
                    + "so it can't be a sub-power either. It is ignored.", "$." + key);
                continue;
            }
            ResourceLocation subId = subPowerId(id, key);
            if (subId == null) {
                LoadProblems.error(KIND, id, source, "The sub-power key '" + key + "' would make an invalid identifier, so it was skipped.", "$." + key);
                continue;
            }
            Dynamic<T> substituted = IdWildcards.apply(value, id);
            if (substituted.get("type").result().isEmpty()) {
                LoadProblems.error(KIND, id, source, "The sub-power '" + key + "' has no 'type' field, so it was skipped. "
                    + "If it was meant as power data, it is not a field of apoli:multiple.", "$." + key);
                continue;
            }
            if (isMultiple(substituted)) {
                LoadProblems.error(KIND, id, source, "The sub-power '" + key + "' is another apoli:multiple, which isn't allowed, so it was skipped.", "$." + key);
                continue;
            }
            if (!loadConditionPasses(substituted, subId, source)) continue;
            if (out.containsKey(subId)) {
                LoadProblems.warn(KIND, id, source, "The sub-power '" + key + "' becomes " + subId
                    + ", which another power already uses. The sub-power replaces it.", "$." + key);
            }
            out.put(subId, substituted);
            origins.put(subId, new Origin(id, "$." + key));
            subIds.add(subId);
        }
        superPower = superPower.set("type", power.createString(MULTIPLE.toString()));
        superPower = superPower.set("sub_powers",
            power.createList(subIds.stream().map(sub -> power.createString(sub.toString()))));
        out.put(id, superPower);
    }

    private static <T> Dynamic<T> applyAliasFieldRenames(Dynamic<T> power) {
        ResourceLocation aliasId = declaredType(power);
        if (aliasId == null) return power;
        Map<String, String> renames = PowerTypeRegistry.aliasFieldRenames(aliasId);
        if (renames.isEmpty() && NamespaceAlias.hasAlias(aliasId.getNamespace())) {
            renames = PowerTypeRegistry.aliasFieldRenames(NamespaceAlias.resolve(aliasId));
        }
        if (renames.isEmpty()) return power;
        Dynamic<T> out = power;
        for (Map.Entry<String, String> rename : renames.entrySet()) {
            Dynamic<T> value = out.get(rename.getKey()).result().orElse(null);
            if (value == null || out.get(rename.getValue()).result().isPresent()) continue;
            out = out.remove(rename.getKey()).set(rename.getValue(), value);
        }
        return out;
    }

    private static <T> Dynamic<T> applyAliasDefaults(Dynamic<T> power) {
        ResourceLocation aliasId = declaredType(power);
        if (aliasId == null) return power;
        List<AliasDefault<?>> defaults = PowerTypeRegistry.aliasDefaults(aliasId);
        if (defaults.isEmpty() && NamespaceAlias.hasAlias(aliasId.getNamespace())) {
            defaults = PowerTypeRegistry.aliasDefaults(NamespaceAlias.resolve(aliasId));
        }
        if (defaults.isEmpty()) return power;
        DynamicOps<T> ops = power.getOps();
        Dynamic<T> out = power;
        for (AliasDefault<?> def : defaults) {
            if (out.get(def.field()).result().isPresent()) continue;
            T encoded = def.encode(ops).resultOrPartial(err ->
                LOG.error("[Apoli] Could not build the default '{}' for {}: {}", def.field(), aliasId, err)).orElse(null);
            if (encoded != null) out = out.set(def.field(), new Dynamic<>(ops, encoded));
        }
        return out;
    }

    public static <T> boolean loadConditionPasses(Dynamic<T> power, ResourceLocation id) {
        return loadConditionPasses(power, id, null);
    }

    public static <T> boolean loadConditionPasses(Dynamic<T> power, ResourceLocation id, JsonDataFiles.Source source) {
        Dynamic<T> declared = power.get("load_condition").result().orElse(null);
        if (declared == null) return true;
        StaticCondition condition = StaticCondition.CODEC.parse(declared)
            .resultOrPartial(err -> LoadProblems.warn(KIND, id, source,
                "The load_condition couldn't be read, so it is ignored: " + dev.overgrown.apoli.codec.ErrorLocator.tidy(err), "$.load_condition"))
            .orElse(null);
        if (condition == null) return true;
        if (condition.test()) return true;
        LOG.debug("[Apoli] Skipping power {} — its load_condition does not hold on this installation.", id);
        return false;
    }

    private static <T> ResourceLocation declaredType(Dynamic<T> power) {
        String declared = power.get("type").asString().result().orElse(null);
        return declared == null ? null : ResourceLocation.tryParse(declared);
    }

    private static <T> boolean isMultiple(Dynamic<T> power) {
        ResourceLocation declared = declaredType(power);
        return declared != null && MULTIPLE.equals(PowerTypeRegistry.resolveId(declared));
    }

    private static ResourceLocation subPowerId(ResourceLocation superId, String key) {
        return ResourceLocation.tryParse(superId.getNamespace() + ":" + superId.getPath() + "_" + key);
    }
}
