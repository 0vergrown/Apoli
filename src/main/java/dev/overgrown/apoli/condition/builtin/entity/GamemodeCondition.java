package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public final class GamemodeCondition implements ConditionType<EntityCtx, GamemodeCondition.Cfg> {
    public enum Mode implements StringRepresentable {
        SURVIVAL("survival", GameType.SURVIVAL),
        CREATIVE("creative", GameType.CREATIVE),
        ADVENTURE("adventure", GameType.ADVENTURE),
        SPECTATOR("spectator", GameType.SPECTATOR);

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;
        private final GameType vanilla;
        Mode(String n, GameType v) { this.name = n; this.vanilla = v; }
        @Override public String getSerializedName() { return name; }
        public GameType vanilla() { return vanilla; }
    }

    public record Cfg(Mode gamemode) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.fieldOf("gamemode").forGetter(Cfg::gamemode)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.entity() instanceof Player player && gameTypeOf(player) == cfg.gamemode.vanilla();
    }

    private static GameType gameTypeOf(Player player) {
        if (player instanceof ServerPlayer server) return server.gameMode.getGameModeForPlayer();
        if (player.isSpectator()) return GameType.SPECTATOR;
        if (player.isCreative()) return GameType.CREATIVE;
        return player.getAbilities().mayBuild ? GameType.SURVIVAL : GameType.ADVENTURE;
    }
}
