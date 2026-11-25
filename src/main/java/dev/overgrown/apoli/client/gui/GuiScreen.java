package dev.overgrown.apoli.client.gui;

import dev.overgrown.apoli.gui.GuiElement;
import dev.overgrown.apoli.gui.GuiLayout;
import dev.overgrown.apoli.gui.GuiMenu;
import dev.overgrown.apoli.gui.GuiPlacement;
import dev.overgrown.apoli.power.builtin.GuiPower;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class GuiScreen extends AbstractContainerScreen<GuiMenu> {
    private static final int LABEL_COLOR = 0x404040;
    private static final float ENTITY_FILL = 0.75F;

    private final GuiPower.Config config;
    private final GuiLayout layout;
    private final boolean hasTitle;
    private final GuiButtonWidget[] buttons;
    private final GuiSliderWidget[] sliders;
    private final Int2ObjectOpenHashMap<Optional<LivingEntity>> dummies = new Int2ObjectOpenHashMap<>();

    public GuiScreen(GuiMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.config = menu.config();
        this.layout = menu.layout();
        this.hasTitle = !title.getString().isEmpty();
        this.imageWidth = config.width();
        this.imageHeight = config.height();
        this.inventoryLabelX = GuiMenu.inventoryLeft(imageWidth);
        this.inventoryLabelY = GuiMenu.inventoryTop(imageHeight) - 12;
        this.buttons = new GuiButtonWidget[layout.buttonCount()];
        this.sliders = new GuiSliderWidget[layout.sliderCount()];
    }

    @Override
    protected void init() {
        super.init();
        for (int k = 0; k < buttons.length; k++) {
            buttons[k] = new GuiButtonWidget(this, menu, k);
            buttons[k].place();
            addRenderableWidget(buttons[k]);
        }
        for (int k = 0; k < sliders.length; k++) {
            sliders[k] = new GuiSliderWidget(this, menu, k);
            sliders[k].place();
            addRenderableWidget(sliders[k]);
        }
    }

    public float scaleOf(GuiPlacement placement) {
        if (placement.guiScaleLock() <= 0 || minecraft == null) return 1.0F;
        return (float) (placement.guiScaleLock() / minecraft.getWindow().getGuiScale());
    }

    public int anchorX(GuiPlacement placement) {
        return leftPos + placement.anchorX(imageWidth);
    }

    public int anchorY(GuiPlacement placement) {
        return topPos + placement.anchorY(imageHeight);
    }

    public double localX(GuiPlacement placement, double mouseX) {
        return (mouseX - anchorX(placement)) / scaleOf(placement);
    }

    public void place(GuiButtonWidget widget, GuiPlacement placement) {
        float scale = scaleOf(placement);
        widget.bounds(screenX(placement, scale), screenY(placement, scale), screenWidth(placement, scale),
            screenHeight(placement, scale));
    }

    public void place(GuiSliderWidget widget, GuiPlacement placement) {
        float scale = scaleOf(placement);
        widget.bounds(screenX(placement, scale), screenY(placement, scale), screenWidth(placement, scale),
            screenHeight(placement, scale));
    }

    private int screenX(GuiPlacement placement, float scale) {
        return anchorX(placement) + Math.round(placement.offsetX() * scale);
    }

    private int screenY(GuiPlacement placement, float scale) {
        return anchorY(placement) + Math.round(placement.offsetY() * scale);
    }

    private static int screenWidth(GuiPlacement placement, float scale) {
        return Math.max(1, Math.round(placement.width() * scale));
    }

    private static int screenHeight(GuiPlacement placement, float scale) {
        return Math.max(1, Math.round(placement.height() * scale));
    }

    public void begin(GuiGraphics graphics, GuiPlacement placement) {
        float scale = scaleOf(placement);
        graphics.pose().pushPose();
        graphics.pose().translate(anchorX(placement), anchorY(placement), 0.0F);
        if (scale != 1.0F) graphics.pose().scale(scale, scale, 1.0F);
    }

    public void end(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    public void sendButton(int ordinal, boolean release) {
        if (minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ordinal * 2 + (release ? 1 : 0));
    }

    public void sendSlider(int ordinal, double value, boolean released) {
        GuiNet.slider(menu.containerId, ordinal, value, released);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (GuiButtonWidget button : buttons) button.sync();
        for (GuiSliderWidget slider : sliders) slider.sync();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (config.showVanillaBackground()) GuiDraw.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (config.showInventory()) drawInventory(graphics);
        if (config.showVanillaBackground()) {
            for (int k = 0; k < layout.slotCount(); k++) {
                Slot slot = menu.slots.get(k);
                if (slot.isActive()) GuiDraw.slotFrame(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
            }
        }
        for (int i = 0; i < layout.size(); i++) {
            if (!menu.isVisible(i)) continue;
            GuiElement element = layout.element(i);
            if (element instanceof GuiElement.Image image) {
                drawImage(graphics, i, image);
            } else if (element instanceof GuiElement.EntityView view) {
                drawEntity(graphics, i, view, mouseX, mouseY);
            } else if (element instanceof GuiElement.Button) {
                buttons[layout.ordinal(i)].draw(graphics, mouseX, mouseY);
            } else if (element instanceof GuiElement.Slider) {
                sliders[layout.ordinal(i)].draw(graphics, mouseX, mouseY);
            }
        }
    }

    private void drawInventory(GuiGraphics graphics) {
        int left = leftPos + GuiMenu.inventoryLeft(imageWidth) - 1;
        int top = topPos + GuiMenu.inventoryTop(imageHeight) - 1;
        if (!config.showVanillaBackground()) {
            GuiDraw.panel(graphics, left - 7, top - 14, GuiLayout.INVENTORY_WIDTH, topPos + imageHeight - (top - 14));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                GuiDraw.slotFrame(graphics, left + column * GuiLayout.SLOT_SIZE, top + row * GuiLayout.SLOT_SIZE);
            }
        }
        int hotbar = topPos + GuiMenu.hotbarTop(imageHeight) - 1;
        for (int column = 0; column < 9; column++) {
            GuiDraw.slotFrame(graphics, left + column * GuiLayout.SLOT_SIZE, hotbar);
        }
    }

    private void drawImage(GuiGraphics graphics, int element, GuiElement.Image image) {
        GuiPlacement placement = image.placement();
        int x = placement.offsetX();
        int y = placement.offsetY();
        int width = placement.width();
        int height = placement.height();
        float progress = image.progress().isPresent() ? menu.progress(layout.ordinal(element)) : 1.0F;
        begin(graphics, placement);
        if (progress >= 1.0F) {
            GuiDraw.texture(graphics, image.texture(), x, y, width, height);
        } else if (progress > 0.0F) {
            int across = Math.round(width * progress);
            int down = Math.round(height * progress);
            switch (image.direction()) {
                case RIGHT -> GuiDraw.region(graphics, image.texture(), x, y, across, height,
                    0, 0, across, height, width, height);
                case LEFT -> GuiDraw.region(graphics, image.texture(), x + width - across, y, across, height,
                    width - across, 0, across, height, width, height);
                case UP -> GuiDraw.region(graphics, image.texture(), x, y + height - down, width, down,
                    0, height - down, width, down, width, height);
                case DOWN -> GuiDraw.region(graphics, image.texture(), x, y, width, down,
                    0, 0, width, down, width, height);
            }
        }
        end(graphics);
    }

    private void drawEntity(GuiGraphics graphics, int element, GuiElement.EntityView view, int mouseX, int mouseY) {
        LivingEntity entity = entityFor(element, view);
        if (entity == null) return;
        GuiPlacement placement = view.placement();
        float scale = scaleOf(placement);
        int x = screenX(placement, scale);
        int y = screenY(placement, scale);
        int width = screenWidth(placement, scale);
        int height = screenHeight(placement, scale);
        int size = view.scale().isPresent()
            ? Math.round(view.scale().get() * scale)
            : (int) Math.min(height * ENTITY_FILL / Math.max(entity.getBbHeight(), 0.1F),
                width * ENTITY_FILL / Math.max(entity.getBbWidth(), 0.1F));
        GuiDraw.entity(graphics, x, y, width, height, Math.max(1, size), view.followMouse(), mouseX, mouseY, entity);
    }

    @Nullable
    private LivingEntity entityFor(int element, GuiElement.EntityView view) {
        if (minecraft == null || minecraft.level == null) return null;
        if (view.tracksWorldEntity()) {
            int id = menu.entityId(layout.ordinal(element));
            if (id == GuiMenu.NO_ENTITY) return null;
            Entity entity = minecraft.level.getEntity(id);
            return entity instanceof LivingEntity living ? living : null;
        }
        Optional<LivingEntity> dummy = dummies.get(element);
        if (dummy == null) {
            dummy = Optional.ofNullable(createDummy(view));
            dummies.put(element, dummy);
        }
        return dummy.orElse(null);
    }

    @Nullable
    private LivingEntity createDummy(GuiElement.EntityView view) {
        if (view.entityType().isEmpty() || minecraft == null || minecraft.level == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(view.entityType().get()).orElse(null);
        if (type == null) return null;
        Entity created = type.create(minecraft.level);
        if (!(created instanceof LivingEntity living)) return null;
        for (GuiElement.Equipment equipment : view.equipment()) {
            living.setItemSlot(equipment.slot().vanilla(), equipment.item().copy());
        }
        return living;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Optional<LivingEntity> dummy : dummies.values()) {
            if (dummy.isPresent()) dummy.get().tickCount++;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hasTitle) graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        if (config.showInventory()) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (GuiSliderWidget slider : sliders) {
            if (slider.drag(mouseX)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        for (GuiButtonWidget widget : buttons) handled |= widget.releaseIfPressed();
        for (GuiSliderWidget slider : sliders) handled |= slider.releaseIfDragging();
        if (handled) {
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return config.pauseGame();
    }
}
