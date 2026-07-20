package dev.overgrown.apoli.skill;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class SkillTrees {
    private SkillTrees() {}

    public static final ResourceLocation POWER_SOURCE = Apoli.id("skill_tree");

    public static boolean treeAvailable(SkillData data, ResourceLocation treeId) {
        SkillTree tree = SkillRegistry.tree(treeId);
        if (tree == null) return false;
        return tree.autoGrant() || data.hasTree(treeId);
    }

    public static boolean grantTree(ServerPlayer player, ResourceLocation treeId) {
        if (SkillRegistry.tree(treeId) == null) return false;
        SkillData data = SkillDataAttachment.get(player);
        if (!data.grantTree(treeId)) return false;
        reconcilePowers(player);
        ApoliNetwork.sendSkillState(player);
        return true;
    }

    public static boolean revokeTree(ServerPlayer player, ResourceLocation treeId) {
        SkillData data = SkillDataAttachment.get(player);
        if (!data.revokeTree(treeId)) return false;
        reconcilePowers(player);
        ApoliNetwork.sendSkillState(player);
        return true;
    }

    public static void grantOnJoin(ServerPlayer player) {
        reconcilePowers(player);
    }

    private static Set<ResourceLocation> desiredPowers(ServerPlayer player) {
        SkillData data = SkillDataAttachment.get(player);
        Set<ResourceLocation> desired = new HashSet<>();
        for (SkillTree tree : SkillRegistry.trees()) {
            if (!treeAvailable(data, tree.id())) continue;
            desired.addAll(tree.defaultPowers());
        }
        for (ResourceLocation skillId : data.purchasedView()) {
            Skill skill = SkillRegistry.get(skillId);
            if (skill == null || !treeAvailable(data, SkillRegistry.rootOf(skillId))) continue;
            desired.addAll(skill.powers());
        }
        return desired;
    }

    private static void reconcilePowers(ServerPlayer player) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        Set<ResourceLocation> desired = desiredPowers(player);
        Set<ResourceLocation> current = new HashSet<>();
        for (ResourceLocation power : container.allPowers()) {
            if (container.sourcesOf(power).contains(POWER_SOURCE)) current.add(power);
        }
        for (ResourceLocation power : current) if (!desired.contains(power)) container.removePower(power, POWER_SOURCE);
        for (ResourceLocation power : desired) if (!current.contains(power)) container.addPower(power, POWER_SOURCE);
    }

    public static boolean parentSatisfied(SkillData data, Skill skill) {
        ResourceLocation parentId = skill.parent();
        if (SkillRegistry.tree(parentId) != null) return true;
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

        ResourceLocation root = SkillRegistry.rootOf(skillId);
        if (!treeAvailable(data, root)) return false;
        if (excluded(data, skillId)) return false;
        if (data.getPoints(root) < skill.cost()) return false;
        EntityCtx ctx = new EntityCtx(player, player.level());
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

    public static boolean canRefund(SkillData data, ResourceLocation skillId) {
        if (!data.isPurchased(skillId)) return false;
        Skill skill = SkillRegistry.get(skillId);
        if (skill == null) return false;
        SkillTree tree = SkillRegistry.tree(SkillRegistry.rootOf(skillId));
        if (tree == null || !tree.refundable()) return false;
        for (ResourceLocation child : SkillRegistry.childrenOf(skillId)) {
            if (data.isPurchased(child)) return false;
        }
        return true;
    }

    public static boolean tryRefund(ServerPlayer player, ResourceLocation skillId, boolean force) {
        SkillData data = SkillDataAttachment.get(player);
        if (force ? !data.isPurchased(skillId) : !canRefund(data, skillId)) return false;
        Skill skill = SkillRegistry.get(skillId);
        data.purchasedView().remove(skillId);
        if (skill != null) data.addPoints(SkillRegistry.rootOf(skillId), skill.cost());
        reconcilePowers(player);
        return true;
    }

    public static boolean forcePurchase(ServerPlayer player, ResourceLocation skillId) {
        Skill skill = SkillRegistry.get(skillId);
        if (skill == null || skill.powers().isEmpty()) return false;
        SkillData data = SkillDataAttachment.get(player);
        if (data.isPurchased(skillId)) return false;
        data.purchase(skillId);
        reconcilePowers(player);
        return true;
    }

    public record Visibility(Set<ResourceLocation> hidden, Set<ResourceLocation> locked) {}

    public static Visibility computeVisibility(ServerPlayer player) {
        SkillData data = SkillDataAttachment.get(player);
        EntityCtx ctx = new EntityCtx(player, player.level());
        Set<ResourceLocation> hidden = new HashSet<>();
        Set<ResourceLocation> locked = new HashSet<>();
        for (SkillTree tree : SkillRegistry.trees()) {
            if (!treeAvailable(data, tree.id())) hidden.add(tree.id());
        }
        for (Skill skill : SkillRegistry.all()) {
            ResourceLocation id = skill.id();
            boolean treeVisible = treeAvailable(data, SkillRegistry.rootOf(id));
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
}
