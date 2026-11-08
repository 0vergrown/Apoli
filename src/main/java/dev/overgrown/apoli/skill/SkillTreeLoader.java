package dev.overgrown.apoli.skill;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class SkillTreeLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "skill_trees";
    private volatile Map<ResourceLocation, JsonDataFiles.Source> sources = Map.of();

    public SkillTreeLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, JsonDataFiles.Source> found = new HashMap<>();
        Map<ResourceLocation, JsonElement> loaded = JsonDataFiles.load(resourceManager, DIR, LOG, found);
        sources = found;
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillTree> trees = new HashMap<>();
        Map<ResourceLocation, Skill> skills = new HashMap<>();
        Map<ResourceLocation, JsonDataFiles.Source> files = sources;

        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            Dynamic<JsonElement> file = IdWildcards.apply(new Dynamic<>(dev.overgrown.apoli.codec.ApoliOps.of(JsonOps.INSTANCE), e.getValue()), id);
            if (file.getMapValues().result().isEmpty()) {
                dev.overgrown.apoli.loader.LoadProblems.error("skill tree", id, files.get(id), "The file is empty or isn't a JSON object, so it was skipped.");
                continue;
            }
            if (file.get("parent").result().isPresent()) {
                Dynamic<JsonElement> power = file.get("power").result().orElse(null);
                if (power != null && file.get("powers").result().isEmpty()) {
                    file = file.remove("power").set("powers", power);
                }
                dev.overgrown.apoli.loader.LoadProblems.parse("skill", id, files.get(id), "$", Skill.fileCodec(id).codec(), file)
                    .ifPresent(skill -> skills.put(id, skill));
                continue;
            }
            dev.overgrown.apoli.loader.LoadProblems.parse("skill tree", id, files.get(id), "$", SkillTree.codec(id).codec(), file)
                .ifPresent(tree -> trees.put(id, tree));
        }

        SkillRegistry.setTrees(trees);
        SkillRegistry.setFileSkills(skills);
        LOG.info("[Apoli] Loaded {} skill tree(s) and {} skill(s) from skill_trees files.", trees.size(), skills.size());
    }
}
