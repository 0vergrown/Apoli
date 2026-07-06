package dev.overgrown.apoli.skill;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SkillTrees {
    private SkillTrees() {}

    public static final ResourceLocation POWER_SOURCE = Apoli.id("skill_tree");

    private static final int REFRESH_INTERVAL = 20; 
    private static int refreshTick = 0;
    private static final Map<UUID, Integer> LAST_SIGNATURE = new HashMap<>();

    
    public static boolean treeAvailable(EntityCtx ctx, ResourceLocation root) {
        Skill rootSkill = SkillRegistry.get(root);
        if (rootSkill == null) return false;
        return rootSkill.condition().isEmpty() || rootSkill.condition().get().test(ctx);
    }

    public static void grantOnJoin(ServerPlayer player) {
        reconcilePowers(player);
    }

    
    private static Set<ResourceLocation> desiredPowers(ServerPlayer player, EntityCtx ctx) {
        SkillData data = SkillDataAttachment.get(player);
        Set<ResourceLocation> desired = new HashSet<>();
        for (ResourceLocation rootId : SkillRegistry.roots()) {
            Skill root = SkillRegistry.get(rootId);
            if (root == null || !treeAvailable(ctx, rootId)) continue; 
            desired.addAll(root.defaultPowers());
        }
        for (ResourceLocation skillId : data.purchasedView()) {
            Skill skill = SkillRegistry.get(skillId);
            if (skill == null || !treeAvailable(ctx, SkillRegistry.rootOf(skillId))) continue;
            desired.addAll(skill.powers());
        }
        return desired;
    }

    
    private static void reconcilePowers(ServerPlayer player) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        Set<ResourceLocation> desired = desiredPowers(player, new EntityCtx(player, player.level()));
        Set<ResourceLocation> current = new HashSet<>();
        for (ResourceLocation power : container.allPowers()) {
            if (container.sourcesOf(power).contains(POWER_SOURCE)) current.add(power);
        }
        for (ResourceLocation power : current) if (!desired.contains(power)) container.removePower(power, POWER_SOURCE);
        for (ResourceLocation power : desired) if (!current.contains(power)) container.addPower(power, POWER_SOURCE);
    }

    public static boolean parentSatisfied(SkillData data, Skill skill) {
        if (skill.parent().isEmpty()) return true;
        ResourceLocation parentId = skill.parent().get();
        if (data.isPurchased(parentId)) return true;
        Skill parent = SkillRegistry.get(parentId);
        return parent != null && parent.powers().isEmpty();
    }

    
    public static boolean excluded(SkillData data, ResourceLocation skillId) {
        Skill skill = SkillRegistry.get(skillId);
        if (skill != null) {
            for (ResourceLocation ex : skill.excludes()) if (data.isPurchased(ex)) return true;
        }
        for (ResourceLocation purchasedId : data.purchasedView()) {
            Skill p = SkillRegistry.get(purchasedId);
            if (p != null && p.excludes().contains(skillId)) return true;
        }
        return false;
    }

    public static boolean tryPurchase(ServerPlayer player, ResourceLocation skillId) {
        Skill skill = SkillRegistry.get(skillId);
        if (skill == null || skill.powers().isEmpty()) return false;
        SkillData data = SkillDataAttachment.get(player);
        if (data.isPurchased(skillId)) return false;
        if (!parentSatisfied(data, skill)) return false;

        EntityCtx ctx = new EntityCtx(player, player.level());
        ResourceLocation root = SkillRegistry.rootOf(skillId);
        if (!treeAvailable(ctx, root)) return false;            
        if (excluded(data, skillId)) return false;              
        if (data.getPoints(root) < skill.cost()) return false;  
        if (skill.condition().isPresent() && !skill.condition().get().test(ctx)) return false; 

        data.addPoints(root, -skill.cost());
        data.purchase(skillId);

        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) {
            for (ResourceLocation power : skill.powers()) container.addPower(power, POWER_SOURCE);
        }
        return true;
    }

    
    public static void resetSkills(ServerPlayer player, @Nullable ResourceLocation tree, boolean refund) {
        SkillData data = SkillDataAttachment.get(player);
        for (ResourceLocation skillId : new ArrayList<>(data.purchasedView())) {
            ResourceLocation root = SkillRegistry.rootOf(skillId);
            if (tree != null && !tree.equals(root)) continue;
            Skill skill = SkillRegistry.get(skillId);
            if (refund && skill != null) data.addPoints(root, skill.cost());
            data.purchasedView().remove(skillId);
        }
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) container.removeAllFromSource(POWER_SOURCE);
        grantOnJoin(player); 
    }

    
    public record Visibility(Set<ResourceLocation> hidden, Set<ResourceLocation> locked) {}

    public static Visibility computeVisibility(ServerPlayer player) {
        SkillData data = SkillDataAttachment.get(player);
        EntityCtx ctx = new EntityCtx(player, player.level());
        Set<ResourceLocation> hidden = new HashSet<>();
        Set<ResourceLocation> locked = new HashSet<>();
        for (Skill skill : SkillRegistry.all()) {
            ResourceLocation id = skill.id();
            boolean treeVisible = treeAvailable(ctx, SkillRegistry.rootOf(id));
            boolean selfVisible = skill.visibilityCondition().isEmpty() || skill.visibilityCondition().get().test(ctx);
            if (!treeVisible || !selfVisible) {
                hidden.add(id);
                continue;
            }
            boolean lock = skill.condition().isPresent() && !skill.condition().get().test(ctx);
            if (!lock) lock = excluded(data, id);
            if (lock) locked.add(id);
        }
        return new Visibility(hidden, locked);
    }

    
    public static void tickRefresh(MinecraftServer server) {
        if (++refreshTick < REFRESH_INTERVAL) return;
        refreshTick = 0;
        if (SkillRegistry.roots().isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EntityCtx ctx = new EntityCtx(player, player.level());
            Set<ResourceLocation> desired = desiredPowers(player, ctx);
            Visibility vis = computeVisibility(player);
            int signature = Objects.hash(desired, vis.hidden(), vis.locked());
            Integer previous = LAST_SIGNATURE.get(player.getUUID());
            if (previous != null && previous == signature) continue;
            LAST_SIGNATURE.put(player.getUUID(), signature);
            reconcilePowers(player);
            ApoliNetwork.sendSkillState(player);
        }
    }

    
    public static void forget(UUID playerId) {
        LAST_SIGNATURE.remove(playerId);
    }
}
