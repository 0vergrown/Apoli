package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillCost;
import dev.overgrown.apoli.skill.SkillFrame;
import dev.overgrown.apoli.skill.SkillPlacement;
import dev.overgrown.apoli.skill.SkillPoints;
import dev.overgrown.apoli.skill.SkillRegistry;
import dev.overgrown.apoli.skill.SkillReward;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SkillDefsSyncS2C(List<Entry> entries, boolean legacyFormat) {
    public record Entry(ResourceLocation id, Optional<ResourceLocation> parent, Component name, Component description,
                        IconData icon, List<ResourceLocation> powers, int cost, Optional<ResourceLocation> background,
                        int order, SkillFrame frame, List<ResourceLocation> excludes, List<CostItem> costItems,
                        List<PointEntry> costPoints, boolean costHidden, List<PointEntry> rewardPoints,
                        List<RewardItem> rewardItems, List<Component> rewardPowers, boolean rewardHidden,
                        Optional<SkillPlacement> placement) {
        static Entry tree(ResourceLocation id, Component name, Component description, IconData icon,
                          Optional<ResourceLocation> background, int order, SkillFrame frame) {
            return new Entry(id, Optional.empty(), name, description, icon, List.of(), 0, background, order, frame,
                List.of(), List.of(), List.of(), false, List.of(), List.of(), List.of(), false, Optional.empty());
        }
    }

    public record CostItem(ItemStack display, int count, boolean conditional, boolean hidden) {
        static CostItem of(SkillCost.ItemCost cost) {
            return new CostItem(cost.stack().copyWithCount(1), cost.count(), cost.condition().isPresent(), cost.hidden());
        }
    }

    public record PointEntry(ResourceLocation tree, int amount, boolean hidden) {}

    public record RewardItem(ItemStack display, int count, boolean hidden) {}

    public static final ResourceLocation CHANNEL = Apoli.id("skill_defs_sync");

    public SkillDefsSyncS2C(List<Entry> entries) {
        this(entries, false);
    }

    public static SkillDefsSyncS2C fromCurrent() {
        return fromCurrent(false);
    }

    public static SkillDefsSyncS2C fromCurrent(boolean legacyFormat) {
        List<Entry> entries = new ArrayList<>();
        for (dev.overgrown.apoli.skill.SkillTree tree : SkillRegistry.trees()) {
            entries.add(Entry.tree(tree.id(), tree.name(), tree.description(), tree.icon(), tree.background(),
                tree.order(), tree.frame()));
        }
        for (Skill skill : SkillRegistry.all()) {
            ResourceLocation root = SkillRegistry.rootOf(skill.id());
            List<CostItem> costItems = new ArrayList<>(skill.cost().items().size());
            for (SkillCost.ItemCost item : skill.cost().items()) costItems.add(CostItem.of(item));
            SkillReward reward = skill.reward();
            List<RewardItem> rewardItems = new ArrayList<>(reward.items().size());
            for (SkillReward.RewardItem item : reward.items()) {
                rewardItems.add(new RewardItem(item.stack().copyWithCount(1), item.stack().getCount(), item.hidden()));
            }
            entries.add(new Entry(skill.id(), Optional.of(skill.parent()), skill.name(), skill.description(),
                skill.icon(), skill.powers(), skill.cost().pointsIn(root, root), Optional.empty(), skill.order(),
                skill.frame(), skill.excludes(), costItems, pointEntries(skill.cost().points(), root),
                skill.cost().hidden(), pointEntries(reward.points(), root), rewardItems,
                reward.hidden() ? List.of() : powerNames(skill), reward.hidden(), skill.placement()));
        }
        return new SkillDefsSyncS2C(entries, legacyFormat);
    }

    private static List<PointEntry> pointEntries(List<SkillPoints> points, ResourceLocation root) {
        if (points.isEmpty()) return List.of();
        List<PointEntry> entries = new ArrayList<>(points.size());
        for (SkillPoints point : points) entries.add(new PointEntry(point.treeOr(root), point.amount(), point.hidden()));
        return entries;
    }

    private static List<Component> powerNames(Skill skill) {
        List<Component> names = new ArrayList<>();
        for (ResourceLocation powerId : skill.powers()) {
            if (powerId.equals(skill.id())) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || power.hidden()) continue;
            names.add(power.displayName(powerId));
        }
        return names;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeResourceLocation(e.id);
            buf.writeOptional(e.parent, (b, p) -> b.writeResourceLocation(p));
            buf.writeComponent(e.name);
            buf.writeComponent(e.description);
            if (legacyFormat) {
                buf.writeItem(e.icon.stack());
            } else {
                e.icon.write(buf);
            }
            buf.writeCollection(e.powers, (b, p) -> b.writeResourceLocation(p));
            buf.writeVarInt(e.cost);
            buf.writeOptional(e.background, (b, bg) -> b.writeResourceLocation(bg));
            if (!legacyFormat) {
                buf.writeVarInt(e.order);
                buf.writeEnum(e.frame);
                buf.writeCollection(e.excludes, (b, x) -> b.writeResourceLocation(x));
                buf.writeVarInt(e.costItems.size());
                for (CostItem item : e.costItems) {
                    buf.writeItem(item.display);
                    buf.writeVarInt(item.count);
                    buf.writeBoolean(item.conditional);
                    buf.writeBoolean(item.hidden);
                }
                writePoints(buf, e.costPoints);
                buf.writeBoolean(e.costHidden);
                writePoints(buf, e.rewardPoints);
                buf.writeVarInt(e.rewardItems.size());
                for (RewardItem item : e.rewardItems) {
                    buf.writeItem(item.display);
                    buf.writeVarInt(item.count);
                    buf.writeBoolean(item.hidden);
                }
                buf.writeVarInt(e.rewardPowers.size());
                for (Component name : e.rewardPowers) buf.writeComponent(name);
                buf.writeBoolean(e.rewardHidden);
                buf.writeBoolean(e.placement.isPresent());
                if (e.placement.isPresent()) {
                    buf.writeFloat(e.placement.get().x());
                    buf.writeFloat(e.placement.get().y());
                }
            }
        }
    }

    private static void writePoints(FriendlyByteBuf buf, List<PointEntry> points) {
        buf.writeVarInt(points.size());
        for (PointEntry point : points) {
            buf.writeResourceLocation(point.tree);
            buf.writeVarInt(point.amount);
            buf.writeBoolean(point.hidden);
        }
    }

    public static SkillDefsSyncS2C read(FriendlyByteBuf buf) {

        int start = buf.readerIndex();
        try {
            SkillDefsSyncS2C parsed = readEntries(buf, true);
            if (!buf.isReadable()) {
                return parsed;
            }
        } catch (Exception ignored) {
        }
        buf.readerIndex(start);
        return readEntries(buf, false);
    }

    private static SkillDefsSyncS2C readEntries(FriendlyByteBuf buf, boolean modern) {
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(Math.min(n, 1024));
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Optional<ResourceLocation> parent = buf.readOptional(b -> b.readResourceLocation());
            Component name = buf.readComponent();
            Component description = buf.readComponent();
            IconData icon = modern
                ? IconData.read(buf)
                : IconData.ofItem(buf.readItem());
            List<ResourceLocation> powers = buf.readList(b -> b.readResourceLocation());
            int cost = buf.readVarInt();
            Optional<ResourceLocation> background = buf.readOptional(b -> b.readResourceLocation());
            if (!modern) {
                List<PointEntry> legacyPoints = cost == 0 || parent.isEmpty()
                    ? List.of()
                    : List.of(new PointEntry(parent.get(), cost, false));
                entries.add(new Entry(id, parent, name, description, icon, powers, cost, background, 0, SkillFrame.TASK,
                    List.of(), List.of(), legacyPoints, false, List.of(), List.of(), List.of(), false, Optional.empty()));
                continue;
            }
            int order = buf.readVarInt();
            SkillFrame frame = buf.readEnum(SkillFrame.class);
            List<ResourceLocation> excludes = buf.readList(b -> b.readResourceLocation());
            List<CostItem> costItems = readCostItems(buf);
            List<PointEntry> costPoints = readPoints(buf);
            boolean costHidden = buf.readBoolean();
            List<PointEntry> rewardPoints = readPoints(buf);
            List<RewardItem> rewardItems = readRewardItems(buf);
            int powerNames = buf.readVarInt();
            List<Component> rewardPowers = new ArrayList<>(Math.min(powerNames, 64));
            for (int k = 0; k < powerNames; k++) rewardPowers.add(buf.readComponent());
            boolean rewardHidden = buf.readBoolean();
            Optional<SkillPlacement> placement = buf.readBoolean()
                ? Optional.of(new SkillPlacement(buf.readFloat(), buf.readFloat()))
                : Optional.empty();
            entries.add(new Entry(id, parent, name, description, icon, powers, cost, background, order, frame, excludes,
                costItems, costPoints, costHidden, rewardPoints, rewardItems, rewardPowers, rewardHidden, placement));
        }
        return new SkillDefsSyncS2C(entries);
    }

    private static List<PointEntry> readPoints(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<PointEntry> points = new ArrayList<>(Math.min(n, 64));
        for (int i = 0; i < n; i++) points.add(new PointEntry(buf.readResourceLocation(), buf.readVarInt(), buf.readBoolean()));
        return points;
    }

    private static List<CostItem> readCostItems(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<CostItem> items = new ArrayList<>(Math.min(n, 64));
        for (int i = 0; i < n; i++) {
            ItemStack display = buf.readItem();
            items.add(new CostItem(display, buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));
        }
        return items;
    }

    private static List<RewardItem> readRewardItems(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<RewardItem> items = new ArrayList<>(Math.min(n, 64));
        for (int i = 0; i < n; i++) {
            ItemStack display = buf.readItem();
            items.add(new RewardItem(display, buf.readVarInt(), buf.readBoolean()));
        }
        return items;
    }
}
