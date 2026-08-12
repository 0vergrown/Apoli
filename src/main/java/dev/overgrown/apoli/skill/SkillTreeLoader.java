package dev.overgrown.apoli.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class SkillTreeLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public SkillTreeLoader() {
        super(GSON, "skill_trees");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillTree> trees = new HashMap<>();
        Map<ResourceLocation, Skill> skills = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            if (!(IdWildcards.apply(e.getValue(), id) instanceof JsonObject obj)) {
                LOG.error("[Apoli] Skill tree file {} is empty or not a JSON object — skipping.", id);
                continue;
            }
            if (obj.has("parent")) {
                if (obj.has("power") && !obj.has("powers")) {
                    obj.add("powers", obj.get("power"));
                    obj.remove("power");
                }
                Skill.fileCodec(id).codec().parse(JsonOps.INSTANCE, obj)
                    .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill {}: {}", id, err))
                    .ifPresent(skill -> skills.put(id, skill));
                continue;
            }
            SkillTree.codec(id).codec().parse(JsonOps.INSTANCE, obj)
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill tree {}: {}", id, err))
                .ifPresent(tree -> trees.put(id, tree));
        }

        SkillRegistry.setTrees(trees);
        SkillRegistry.setFileSkills(skills);
        LOG.info("[Apoli] Loaded {} skill tree(s) and {} skill(s) from skill_trees files.", trees.size(), skills.size());
    }
}
