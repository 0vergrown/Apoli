package dev.overgrown.apoli.skill;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class SkillRegistry {
    private SkillRegistry() {}

    private static volatile Map<ResourceLocation, Skill> fileSkills = Map.of();
    private static volatile Map<ResourceLocation, Skill> powerSkills = Map.of();

    private static volatile Map<ResourceLocation, Skill> byId = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> children = Map.of();
    private static volatile List<ResourceLocation> roots = List.of();
    private static volatile Map<ResourceLocation, ResourceLocation> rootCache = Map.of();

    public static void setFileSkills(Map<ResourceLocation, Skill> skills) {
        fileSkills = Map.copyOf(skills);
        rebuild();
    }

    public static void setPowerSkills(Map<ResourceLocation, Skill> skills) {
        powerSkills = Map.copyOf(skills);
        rebuild();
    }

    private static synchronized void rebuild() {
        Map<ResourceLocation, Skill> map = new LinkedHashMap<>(powerSkills);
        map.putAll(fileSkills);

        Map<ResourceLocation, List<ResourceLocation>> kids = new HashMap<>();
        List<ResourceLocation> rootList = new ArrayList<>();
        for (Skill skill : map.values()) {
            if (skill.parent().isPresent() && map.containsKey(skill.parent().get())) {
                kids.computeIfAbsent(skill.parent().get(), k -> new ArrayList<>()).add(skill.id());
            } else {
                rootList.add(skill.id());
            }
        }
        java.util.Comparator<ResourceLocation> byOrder = java.util.Comparator.comparingInt(id -> map.get(id).order());
        for (List<ResourceLocation> siblings : kids.values()) siblings.sort(byOrder);
        rootList.sort(byOrder);
        Map<ResourceLocation, ResourceLocation> rc = new HashMap<>();
        for (ResourceLocation id : map.keySet()) {
            ResourceLocation cur = id;
            int guard = 0;
            while (true) {
                Skill s = map.get(cur);
                if (s == null || s.parent().isEmpty() || !map.containsKey(s.parent().get()) || guard++ > 256) break;
                cur = s.parent().get();
            }
            rc.put(id, cur);
        }
        byId = Map.copyOf(map);
        children = Map.copyOf(kids);
        roots = List.copyOf(rootList);
        rootCache = Map.copyOf(rc);
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
