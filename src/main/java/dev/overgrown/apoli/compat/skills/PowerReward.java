package dev.overgrown.apoli.compat.skills;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
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

public final class PowerReward implements Reward {

    public static final ResourceLocation ID = Apoli.id("power");

    private enum Operation {
        ADD,
        REMOVE
    }

    private final ResourceLocation power;
    private final Operation operation;
    private final ResourceLocation source;

    private PowerReward(ResourceLocation power, Operation operation, ResourceLocation source) {
        this.power = power;
        this.operation = operation;
        this.source = source;
    }

    static void register() {
        SkillsAPI.registerReward(ID, PowerReward::parse);
    }

    private static Result<PowerReward, Problem> parse(RewardConfigContext context) {
        return context.getData()
            .andThen(JsonElement::getAsObject)
            .andThen(rootObject -> rootObject.noUnused(PowerReward::parse));
    }

    private static Result<PowerReward, Problem> parse(JsonObject rootObject) {
        var problems = new ArrayList<Problem>();

        var optPower = rootObject.get("power")
            .andThen(BuiltinJson::parseIdentifier)
            .ifFailure(problems::add)
            .getSuccess();

        var operation = rootObject.get("operation")
            .getSuccess()
            .flatMap(element -> parseOperation(element).ifFailure(problems::add).getSuccess())
            .orElse(Operation.ADD);

        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }
        return Result.success(new PowerReward(optPower.orElseThrow(), operation, SkillsCompat.newSource()));
    }

    private static Result<Operation, Problem> parseOperation(JsonElement element) {
        return element.getAsString().andThen(string -> switch (string) {
            case "add" -> Result.success(Operation.ADD);
            case "remove" -> Result.success(Operation.REMOVE);
            default -> Result.failure(element.getPath().createProblem(
                "Expected operation `add` or `remove`, but got `" + string + "`"));
        });
    }

    @Override
    public void update(RewardUpdateContext context) {
        apply(context.getPlayer(), context.getCount() > 0);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            apply(player, false);
        }
    }

    private void apply(ServerPlayer player, boolean unlocked) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        if ((operation == Operation.ADD) == unlocked) {
            container.addPower(power, source);
        } else {
            container.removePower(power, source);
        }
    }
}
