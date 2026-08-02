package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;

public enum PlayerModelType implements StringRepresentable {
    WIDE("wide"),
    SLIM("slim");

    public static final Codec<PlayerModelType> CODEC = Codec.STRING.comapFlatMap(
        PlayerModelType::byName, PlayerModelType::getSerializedName);

    private final String name;

    PlayerModelType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static DataResult<PlayerModelType> byName(String raw) {
        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "wide", "default", "classic", "steve" -> DataResult.success(WIDE);
            case "slim", "alex" -> DataResult.success(SLIM);
            default -> DataResult.error(() -> "Unknown player model type '" + raw + "' (expected wide or slim)");
        };
    }
}
