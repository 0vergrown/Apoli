package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.builtin.ReceiveActionPower;
import net.minecraft.resources.ResourceLocation;

public final class SendActionMeta {
    private SendActionMeta() {}

    public record Cfg(ResourceLocation receiver) {
        public static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("receiver").forGetter(Cfg::receiver)
        ).apply(i, Cfg::new));
    }

    public static final class Entity implements ActionType<EntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public void run(Cfg cfg, EntityCtx ctx) {
            if (ctx.raw() == null) return;
            ReceiveActionPower.receiveEntity(ctx.raw(), cfg.receiver, ctx);
        }
    }

    public static final class BiEntity implements ActionType<BiEntityCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public void run(Cfg cfg, BiEntityCtx ctx) {
            if (ctx.rawActor() == null) return;
            ReceiveActionPower.receiveBiEntity(ctx.rawActor(), cfg.receiver, ctx);
        }
    }

    public static final class Item implements ActionType<ItemCtx, Cfg> {
        @Override
        public MapCodec<Cfg> codec() {
            return Cfg.CODEC;
        }

        @Override
        public void run(Cfg cfg, ItemCtx ctx) {
            if (ctx.holder() == null) return;
            ReceiveActionPower.receiveItem(ctx.holder(), cfg.receiver, ctx);
        }
    }
}
