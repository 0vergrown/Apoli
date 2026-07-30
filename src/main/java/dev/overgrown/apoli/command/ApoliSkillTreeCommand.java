package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.skill.Skill;
import dev.overgrown.apoli.skill.SkillData;
import dev.overgrown.apoli.skill.SkillDataAttachment;
import dev.overgrown.apoli.skill.SkillRegistry;
import dev.overgrown.apoli.skill.SkillTree;
import dev.overgrown.apoli.skill.SkillTrees;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ApoliSkillTreeCommand {

    private static final SuggestionProvider<CommandSourceStack> TREES =
        (ctx, builder) -> SharedSuggestionProvider.suggestResource(SkillRegistry.roots(), builder);

    private static final SuggestionProvider<CommandSourceStack> SKILLS = (ctx, builder) -> {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Skill skill : SkillRegistry.all()) ids.add(skill.id());
        return SharedSuggestionProvider.suggestResource(ids, builder);
    };

    private ApoliSkillTreeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:skill_tree")
            .requires(ApoliPermissions.require("apoli.command.skill_tree", 2));

        root.then(Commands.literal("buy")
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("skill", ResourceLocationArgument.id())
                    .suggests(SKILLS)
                    .executes(ctx -> buy(ctx, false))
                    .then(Commands.literal("force")
                        .executes(ctx -> buy(ctx, true))))));

        root.then(Commands.literal("unbuy")
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("skill", ResourceLocationArgument.id())
                    .suggests(SKILLS)
                    .executes(ApoliSkillTreeCommand::unbuy))));

        root.then(Commands.literal("points")
            .then(Commands.literal("get")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("tree", ResourceLocationArgument.id())
                        .suggests(TREES)
                        .executes(ApoliSkillTreeCommand::pointsGet))))
            .then(Commands.literal("set")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("tree", ResourceLocationArgument.id())
                        .suggests(TREES)
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(ApoliSkillTreeCommand::pointsSet)))))
            .then(Commands.literal("add")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("tree", ResourceLocationArgument.id())
                        .suggests(TREES)
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(ctx -> pointsAdd(ctx, 1))))))
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("tree", ResourceLocationArgument.id())
                        .suggests(TREES)
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                            .executes(ctx -> pointsAdd(ctx, -1)))))));

        root.then(Commands.literal("grant")
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("tree", ResourceLocationArgument.id())
                    .suggests(TREES)
                    .executes(ApoliSkillTreeCommand::grant))));

        root.then(Commands.literal("revoke")
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("tree", ResourceLocationArgument.id())
                    .suggests(TREES)
                    .executes(ApoliSkillTreeCommand::revoke))));

        root.then(Commands.literal("reset")
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> reset(ctx, null, true))
                .then(Commands.literal("norefund")
                    .executes(ctx -> reset(ctx, null, false)))
                .then(Commands.argument("tree", ResourceLocationArgument.id())
                    .suggests(TREES)
                    .executes(ctx -> reset(ctx, ResourceLocationArgument.getId(ctx, "tree"), true))
                    .then(Commands.literal("norefund")
                        .executes(ctx -> reset(ctx, ResourceLocationArgument.getId(ctx, "tree"), false))))));

        root.then(Commands.literal("list")
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ApoliSkillTreeCommand::list)));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);

        dispatcher.register(Commands.literal("skills")
            .requires(ApoliPermissions.require("apoli.command.skill_tree", 2))
            .redirect(node));
    }

    private static int buy(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
        ResourceLocation skill = ResourceLocationArgument.getId(ctx, "skill");
        if (!knownSkill(ctx, skill)) return 0;
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            boolean ok = force ? SkillTrees.forcePurchase(player, skill) : SkillTrees.tryPurchase(player, skill);
            if (ok) {
                ApoliNetwork.sendSkillState(player);
                done++;
            }
        }
        feedbackCount(ctx, done, (force ? "Force-bought " : "Bought ") + skill + " for %d player(s).",
            "No purchases made (already purchased, or requirements not met — try 'force').");
        return done;
    }

    private static boolean knownSkill(CommandContext<CommandSourceStack> ctx, ResourceLocation skill) {
        if (SkillRegistry.get(skill) != null) return true;
        int loaded = SkillRegistry.all().size();
        ctx.getSource().sendFailure(Component.literal("Unknown skill: " + skill
            + (loaded == 0
                ? ". No skills are loaded — define them with a 'skill' block in a power file, or as skill_trees files carrying a 'parent'."
                : ". " + loaded + " skill(s) are loaded; check the id and that its parent chain ends at a skill tree.")));
        return false;
    }

    private static int unbuy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation skill = ResourceLocationArgument.getId(ctx, "skill");
        if (!knownSkill(ctx, skill)) return 0;
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            if (SkillTrees.tryRefund(player, skill, true)) {
                ApoliNetwork.sendSkillState(player);
                done++;
            }
        }
        feedbackCount(ctx, done, "Refunded " + skill + " for %d player(s).", "Nobody had that skill purchased.");
        return done;
    }

    private static int pointsGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation tree = ResourceLocationArgument.getId(ctx, "tree");
        if (!knownTree(ctx, tree)) return 0;
        int points = SkillDataAttachment.get(player).getPoints(tree);
        ctx.getSource().sendSuccess(() -> Component.literal(
            player.getGameProfile().getName() + " has " + points + " point(s) in " + tree + "."), false);
        return points;
    }

    private static int pointsSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation tree = ResourceLocationArgument.getId(ctx, "tree");
        if (!knownTree(ctx, tree)) return 0;
        int value = IntegerArgumentType.getInteger(ctx, "value");
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            SkillDataAttachment.get(player).setPoints(tree, value);
            ApoliNetwork.sendSkillState(player);
            done++;
        }
        feedbackCount(ctx, done, "Set " + tree + " points to " + value + " for %d player(s).", "No players matched.");
        return done;
    }

    private static int pointsAdd(CommandContext<CommandSourceStack> ctx, int sign) throws CommandSyntaxException {
        ResourceLocation tree = ResourceLocationArgument.getId(ctx, "tree");
        if (!knownTree(ctx, tree)) return 0;
        int value = IntegerArgumentType.getInteger(ctx, "value") * sign;
        int done = 0;
        int last = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            SkillData data = SkillDataAttachment.get(player);
            data.addPoints(tree, value);
            last = data.getPoints(tree);
            ApoliNetwork.sendSkillState(player);
            done++;
        }
        final int now = last;
        feedbackCount(ctx, done, (value < 0 ? "Removed " + (-value) : "Added " + value) + " " + tree
            + " point(s) for %d player(s); now " + now + ".", "No players matched.");
        return done;
    }

    private static boolean knownTree(CommandContext<CommandSourceStack> ctx, ResourceLocation tree) {
        if (SkillRegistry.tree(tree) != null) return true;
        StringBuilder known = new StringBuilder();
        for (ResourceLocation id : SkillRegistry.roots()) {
            if (known.length() > 0) known.append(", ");
            known.append(id);
        }
        ctx.getSource().sendFailure(Component.literal("Unknown skill tree: " + tree
            + (known.length() == 0
                ? ". No skill trees are loaded — check data/<namespace>/skill_trees/."
                : ". Loaded trees: " + known)));
        return false;
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation tree = ResourceLocationArgument.getId(ctx, "tree");
        if (SkillRegistry.tree(tree) == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown skill tree: " + tree));
            return 0;
        }
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            if (SkillTrees.grantTree(player, tree)) done++;
        }
        feedbackCount(ctx, done, "Granted " + tree + " to %d player(s).",
            "No changes (already granted; note auto_grant trees are always available).");
        return done;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation tree = ResourceLocationArgument.getId(ctx, "tree");
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            if (SkillTrees.revokeTree(player, tree)) done++;
        }
        feedbackCount(ctx, done, "Revoked " + tree + " from %d player(s).",
            "No changes (nobody had an explicit grant; auto_grant trees cannot be revoked).");
        return done;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, ResourceLocation tree, boolean refund)
        throws CommandSyntaxException {
        int done = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            SkillTrees.resetSkills(player, tree, refund);
            ApoliNetwork.sendSkillState(player);
            done++;
        }
        String scope = tree == null ? "all trees" : tree.toString();
        feedbackCount(ctx, done, "Reset " + scope + (refund ? " (points refunded)" : " (no refund)")
            + " for %d player(s).", "No players matched.");
        return done;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        SkillData data = SkillDataAttachment.get(player);

        StringBuilder trees = new StringBuilder();
        for (SkillTree tree : SkillRegistry.trees()) {
            boolean available = SkillTrees.treeAvailable(data, tree.id());
            if (trees.length() > 0) trees.append(", ");
            trees.append(tree.id()).append(tree.autoGrant() ? " (auto)" : available ? " (granted)" : " (not granted)");
        }
        String name = player.getGameProfile().getName();
        ctx.getSource().sendSuccess(() -> Component.literal(name + " — trees: "
            + (trees.length() == 0 ? "none" : trees)), false);

        StringBuilder points = new StringBuilder();
        for (Map.Entry<ResourceLocation, Integer> e : data.pointsView().entrySet()) {
            if (points.length() > 0) points.append(", ");
            points.append(e.getKey()).append(" = ").append(e.getValue());
        }
        ctx.getSource().sendSuccess(() -> Component.literal("  points: "
            + (points.length() == 0 ? "none" : points)), false);

        Collection<ResourceLocation> purchased = data.purchasedView();
        StringBuilder skills = new StringBuilder();
        for (ResourceLocation id : purchased) {
            if (skills.length() > 0) skills.append(", ");
            skills.append(id);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("  purchased (" + purchased.size() + "): "
            + (skills.length() == 0 ? "none" : skills)), false);
        return purchased.size();
    }

    private static void feedbackCount(CommandContext<CommandSourceStack> ctx, int count,
                                      String successFormat, String noneMessage) {
        if (count > 0) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(successFormat, count)), true);
        } else {
            ctx.getSource().sendFailure(Component.literal(noneMessage));
        }
    }
}
