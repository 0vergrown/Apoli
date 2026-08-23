package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.compat.voicechat.VoiceState;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class ExprVars {
    private ExprVars() {}

    public record ResolvedVar(ExprVar accessor, boolean needsContainer, boolean needsPeer) {
        public ResolvedVar(ExprVar accessor, boolean needsContainer) {
            this(accessor, needsContainer, false);
        }
    }

    private static final String ACTOR_PREFIX = "actor_";
    private static final String TARGET_PREFIX = "target_";

    private static final Map<String, ResolvedVar> VARS = new HashMap<>();

    public static void register(String name, ExprVar accessor) {
        VARS.put(name, new ResolvedVar(accessor, false));
    }

    public static void registerContext(String name) {
        int slot = ExprContext.slot(name);
        register(name, (e, c, l, v) -> ExprContext.get(slot));
    }

    public static @Nullable ResolvedVar resolve(String name) {
        ResolvedVar known = VARS.get(name);
        if (known != null) return known;
        ResolvedVar prefixed = resolvePrefixed(name);
        if (prefixed != null) return prefixed;
        if (name.indexOf(':') < 0) return null;
        ResourceLocation literal = ResourceLocation.tryParse(name);
        ResolvedVar bound = resolveBound(name, "_max", true, literal);
        if (bound != null) return bound;
        bound = resolveBound(name, "_min", false, literal);
        if (bound != null) return bound;
        bound = resolveSize(name, literal);
        if (bound != null) return bound;
        if (literal == null) return null;
        return new ResolvedVar((e, c, l, v) -> readResource(c, literal), true);
    }

    public static ResolvedVar resolveIndexed(ResourceLocation id, ExprNode index) {
        return new ResolvedVar((e, c, l, v) -> {
            int slot = (int) Math.round(index.eval(e, c, l, v));
            return readResourceAt(c, id, slot);
        }, true);
    }

    private static @Nullable ResolvedVar resolvePrefixed(String name) {
        boolean target = name.startsWith(TARGET_PREFIX);
        if (!target && !name.startsWith(ACTOR_PREFIX)) return null;
        String base = name.substring(target ? TARGET_PREFIX.length() : ACTOR_PREFIX.length());
        if (base.indexOf(':') >= 0) return null;
        ResolvedVar delegate = VARS.get(base);
        if (delegate == null) return null;
        ExprVar accessor = delegate.accessor();
        int slot = target ? ExprPeer.TARGET : ExprPeer.ACTOR;
        return new ResolvedVar((e, c, l, v) -> {
            Entity bound = ExprPeer.frame()[slot];
            return accessor.get(bound != null ? bound : e, c, l, v);
        }, delegate.needsContainer(), true);
    }

    private static @Nullable ResolvedVar resolveBound(String name, String suffix, boolean max,
                                                      @Nullable ResourceLocation literal) {
        if (!name.endsWith(suffix)) return null;
        ResourceLocation base = ResourceLocation.tryParse(name.substring(0, name.length() - suffix.length()));
        if (base == null) return null;
        return new ResolvedVar((e, c, l, v) -> readBound(c, base, literal, max), true);
    }

    private static @Nullable ResolvedVar resolveSize(String name, @Nullable ResourceLocation literal) {
        if (!name.endsWith("_size")) return null;
        ResourceLocation base = ResourceLocation.tryParse(name.substring(0, name.length() - "_size".length()));
        if (base == null) return null;
        return new ResolvedVar((e, c, l, v) -> {
            int size = PowerResources.size(c, base);
            if (size > 0) return size;
            return literal == null ? 0 : readResource(c, literal);
        }, true);
    }

    private static final ThreadLocal<int[]> BOUND_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private static double readBound(@Nullable PowerContainer container, ResourceLocation base,
                                    @Nullable ResourceLocation literal, boolean max) {
        int[] depth = BOUND_DEPTH.get();
        if (depth[0] >= 8) return 0;
        depth[0]++;
        try {
            OptionalInt bound = PowerResources.bound(container, base, max);
            if (bound.isPresent()) return bound.getAsInt();
        } finally {
            depth[0]--;
        }
        return literal == null ? 0 : readResource(container, literal);
    }

    public static double readResource(@Nullable PowerContainer container, ResourceLocation id) {
        if (container == null) return 0;
        OptionalInt value = PowerResources.read(container, id);
        return value.isPresent() ? value.getAsInt() : container.getAuxIntOr(id, 0);
    }

    public static double readResourceAt(@Nullable PowerContainer container, ResourceLocation id, int slot) {
        if (container == null) return 0;
        OptionalInt value = PowerResources.readAt(container, id, slot);
        return value.isPresent() ? value.getAsInt() : 0;
    }

    private static void registerIdFunctions() {
        ExprParser.registerIdFunction("resource", (id, args, name, at) -> {
            if (args.isEmpty()) return new ResolvedVar((e, c, l, v) -> readResource(c, id), true);
            ExprNode index = args.get(0);
            return new ResolvedVar((e, c, l, v) ->
                readResourceAt(c, id, (int) Math.round(index.eval(e, c, l, v))), true);
        });
        ExprParser.registerIdFunction("target_resource", (id, args, name, at) -> {
            ExprNode index = args.isEmpty() ? null : args.get(0);
            return new ResolvedVar((e, c, l, v) -> {
                PowerContainer peer = peerContainer(e, c);
                if (index == null) return readResource(peer, id);
                return readResourceAt(peer, id, (int) Math.round(index.eval(e, c, l, v)));
            }, true, true);
        });
        ExprParser.registerIdFunction("has_power", (id, args, name, at) ->
            new ResolvedVar((e, c, l, v) -> c != null && c.hasPower(id) ? 1 : 0, true));
        ExprParser.registerIdFunction("target_has_power", (id, args, name, at) ->
            new ResolvedVar((e, c, l, v) -> {
                PowerContainer peer = peerContainer(e, c);
                return peer != null && peer.hasPower(id) ? 1 : 0;
            }, true, true));
        ExprParser.registerIdFunction("has_resource", (id, args, name, at) ->
            new ResolvedVar((e, c, l, v) -> PowerResources.read(c, id).isPresent() ? 1 : 0, true));
        ExprParser.registerIdFunction("resource_size", (id, args, name, at) ->
            new ResolvedVar((e, c, l, v) -> PowerResources.size(c, id), true));
        ExprParser.registerIdFunction("resource_contains", (id, args, name, at) -> {
            if (args.isEmpty()) {
                throw new ExprParseException("resource_contains(id, value) expects a value to look for", at);
            }
            ExprNode wanted = args.get(0);
            return new ResolvedVar((e, c, l, v) ->
                PowerResources.indexOf(c, id, (int) Math.round(wanted.eval(e, c, l, v))) >= 0 ? 1 : 0, true);
        });
        ExprParser.registerIdFunction("resource_index_of", (id, args, name, at) -> {
            if (args.isEmpty()) {
                throw new ExprParseException("resource_index_of(id, value) expects a value to look for", at);
            }
            ExprNode wanted = args.get(0);
            return new ResolvedVar((e, c, l, v) ->
                PowerResources.indexOf(c, id, (int) Math.round(wanted.eval(e, c, l, v))), true);
        });
    }

    private static @Nullable PowerContainer peerContainer(@Nullable Entity primary, @Nullable PowerContainer own) {
        Entity peer = ExprPeer.target();
        if (peer == null || peer == primary) return own;
        return PowerContainer.of(peer);
    }

    private static Level levelOf(@Nullable Entity entity, @Nullable Level level) {
        if (level != null) return level;
        return entity != null ? entity.level() : null;
    }

    static {
        register("value", (e, c, l, v) -> v);

        registerContext("damage");
        registerContext("distance");
        registerContext("hit_x");
        registerContext("hit_y");
        registerContext("hit_z");
        registerContext("count");
        registerContext("index");

        register("health", (e, c, l, v) -> e instanceof LivingEntity le ? le.getHealth() : 0);
        register("max_health", (e, c, l, v) -> e instanceof LivingEntity le ? le.getMaxHealth() : 0);
        register("absorption", (e, c, l, v) -> e instanceof LivingEntity le ? le.getAbsorptionAmount() : 0);
        register("armor", (e, c, l, v) -> e instanceof LivingEntity le ? le.getArmorValue() : 0);
        register("air", (e, c, l, v) -> e != null ? e.getAirSupply() : 0);
        register("max_air", (e, c, l, v) -> e != null ? e.getMaxAirSupply() : 0);
        register("fall_distance", (e, c, l, v) -> e != null ? e.fallDistance : 0);

        register("x", (e, c, l, v) -> e != null ? e.getX() : 0);
        register("y", (e, c, l, v) -> e != null ? e.getY() : 0);
        register("z", (e, c, l, v) -> e != null ? e.getZ() : 0);
        register("yaw", (e, c, l, v) -> e != null ? e.getYRot() : 0);
        register("pitch", (e, c, l, v) -> e != null ? e.getXRot() : 0);
        register("velocity_x", (e, c, l, v) -> e != null ? e.getDeltaMovement().x : 0);
        register("velocity_y", (e, c, l, v) -> e != null ? e.getDeltaMovement().y : 0);
        register("velocity_z", (e, c, l, v) -> e != null ? e.getDeltaMovement().z : 0);

        register("food", (e, c, l, v) -> e instanceof Player p ? p.getFoodData().getFoodLevel() : 0);
        register("saturation", (e, c, l, v) -> e instanceof Player p ? p.getFoodData().getSaturationLevel() : 0);
        register("xp_level", (e, c, l, v) -> e instanceof Player p ? p.experienceLevel : 0);
        register("xp_progress", (e, c, l, v) -> e instanceof Player p ? p.experienceProgress : 0);

        register("power_count", (e, c, l, v) -> c == null ? 0 : c.allPowers().size());

        register("voice_loudness", (e, c, l, v) ->
            e == null ? 0 : VoiceState.loudness(e.getUUID()));
        register("voice_loudness_normalized", (e, c, l, v) ->
            e == null ? 0 : VoiceState.loudness(e.getUUID()) / 100.0);
        register("voice_speaking", (e, c, l, v) ->
            e != null && VoiceState.isSpeaking(e.getUUID()) ? 1 : 0);
        register("voice_whispering", (e, c, l, v) ->
            e != null && VoiceState.isWhispering(e.getUUID()) ? 1 : 0);
        register("voice_disabled", (e, c, l, v) ->
            e != null && (VoiceState.isDisabled(e.getUUID()) || VoiceState.isDisconnected(e.getUUID())) ? 1 : 0);

        registerIdFunctions();

        register("world_time", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getGameTime() : 0;
        });
        register("day_time", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getDayTime() : 0;
        });
        register("difficulty", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getDifficulty().getId() : 0;
        });
        register("moon_phase", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getMoonPhase() : 0;
        });
    }
}
