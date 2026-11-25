package dev.overgrown.apoli.gui;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.MacroArguments;
import dev.overgrown.apoli.data.expr.ExprContext;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import dev.overgrown.apoli.power.builtin.GuiPower;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GuiMenu extends AbstractContainerMenu {
    public static final int SLIDER_RESOLUTION = 30000;
    public static final int PROGRESS_RESOLUTION = 10000;
    public static final int NO_ENTITY = -1;
    private static final int ENTITY_REFRESH_TICKS = 10;
    private static final int SLIDER_CONTEXT = ExprContext.slot("slider");

    private final Player player;
    private final ResourceLocation powerId;
    private final ResourceLocation storageKey;
    private final GuiPower.Config config;
    private final GuiLayout layout;
    private final SimpleContainer container;
    private final int[] data;
    private final double[] sliders;
    private final boolean[] pressed;
    private final int guiSlots;
    @Nullable
    private final ServerPlayer owner;
    @Nullable
    private final PowerContainerImpl holder;
    @Nullable
    private EntitySelector[] selectors;
    @Nullable
    private boolean[] parsed;
    private long lastTick = Long.MIN_VALUE;
    private int entityRefresh;

    private GuiMenu(int id, Inventory inventory, ResourceLocation powerId, ResourceLocation storageKey,
                    GuiPower.Config config, SimpleContainer container, double[] sliders,
                    @Nullable ServerPlayer owner, @Nullable PowerContainerImpl holder) {
        super(ApoliMenus.gui(), id);
        this.player = inventory.player;
        this.powerId = powerId;
        this.storageKey = storageKey;
        this.config = config;
        this.layout = config.layout();
        this.container = container;
        this.owner = owner;
        this.holder = holder;
        this.pressed = new boolean[layout.buttonCount()];
        this.data = new int[layout.dataCount()];
        this.sliders = new double[layout.sliderCount()];
        for (int k = 0; k < this.sliders.length; k++) {
            this.sliders[k] = k < sliders.length ? slider(k).snap(sliders[k]) : slider(k).start();
        }

        int width = config.width();
        int height = config.height();
        for (int k = 0; k < layout.slotCount(); k++) {
            int element = layout.slotElement(k);
            GuiElement.SlotView view = (GuiElement.SlotView) layout.element(element);
            GuiPlacement placement = view.placement();
            addSlot(new GuiSlot(this, container, k, placement.left(width) + 1, placement.top(height) + 1, element, view));
        }
        this.guiSlots = layout.slotCount();
        if (config.showInventory()) {
            int left = inventoryLeft(width);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, inventoryTop(height) + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, left + column * 18, hotbarTop(height)));
            }
        }

        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                return data[index];
            }

            @Override
            public void set(int index, int value) {
                data[index] = value;
                if (GuiMenu.this.owner == null) receive(index, value);
            }

            @Override
            public int getCount() {
                return data.length;
            }
        });

        if (owner != null) {
            for (int k = 0; k < this.sliders.length; k++) data[layout.sliderIndex(k)] = encodeSlider(k);
            refresh(true);
            if (config.memory()) container.addListener(changed -> persist());
        }
    }

    public static GuiMenu server(int id, Inventory inventory, ResourceLocation powerId, GuiPower.Config config,
                                 ResourceLocation storageKey, GuiStorage.State state, ServerPlayer owner,
                                 PowerContainerImpl holder) {
        return new GuiMenu(id, inventory, powerId, storageKey, config, state.items(), state.sliders(), owner, holder);
    }

    public static GuiMenu client(int id, Inventory inventory, GuiOpenData data) {
        GuiPower.Config config = GuiPower.configOf(data.power());
        if (config == null) config = GuiPower.Config.EMPTY;
        return new GuiMenu(id, inventory, data.power(), GuiPower.storageKey(data.power(), config), config,
            new SimpleContainer(config.layout().slotCount()), data.sliders(), null, null);
    }

    public static int inventoryLeft(int width) {
        return (width - GuiLayout.INVENTORY_WIDTH) / 2 + 8;
    }

    public static int inventoryTop(int height) {
        return height - 82;
    }

    public static int hotbarTop(int height) {
        return height - 24;
    }

    public ResourceLocation powerId() {
        return powerId;
    }

    public ResourceLocation storageKey() {
        return storageKey;
    }

    public GuiPower.Config config() {
        return config;
    }

    public GuiLayout layout() {
        return layout;
    }

    public SimpleContainer container() {
        return container;
    }

    public Player player() {
        return player;
    }

    public boolean isVisible(int element) {
        return (data[layout.visibleIndex(element)] & (1 << (element & 15))) != 0;
    }

    public boolean isEnabled(int element) {
        return (data[layout.enabledIndex(element)] & (1 << (element & 15))) != 0;
    }

    public float progress(int ordinal) {
        return Mth.clamp((data[layout.progressIndex(ordinal)] & 0xFFFF) / (float) PROGRESS_RESOLUTION, 0.0F, 1.0F);
    }

    public int entityId(int ordinal) {
        int base = layout.entityIndex(ordinal);
        return (data[base] & 0xFFFF) | (data[base + 1] << 16);
    }

    public double sliderValue(int ordinal) {
        return sliders[ordinal];
    }

    public void setClientSlider(int ordinal, double value) {
        if (owner == null && ordinal >= 0 && ordinal < sliders.length) sliders[ordinal] = slider(ordinal).snap(value);
    }

    public GuiElement.Slider slider(int ordinal) {
        return (GuiElement.Slider) layout.element(layout.sliderElement(ordinal));
    }

    public GuiElement.Button button(int ordinal) {
        return (GuiElement.Button) layout.element(layout.buttonElement(ordinal));
    }

    private void receive(int index, int value) {
        for (int k = 0; k < sliders.length; k++) {
            if (layout.sliderIndex(k) == index) {
                sliders[k] = slider(k).valueAt((value & 0xFFFF) / (double) SLIDER_RESOLUTION);
                return;
            }
        }
    }

    private int encodeSlider(int ordinal) {
        return (int) Math.round(slider(ordinal).fraction(sliders[ordinal]) * SLIDER_RESOLUTION);
    }

    @Override
    public void broadcastChanges() {
        if (owner != null) {
            long now = owner.level().getGameTime();
            if (now != lastTick) {
                lastTick = now;
                refresh(false);
                tickPressed();
            }
        }
        super.broadcastChanges();
    }

    private EntityCtx context() {
        return new EntityCtx(owner, owner.level());
    }

    private static boolean passes(Optional<EntityCondition> condition, EntityCtx ctx) {
        return condition.isEmpty() || condition.get().test(ctx);
    }

    private void refresh(boolean entitiesNow) {
        EntityCtx ctx = context();
        int words = layout.words();
        for (int w = 0; w < words * 2; w++) data[w] = 0;
        for (int i = 0; i < layout.size(); i++) {
            GuiElement element = layout.element(i);
            if (!passes(element.placement().condition(), ctx)) continue;
            data[layout.visibleIndex(i)] |= 1 << (i & 15);
            boolean enabled = true;
            if (element instanceof GuiElement.Button button) {
                enabled = passes(button.pressCondition(), ctx);
            } else if (element instanceof GuiElement.Slider slider) {
                enabled = passes(slider.slideCondition(), ctx);
            }
            if (enabled) data[layout.enabledIndex(i)] |= 1 << (i & 15);
        }
        for (int k = 0; k < layout.progressCount(); k++) {
            GuiElement.Image image = (GuiElement.Image) layout.element(layout.progressElement(k));
            double value = image.progress().get().eval(owner);
            double clamped = Double.isFinite(value) ? Mth.clamp(value, 0.0, 1.0) : 0.0;
            data[layout.progressIndex(k)] = (int) Math.round(clamped * PROGRESS_RESOLUTION);
        }
        if (layout.entityCount() > 0 && (entitiesNow || ++entityRefresh >= ENTITY_REFRESH_TICKS)) {
            entityRefresh = 0;
            for (int k = 0; k < layout.entityCount(); k++) {
                int id = resolveEntity(k);
                int base = layout.entityIndex(k);
                data[base] = id & 0xFFFF;
                data[base + 1] = (id >>> 16) & 0xFFFF;
            }
        }
    }

    private int resolveEntity(int ordinal) {
        GuiElement.EntityView view = (GuiElement.EntityView) layout.element(layout.entityElement(ordinal));
        if (view.set().isPresent()) {
            List<UUID> members = EntitySetPower.iterationOrder(owner, view.set().get(), false);
            for (int i = 0; i < members.size(); i++) {
                Entity member = EntitySetPower.resolveEntity(owner.server, members.get(i));
                if (member != null && member.level() == owner.level()) return member.getId();
            }
            return NO_ENTITY;
        }
        EntitySelector selector = selector(ordinal, view);
        if (selector == null) return NO_ENTITY;
        try {
            List<? extends Entity> found = selector.findEntities(
                owner.createCommandSourceStack().withPermission(4).withSuppressedOutput());
            for (int i = 0; i < found.size(); i++) {
                Entity entity = found.get(i);
                if (entity.level() == owner.level()) return entity.getId();
            }
        } catch (CommandSyntaxException ignored) {
            return NO_ENTITY;
        }
        return NO_ENTITY;
    }

    @Nullable
    private EntitySelector selector(int ordinal, GuiElement.EntityView view) {
        if (selectors == null) {
            selectors = new EntitySelector[layout.entityCount()];
            parsed = new boolean[layout.entityCount()];
        }
        if (!parsed[ordinal]) {
            parsed[ordinal] = true;
            String raw = view.selector().orElse("");
            try {
                selectors[ordinal] = new EntitySelectorParser(new StringReader(raw), true).parse();
            } catch (CommandSyntaxException e) {
                Apoli.LOGGER.warn("[Apoli] The apoli:entity element in GUI '{}' has a selector that does not parse: \"{}\" ({})",
                    powerId, raw, e.getMessage());
            }
        }
        return selectors[ordinal];
    }

    private void tickPressed() {
        for (int k = 0; k < pressed.length; k++) {
            if (!pressed[k]) continue;
            int element = layout.buttonElement(k);
            if (!isVisible(element) || !isEnabled(element)) {
                release(k);
                continue;
            }
            Optional<EntityAction> action = button(k).whilePressed();
            if (action.isPresent()) action.get().run(context());
        }
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (owner == null || clicker != owner) return false;
        int ordinal = id >> 1;
        if (ordinal < 0 || ordinal >= pressed.length) return false;
        if ((id & 1) == 0) {
            press(ordinal);
        } else {
            release(ordinal);
        }
        return true;
    }

    private void press(int ordinal) {
        int element = layout.buttonElement(ordinal);
        if (pressed[ordinal] || !isVisible(element) || !isEnabled(element)) return;
        pressed[ordinal] = true;
        Optional<EntityAction> action = button(ordinal).onPress();
        if (action.isPresent()) action.get().run(context());
    }

    private void release(int ordinal) {
        if (!pressed[ordinal]) return;
        pressed[ordinal] = false;
        Optional<EntityAction> action = button(ordinal).onRelease();
        if (action.isPresent()) action.get().run(context());
    }

    public void slide(int ordinal, double value, boolean released) {
        if (owner == null || ordinal < 0 || ordinal >= sliders.length) return;
        int element = layout.sliderElement(ordinal);
        if (!isVisible(element) || !isEnabled(element)) return;
        GuiElement.Slider slider = slider(ordinal);
        double snapped = slider.snap(value);
        if (snapped != sliders[ordinal]) {
            sliders[ordinal] = snapped;
            data[layout.sliderIndex(ordinal)] = encodeSlider(ordinal);
            persist();
            if (slider.onSlide().isPresent()) runWithSlider(slider.onSlide().get(), snapped);
        }
        if (released && slider.onRelease().isPresent()) runWithSlider(slider.onRelease().get(), snapped);
    }

    private void runWithSlider(EntityAction action, double value) {
        double previous = ExprContext.push(SLIDER_CONTEXT, value);
        try {
            MacroArguments.runWith("slider", value, () -> action.run(context()));
        } finally {
            ExprContext.pop(SLIDER_CONTEXT, previous);
        }
    }

    private void persist() {
        if (holder == null || owner == null || !config.memory()) return;
        holder.setAuxNbt(storageKey, GuiStorage.save(container, sliders, config, owner.registryAccess()));
    }

    @Override
    public void removed(Player closing) {
        super.removed(closing);
        if (owner == null) return;
        for (int k = 0; k < pressed.length; k++) release(k);
        if (config.memory()) {
            persist();
        } else {
            clearContainer(closing, container);
        }
    }

    @Override
    public boolean stillValid(Player viewer) {
        return owner == null || GuiPower.stillValid(viewer, powerId);
    }

    @Override
    public ItemStack quickMoveStack(Player mover, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < guiSlots) {
            if (slots.size() == guiSlots || !moveItemStackTo(stack, guiSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (guiSlots == 0 || !moveItemStackTo(stack, 0, guiSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(mover, stack);
        return original;
    }

    private static final class GuiSlot extends Slot {
        private final GuiMenu menu;
        private final int element;
        private final GuiElement.SlotView view;

        private GuiSlot(GuiMenu menu, Container container, int index, int x, int y, int element,
                        GuiElement.SlotView view) {
            super(container, index, x, y);
            this.menu = menu;
            this.element = element;
            this.view = view;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (view.mode() == GuiElement.SlotMode.OUTPUT || !menu.isVisible(element)) return false;
            return view.itemCondition().isEmpty()
                || view.itemCondition().get().test(new ItemCtx(stack, menu.player.level(), menu.player));
        }

        @Override
        public boolean mayPickup(Player picker) {
            return menu.isVisible(element);
        }

        @Override
        public boolean isActive() {
            return menu.isVisible(element);
        }
    }
}
