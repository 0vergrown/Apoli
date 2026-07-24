package dev.overgrown.apoli.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ApoliDisguiseCommand {

    private static final ResourceLocation PLAYER_TYPE = ResourceLocation.withDefaultNamespace("player");

    private ApoliDisguiseCommand() {}

    private static final SuggestionProvider<CommandSourceStack> ENTITY_TYPES =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(
            ctx.getSource().getServer().getPlayerList().getPlayers().stream().map(p -> p.getGameProfile().getName()), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:disguise")
            .requires(ApoliPermissions.require("apoli.command.disguise", 2));

        root.then(Commands.literal("entity")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                    .suggests(ENTITY_TYPES)
                    .executes(ctx -> disguiseAsEntity(ctx, null))
                    .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                        .executes(ctx -> disguiseAsEntity(ctx, CompoundTagArgument.getCompoundTag(ctx, "nbt")))))));

        root.then(Commands.literal("player")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("player_name", StringArgumentType.word())
                    .suggests(ONLINE_PLAYERS)
                    .executes(ApoliDisguiseCommand::disguiseAsPlayer))));

        root.then(Commands.literal("random")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ctx -> disguiseRandom(ctx, RandomKind.ANY))
                .then(Commands.literal("player")
                    .executes(ctx -> disguiseRandom(ctx, RandomKind.PLAYER)))
                .then(Commands.literal("entity")
                    .executes(ctx -> disguiseRandom(ctx, RandomKind.ENTITY)))
                .then(Commands.literal("any")
                    .executes(ctx -> disguiseRandom(ctx, RandomKind.ANY)))));

        root.then(Commands.literal("clear")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(ApoliDisguiseCommand::clear)));

        root.then(Commands.literal("query")
            .then(Commands.argument("target", EntityArgument.entity())
                .executes(ApoliDisguiseCommand::query)));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);

        dispatcher.register(Commands.literal("disguise")
            .requires(ApoliPermissions.require("apoli.command.disguise", 2))
            .redirect(node));
    }

    private enum RandomKind { PLAYER, ENTITY, ANY }

    private static int disguiseAsEntity(CommandContext<CommandSourceStack> ctx, CompoundTag nbt) throws CommandSyntaxException {
        ResourceLocation typeId = ResourceLocationArgument.getId(ctx, "entity_type");
        if (BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown entity type: " + typeId));
            return 0;
        }
        DisguiseData data = new DisguiseData(typeId, Optional.empty(), Optional.ofNullable(nbt), Optional.empty());
        int count = 0;
        for (Entity target : EntityArgument.getEntities(ctx, "targets")) {
            DisguiseManager.apply(target, data, true);
            count++;
        }
        feedback(ctx, count, "as " + typeId);
        return count;
    }

    private static int disguiseAsPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "player_name");
        Optional<GameProfile> profile = resolveProfile(ctx.getSource().getServer(), name);
        if (profile.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown player: " + name));
            return 0;
        }
        int count = 0;
        for (Entity target : EntityArgument.getEntities(ctx, "targets")) {
            DisguiseManager.apply(target, playerData(profile.get()), true);
            count++;
        }
        feedback(ctx, count, "as player " + profile.get().getName());
        return count;
    }

    private static int disguiseRandom(CommandContext<CommandSourceStack> ctx, RandomKind kind) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();
        int count = 0;
        for (Entity target : EntityArgument.getEntities(ctx, "targets")) {
            RandomKind resolved = kind;
            if (resolved == RandomKind.ANY) {
                resolved = ThreadLocalRandom.current().nextBoolean() ? RandomKind.PLAYER : RandomKind.ENTITY;
            }
            if (resolved == RandomKind.PLAYER) {
                GameProfile profile = randomPlayerProfile(server, target.getUUID());
                if (profile != null) {
                    DisguiseManager.apply(target, playerData(profile), true);
                    count++;
                    continue;
                }
                if (kind == RandomKind.PLAYER) continue;
            }
            ResourceLocation typeId = randomEntityTypeId(target);
            if (typeId == null) continue;
            DisguiseManager.apply(target, new DisguiseData(typeId, Optional.empty(), Optional.empty(), Optional.empty()), true);
            count++;
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal("No valid random disguise found for the given targets."));
            return 0;
        }
        feedback(ctx, count, "randomly");
        return count;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int count = 0;
        for (Entity target : EntityArgument.getEntities(ctx, "targets")) {
            if (DisguiseManager.isDisguised(target)) {
                DisguiseManager.remove(target);
                count++;
            }
        }
        int result = count;
        ctx.getSource().sendSuccess(() -> Component.literal("Removed disguises from " + result + " entit" + (result == 1 ? "y" : "ies") + "."), true);
        return count;
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(ctx, "target");
        DisguiseData data = DisguiseManager.get(target.getUUID());
        if (data == null) {
            ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is not disguised."), false);
            return 0;
        }
        String as = data.isPlayerDisguise()
            ? "player " + data.name().filter(n -> !n.isEmpty()).orElseGet(() -> data.playerUuid().map(UUID::toString).orElse("?"))
            : data.entityTypeId().toString();
        ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is disguised as " + as + "."), false);
        return 1;
    }

    private static DisguiseData playerData(GameProfile profile) {
        String name = profile.getName();
        return new DisguiseData(PLAYER_TYPE, Optional.of(profile.getId()), Optional.empty(),
            Optional.ofNullable(name == null || name.isEmpty() ? null : name));
    }

    private static Optional<GameProfile> resolveProfile(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return Optional.of(online.getGameProfile());
        return server.getProfileCache() != null ? server.getProfileCache().get(name) : Optional.empty();
    }

    private static GameProfile randomPlayerProfile(MinecraftServer server, UUID excluded) {
        List<ServerPlayer> pool = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(excluded)) pool.add(player);
        }
        if (pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size())).getGameProfile();
    }

    private static ResourceLocation randomEntityTypeId(Entity target) {
        List<ResourceLocation> pool = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!type.canSummon() || type.getCategory() == MobCategory.MISC) continue;
            if (type == target.getType() && !(target instanceof Player)) continue;
            pool.add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        if (pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, int count, String what) {
        if (count == 0) {
            ctx.getSource().sendFailure(Component.literal("No entities were disguised."));
            return;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Disguised " + count + " entit" + (count == 1 ? "y" : "ies") + " " + what + "."), true);
    }
}
