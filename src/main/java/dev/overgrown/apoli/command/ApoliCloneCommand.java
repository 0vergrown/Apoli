package dev.overgrown.apoli.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.entity.summon.Summons;
import dev.overgrown.apoli.power.ApoliPowers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ApoliCloneCommand {

    private static final CompoundTag NO_OPTIONS = new CompoundTag();

    private ApoliCloneCommand() {}

    private record Options(int count, int lifetime, boolean canAttack, boolean canSit, boolean followOwner,
                           boolean slim, boolean inheritEquipment, List<ResourceLocation> powers,
                           @Nullable ResourceLocation summonId, @Nullable ResourceLocation wideTexture,
                           @Nullable ResourceLocation slimTexture) {

        static Options read(CompoundTag tag) {
            return new Options(
                clamp(tag.contains("count") ? tag.getInt("count") : 1, 1, 64),
                tag.contains("lifetime") ? tag.getInt("lifetime") : 1200,
                !tag.contains("can_attack") || tag.getBoolean("can_attack"),
                !tag.contains("can_sit") || tag.getBoolean("can_sit"),
                !tag.contains("follow_owner") || tag.getBoolean("follow_owner"),
                tag.getBoolean("slim"),
                !tag.contains("inherit_equipment") || tag.getBoolean("inherit_equipment"),
                readIds(tag),
                readId(tag, "summon_id"),
                readId(tag, "wide_texture"),
                readId(tag, "slim_texture"));
        }

        private static int clamp(int value, int min, int max) {
            return value < min ? min : Math.min(value, max);
        }

        private static @Nullable ResourceLocation readId(CompoundTag tag, String key) {
            return tag.contains(key) ? ResourceLocation.tryParse(tag.getString(key)) : null;
        }

        private static List<ResourceLocation> readIds(CompoundTag tag) {
            if (tag.contains("powers", Tag.TAG_STRING)) {
                ResourceLocation single = ResourceLocation.tryParse(tag.getString("powers"));
                return single == null ? List.of() : List.of(single);
            }
            if (!tag.contains("powers", Tag.TAG_LIST)) return List.of();
            ListTag list = tag.getList("powers", Tag.TAG_STRING);
            List<ResourceLocation> out = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) out.add(id);
            }
            return out;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("apoli:clone")
            .requires(ApoliPermissions.require("apoli.command.clone", 2));

        root.then(Commands.literal("summon")
            .then(Commands.argument("owners", EntityArgument.entities())
                .executes(ctx -> summon(ctx, null, NO_OPTIONS))
                .then(Commands.argument("options", CompoundTagArgument.compoundTag())
                    .executes(ctx -> summon(ctx, null, CompoundTagArgument.getCompoundTag(ctx, "options"))))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                    .executes(ctx -> summon(ctx, Vec3Argument.getVec3(ctx, "pos"), NO_OPTIONS))
                    .then(Commands.argument("options", CompoundTagArgument.compoundTag())
                        .executes(ctx -> summon(ctx, Vec3Argument.getVec3(ctx, "pos"),
                            CompoundTagArgument.getCompoundTag(ctx, "options")))))));

        root.then(Commands.literal("remove")
            .then(Commands.argument("owners", EntityArgument.entities())
                .executes(ctx -> remove(ctx, null))
                .then(Commands.argument("summon_id", ResourceLocationArgument.id())
                    .executes(ctx -> remove(ctx, ResourceLocationArgument.getId(ctx, "summon_id"))))));

        root.then(Commands.literal("list")
            .then(Commands.argument("owners", EntityArgument.entities())
                .executes(ApoliCloneCommand::list)));

        dispatcher.register(root);
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, @Nullable Vec3 pos, CompoundTag optionsTag)
        throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        Options options = Options.read(optionsTag);
        for (ResourceLocation power : options.powers()) {
            if (ApoliPowers.get(power) == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown power: " + power));
                return 0;
            }
        }
        int spawned = 0;
        for (Entity owner : EntityArgument.getEntities(ctx, "owners")) {
            if (!(owner instanceof Player player)) {
                ctx.getSource().sendFailure(Component.literal(owner.getName().getString()
                    + " is not a player — clones copy a player's skin and equipment."));
                continue;
            }
            Vec3 at = pos != null ? pos : player.position();
            for (int i = 0; i < options.count(); i++) {
                if (spawn(level, player, at, options) != null) spawned++;
            }
        }
        final int result = spawned;
        if (result == 0) {
            ctx.getSource().sendFailure(Component.literal("No clones were summoned."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Summoned " + result + " clone(s)."), true);
        return spawned;
    }

    private static @Nullable CloneEntity spawn(ServerLevel level, Player owner, Vec3 pos, Options options) {
        CloneEntity clone = ApoliEntities.cloneType().create(level);
        if (clone == null) return null;
        clone.setOwner(owner);
        clone.setCanAttack(options.canAttack());
        clone.setCanSit(options.canSit());
        clone.setFollowOwner(options.followOwner());
        clone.setSlim(options.slim());
        clone.setMaxLifeTime(options.lifetime());
        clone.setCustomName(owner.getName());
        if (options.summonId() != null) clone.setSummonId(options.summonId());
        if (options.wideTexture() != null) clone.setWideTexture(options.wideTexture());
        if (options.slimTexture() != null) clone.setSlimTexture(options.slimTexture());
        clone.moveTo(pos.x, pos.y, pos.z, owner.getYHeadRot(), 0);
        if (options.inheritEquipment()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack equipped = owner.getItemBySlot(slot);
                if (equipped.isEmpty()) continue;
                clone.setDropChance(slot, 0f);
                clone.setItemSlot(slot, equipped.copy());
            }
        }
        level.addFreshEntity(clone);
        clone.updateWeaponGoals();
        Summons.grantPowers(clone, options.powers());
        return clone;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, @Nullable ResourceLocation summonId)
        throws CommandSyntaxException {
        int removed = 0;
        for (CloneEntity clone : clonesOf(ctx)) {
            if (summonId != null && !summonId.equals(clone.getSummonId())) continue;
            clone.discard();
            removed++;
        }
        final int result = removed;
        if (result == 0) {
            ctx.getSource().sendFailure(Component.literal("No matching clones found."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Removed " + result + " clone(s)."), true);
        return removed;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<CloneEntity> clones = clonesOf(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal(clones.size() + " clone(s):"), false);
        for (CloneEntity clone : clones) {
            ResourceLocation summonId = clone.getSummonId();
            ctx.getSource().sendSuccess(() -> Component.literal("  " + clone.getStringUUID()
                + " owner=" + (clone.getOwner() == null ? "none" : clone.getOwner().getName().getString())
                + (summonId == null ? "" : " id=" + summonId)
                + " lifetime=" + clone.getMaxLifeTime()), false);
        }
        return clones.size();
    }

    private static List<CloneEntity> clonesOf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<CloneEntity> out = new ArrayList<>();
        ServerLevel level = ctx.getSource().getLevel();
        for (Entity owner : EntityArgument.getEntities(ctx, "owners")) {
            for (CloneEntity clone : level.getEntitiesOfClass(CloneEntity.class,
                owner.getBoundingBox().inflate(256.0), c -> c.getOwner() == owner)) {
                out.add(clone);
            }
        }
        return out;
    }
}
