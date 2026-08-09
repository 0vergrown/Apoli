package dev.overgrown.apoli.skill;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillRegistry {
    private static final Logger LOG = LogUtils.getLogger();

    private SkillRegistry() {}

    private static volatile Map<ResourceLocation, SkillTree> trees = Map.of();
    private static volatile Map<ResourceLocation, Skill> powerSkills = Map.of();
    private static volatile Map<ResourceLocation, Skill> fileSkills = Map.of();

    private static volatile Map<ResourceLocation, Skill> byId = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> children = Map.of();
    private static volatile List<ResourceLocation> roots = List.of();
    private static volatile Map<ResourceLocation, ResourceLocation> rootCache = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> orphanedSkills = Map.of();

    public static void setTrees(Map<ResourceLocation, SkillTree> loaded) {
        trees = Map.copyOf(loaded);
        rebuild();
    }

    public static void setPowerSkills(Map<ResourceLocation, Skill> skills) {
        powerSkills = Map.copyOf(skills);
        rebuild();
    }

    public static void setFileSkills(Map<ResourceLocation, Skill> skills) {
        fileSkills = Map.copyOf(skills);
        rebuild();
    }

    private static synchronized void rebuild() {
        Map<ResourceLocation, SkillTree> treeMap = trees;
        Map<ResourceLocation, Skill> candidates = new LinkedHashMap<>(fileSkills);
        candidates.putAll(powerSkills);

        Map<ResourceLocation, ResourceLocation> rc = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> orphans = new LinkedHashMap<>();
        Map<ResourceLocation, Skill> valid = new LinkedHashMap<>();
        for (Skill skill : candidates.values()) {
            ResourceLocation cur = skill.id();
            ResourceLocation root = null;
            int guard = 0;
            while (guard++ <= 256) {
                Skill s = candidates.get(cur);
                if (s == null) break;
                ResourceLocation parent = s.parent();
                if (treeMap.containsKey(parent)) {
                    root = parent;
                    break;
                }
                if (!candidates.containsKey(parent)) break;
                cur = parent;
            }
            if (root == null) {
                orphans.put(skill.id(), skill.parent());
                continue;
            }
            valid.put(skill.id(), skill);
            rc.put(skill.id(), root);
        }

        Map<ResourceLocation, List<ResourceLocation>> kids = new HashMap<>();
        for (Skill skill : valid.values()) {
            kids.computeIfAbsent(skill.parent(), k -> new ArrayList<>()).add(skill.id());
        }
        java.util.Comparator<ResourceLocation> byOrder = java.util.Comparator.comparingInt(id -> valid.get(id).order());
        for (List<ResourceLocation> siblings : kids.values()) siblings.sort(byOrder);

        List<ResourceLocation> rootList = new ArrayList<>(treeMap.keySet());
        rootList.sort(java.util.Comparator.comparingInt(id -> treeMap.get(id).order()));

        orphanedSkills = Map.copyOf(orphans);
        byId = Map.copyOf(valid);
        children = Map.copyOf(kids);
        roots = List.copyOf(rootList);
        rootCache = Map.copyOf(rc);
    }

    public static void reportOrphanedSkills() {
        Map<ResourceLocation, ResourceLocation> orphans = orphanedSkills;
        if (orphans.isEmpty()) return;
        for (Map.Entry<ResourceLocation, ResourceLocation> e : orphans.entrySet()) {
            LOG.warn("[Apoli] Skill {} has no skill tree at the top of its parent chain (parent '{}'); it will not appear anywhere. Point its 'parent' at a skill_trees file id or another skill.",
                e.getKey(), e.getValue());
        }
    }

    @Nullable
    public static SkillTree tree(ResourceLocation id) {
        return trees.get(id);
    }

    public static Collection<SkillTree> trees() {
        return trees.values();
    }

    @Nullable
    public static Skill get(ResourceLocation id) {
        return byId.get(id);
    }

    public static Collection<Skill> all() {
        return byId.values();
    }

    public static List<ResourceLocation> roots() {
        return roots;
    }

    public static List<ResourceLocation> childrenOf(ResourceLocation id) {
        return children.getOrDefault(id, List.of());
    }

    public static ResourceLocation rootOf(ResourceLocation id) {
        return rootCache.getOrDefault(id, id);
    }
}
