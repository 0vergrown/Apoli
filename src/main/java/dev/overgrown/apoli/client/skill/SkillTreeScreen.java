package dev.overgrown.apoli.client.skill;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.network.payload.BuySkillC2S;
import dev.overgrown.apoli.client.IconRenderer;
import dev.overgrown.apoli.network.payload.SkillDefsSyncS2C;
import dev.overgrown.apoli.skill.SkillFrame;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeScreen extends Screen {
    private static final ResourceLocation WINDOW = ResourceLocation.withDefaultNamespace("textures/gui/advancements/window.png");
    private static final ResourceLocation TITLE_BOX = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final ResourceLocation BOX_OBTAINED = ResourceLocation.withDefaultNamespace("advancements/box_obtained");
    private static final ResourceLocation BOX_UNOBTAINED = ResourceLocation.withDefaultNamespace("advancements/box_unobtained");
    private static final ResourceLocation[] FRAMES_OBTAINED = {
        ResourceLocation.withDefaultNamespace("advancements/task_frame_obtained"),
        ResourceLocation.withDefaultNamespace("advancements/goal_frame_obtained"),
        ResourceLocation.withDefaultNamespace("advancements/challenge_frame_obtained")
    };
    private static final ResourceLocation[] FRAMES_UNOBTAINED = {
        ResourceLocation.withDefaultNamespace("advancements/task_frame_unobtained"),
        ResourceLocation.withDefaultNamespace("advancements/goal_frame_unobtained"),
        ResourceLocation.withDefaultNamespace("advancements/challenge_frame_unobtained")
    };
    private static final ResourceLocation DEFAULT_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/andesite.png");

    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 140;
    private static final int INSIDE_X = 9;
    private static final int INSIDE_Y = 18;
    private static final int INSIDE_W = 234;
    private static final int INSIDE_H = 113;

    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 1.0F;

    private final List<ResourceLocation> roots;
    private ResourceLocation currentRoot;
    private final Map<ResourceLocation, int[]> positions = new HashMap<>();
    private List<ResourceLocation> treeSkills = new ArrayList<>();
    private double scrollX, scrollY;
    private float zoom = 1.0F;
    private boolean centered;
    private int minX, minY, maxX, maxY;
    private boolean isScrolling;

    public SkillTreeScreen() {
        super(Component.translatable("screen.apoli.skill_tree"));
        this.roots = new ArrayList<>();
        for (ResourceLocation r : ClientSkillState.roots()) {
            if (!ClientSkillState.isHidden(r)) this.roots.add(r);
        }
        this.currentRoot = roots.isEmpty() ? null : roots.get(0);
    }

    public void refreshFromState() {
        ResourceLocation previousRoot = currentRoot;
        double previousScrollX = scrollX;
        double previousScrollY = scrollY;
        boolean hadView = centered;
        this.roots.clear();
        for (ResourceLocation r : ClientSkillState.roots()) {
            if (!ClientSkillState.isHidden(r)) this.roots.add(r);
        }
        if (currentRoot == null || !roots.contains(currentRoot)) {
            this.currentRoot = roots.isEmpty() ? null : roots.get(0);
        }
        layout();
        if (hadView && currentRoot != null && currentRoot.equals(previousRoot)) {
            scrollX = previousScrollX;
            scrollY = previousScrollY;
            centered = true;
            clampScroll();
        }
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        positions.clear();
        treeSkills.clear();
        centered = false;
        minX = minY = Integer.MAX_VALUE;
        maxX = maxY = Integer.MIN_VALUE;
        if (currentRoot == null) return;
        gather(currentRoot, treeSkills);
        Map<ResourceLocation, float[]> grid = new HashMap<>();
        placeRow(currentRoot, 0, new float[]{0}, grid, null);
        for (ResourceLocation id : treeSkills) {
            SkillDefsSyncS2C.Entry e = ClientSkillState.def(id);
            if (e == null || e.placement().isEmpty()) continue;
            List<ResourceLocation> branch = new ArrayList<>();
            float row = placeRow(id, e.placement().get().x(), new float[]{0}, grid, branch);
            float shift = e.placement().get().y() - row;
            for (ResourceLocation member : branch) grid.get(member)[1] += shift;
        }
        for (Map.Entry<ResourceLocation, float[]> cell : grid.entrySet()) {
            positions.put(cell.getKey(), new int[]{Mth.floor(cell.getValue()[0] * 28f), Mth.floor(cell.getValue()[1] * 27f)});
        }
        for (ResourceLocation id : treeSkills) {
            int[] p = positions.get(id);
            if (p == null) continue;
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0] + 28);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1] + 27);
        }
    }

    private void gather(ResourceLocation id, List<ResourceLocation> out) {
        if (ClientSkillState.def(id) == null || ClientSkillState.isHidden(id)) return;
        out.add(id);
        for (ResourceLocation child : ClientSkillState.childrenOf(id)) gather(child, out);
    }

    private float placeRow(ResourceLocation id, float column, float[] leaf, Map<ResourceLocation, float[]> grid,
                           @Nullable List<ResourceLocation> branch) {
        List<ResourceLocation> children = flowChildren(id);
        float row;
        if (children.isEmpty()) {
            row = leaf[0]++;
        } else {
            float first = 0, last = 0;
            for (int k = 0; k < children.size(); k++) {
                float r = placeRow(children.get(k), column + 1, leaf, grid, branch);
                if (k == 0) first = r;
                if (k == children.size() - 1) last = r;
            }
            row = (first + last) / 2f;
        }
        grid.put(id, new float[]{column, row});
        if (branch != null) branch.add(id);
        return row;
    }

    private static List<ResourceLocation> flowChildren(ResourceLocation id) {
        List<ResourceLocation> children = ClientSkillState.childrenOf(id);
        List<ResourceLocation> flow = new ArrayList<>(children.size());
        for (ResourceLocation child : children) {
            SkillDefsSyncS2C.Entry e = ClientSkillState.def(child);
            if (e == null || e.placement().isEmpty()) flow.add(child);
        }
        return flow;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;
        renderInside(g, mouseX, mouseY, x, y);
        renderWindow(g, x, y);
        renderTooltips(g, mouseX, mouseY, x, y);
    }

    private void renderInside(GuiGraphics g, int mouseX, int mouseY, int x, int y) {
        if (currentRoot == null) {
            g.fill(x + INSIDE_X, y + INSIDE_Y, x + INSIDE_X + INSIDE_W, y + INSIDE_Y + INSIDE_H, 0xFF000000);
            g.drawCenteredString(this.font, Component.translatable("screen.apoli.skill_tree.empty"),
                x + INSIDE_X + INSIDE_W / 2, y + INSIDE_Y + INSIDE_H / 2 - 4, 0xFFFFFF);
            return;
        }
        if (!centered) {
            scrollX = INSIDE_W / (2.0 * zoom) - (maxX + minX) / 2.0;
            scrollY = INSIDE_H / (2.0 * zoom) - (maxY + minY) / 2.0;
            centered = true;
        }
        int ox = x + INSIDE_X, oy = y + INSIDE_Y;
        g.enableScissor(ox, oy, ox + INSIDE_W, oy + INSIDE_H);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0.0F);
        g.pose().scale(zoom, zoom, 1.0F);
        int k = Mth.floor(scrollX);
        int l = Mth.floor(scrollY);
        ResourceLocation bg = resolveBackground();
        int m = k % 16, n = l % 16;
        int tilesX = Mth.ceil(INSIDE_W / zoom / 16.0F) + 1;
        int tilesY = Mth.ceil(INSIDE_H / zoom / 16.0F) + 1;
        for (int a = -1; a <= tilesX; a++) {
            for (int b = -1; b <= tilesY; b++) {
                g.blit(bg, m + 16 * a, n + 16 * b, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
        drawConnectivity(g, k, l, true);
        drawConnectivity(g, k, l, false);
        drawWidgets(g, k, l);
        g.pose().popPose();
        g.disableScissor();
    }

    private ResourceLocation resolveBackground() {
        SkillDefsSyncS2C.Entry root = ClientSkillState.def(currentRoot);
        if (root != null && root.background().isPresent()) return root.background().get();
        return DEFAULT_BACKGROUND;
    }

    private void drawConnectivity(GuiGraphics g, int ox, int oy, boolean border) {
        for (ResourceLocation id : treeSkills) {
            SkillDefsSyncS2C.Entry e = ClientSkillState.def(id);
            if (e == null || e.parent().isEmpty() || e.placement().isPresent()) continue;
            int[] parent = positions.get(e.parent().get());
            int[] self = positions.get(id);
            if (parent == null || self == null) continue;
            int k = ox + parent[0] + 13;
            int l = ox + parent[0] + 26 + 4;
            int m = oy + parent[1] + 13;
            int nn = ox + self[0] + 13;
            int o = oy + self[1] + 13;
            int color = border ? 0xFF000000 : 0xFFFFFFFF;
            if (border) {
                g.hLine(l, k, m - 1, color);
                g.hLine(l + 1, k, m, color);
                g.hLine(l, k, m + 1, color);
                g.hLine(nn, l - 1, o - 1, color);
                g.hLine(nn, l - 1, o, color);
                g.hLine(nn, l - 1, o + 1, color);
                g.vLine(l - 1, o, m, color);
                g.vLine(l + 1, o, m, color);
            } else {
                g.hLine(l, k, m, color);
                g.hLine(nn, l, o, color);
                g.vLine(l, o, m, color);
            }
        }
    }

    private void drawWidgets(GuiGraphics g, int ox, int oy) {
        for (ResourceLocation id : treeSkills) {
            SkillDefsSyncS2C.Entry e = ClientSkillState.def(id);
            int[] pos = positions.get(id);
            if (e == null || pos == null) continue;
            drawFrame(g, ox + pos[0] + 3, oy + pos[1], e.frame(), isObtained(e));
            IconRenderer.renderFake(g, e.icon(), ox + pos[0] + 8, oy + pos[1] + 5);

            if (ClientSkillState.isLocked(id)) {
                g.fill(ox + pos[0] + 3, oy + pos[1], ox + pos[0] + 29, oy + pos[1] + 26, 0xB0000000);
            }
        }
    }

    private static void drawFrame(GuiGraphics g, int x, int y, SkillFrame frame, boolean obtained) {
        ResourceLocation[] frames = obtained ? FRAMES_OBTAINED : FRAMES_UNOBTAINED;
        g.blitSprite(frames[frameIndex(frame)], x, y, 26, 26);
    }

    private static int frameIndex(SkillFrame frame) {
        return switch (frame) {
            case TASK -> 0;
            case GOAL -> 1;
            case CHALLENGE -> 2;
        };
    }

    private static boolean isObtained(SkillDefsSyncS2C.Entry e) {
        return ClientSkillState.isObtained(e.id());
    }

    private void renderWindow(GuiGraphics g, int x, int y) {
        RenderSystem.enableBlend();
        g.blit(WINDOW, x, y, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        if (roots.size() > 1) drawTabs(g, x, y);

        SkillDefsSyncS2C.Entry rootDef = currentRoot == null ? null : ClientSkillState.def(currentRoot);
        Component title = rootDef != null ? rootDef.name() : getTitle();
        g.drawString(this.font, title, x + 8, y + 6, 0x404040, false);

        if (currentRoot != null) {
            Component points = Component.translatable("screen.apoli.skill_tree.points", ClientSkillState.points(currentRoot));
            int w = this.font.width(points);
            g.drawString(this.font, points, x + WINDOW_WIDTH - w - 8, y + 6, 0x404040, false);
        }
    }

    private void drawTabs(GuiGraphics g, int x, int y) {
        for (int i = 0; i < roots.size(); i++) {
            ResourceLocation r = roots.get(i);
            SkillDefsSyncS2C.Entry d = ClientSkillState.def(r);
            int tx = x + 4 + i * 29;
            int ty = y - 28 + 4;
            drawFrame(g, tx, ty, d != null ? d.frame() : SkillFrame.TASK, r.equals(currentRoot));
            if (d != null) IconRenderer.renderFake(g, d.icon(), tx + 5, ty + 5);
        }
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY, int x, int y) {
        if (currentRoot == null) return;
        int screenX = mouseX - x - INSIDE_X;
        int screenY = mouseY - y - INSIDE_Y;
        if (screenX <= 0 || screenX >= INSIDE_W || screenY <= 0 || screenY >= INSIDE_H) return;
        double localX = screenX / zoom;
        double localY = screenY / zoom;
        int k = Mth.floor(scrollX), l = Mth.floor(scrollY);
        for (ResourceLocation id : treeSkills) {
            SkillDefsSyncS2C.Entry e = ClientSkillState.def(id);
            int[] pos = positions.get(id);
            if (e == null || pos == null) continue;
            int wx = k + pos[0], wy = l + pos[1];
            if (localX >= wx && localX <= wx + 26 && localY >= wy && localY <= wy + 26) {
                g.pose().pushPose();
                g.pose().translate(x + INSIDE_X, y + INSIDE_Y, 400.0F);
                drawHover(g, e, Mth.floor(wx * zoom), Mth.floor(wy * zoom));
                g.pose().popPose();
                break;
            }
        }
    }

    private void drawHover(GuiGraphics g, SkillDefsSyncS2C.Entry e, int wx, int wy) {
        List<FormattedCharSequence> lines = tooltipLines(e);
        int boxWidth = Math.max(this.font.width(e.name()) + 32 + 5, 120);
        for (FormattedCharSequence line : lines) boxWidth = Math.max(boxWidth, this.font.width(line) + 10);

        boolean obtained = isObtained(e);
        ResourceLocation box = obtained ? BOX_OBTAINED : BOX_UNOBTAINED;

        boolean flipRight = wx + 26 + boxWidth >= INSIDE_W;
        int bx = flipRight ? wx + 26 - boxWidth : wx;
        int by = wy;

        int descPanelH = 32 + lines.size() * 9;
        boolean flipUp = INSIDE_H - by - 26 <= 6 + lines.size() * 9;
        if (!lines.isEmpty()) {
            g.blitSprite(TITLE_BOX, bx, flipUp ? by + 26 - descPanelH : by, boxWidth, descPanelH);
        }
        int half = boxWidth / 2;
        g.blitSprite(box, 200, 26, 0, 0, bx, by, half, 26);
        g.blitSprite(box, 200, 26, 200 - (boxWidth - half), 0, bx + half, by, boxWidth - half, 26);
        drawFrame(g, wx + 3, wy, e.frame(), obtained);
        g.drawString(this.font, e.name(), bx + 32, by + 9, 0xFFFFFF);
        if (!lines.isEmpty()) {
            int textY = flipUp ? by + 26 - descPanelH + 7 : by + 9 + 17;
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(this.font, lines.get(i), bx + 5, textY + i * 9, 0xAAAAAA, false);
            }
        }
        IconRenderer.renderFake(g, e.icon(), wx + 8, wy + 5);
    }

    private List<FormattedCharSequence> tooltipLines(SkillDefsSyncS2C.Entry e) {
        List<Component> parts = new ArrayList<>();
        if (!e.description().getString().isEmpty()) parts.add(e.description().copy().withStyle(ChatFormatting.GRAY));
        if (isObtained(e)) {
            if (!ClientSkillState.isGate(e)) {
                parts.add(Component.translatable("screen.apoli.skill_tree.purchased").withStyle(ChatFormatting.GREEN));
                if (ClientSkillState.canRefund(e.id())) {
                    parts.add(refundLine(e).withStyle(ChatFormatting.YELLOW));
                } else if (ClientSkillState.refundBlockedByRewards(e.id())) {
                    parts.add(Component.translatable("screen.apoli.skill_tree.refund_blocked").withStyle(ChatFormatting.RED));
                }
            }
        } else {
            addCostLines(parts, e);
            addRewardLines(parts, e);
            parts.add(statusLine(e));
        }
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component c : parts) out.addAll(this.font.split(c, 200));
        return out;
    }

    private static void addCostLines(List<Component> parts, SkillDefsSyncS2C.Entry e) {
        if (e.costPoints().isEmpty() && e.costItems().isEmpty()) {
            parts.add(Component.translatable("screen.apoli.skill_tree.cost", 0).withStyle(ChatFormatting.GOLD));
            return;
        }
        if (e.costHidden()) {
            parts.add(Component.translatable("screen.apoli.skill_tree.cost_hidden").withStyle(ChatFormatting.GOLD));
            return;
        }
        List<Component> lines = new ArrayList<>();
        boolean hiddenEntry = false;
        for (SkillDefsSyncS2C.PointEntry point : e.costPoints()) {
            if (point.hidden()) {
                hiddenEntry = true;
                continue;
            }
            lines.add(pointLine(e, point, "screen.apoli.skill_tree.cost_points", "screen.apoli.skill_tree.cost_tree_points")
                .withStyle(ChatFormatting.GOLD));
        }
        for (SkillDefsSyncS2C.CostItem item : e.costItems()) {
            if (item.hidden()) {
                hiddenEntry = true;
                continue;
            }
            boolean enough = ClientSkillState.heldCount(item) >= item.count();
            lines.add(Component.translatable("screen.apoli.skill_tree.cost_item", item.count(), item.display().getHoverName())
                .withStyle(enough ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
        if (lines.isEmpty()) {
            parts.add(Component.translatable("screen.apoli.skill_tree.cost_hidden").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!hiddenEntry && e.costItems().isEmpty() && e.costPoints().size() == 1
            && ClientSkillState.isOwnTree(e, e.costPoints().get(0))) {
            parts.add(Component.translatable("screen.apoli.skill_tree.cost", e.costPoints().get(0).amount())
                .withStyle(ChatFormatting.GOLD));
            return;
        }
        parts.add(Component.translatable("screen.apoli.skill_tree.cost_items").withStyle(ChatFormatting.GOLD));
        parts.addAll(lines);
        if (hiddenEntry) parts.add(Component.translatable("screen.apoli.skill_tree.hidden_entry").withStyle(ChatFormatting.GOLD));
    }

    private static void addRewardLines(List<Component> parts, SkillDefsSyncS2C.Entry e) {
        boolean grantsOtherPowers = false;
        for (ResourceLocation power : e.powers()) {
            if (!power.equals(e.id())) {
                grantsOtherPowers = true;
                break;
            }
        }
        if (e.rewardHidden()) {
            if (grantsOtherPowers || !e.rewardPoints().isEmpty() || !e.rewardItems().isEmpty()) {
                parts.add(Component.translatable("screen.apoli.skill_tree.rewards_hidden").withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        List<Component> lines = new ArrayList<>();
        boolean hiddenEntry = false;
        for (Component power : e.rewardPowers()) {
            lines.add(Component.translatable("screen.apoli.skill_tree.reward_power", power).withStyle(ChatFormatting.AQUA));
        }
        for (SkillDefsSyncS2C.RewardItem item : e.rewardItems()) {
            if (item.hidden()) {
                hiddenEntry = true;
                continue;
            }
            lines.add(Component.translatable("screen.apoli.skill_tree.reward_item", item.count(), item.display().getHoverName())
                .withStyle(ChatFormatting.AQUA));
        }
        for (SkillDefsSyncS2C.PointEntry point : e.rewardPoints()) {
            if (point.hidden()) {
                hiddenEntry = true;
                continue;
            }
            lines.add(pointLine(e, point, "screen.apoli.skill_tree.reward_points", "screen.apoli.skill_tree.reward_tree_points")
                .withStyle(ChatFormatting.AQUA));
        }
        if (lines.isEmpty()) {
            if (hiddenEntry) parts.add(Component.translatable("screen.apoli.skill_tree.rewards_hidden").withStyle(ChatFormatting.AQUA));
            return;
        }
        parts.add(Component.translatable("screen.apoli.skill_tree.rewards").withStyle(ChatFormatting.AQUA));
        parts.addAll(lines);
        if (hiddenEntry) parts.add(Component.translatable("screen.apoli.skill_tree.hidden_entry").withStyle(ChatFormatting.AQUA));
    }

    private static MutableComponent pointLine(SkillDefsSyncS2C.Entry e, SkillDefsSyncS2C.PointEntry point,
                                              String ownKey, String treeKey) {
        if (ClientSkillState.isOwnTree(e, point)) return Component.translatable(ownKey, point.amount());
        return Component.translatable(treeKey, point.amount(), ClientSkillState.treeName(point.tree()));
    }

    private static Component statusLine(SkillDefsSyncS2C.Entry e) {
        ResourceLocation blocker = ClientSkillState.excludedBy(e.id());
        if (blocker != null) {
            SkillDefsSyncS2C.Entry blockerDef = ClientSkillState.def(blocker);
            return Component.translatable("screen.apoli.skill_tree.excluded",
                blockerDef != null ? blockerDef.name() : Component.literal(blocker.toString()))
                .withStyle(ChatFormatting.RED);
        }
        if (!ClientSkillState.parentSatisfied(e)) {
            if (e.placement().isPresent() && e.parent().isPresent()) {
                SkillDefsSyncS2C.Entry parentDef = ClientSkillState.def(e.parent().get());
                Component parentName = parentDef != null ? parentDef.name() : Component.literal(e.parent().get().toString());
                return Component.translatable("screen.apoli.skill_tree.locked_named", parentName).withStyle(ChatFormatting.RED);
            }
            return Component.translatable("screen.apoli.skill_tree.locked").withStyle(ChatFormatting.RED);
        }
        if (ClientSkillState.isLocked(e.id())) {
            return Component.translatable("screen.apoli.skill_tree.unavailable").withStyle(ChatFormatting.RED);
        }
        if (!ClientSkillState.canAfford(e, true)) {
            return Component.translatable("screen.apoli.skill_tree.not_enough").withStyle(ChatFormatting.RED);
        }
        if (!ClientSkillState.hasCostItems(e, true)) {
            return Component.translatable("screen.apoli.skill_tree.missing_items").withStyle(ChatFormatting.RED);
        }
        if (!ClientSkillState.canAfford(e, false) || !ClientSkillState.hasCostItems(e, false)) {
            return Component.translatable("screen.apoli.skill_tree.cost_unmet").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("screen.apoli.skill_tree.click_buy").withStyle(ChatFormatting.YELLOW);
    }

    private static MutableComponent refundLine(SkillDefsSyncS2C.Entry e) {
        if (!e.rewardPoints().isEmpty() || !e.rewardItems().isEmpty()) {
            return Component.translatable("screen.apoli.skill_tree.refund_rewards");
        }
        boolean hiddenItem = false;
        for (SkillDefsSyncS2C.CostItem item : e.costItems()) hiddenItem |= item.hidden();
        boolean simplePoints = e.costPoints().isEmpty()
            || e.costPoints().size() == 1 && !e.costPoints().get(0).hidden() && ClientSkillState.isOwnTree(e, e.costPoints().get(0));
        if (e.costHidden() || hiddenItem || !simplePoints) {
            return Component.translatable("screen.apoli.skill_tree.refund_paid");
        }
        int points = e.costPoints().isEmpty() ? 0 : e.costPoints().get(0).amount();
        return Component.translatable(e.costItems().isEmpty()
            ? "screen.apoli.skill_tree.refund" : "screen.apoli.skill_tree.refund_items", points);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;
        if (button == 0 && roots.size() > 1) {
            for (int i = 0; i < roots.size(); i++) {
                int tx = x + 4 + i * 29, ty = y - 28 + 4;
                if (mouseX >= tx && mouseX <= tx + 26 && mouseY >= ty && mouseY <= ty + 26) {
                    currentRoot = roots.get(i);
                    layout();
                    return true;
                }
            }
        }
        if (button == 0 && currentRoot != null) {
            int screenX = (int) (mouseX - x - INSIDE_X);
            int screenY = (int) (mouseY - y - INSIDE_Y);
            if (screenX > 0 && screenX < INSIDE_W && screenY > 0 && screenY < INSIDE_H) {
                double localX = screenX / zoom;
                double localY = screenY / zoom;
                int k = Mth.floor(scrollX), l = Mth.floor(scrollY);
                for (ResourceLocation id : treeSkills) {
                    SkillDefsSyncS2C.Entry e = ClientSkillState.def(id);
                    int[] pos = positions.get(id);
                    if (e == null || pos == null) continue;
                    int wx = k + pos[0], wy = l + pos[1];
                    if (localX >= wx && localX <= wx + 26 && localY >= wy && localY <= wy + 26) {
                        if (hasShiftDown() && ClientSkillState.canRefund(id)) {
                            net.minecraft.client.multiplayer.ClientPacketListener connection =
                                net.minecraft.client.Minecraft.getInstance().getConnection();
                            if (connection != null && connection.hasChannel(dev.overgrown.apoli.network.payload.RefundSkillC2S.TYPE)) {
                                PacketDistributor.sendToServer(new dev.overgrown.apoli.network.payload.RefundSkillC2S(id));
                            }
                        } else if (ClientSkillState.canBuy(e)) {
                            PacketDistributor.sendToServer(new BuySkillC2S(id));
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            isScrolling = false;
            return false;
        }
        if (!isScrolling) {
            isScrolling = true;
        } else {
            scroll(dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXAmount, double scrollYAmount) {
        setZoom(zoom * (float) Math.pow(1.2, scrollYAmount));
        return true;
    }

    private void setZoom(float newZoom) {
        newZoom = Mth.clamp(newZoom, MIN_ZOOM, MAX_ZOOM);
        if (newZoom == zoom) return;
        double centerLogicalX = INSIDE_W / (2.0 * zoom) - scrollX;
        double centerLogicalY = INSIDE_H / (2.0 * zoom) - scrollY;
        zoom = newZoom;
        scrollX = INSIDE_W / (2.0 * zoom) - centerLogicalX;
        scrollY = INSIDE_H / (2.0 * zoom) - centerLogicalY;
        clampScroll();
    }

    private void scroll(double dx, double dy) {
        scrollX += dx / zoom;
        scrollY += dy / zoom;
        clampScroll();
    }

    private void clampScroll() {
        double visW = INSIDE_W / zoom;
        double visH = INSIDE_H / zoom;
        if (maxX - minX > visW) {
            scrollX = Mth.clamp(scrollX, visW - maxX, -minX);
        } else {
            scrollX = visW / 2.0 - (maxX + minX) / 2.0;
        }
        if (maxY - minY > visH) {
            scrollY = Mth.clamp(scrollY, visH - maxY, -minY);
        } else {
            scrollY = visH / 2.0 - (maxY + minY) / 2.0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
