package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;

public enum ActionResult implements StringRepresentable {
    SUCCESS("success", InteractionResult.SUCCESS),
    CONSUME("consume", InteractionResult.CONSUME),
    CONSUME_PARTIAL("consume_partial", InteractionResult.CONSUME_PARTIAL),
    PASS("pass", InteractionResult.PASS),
    FAIL("fail", InteractionResult.FAIL);

    public static final Codec<ActionResult> CODEC = StringRepresentable.fromEnum(ActionResult::values);

    private final String name;
    private final InteractionResult vanilla;

    ActionResult(String name, InteractionResult vanilla) {
        this.name = name;
        this.vanilla = vanilla;
    }

    public InteractionResult vanilla() {
        return vanilla;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
