package dev.overgrown.apoli.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillTreeLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final List<String> LEGACY_SKILL_FIELDS = List.of(
        "parent", "power", "powers", "cost", "excludes", "condition", "visibility_condition");

    public SkillTreeLoader() {
        super(GSON, "skill_trees");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillTree> loaded = new HashMap<>(data.size());
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            if (e.getValue() instanceof JsonObject obj) {
                for (String legacy : LEGACY_SKILL_FIELDS) {
                    if (obj.has(legacy)) {
                        LOG.warn("[Apoli] Skill tree {} declares '{}' — skill_trees files now only define the tree itself; individual skills belong in the 'skill' data of their power files. The field is ignored.",
                            id, legacy);
                    }
                }
            }
            SkillTree.codec(id).codec().parse(JsonOps.INSTANCE, e.getValue())
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill tree {}: {}", id, err))
                .ifPresent(tree -> loaded.put(id, tree));
        }
        SkillRegistry.setTrees(loaded);
        LOG.info("[Apoli] Loaded {} skill tree(s).", loaded.size());
    }
}
