package dev.overgrown.apoli.compat.birdsong;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.expr.ExprVars;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class BirdsongPlaceholders {
    private BirdsongPlaceholders() {}

    private static final String[] POWERS = {"action_on_speak", "action_on_reply", "action_on_speech", "modify_speaking_range"};
    private static final String[] VARIABLES = {"voice_loudness", "voice_loudness_normalized", "voice_speaking",
        "voice_whispering", "voice_disabled"};

    public static void register() {
        for (String power : POWERS) {
            PowerTypeRegistry.aliases().registerTypeAlias(Apoli.id(power), Apoli.id("simple"));
        }
        ConditionTypes.ENTITY.register(Apoli.id("voice_speaking"), new Silent(), AliasingOptions.builder()
            .addTypeAlias(Apoli.id("voice_disabled"))
            .addTypeAlias(Apoli.id("voice_muted"))
            .addTypeAlias(Apoli.id("voice_loudness"))
            .addTypeAlias(Apoli.id("voice_listeners"))
            .addTypeAlias(Apoli.id("voice_whispering"))
            .build());
        for (String variable : VARIABLES) {
            ExprVars.register(variable, (entity, container, level, value) -> 0);
        }
    }

    private static final class Silent implements ConditionType<EntityCtx, EmptyCfg> {
        private static final MapCodec<EmptyCfg> CODEC = MapCodec.unit(EmptyCfg.INSTANCE);

        @Override
        public MapCodec<EmptyCfg> codec() {
            return CODEC;
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return false;
        }
    }
}
