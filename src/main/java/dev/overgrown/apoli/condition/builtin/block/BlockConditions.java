package dev.overgrown.apoli.condition.builtin.block;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class BlockConditions {
    private BlockConditions() {}

    public static void register() {
        ConditionTypes.BLOCK.register(Apoli.id("adjacent"), new AdjacentBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("attachable"), new AttachableBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("blast_resistance"), new BlastResistanceBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("block"), new BlockIdCondition());
        ConditionTypes.BLOCK.register(Apoli.id("block_entity"), new BlockEntityBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("block_state"), new BlockStateBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("command"), new CommandBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("distance_from_coordinates"), new DistanceFromCoordinatesBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("exposed_to_sky"), new ExposedToSkyBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("fluid"), new FluidBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("hardness"), new HardnessBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("height"), new HeightBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("in_tag"), new InTagBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("light_blocking"), new LightBlockingBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("light_level"), new LightLevelBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("movement_blocking"), new MovementBlockingBlockCondition());
        ConditionTypes.BLOCK.register(Apoli.id("nbt"), new NbtBlockCondition());
        ConditionTypes.BLOCK.register(
            Apoli.id("replacable"),
            new ReplacableBlockCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("replaceable")).build()
        );
        ConditionTypes.BLOCK.register(
            Apoli.id("slipperiness"),
            new SlipperinessBlockCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("slipperness")).build()
        );
        ConditionTypes.BLOCK.register(Apoli.id("water_loggable"), new WaterLoggableBlockCondition());

        ConditionTypes.BLOCK.register(Apoli.id("script"), new ScriptBlockCondition());
    }
}
