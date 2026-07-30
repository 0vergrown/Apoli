package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ModifyCursorSpeedPower extends PowerType<ModifyCursorSpeedPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_cursor_speed");

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         boolean horizontal,
                         boolean vertical) {
        public List<AttributeModifier> flattened() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("horizontal", true).forGetter(Config::horizontal),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("vertical", true).forGetter(Config::vertical)
        ).apply(i, Config::new));
    }

    public record Multipliers(double x, double y) {
        public static final Multipliers NONE = new Multipliers(1.0, 1.0);

        public boolean isIdentity() {
            return x == 1.0 && y == 1.0;
        }
    }

    public static Multipliers compute(@Nullable Entity entity) {
        if (entity == null) return Multipliers.NONE;
        List<Config> configs = PowerLookup.active(entity, CANONICAL, Config.class);
        if (configs.isEmpty()) return Multipliers.NONE;
        double x = 1.0;
        double y = 1.0;
        for (int i = 0, n = configs.size(); i < n; i++) {
            Config cfg = configs.get(i);
            List<AttributeModifier> mods = cfg.flattened();
            if (mods.isEmpty()) continue;
            if (cfg.horizontal()) x = AttributeModifierHelper.apply(x, mods, entity);
            if (cfg.vertical()) y = AttributeModifierHelper.apply(y, mods, entity);
        }
        return x == 1.0 && y == 1.0 ? Multipliers.NONE : new Multipliers(x, y);
    }
}
