package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApoliReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "powers";
    private static final ResourceLocation MULTIPLE = Apoli.id("multiple");

    private MinecraftServer server;

    public ApoliReloadListener() {
        super(GSON, DIR);
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> expanded = new LinkedHashMap<>(data.size());
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            try {
                expandMultiples(id, e.getValue(), expanded);
            } catch (Exception ex) {
                LOG.error("[Apoli] Failed to expand power {}: {}", id, ex.getMessage());
            }
        }

        Map<ResourceLocation, Power> loaded = new HashMap<>(expanded.size());
        for (Map.Entry<ResourceLocation, JsonElement> e : expanded.entrySet()) {
            ResourceLocation id = e.getKey();
            JsonElement json = IdWildcards.apply(e.getValue(), id);
            json = applyAliasFieldRenames(json);
            json = applyAliasDefaults(json);
            dev.overgrown.apoli.codec.LoggedOptionalField.setContext(id);
            try {
                Power.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> LOG.error("Failed to parse power {}: {}", id, err))
                    .ifPresent(power -> loaded.put(id, power));
            } finally {
                dev.overgrown.apoli.codec.LoggedOptionalField.clearContext();
            }
        }
        ApoliPowers.replaceAll(loaded);
        LOG.info("[Apoli] Loaded {} power(s).", loaded.size());

        Map<ResourceLocation, dev.overgrown.apoli.skill.Skill> powerSkills = new HashMap<>();
        for (Map.Entry<ResourceLocation, Power> e : loaded.entrySet()) {
            e.getValue().toSkill(e.getKey()).ifPresent(skill -> powerSkills.put(e.getKey(), skill));
        }
        dev.overgrown.apoli.skill.SkillRegistry.setPowerSkills(powerSkills);

        if (server != null) {
            ApoliNetwork.broadcastPowers(server);
        }
    }

    private static void expandMultiples(ResourceLocation id, JsonElement json, Map<ResourceLocation, JsonElement> out) {
        if (!(json instanceof JsonObject obj) || !isMultiple(obj)) {
            out.put(id, json);
            return;
        }
        List<ResourceLocation> subIds = new ArrayList<>();
        JsonObject superJson = new JsonObject();
        for (Map.Entry<String, JsonElement> field : obj.entrySet()) {
            String key = field.getKey();
            JsonElement value = field.getValue();
            if (MultiplePower.RESERVED_FIELDS.contains(key)) {
                superJson.add(key, value);
                continue;
            }
            if (!(value instanceof JsonObject subObj)) {
                continue;
            }
            ResourceLocation subId = subPowerId(id, key);
            if (subId == null) {
                LOG.error("[Apoli] Sub-power key '{}' on {} would produce an invalid identifier — skipping.", key, id);
                continue;
            }
            JsonObject substituted = (JsonObject) IdWildcards.apply(subObj.deepCopy(), id);
            if (!substituted.has("type")) {
                LOG.error("[Apoli] Sub-power '{}' of {} has no 'type' field — skipping. (If this was meant to be power data rather than a sub-power, it is not a recognized field of apoli:multiple.)", key, id);
                continue;
            }
            if (isMultiple(substituted)) {
                LOG.error("[Apoli] Nested apoli:multiple is not allowed (sub-power '{}' of {}) — skipping.", key, id);
                continue;
            }
            if (out.containsKey(subId)) {
                LOG.warn("[Apoli] Sub-power id {} (from {}/{}) collides with an existing power — overwriting.", subId, id, key);
            }
            out.put(subId, substituted);
            subIds.add(subId);
        }
        superJson.addProperty("type", MULTIPLE.toString());
        JsonArray subPowers = new JsonArray();
        for (ResourceLocation sub : subIds) subPowers.add(sub.toString());
        superJson.add("sub_powers", subPowers);
        out.put(id, superJson);
    }

    private static JsonElement applyAliasFieldRenames(JsonElement json) {
        if (!(json instanceof JsonObject obj)) return json;
        JsonElement typeEl = obj.get("type");
        if (typeEl == null || !typeEl.isJsonPrimitive()) return json;
        ResourceLocation aliasId = ResourceLocation.tryParse(typeEl.getAsString());
        if (aliasId == null) return json;
        Map<String, String> renames = PowerTypeRegistry.aliasFieldRenames(aliasId);
        if (renames.isEmpty() && NamespaceAlias.hasAlias(aliasId.getNamespace())) {
            renames = PowerTypeRegistry.aliasFieldRenames(NamespaceAlias.resolve(aliasId));
        }
        if (renames.isEmpty()) return json;
        for (Map.Entry<String, String> rename : renames.entrySet()) {
            String oldName = rename.getKey();
            String newName = rename.getValue();
            if (obj.has(oldName) && !obj.has(newName)) {
                obj.add(newName, obj.get(oldName));
                obj.remove(oldName);
            }
        }
        return obj;
    }

    private static JsonElement applyAliasDefaults(JsonElement json) {
        if (!(json instanceof JsonObject obj)) return json;
        JsonElement typeEl = obj.get("type");
        if (typeEl == null || !typeEl.isJsonPrimitive()) return json;
        ResourceLocation aliasId = ResourceLocation.tryParse(typeEl.getAsString());
        if (aliasId == null) return json;
        JsonObject defaults = PowerTypeRegistry.aliasDefaults(aliasId);
        if ((defaults == null || defaults.size() == 0) && NamespaceAlias.hasAlias(aliasId.getNamespace())) {
            defaults = PowerTypeRegistry.aliasDefaults(NamespaceAlias.resolve(aliasId));
        }
        if (defaults == null || defaults.size() == 0) return json;
        for (Map.Entry<String, JsonElement> def : defaults.entrySet()) {
            if (!obj.has(def.getKey())) obj.add(def.getKey(), def.getValue());
        }
        return obj;
    }

    private static boolean isMultiple(JsonObject obj) {
        JsonElement type = obj.get("type");
        if (type == null || !type.isJsonPrimitive()) return false;
        ResourceLocation parsed = ResourceLocation.tryParse(type.getAsString());
        if (parsed == null) return false;
        return MULTIPLE.equals(PowerTypeRegistry.resolveId(parsed));
    }

    private static ResourceLocation subPowerId(ResourceLocation superId, String key) {
        return ResourceLocation.tryParse(superId.getNamespace() + ":" + superId.getPath() + "_" + key);
    }
}
