package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public final class DisguiseAsAction implements ActionType<EntityCtx, DisguiseAsAction.Cfg> {
    private static final ResourceLocation PLAYER_TYPE = new ResourceLocation("player");

    public record Cfg(
        Optional<ResourceLocation> entityType,
        Optional<String> playerName,
        Optional<String> playerUuid,
        Optional<Nbt> nbt,
        boolean overwrite,
        boolean changeName,
        Optional<EntityAction> beforeAction,
        Optional<EntityAction> afterAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("entity_type").forGetter(Cfg::entityType),
            Codec.STRING.optionalFieldOf("player_name").forGetter(Cfg::playerName),
            Codec.STRING.optionalFieldOf("player_uuid").forGetter(Cfg::playerUuid),
            Nbt.CODEC.optionalFieldOf("nbt").forGetter(Cfg::nbt),
            Codec.BOOL.optionalFieldOf("overwrite", true).forGetter(Cfg::overwrite),
            Codec.BOOL.optionalFieldOf("change_name", true).forGetter(Cfg::changeName),
            EntityAction.CODEC.optionalFieldOf("before_action").forGetter(Cfg::beforeAction),
            EntityAction.CODEC.optionalFieldOf("after_action").forGetter(Cfg::afterAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity actor = ctx.entity();
        if (actor == null || ctx.level().isClientSide) return;
        if (!cfg.overwrite && DisguiseManager.isDisguised(actor)) return;

        cfg.beforeAction.ifPresent(a -> a.run(ctx));

        Optional<String> noName = cfg.changeName ? Optional.empty() : Optional.of("");
        Optional<DisguiseData> data = cfg.playerName.isPresent() || cfg.playerUuid.isPresent()
            ? resolvePlayer(cfg, ctx.level().getServer())
            : cfg.entityType.map(type -> new DisguiseData(type, Optional.empty(), cfg.nbt.map(Nbt::tag), noName));

        data.ifPresent(d -> DisguiseManager.apply(actor, d, cfg.overwrite));
        cfg.afterAction.ifPresent(a -> a.run(ctx));
    }

    private static Optional<DisguiseData> resolvePlayer(Cfg cfg, MinecraftServer server) {
        if (server == null) return Optional.empty();
        Optional<GameProfile> profile = Optional.empty();
        if (cfg.playerUuid.isPresent()) {
            try {
                profile = server.getProfileCache().get(UUID.fromString(cfg.playerUuid.get()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (profile.isEmpty() && cfg.playerName.isPresent()) {
            profile = server.getProfileCache().get(cfg.playerName.get());
        }
        if (profile.isEmpty() && cfg.playerName.isPresent()) {
            var online = server.getPlayerList().getPlayerByName(cfg.playerName.get());
            if (online != null) profile = Optional.of(online.getGameProfile());
        }
        return profile.map(p -> new DisguiseData(
            PLAYER_TYPE, Optional.of(p.getId()), Optional.empty(),
            cfg.changeName
                ? Optional.ofNullable(p.getName() == null || p.getName().isEmpty() ? null : p.getName())
                : Optional.of("")));
    }
}
