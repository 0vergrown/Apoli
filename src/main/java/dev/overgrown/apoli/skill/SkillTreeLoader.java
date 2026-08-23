package dev.overgrown.apoli.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
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
            Dynamic<JsonElement> file = IdWildcards.apply(new Dynamic<>(JsonOps.INSTANCE, e.getValue()), id);
            if (file.getMapValues().result().isEmpty()) {
                LOG.error("[Apoli] Skill tree file {} is empty or not a JSON object — skipping.", id);
                continue;
            }
            if (file.get("parent").result().isPresent()) {
                Dynamic<JsonElement> power = file.get("power").result().orElse(null);
                if (power != null && file.get("powers").result().isEmpty()) {
                    file = file.remove("power").set("powers", power);
                }
                Skill.fileCodec(id).codec().parse(file)
                    .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill {}: {}", id, err))
                    .ifPresent(skill -> skills.put(id, skill));
                continue;
            }
            SkillTree.codec(id).codec().parse(file)
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill tree {}: {}", id, err))
                .ifPresent(tree -> trees.put(id, tree));
        }

        SkillRegistry.setTrees(trees);
        SkillRegistry.setFileSkills(skills);
        LOG.info("[Apoli] Loaded {} skill tree(s) and {} skill(s) from skill_trees files.", trees.size(), skills.size());
    }
}
