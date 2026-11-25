package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.gui.ApoliMenus;
import dev.overgrown.apoli.gui.GuiElement;
import dev.overgrown.apoli.gui.GuiLayout;
import dev.overgrown.apoli.gui.GuiMenu;
import dev.overgrown.apoli.gui.GuiOpenData;
import dev.overgrown.apoli.gui.GuiStorage;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class GuiPower extends PowerType<GuiPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("gui");
    public static final int DEFAULT_WIDTH = 176;
    public static final int DEFAULT_HEIGHT = 166;

    public static final class Config {
        public static final Config EMPTY = new Config(Optional.empty(), Key.DEFAULT_PRIMARY, Optional.empty(),
            DEFAULT_WIDTH, DEFAULT_HEIGHT, false, false, false, false, List.of());

        public static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TextComponent.CODEC.optionalFieldOf("title").forGetter(Config::title),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key),
            IdCodecs.ID.optionalFieldOf("id").forGetter(Config::id),
            LoggedOptionalField.of("width", ExtraCodecs.POSITIVE_INT, DEFAULT_WIDTH).forGetter(Config::width),
            LoggedOptionalField.of("height", ExtraCodecs.POSITIVE_INT, DEFAULT_HEIGHT).forGetter(Config::height),
            Codec.BOOL.optionalFieldOf("show_inventory", true).forGetter(Config::showInventory),
            Codec.BOOL.optionalFieldOf("show_vanilla_background", false).forGetter(Config::showVanillaBackground),
            Codec.BOOL.optionalFieldOf("memory", true).forGetter(Config::memory),
            Codec.BOOL.optionalFieldOf("pause_game", false).forGetter(Config::pauseGame),
            LoggedOptionalField.strict("elements", GuiElement.CODEC.listOf())
                .xmap(elements -> elements.orElse(List.of()), Optional::of).forGetter(Config::elements)
        ).apply(i, Config::new));

        private final Optional<Component> title;
        private final Key key;
        private final Optional<ResourceLocation> id;
        private final int width;
        private final int height;
        private final boolean showInventory;
        private final boolean showVanillaBackground;
        private final boolean memory;
        private final boolean pauseGame;
        private final List<GuiElement> elements;
        private final GuiLayout layout;

        public Config(Optional<Component> title, Key key, Optional<ResourceLocation> id, int width, int height,
                      boolean showInventory, boolean showVanillaBackground, boolean memory, boolean pauseGame,
                      List<GuiElement> elements) {
            this.title = title;
            this.key = key;
            this.id = id;
            this.width = width;
            this.height = height;
            this.showInventory = showInventory;
            this.showVanillaBackground = showVanillaBackground;
            this.memory = memory;
            this.pauseGame = pauseGame;
            this.elements = List.copyOf(elements);
            this.layout = new GuiLayout(this.elements);
        }

        public Optional<Component> title() {
            return title;
        }

        public Key key() {
            return key;
        }

        public Optional<ResourceLocation> id() {
            return id;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean showInventory() {
            return showInventory;
        }

        public boolean showVanillaBackground() {
            return showVanillaBackground;
        }

        public boolean memory() {
            return memory;
        }

        public boolean pauseGame() {
            return pauseGame;
        }

        public List<GuiElement> elements() {
            return elements;
        }

        public GuiLayout layout() {
            return layout;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return Config.CODEC;
    }

    public void open(ResourceLocation powerId, Config cfg, ServerPlayer player, PowerContainerImpl holder) {
        ResourceLocation key = storageKey(powerId, cfg);
        GuiStorage.State state = GuiStorage.load(cfg.memory() ? holder.getAuxNbt(key) : null, cfg);
        Component title = cfg.title().orElse(Component.empty());
        ApoliMenus.openGui(player, title, new GuiOpenData(powerId, state.sliders()),
            (id, inventory, opener) -> GuiMenu.server(id, inventory, powerId, cfg, key, state, player, holder));
    }

    public static boolean open(ResourceLocation powerId, ServerPlayer player) {
        if (!(PowerContainer.of(player) instanceof PowerContainerImpl impl)) return false;
        if (!impl.hasPower(powerId) || impl.isSuppressed(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || !(loaded.config() instanceof Config cfg)) return false;
        if (loaded.condition().isPresent() && !loaded.condition().get().test(new EntityCtx(player, player.level()))) {
            return false;
        }
        if (!(dev.overgrown.apoli.power.PowerTypeRegistry.get(loaded.typeId()) instanceof GuiPower type)) return false;
        type.open(powerId, cfg, player, impl);
        return true;
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.allPowers().contains(powerId)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        ResourceLocation key = storageKey(powerId, cfg);
        if (holder.owner() instanceof ServerPlayer player && player.containerMenu instanceof GuiMenu menu
            && menu.storageKey().equals(key)) {
            player.closeContainer();
        }
        if (sharedByAnother(impl, powerId, key)) return;
        CompoundTag stored = impl.getAuxNbt(key);
        if (stored != null && holder.owner() instanceof ServerPlayer player) {
            SimpleContainer items = GuiStorage.load(stored, cfg).items();
            for (int slot = 0; slot < items.getContainerSize(); slot++) {
                ItemStack stack = items.getItem(slot);
                if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
            }
        }
        impl.removeAux(key);
    }

    private static boolean sharedByAnother(PowerContainer holder, ResourceLocation powerId, ResourceLocation key) {
        List<ResourceLocation> powers = holder.powersOfType(ApoliIds.GUI);
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation other = powers.get(i);
            if (other.equals(powerId)) continue;
            Config cfg = configOf(other);
            if (cfg != null && storageKey(other, cfg).equals(key)) return true;
        }
        return false;
    }

    public static @Nullable Config configOf(ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        return loaded != null && loaded.config() instanceof Config cfg ? cfg : null;
    }

    public static ResourceLocation storageKey(ResourceLocation powerId, Config cfg) {
        return cfg.id().orElse(powerId);
    }

    public static @Nullable ResourceLocation resolve(@Nullable PowerContainer holder, ResourceLocation ref) {
        if (holder == null || holder.isEmpty()) return null;
        List<ResourceLocation> powers = holder.powersOfType(ApoliIds.GUI);
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            Config cfg = configOf(powerId);
            if (cfg == null) continue;
            if (powerId.equals(ref) || cfg.id().isPresent() && cfg.id().get().equals(ref)) return powerId;
        }
        return null;
    }

    public static @Nullable GuiMenu openMenu(Entity entity, ResourceLocation storageKey) {
        if (entity instanceof Player player && player.containerMenu instanceof GuiMenu menu
            && menu.storageKey().equals(storageKey)) {
            return menu;
        }
        return null;
    }

    public static boolean stillValid(Player player, ResourceLocation powerId) {
        PowerContainer holder = PowerContainer.of(player);
        if (holder == null || !holder.hasPower(powerId) || holder.isSuppressed(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || !(loaded.config() instanceof Config)) return false;
        return loaded.condition().isEmpty() || loaded.condition().get().test(new EntityCtx(player, player.level()));
    }

    public static @Nullable SimpleContainer container(LivingEntity entity, ResourceLocation ref) {
        PowerContainer holder = PowerContainer.of(entity);
        ResourceLocation powerId = resolve(holder, ref);
        if (powerId == null) return null;
        Config cfg = configOf(powerId);
        if (cfg == null) return null;
        ResourceLocation key = storageKey(powerId, cfg);
        GuiMenu live = openMenu(entity, key);
        if (live != null) return live.container();
        if (!(entity.level() instanceof ServerLevel level) || !(holder instanceof PowerContainerImpl impl)) return null;
        if (!cfg.memory()) return new SimpleContainer(cfg.layout().slotCount());
        return GuiStorage.load(impl.getAuxNbt(key), cfg).items();
    }

    public static boolean saveContainer(LivingEntity entity, ResourceLocation ref, SimpleContainer container) {
        PowerContainer holder = PowerContainer.of(entity);
        ResourceLocation powerId = resolve(holder, ref);
        if (powerId == null) return false;
        Config cfg = configOf(powerId);
        if (cfg == null) return false;
        ResourceLocation key = storageKey(powerId, cfg);
        GuiMenu live = openMenu(entity, key);
        if (live != null && live.container() == container) {
            container.setChanged();
            return true;
        }
        if (!cfg.memory() || !(entity.level() instanceof ServerLevel level) || !(holder instanceof PowerContainerImpl impl)) {
            return true;
        }
        CompoundTag stored = impl.getAuxNbt(key);
        double[] sliders = GuiStorage.load(stored, cfg).sliders();
        impl.setAuxNbt(key, GuiStorage.save(container, sliders, cfg));
        return true;
    }

    public static OptionalDouble sliderValue(LivingEntity entity, ResourceLocation ref, String sliderId) {
        PowerContainer holder = PowerContainer.of(entity);
        ResourceLocation powerId = resolve(holder, ref);
        if (powerId == null) return OptionalDouble.empty();
        Config cfg = configOf(powerId);
        if (cfg == null) return OptionalDouble.empty();
        int ordinal = cfg.layout().sliderOrdinal(sliderId);
        if (ordinal < 0) return OptionalDouble.empty();
        ResourceLocation key = storageKey(powerId, cfg);
        GuiMenu live = openMenu(entity, key);
        if (live != null) return OptionalDouble.of(live.sliderValue(ordinal));
        GuiElement.Slider slider = (GuiElement.Slider) cfg.layout().element(cfg.layout().sliderElement(ordinal));
        CompoundTag stored = cfg.memory() && holder instanceof PowerContainerImpl impl ? impl.getAuxNbt(key) : null;
        return OptionalDouble.of(GuiStorage.slider(stored, slider, ordinal));
    }
}
