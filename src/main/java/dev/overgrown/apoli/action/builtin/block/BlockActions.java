package dev.overgrown.apoli.action.builtin.block;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;

public final class BlockActions {
    private BlockActions() {}

    public static void register() {
        ActionTypes.BLOCK.register(Apoli.id("add_block"), new AddBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("break_block"), new BreakBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("bonemeal"), new BonemealBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("execute_command"), new ExecuteCommandBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("store_data"), new StoreDataBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("explode"), new ExplodeBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("ghost_block"), new GhostBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("modify_block_state"), new ModifyBlockStateAction());
        ActionTypes.BLOCK.register(Apoli.id("set_block"), new SetBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("spawn_entity"), new SpawnEntityBlockAction());
        ActionTypes.BLOCK.register(Apoli.id("offset"), new OffsetBlockMetaAction());
        ActionTypes.BLOCK.register(Apoli.id("area_of_effect"), new AreaOfEffectBlockMetaAction());

        ActionTypes.BLOCK.register(Apoli.id("script"), new ScriptBlockAction());
    }
}
