package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.builtin.ReceiveConditionPower;
import net.minecraft.resources.ResourceLocation;

public final class SendConditionMeta {
    private SendConditionMeta() {}

    public record Cfg(ResourceLocation receiver) {
        public static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("receiver").forGetter(Cfg::receiver)
        ).apply(i, Cfg::new));
    }

    public static final class Entity implements ConditionType<EntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public boolean test(Cfg cfg, EntityCtx ctx) {
            return ctx.raw() != null && ReceiveConditionPower.testEntity(ctx.raw(), cfg.receiver, ctx);
        }
    }

    public static final class BiEntity implements ConditionType<BiEntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public boolean test(Cfg cfg, BiEntityCtx ctx) {
            return ctx.rawActor() != null && ReceiveConditionPower.testBiEntity(ctx.rawActor(), cfg.receiver, ctx);
        }
    }

    public static final class Item implements ConditionType<ItemCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public boolean test(Cfg cfg, ItemCtx ctx) {
            return ctx.holder() != null && ReceiveConditionPower.testItem(ctx.holder(), cfg.receiver, ctx);
        }
    }
}
