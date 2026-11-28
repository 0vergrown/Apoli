package dev.overgrown.apoli.gamerule;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.expr.ExprParser;
import dev.overgrown.apoli.data.expr.ExprVars;
import dev.overgrown.apoli.loader.JsonDataFiles;
import dev.overgrown.apoli.loader.LoadProblems;
import dev.overgrown.apoli.network.payload.GameRulesS2C;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

// Data pack gamerules: data/<namespace>/gamerules/<name>.json adds the rule <namespace>:<name>. It is changed with
// /gamerule and read with the apoli:gamerule condition or the gamerule(...) expression function. Values are saved
// with the world and sent to players, so conditions checked on the client see them too.
public final class DataGameRules {
    private DataGameRules() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "gamerules";
    private static final String SAVE_NAME = "apoli_gamerules";

    public enum Type implements StringRepresentable {
        BOOLEAN, INTEGER;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // true/false or a whole number. The JSON itself is checked, since some versions read a number as a boolean.
    private static final Codec<Either<Boolean, Integer>> DEFAULT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        JsonElement json = dynamic.convert(JsonOps.INSTANCE).getValue();
        if (json instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) return DataResult.success(Either.left(primitive.getAsBoolean()));
            if (primitive.isNumber() && primitive.getAsDouble() == Math.rint(primitive.getAsDouble())) return DataResult.success(Either.right(primitive.getAsInt()));
        }
        return DataResult.error(() -> "Not true, false or a whole number: " + json);
    }, value -> new Dynamic<>(JsonOps.INSTANCE, value.map(JsonPrimitive::new, JsonPrimitive::new)));

    // A rule's definition. Booleans are stored as 1 and 0.
    public record Rule(Type type, int defaultValue, int min, int max) {
        static final Codec<Rule> CODEC = RecordCodecBuilder.<Raw>create(i -> i.group(
            Type.CODEC.fieldOf("type").forGetter(Raw::type),
            DEFAULT.optionalFieldOf("default").forGetter(Raw::def),
            Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(Raw::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(Raw::max)
        ).apply(i, Raw::new)).comapFlatMap(Raw::toRule, Raw::of);

        int clamp(int value) {
            return type == Type.BOOLEAN ? (value != 0 ? 1 : 0) : Mth.clamp(value, min, max);
        }

        String display(int value) {
            return type == Type.BOOLEAN ? String.valueOf(value != 0) : String.valueOf(value);
        }
    }

    private record Raw(Type type, Optional<Either<Boolean, Integer>> def, int min, int max) {
        DataResult<Rule> toRule() {
            if (min > max) return DataResult.error(() -> "min is larger than max");
            int value = def.map(e -> e.map(b -> b ? 1 : 0, n -> n)).orElse(0);
            if (type == Type.BOOLEAN) return DataResult.success(new Rule(type, value != 0 ? 1 : 0, 0, 1));
            if (def.isPresent() && def.get().left().isPresent()) return DataResult.error(() -> "an integer rule needs a number as its default");
            return DataResult.success(new Rule(type, Mth.clamp(value, min, max), min, max));
        }

        static Raw of(Rule rule) {
            Either<Boolean, Integer> def = rule.type() == Type.BOOLEAN ? Either.left(rule.defaultValue() != 0) : Either.right(rule.defaultValue());
            return new Raw(rule.type(), Optional.of(def), rule.min(), rule.max());
        }
    }

    private static volatile Map<ResourceLocation, Rule> rules = Map.of();
    private static volatile Map<ResourceLocation, Integer> client = Map.of();
    private static @Nullable Values saved;
    private static @Nullable MinecraftServer server;

    public static void init() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Loader());
        ServerLifecycleEvents.SERVER_STARTED.register(started -> {
            server = started;
            saved = started.overworld().getDataStorage().computeIfAbsent(Values.FACTORY, SAVE_NAME);
            addCommands(started);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((reloaded, resources, success) -> {
            addCommands(reloaded);
            sendAll(reloaded);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            server = null;
            saved = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, joined) -> joined.execute(() -> send(handler.getPlayer(), payload())));
        // gamerule(<namespace>:<name>): the rule's value, with true and false as 1 and 0.
        ExprParser.registerIdFunction("gamerule", (rule, args, name, at) -> new ExprVars.ResolvedVar(
            (entity, container, level, value) -> value(rule, level != null ? level : entity != null ? entity.level() : null), false));
        GameRuleCondition.register();
    }

    private static final class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>
        implements IdentifiableResourceReloadListener {
        private volatile Map<ResourceLocation, JsonDataFiles.Source> sources = Map.of();

        @Override
        public ResourceLocation getFabricId() {
            return Apoli.id("gamerules_reloader");
        }

        @Override
        protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, JsonDataFiles.Source> found = new HashMap<>();
            Map<ResourceLocation, JsonElement> loaded = JsonDataFiles.load(manager, DIR, LOG, found);
            sources = found;
            return loaded;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, Rule> found = new TreeMap<>();
            Map<ResourceLocation, JsonDataFiles.Source> files = sources;
            for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
                ResourceLocation id = e.getKey();
                LoadProblems.parse("gamerule", id, files.get(id), "$", Rule.CODEC, new Dynamic<>(JsonOps.INSTANCE, e.getValue()))
                    .ifPresent(rule -> found.put(id, rule));
            }
            rules = Map.copyOf(found);
            if (!found.isEmpty()) LOG.info("[Apoli] Loaded {} data pack gamerule(s).", found.size());
        }
    }

    // Adds the rules to the existing /gamerule command, then sends players the new command tree. This runs after
    // data packs load, since the command tree is built before them.
    private static void addCommands(MinecraftServer s) {
        Map<ResourceLocation, Rule> current = rules;
        if (current.isEmpty()) return;
        CommandDispatcher<CommandSourceStack> dispatcher = s.getCommands().getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("gamerule").requires(source -> source.hasPermission(2));
        for (Map.Entry<ResourceLocation, Rule> entry : current.entrySet()) {
            ResourceLocation id = entry.getKey();
            Rule rule = entry.getValue();
            ArgumentType<?> argument = rule.type() == Type.BOOLEAN ? BoolArgumentType.bool() : IntegerArgumentType.integer(rule.min(), rule.max());
            root.then(Commands.literal(id.toString())
                .executes(c -> query(c, id))
                .then(Commands.argument("value", argument).executes(c -> set(c, id, rule.type() == Type.BOOLEAN
                    ? (BoolArgumentType.getBool(c, "value") ? 1 : 0) : IntegerArgumentType.getInteger(c, "value")))));
        }
        dispatcher.register(root);
        for (ServerPlayer player : s.getPlayerList().getPlayers()) s.getCommands().sendCommands(player);
    }

    private static int query(CommandContext<CommandSourceStack> c, ResourceLocation id) {
        Rule rule = rules.get(id);
        if (rule == null) {
            c.getSource().sendFailure(Component.literal("The gamerule " + id + " is no longer defined by any data pack."));
            return 0;
        }
        int value = serverValue(id, rule);
        c.getSource().sendSuccess(() -> Component.translatable("commands.gamerule.query", id.toString(), rule.display(value)), false);
        return value;
    }

    private static int set(CommandContext<CommandSourceStack> c, ResourceLocation id, int value) {
        Rule rule = rules.get(id);
        Values values = saved;
        if (rule == null || values == null) {
            c.getSource().sendFailure(Component.literal("The gamerule " + id + " is no longer defined by any data pack."));
            return 0;
        }
        int clamped = rule.clamp(value);
        values.values.put(id.toString(), clamped);
        values.setDirty();
        sendAll(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable("commands.gamerule.set", id.toString(), rule.display(clamped)), true);
        return clamped;
    }

    // Saved values are keyed by the id's string; cached so a condition test doesn't build one each time.
    private static final Map<ResourceLocation, String> KEYS = new ConcurrentHashMap<>();

    private static int serverValue(ResourceLocation id, Rule rule) {
        Values values = saved;
        Integer stored = values == null ? null : values.values.get(KEYS.computeIfAbsent(id, ResourceLocation::toString));
        return stored == null ? rule.defaultValue() : rule.clamp(stored);
    }

    // The value of a data pack gamerule as a number (booleans are 1 and 0), or 0 when no pack defines it.
    public static int value(ResourceLocation id, @Nullable Level level) {
        if (level != null && level.isClientSide()) return client.getOrDefault(id, 0);
        Rule rule = rules.get(id);
        return rule == null ? 0 : serverValue(id, rule);
    }

    private static volatile Map<String, GameRules.Key<?>> vanillaKeys;

    // The value of a vanilla gamerule, such as keepInventory. The player's game only knows a few vanilla rules, so
    // this is only reliable on the server.
    public static int vanillaValue(String name, @Nullable Level level) {
        Map<String, GameRules.Key<?>> keys = vanillaKeys;
        if (keys == null) {
            Map<String, GameRules.Key<?>> found = new HashMap<>();
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    found.put(key.getId(), key);
                }
            });
            vanillaKeys = keys = Map.copyOf(found);
        }
        GameRules.Key<?> key = keys.get(name);
        if (key == null || level == null) return 0;
        GameRules.Value<?> value = level.getGameRules().getRule(key);
        if (value instanceof GameRules.BooleanValue b) return b.get() ? 1 : 0;
        if (value instanceof GameRules.IntegerValue n) return n.get();
        return 0;
    }

    // The level a static condition reads: the server's overworld, or null on a client with no server.
    static @Nullable Level serverLevel() {
        MinecraftServer s = server;
        return s == null ? null : s.overworld();
    }

    public static void applyClient(Map<ResourceLocation, Integer> values) {
        client = Map.copyOf(values);
    }

    public static void clearClient() {
        client = Map.of();
    }

    private static GameRulesS2C payload() {
        Map<ResourceLocation, Integer> values = new HashMap<>();
        rules.forEach((id, rule) -> values.put(id, serverValue(id, rule)));
        return new GameRulesS2C(values);
    }

    private static void sendAll(MinecraftServer s) {
        GameRulesS2C payload = payload();
        for (ServerPlayer player : s.getPlayerList().getPlayers()) send(player, payload);
    }

    private static void send(ServerPlayer player, GameRulesS2C payload) {
        if (player.connection == null) return;
        if (ServerPlayNetworking.canSend(player, GameRulesS2C.TYPE)) ServerPlayNetworking.send(player, payload);
    }

    // The values set with /gamerule, saved with the world. Values of rules no pack defines are kept.
    private static final class Values extends SavedData {
        final Map<String, Integer> values = new HashMap<>();

        static final SavedData.Factory<Values> FACTORY = new SavedData.Factory<>(Values::new, Values::read, null);

        static Values read(CompoundTag tag, HolderLookup.Provider registries) {
            Values out = new Values();
            for (String key : tag.getAllKeys()) out.values.put(key, tag.getInt(key));
            return out;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            values.forEach(tag::putInt);
            return tag;
        }
    }
}
