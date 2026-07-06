package dev.overgrown.apoli.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
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
        Map<ResourceLocation, Skill> loaded = new HashMap<>(data.size());
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            Skill.codec(id).codec().parse(JsonOps.INSTANCE, e.getValue())
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse skill {}: {}", id, err))
                .ifPresent(skill -> loaded.put(id, skill));
        }
        SkillRegistry.setFileSkills(loaded);
        LOG.info("[Apoli] Loaded {} skill-tree file(s) across {} tree(s).", loaded.size(), SkillRegistry.roots().size());
    }
}
