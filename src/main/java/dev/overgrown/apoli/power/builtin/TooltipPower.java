package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TooltipPower extends PowerType<TooltipPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("tooltip");

    private static final Comparator<Config> BY_ORDER = Comparator.comparingInt(Config::order);

    public enum Position implements StringRepresentable {
        BELOW_NAME("below_name"),
        BELOW_LORE("below_lore");

        public static final Codec<Position> CODEC = StringRepresentable.fromEnum(Position::values);
        private final String name;

        Position(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(
        Optional<ItemCondition> itemCondition,
        Optional<Component> text,
        Optional<List<Component>> texts,
        int order,
        Position position
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            TextComponent.CODEC.optionalFieldOf("text").forGetter(Config::text),
            Codec.list(TextComponent.CODEC).optionalFieldOf("texts").forGetter(Config::texts),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Config::order),
            LoggedOptionalField.of("position", Position.CODEC, Position.BELOW_LORE).forGetter(Config::position)
        ).apply(i, Config::new));
    }

    public static void appendLines(@Nullable LivingEntity holder, ItemStack stack, Position position, List<Component> out) {
        if (holder == null || stack.isEmpty()) return;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.powersOfType(CANONICAL).isEmpty()) return;
        ItemCtx itemCtx = new ItemCtx(stack, holder.level(), holder);
        List<Config> matched = new ArrayList<>(2);
        PowerLookup.forEach(holder, CANONICAL, Config.class, cfg -> {
            if (cfg.position != position) return;
            if (cfg.itemCondition.isPresent() && !cfg.itemCondition.get().test(itemCtx)) return;
            matched.add(cfg);
        });
        if (matched.isEmpty()) return;
        if (matched.size() > 1) matched.sort(BY_ORDER);
        for (int i = 0; i < matched.size(); i++) {
            Config cfg = matched.get(i);
            if (cfg.text.isPresent()) out.add(cfg.text.get());
            if (cfg.texts.isPresent()) out.addAll(cfg.texts.get());
        }
    }
}
