package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import dev.overgrown.apoli.tick.TickRateVanilla;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickState;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

public final class ApoliTickCommand {

    private static final int MAX_CHUNKS = 4096;
    private static final SimpleCommandExceptionType TOO_MANY_CHUNKS = new SimpleCommandExceptionType(
        Component.literal("That covers more than " + MAX_CHUNKS + " chunks; narrow the area."));

    private static final String[] RATE_SUGGESTIONS = {"20", "reset"};
    private static final String[] STEP_SUGGESTIONS = {"1t", "1s"};
    private static final String[] SPRINT_SUGGESTIONS = {"60s", "1d", "3d"};

    private ApoliTickCommand() {}

    private interface Op {
        int apply(CommandSourceStack source, Targets targets);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(branches("tick"));
        dispatcher.register(branches("apoli:tick"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> branches(String name) {
        return Commands.literal(name)
            .requires(ApoliPermissions.require("apoli.command.tick", 2))
            .then(Commands.literal("entity")
                .then(entityTargets()))
            .then(Commands.literal("chunk")
                .then(chunkTargets()));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> entityTargets() {
        var entities = Commands.argument("entities", EntityArgument.entities());
        attach(entities, ApoliTickCommand::entityTargets0);
        return entities;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> chunkTargets() {
        var from = Commands.argument("from", ColumnPosArgument.columnPos());
        attach(from, ApoliTickCommand::chunkTargetsFrom);
        var to = Commands.argument("to", ColumnPosArgument.columnPos());
        attach(to, ApoliTickCommand::chunkTargetsTo);
        from.then(to);
        var radius = Commands.argument("radius", FloatArgumentType.floatArg(0.0F));
        attach(radius, ApoliTickCommand::chunkTargetsRadius);
        from.then(Commands.literal("radius").then(radius));
        return from;
    }

    private static void attach(ArgumentBuilder<CommandSourceStack, ?> builder, Resolver resolver) {
        builder.then(Commands.literal("query")
            .executes(ctx -> run(ctx, resolver, ApoliTickCommand::query)));
        builder.then(Commands.literal("rate")
            .then(Commands.literal("reset").executes(ctx -> run(ctx, resolver, ApoliTickCommand::reset)))
            .then(Commands.argument("rate", FloatArgumentType.floatArg(1.0F, TickRates.MAX_RATE))
                .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(RATE_SUGGESTIONS, sb))
                .executes(ctx -> {
                    int rate = Math.round(FloatArgumentType.getFloat(ctx, "rate"));
                    return run(ctx, resolver, (source, targets) -> rate(source, targets, rate));
                })));
        builder.then(Commands.literal("freeze")
            .executes(ctx -> run(ctx, resolver, (source, targets) -> freeze(source, targets, true))));
        builder.then(Commands.literal("unfreeze")
            .executes(ctx -> run(ctx, resolver, (source, targets) -> freeze(source, targets, false))));
        builder.then(Commands.literal("step")
            .executes(ctx -> run(ctx, resolver, (source, targets) -> step(source, targets, 1)))
            .then(Commands.literal("stop").executes(ctx -> run(ctx, resolver, (source, targets) -> step(source, targets, 0))))
            .then(Commands.argument("time", TimeArgument.time(1))
                .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(STEP_SUGGESTIONS, sb))
                .executes(ctx -> {
                    int time = IntegerArgumentType.getInteger(ctx, "time");
                    return run(ctx, resolver, (source, targets) -> step(source, targets, time));
                })));
        builder.then(Commands.literal("sprint")
            .then(Commands.literal("stop").executes(ctx -> run(ctx, resolver, (source, targets) -> sprint(source, targets, 0))))
            .then(Commands.argument("time", TimeArgument.time(1))
                .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(SPRINT_SUGGESTIONS, sb))
                .executes(ctx -> {
                    int time = IntegerArgumentType.getInteger(ctx, "time");
                    return run(ctx, resolver, (source, targets) -> sprint(source, targets, time));
                })));
    }

    private interface Resolver {
        Targets resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private record Targets(List<? extends Entity> entities, LongArrayList chunks, ResourceKey<Level> dimension) {
        int size() {
            return entities != null ? entities.size() : chunks.size();
        }

        String noun() {
            return entities != null
                ? (size() == 1 ? "entity" : "entities")
                : (size() == 1 ? "chunk" : "chunks");
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Resolver resolver, Op op)
        throws CommandSyntaxException {
        Targets targets = resolver.resolve(ctx);
        if (targets.size() == 0) {
            ctx.getSource().sendFailure(Component.literal("No targets matched."));
            return 0;
        }
        return op.apply(ctx.getSource(), targets);
    }

    private static Targets entityTargets0(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "entities");
        return new Targets(List.copyOf(entities), null, ctx.getSource().getLevel().dimension());
    }

    private static Targets chunkTargetsFrom(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ColumnPos from = ColumnPosArgument.getColumnPos(ctx, "from");
        LongArrayList chunks = new LongArrayList(1);
        chunks.add(from.toChunkPos().toLong());
        return new Targets(null, chunks, ctx.getSource().getLevel().dimension());
    }

    private static Targets chunkTargetsTo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ColumnPos from = ColumnPosArgument.getColumnPos(ctx, "from");
        ColumnPos to = ColumnPosArgument.getColumnPos(ctx, "to");
        ChunkPos a = from.toChunkPos();
        ChunkPos b = to.toChunkPos();
        int minX = Math.min(a.x, b.x);
        int maxX = Math.max(a.x, b.x);
        int minZ = Math.min(a.z, b.z);
        int maxZ = Math.max(a.z, b.z);
        long total = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        if (total > MAX_CHUNKS) throw TOO_MANY_CHUNKS.create();
        LongArrayList chunks = new LongArrayList((int) total);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) chunks.add(ChunkPos.asLong(x, z));
        }
        return new Targets(null, chunks, ctx.getSource().getLevel().dimension());
    }

    private static Targets chunkTargetsRadius(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ColumnPos centre = ColumnPosArgument.getColumnPos(ctx, "from");
        float radius = FloatArgumentType.getFloat(ctx, "radius");
        int minX = net.minecraft.core.SectionPos.blockToSectionCoord((int) Math.floor(centre.x() - radius));
        int maxX = net.minecraft.core.SectionPos.blockToSectionCoord((int) Math.ceil(centre.x() + radius));
        int minZ = net.minecraft.core.SectionPos.blockToSectionCoord((int) Math.floor(centre.z() - radius));
        int maxZ = net.minecraft.core.SectionPos.blockToSectionCoord((int) Math.ceil(centre.z() + radius));
        long total = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        if (total > MAX_CHUNKS) throw TOO_MANY_CHUNKS.create();
        LongArrayList chunks = new LongArrayList();
        double radiusSq = (double) radius * radius;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int minBlockX = x << 4;
                int minBlockZ = z << 4;
                int nearestX = Math.max(minBlockX, Math.min(centre.x(), minBlockX + 15));
                int nearestZ = Math.max(minBlockZ, Math.min(centre.z(), minBlockZ + 15));
                double dx = nearestX - centre.x();
                double dz = nearestZ - centre.z();
                if (dx * dx + dz * dz <= radiusSq) chunks.add(ChunkPos.asLong(x, z));
            }
        }
        return new Targets(null, chunks, ctx.getSource().getLevel().dimension());
    }

    private static TickState stateOf(Targets targets, int index) {
        return targets.entities() != null
            ? TickRates.entityState(targets.entities().get(index))
            : TickRates.chunkState(targets.dimension(), targets.chunks().getLong(index));
    }

    private static TickState peek(Targets targets, int index) {
        return targets.entities() != null
            ? TickRates.peekEntity(targets.entities().get(index))
            : TickRates.peekChunk(targets.dimension(), targets.chunks().getLong(index));
    }

    private static int rate(CommandSourceStack source, Targets targets, int rate) {
        int base = TickRateVanilla.rate(source.getServer());
        for (int i = 0, n = targets.size(); i < n; i++) {
            TickState state = stateOf(targets, i);
            state.setRate(rate);
            state.setExpiry(TickState.PERMANENT);
        }
        int count = targets.size();
        String noun = targets.noun();
        source.sendSuccess(() -> Component.literal(
            "Set the tick rate of " + count + " " + noun + " to " + rate + " per second."), true);
        if (rate > base) {
            source.sendSuccess(() -> Component.literal(
                "Note: the server itself runs at " + base + " per second, so anything above that ticks normally. "
                    + "Raise the whole server with /tick rate first."), false);
        }
        return count;
    }

    private static int reset(CommandSourceStack source, Targets targets) {
        for (int i = 0, n = targets.size(); i < n; i++) {
            if (targets.entities() != null) TickRates.clearEntity(targets.entities().get(i));
            else TickRates.clearChunk(targets.dimension(), targets.chunks().getLong(i));
        }
        int count = targets.size();
        String noun = targets.noun();
        source.sendSuccess(() -> Component.literal("Reset the tick rate of " + count + " " + noun + "."), true);
        return count;
    }

    private static int freeze(CommandSourceStack source, Targets targets, boolean frozen) {
        for (int i = 0, n = targets.size(); i < n; i++) {
            TickState state = stateOf(targets, i);
            state.setFrozen(frozen);
            state.setExpiry(TickState.PERMANENT);
            if (!frozen && state.isDefault()) {
                if (targets.entities() != null) TickRates.clearEntity(targets.entities().get(i));
                else TickRates.clearChunk(targets.dimension(), targets.chunks().getLong(i));
            }
        }
        int count = targets.size();
        String noun = targets.noun();
        source.sendSuccess(() -> Component.literal(
            count + " " + noun + (frozen ? " have been frozen." : " have been unfrozen.")), true);
        return count;
    }

    private static int step(CommandSourceStack source, Targets targets, int ticks) {
        for (int i = 0, n = targets.size(); i < n; i++) {
            TickState state = peek(targets, i);
            if (state == null || !state.frozen() || state.sprintTicks() > 0) {
                source.sendFailure(Component.literal(
                    "Every target must be frozen and not sprinting before it can step."));
                return 0;
            }
        }
        for (int i = 0, n = targets.size(); i < n; i++) {
            stateOf(targets, i).setStepTicks(ticks);
        }
        int count = targets.size();
        String noun = targets.noun();
        source.sendSuccess(() -> Component.literal(ticks > 0
            ? count + " " + noun + " will step " + ticks + " ticks."
            : count + " " + noun + " have stopped stepping."), true);
        return count;
    }

    private static int sprint(CommandSourceStack source, Targets targets, int ticks) {
        for (int i = 0, n = targets.size(); i < n; i++) {
            TickState state = peek(targets, i);
            if (state != null && state.stepTicks() > 0) {
                source.sendFailure(Component.literal("A target that is stepping cannot sprint."));
                return 0;
            }
        }
        for (int i = 0, n = targets.size(); i < n; i++) {
            TickState state = stateOf(targets, i);
            state.setSprintTicks(ticks);
            state.setExpiry(TickState.PERMANENT);
        }
        int count = targets.size();
        String noun = targets.noun();
        source.sendSuccess(() -> Component.literal(ticks > 0
            ? count + " " + noun + " will sprint for " + ticks + " ticks."
            : count + " " + noun + " have stopped sprinting."), true);
        return count;
    }

    private static int query(CommandSourceStack source, Targets targets) {
        int base = TickRateVanilla.rate(source.getServer());
        StringBuilder sb = new StringBuilder("The tick rates of the specified ").append(targets.noun()).append(":");
        int first = base;
        for (int i = 0, n = targets.size(); i < n; i++) {
            int rate;
            String label;
            if (targets.entities() != null) {
                Entity entity = targets.entities().get(i);
                rate = TickRates.effectiveRate(entity, dev.overgrown.apoli.tick.TickScope.ENTITY);
                label = entity.getType().getDescription().getString() + " " + entity.getScoreboardName();
            } else {
                long pos = targets.chunks().getLong(i);
                ServerLevel level = source.getLevel();
                rate = TickRates.effectiveChunkRate(level, pos, base);
                label = "Chunk " + new ChunkPos(pos);
            }
            if (i == 0) first = rate;
            sb.append('\n').append(label).append(" - ")
                .append(rate == 0 ? "frozen" : rate + " per second");
        }
        String text = sb.toString();
        source.sendSuccess(() -> Component.literal(text), false);
        return first;
    }
}
