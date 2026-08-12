package dev.overgrown.apoli.compat.skills;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.BuiltinJson;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;

public final class ModifyResourceReward implements Reward {

    public static final ResourceLocation ID = Apoli.id("modify_resource");

    private enum Operation {
        ADD,
        SET
    }

    private final ResourceLocation resource;
    private final Operation operation;
    private final int value;
    private final int base;

    private ModifyResourceReward(ResourceLocation resource, Operation operation, int value, int base) {
        this.resource = resource;
        this.operation = operation;
        this.value = value;
        this.base = base;
    }

    static void register() {
        SkillsAPI.registerReward(ID, ModifyResourceReward::parse);
    }

    private static Result<ModifyResourceReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
            .andThen(JsonElement::getAsObject)
            .andThen(rootObject -> rootObject.noUnused(ModifyResourceReward::parse));
    }

    private static Result<ModifyResourceReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        var optResource = rootObject.get("resource")
            .andThen(BuiltinJson::parseIdentifier)
            .ifFailure(problems::add)
            .getSuccess();

        var optValue = rootObject.getInt("value")
            .ifFailure(problems::add)
            .getSuccess();

        var operation = rootObject.get("operation")
            .getSuccess()
            .flatMap(element -> parseOperation(element).ifFailure(problems::add).getSuccess())
            .orElse(Operation.ADD);

        var base = rootObject.getInt("base").getSuccess().orElse(0);

        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }
        return Result.success(new ModifyResourceReward(
            optResource.orElseThrow(), operation, optValue.orElseThrow(), base));
    }

    private static Result<Operation, Problem> parseOperation(JsonElement element) {
        return element.getAsString().andThen(string -> switch (string) {
            case "add" -> Result.success(Operation.ADD);
            case "set" -> Result.success(Operation.SET);
            default -> Result.failure(element.getPath().createProblem(
                "Expected operation `add` or `set`, but got `" + string + "`"));
        });
    }

    @Override
    public void update(RewardUpdateContext context) {
        apply(context.getPlayer(), context.getCount());
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            apply(player, 0);
        }
    }

    private void apply(ServerPlayer player, int count) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        int target = operation == Operation.ADD ? base + value * count : (count > 0 ? value : base);
        PowerResources.write(container, resource, target);
    }
}
