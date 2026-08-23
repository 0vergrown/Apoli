package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TooltipPower extends PowerType<TooltipPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("tooltip");

    public record Config(
        Optional<ItemCondition> itemCondition,
        Optional<Component> text,
        Optional<List<Component>> texts,
        int order
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            TextComponent.CODEC.optionalFieldOf("text").forGetter(Config::text),
            Codec.list(TextComponent.CODEC).optionalFieldOf("texts").forGetter(Config::texts),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Config::order)
        ).apply(i, Config::new));
    }

    public static void appendLines(LivingEntity holder, ItemStack stack, List<Component> out) {
        if (holder == null || stack.isEmpty()) return;
        record Entry(int order, Component line) {}
        List<Entry> entries = new ArrayList<>();
        ItemCtx itemCtx = new ItemCtx(stack, holder.level(), holder);
        PowerLookup.forEach(holder, CANONICAL, Config.class, cfg -> {
            if (cfg.itemCondition.isPresent() && !cfg.itemCondition.get().test(itemCtx)) return;
            cfg.text.ifPresent(t -> entries.add(new Entry(cfg.order, t)));
            cfg.texts.ifPresent(list -> list.forEach(t -> entries.add(new Entry(cfg.order, t))));
        });
        if (entries.isEmpty()) return;
        entries.sort(Comparator.comparingInt(Entry::order));
        for (Entry e : entries) out.add(e.line);
    }
}
