package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExtraTradesPower extends PowerType<ExtraTradesPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition, List<Trade> trades) {}

    public record Trade(TradeItem buy, Optional<TradeItem> buyB, TradeItem sell, int maxUses, int xp,
                        float priceMultiplier) {
        static final Codec<Trade> CODEC = RecordCodecBuilder.create(i -> i.group(
            TradeItem.CODEC.fieldOf("buy").forGetter(Trade::buy),
            TradeItem.CODEC.optionalFieldOf("buy_b").forGetter(Trade::buyB),
            TradeItem.CODEC.fieldOf("sell").forGetter(Trade::sell),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_uses", 12).forGetter(Trade::maxUses),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("xp", 1).forGetter(Trade::xp),
            Codec.floatRange(0.0F, Float.MAX_VALUE).optionalFieldOf("price_multiplier", 0.05F).forGetter(Trade::priceMultiplier)
        ).apply(i, Trade::new));

        @Nullable
        MerchantOffer roll(ServerLevel level, AbstractVillager merchant) {
            ItemStack cost = buy.roll(level, merchant);
            ItemStack result = sell.roll(level, merchant);
            if (cost.isEmpty() || result.isEmpty()) return null;
            Optional<ItemCost> second = buyB.map(item -> item.roll(level, merchant))
                .filter(stack -> !stack.isEmpty())
                .map(ExtraTradesPower::cost);
            return new MerchantOffer(cost(cost), second, result, maxUses, xp, priceMultiplier);
        }
    }

    public record TradeItem(Optional<ItemStackData> stack, Optional<ResourceLocation> lootTable) {
        private static final Codec<ResourceLocation> LOOT_TABLE = ResourceLocation.CODEC.fieldOf("loot_table").codec();

        static final Codec<TradeItem> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<TradeItem, T>> decode(DynamicOps<T> ops, T input) {
                boolean table = ops.getMap(input).result().map(map -> map.get("loot_table") != null).orElse(false);
                return table
                    ? LOOT_TABLE.decode(ops, input).map(p -> p.mapFirst(id -> new TradeItem(Optional.empty(), Optional.of(id))))
                    : ItemStackData.CODEC.decode(ops, input).map(p -> p.mapFirst(data -> new TradeItem(Optional.of(data), Optional.empty())));
            }

            @Override
            public <T> DataResult<T> encode(TradeItem item, DynamicOps<T> ops, T prefix) {
                return item.lootTable.isPresent()
                    ? LOOT_TABLE.encode(item.lootTable.get(), ops, prefix)
                    : ItemStackData.CODEC.encode(item.stack.orElseThrow(), ops, prefix);
            }
        };

        ItemStack roll(ServerLevel level, AbstractVillager merchant) {
            if (stack.isPresent()) return stack.get().stack().copy();
            if (lootTable.isEmpty()) return ItemStack.EMPTY;
            LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTable.get()));
            LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, merchant.position())
                .withParameter(LootContextParams.THIS_ENTITY, merchant)
                .create(LootContextParamSets.GIFT);
            List<ItemStack> rolled = table.getRandomItems(params);
            return rolled.isEmpty() ? ItemStack.EMPTY : rolled.get(0);
        }
    }

    private static final String SAVE_KEY = "apoli:extra_trades";
    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.EXTRA_TRADES);

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Trade.CODEC.listOf().fieldOf("trades").forGetter(Config::trades)
        ).apply(i, Config::new));
    }

    public static List<MerchantOffer> offersFor(AbstractVillager merchant, Player player,
                                                Map<ResourceLocation, MerchantOffers> rolled) {
        if (!IN_USE.inUse() || !(merchant.level() instanceof ServerLevel level)
                || !PowerLookup.holdsType(player, ApoliIds.EXTRA_TRADES)) {
            return List.of();
        }
        BiEntityCtx ctx = BiEntityCtx.of(player, merchant, level);
        List<MerchantOffer> shown = new ArrayList<>();
        PowerLookup.forEachEntry(player, ApoliIds.EXTRA_TRADES, Config.class, (powerId, cfg) -> {
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) return;
            shown.addAll(rolled.computeIfAbsent(powerId, id -> roll(cfg, level, merchant)));
        });
        return shown;
    }

    private static MerchantOffers roll(Config cfg, ServerLevel level, AbstractVillager merchant) {
        MerchantOffers offers = new MerchantOffers();
        for (Trade trade : cfg.trades()) {
            MerchantOffer offer = trade.roll(level, merchant);
            if (offer != null) offers.add(offer);
        }
        return offers;
    }

    @SuppressWarnings("unchecked")
    private static ItemCost cost(ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return new ItemCost(stack.getItemHolder(), stack.getCount(), DataComponentPredicate.EMPTY);
        DataComponentMap.Builder components = DataComponentMap.builder();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            entry.getValue().ifPresent(value -> components.set((DataComponentType<Object>) entry.getKey(), value));
        }
        return new ItemCost(stack.getItemHolder(), stack.getCount(), DataComponentPredicate.allOf(components.build()));
    }

    public static void save(AbstractVillager merchant, Map<ResourceLocation, MerchantOffers> rolled, CompoundTag tag) {
        if (rolled.isEmpty()) return;
        RegistryOps<Tag> ops = merchant.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag saved = new CompoundTag();
        rolled.forEach((powerId, offers) -> MerchantOffers.CODEC.encodeStart(ops, offers).result()
            .ifPresent(encoded -> saved.put(powerId.toString(), encoded)));
        tag.put(SAVE_KEY, saved);
    }

    public static Map<ResourceLocation, MerchantOffers> load(AbstractVillager merchant, CompoundTag tag) {
        Map<ResourceLocation, MerchantOffers> rolled = new LinkedHashMap<>();
        if (!tag.contains(SAVE_KEY, Tag.TAG_COMPOUND)) return rolled;
        RegistryOps<Tag> ops = merchant.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag saved = tag.getCompound(SAVE_KEY);
        for (String key : saved.getAllKeys()) {
            ResourceLocation powerId = ResourceLocation.tryParse(key);
            if (powerId == null) continue;
            MerchantOffers.CODEC.parse(ops, saved.get(key)).result().ifPresent(offers -> rolled.put(powerId, offers));
        }
        return rolled;
    }
}
