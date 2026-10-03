package dev.overgrown.apoli.power.builtin;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.data.CameraAnimation;
import dev.overgrown.apoli.data.CameraKeyframe;
import dev.overgrown.apoli.data.CameraLook;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModifyCameraPower extends PowerType<ModifyCameraPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_camera");

    private static final int RETARGET_TICKS = 20;
    private static final double TRACK_RANGE_SQ = 256.0 * 256.0;
    private static final Map<String, Optional<EntitySelector>> SELECTORS = new ConcurrentHashMap<>();

    public record Transform(
        Space space,
        Expression x,
        Expression y,
        Expression z,
        Expression pitch,
        Expression yaw,
        Expression roll,
        Expression fov
    ) {
        public static final MapCodec<Transform> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Space.CODEC.optionalFieldOf("space", Space.LOCAL).forGetter(Transform::space),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("x", Expression.constant(0)).forGetter(Transform::x),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("y", Expression.constant(0)).forGetter(Transform::y),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("z", Expression.constant(0)).forGetter(Transform::z),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("pitch", Expression.constant(0)).forGetter(Transform::pitch),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("yaw", Expression.constant(0)).forGetter(Transform::yaw),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("roll", Expression.constant(0)).forGetter(Transform::roll),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("fov", Expression.constant(1)).forGetter(Transform::fov)
        ).apply(i, Transform::new));
    }

    public record Config(
        Transform transform,
        CameraLook look,
        boolean follow,
        Optional<ResourceLocation> set,
        Optional<String> selector,
        boolean collision,
        List<CameraKeyframe> keyframes,
        boolean loop,
        int priority,
        CameraAnimation animation
    ) {
        Config(Transform transform, CameraLook look, boolean follow, Optional<ResourceLocation> set,
               Optional<String> selector, boolean collision, List<CameraKeyframe> keyframes, boolean loop,
               int priority) {
            this(transform, look, follow, set, selector, collision, keyframes, loop, priority,
                CameraAnimation.of(keyframes, loop));
        }

        public boolean targetsOther() {
            return set.isPresent() || selector.isPresent();
        }
    }

    private static final MapCodec<Config> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Transform.MAP_CODEC.forGetter(Config::transform),
        CameraLook.CODEC.optionalFieldOf("look", CameraLook.ANCHOR).forGetter(Config::look),
        Codec.BOOL.optionalFieldOf("follow", true).forGetter(Config::follow),
        IdCodecs.ID.optionalFieldOf("set").forGetter(Config::set),
        Codec.STRING.optionalFieldOf("selector").forGetter(Config::selector),
        Codec.BOOL.optionalFieldOf("collision", true).forGetter(Config::collision),
        LoggedOptionalField.of("keyframes", CameraKeyframe.CODEC.listOf(), List.of()).forGetter(Config::keyframes),
        Codec.BOOL.optionalFieldOf("loop", false).forGetter(Config::loop),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
    ).apply(i, Config::new));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }

    @Override
    public boolean resolvesForClient() {
        return true;
    }

    @Override
    public int clientTarget(ResourceLocation powerId, Config cfg, ServerPlayer player, int current) {
        if (!cfg.targetsOther()) return -1;
        if (current >= 0 && player.server.getTickCount() % RETARGET_TICKS != 0) {
            Entity existing = player.level().getEntity(current);
            if (valid(player, existing)) return current;
        }
        Entity found = cfg.set().isPresent() ? nearestMember(player, cfg.set().get()) : null;
        if (found == null && cfg.selector().isPresent()) found = firstSelected(player, cfg.selector().get());
        return found == null ? -1 : found.getId();
    }

    private static boolean valid(ServerPlayer player, @Nullable Entity entity) {
        return entity != null && !entity.isRemoved() && entity.level() == player.level()
            && entity.distanceToSqr(player) <= TRACK_RANGE_SQ;
    }

    private static @Nullable Entity nearestMember(ServerPlayer player, ResourceLocation setId) {
        List<UUID> members = EntitySetPower.iterationOrder(player, setId, false);
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0, n = members.size(); i < n; i++) {
            Entity member = EntitySetPower.resolveEntity(player.server, members.get(i));
            if (!valid(player, member) || member == player) continue;
            double distance = member.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = member;
            }
        }
        return best;
    }

    private static @Nullable Entity firstSelected(ServerPlayer player, String selector) {
        Optional<EntitySelector> parsed = SELECTORS.computeIfAbsent(selector, ModifyCameraPower::parse);
        if (parsed.isEmpty()) return null;
        try {
            List<? extends Entity> found = parsed.get().findEntities(player.createCommandSourceStack().withPermission(2));
            for (int i = 0, n = found.size(); i < n; i++) {
                Entity entity = found.get(i);
                if (entity != player && valid(player, entity)) return entity;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static Optional<EntitySelector> parse(String selector) {
        try {
            return Optional.of(new EntitySelectorParser(new StringReader(selector), true).parse());
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] apoli:modify_camera could not parse selector '{}': {}", selector, e.getMessage());
            return Optional.empty();
        }
    }
}
