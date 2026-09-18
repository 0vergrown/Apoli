package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record TextureRef(ResourceLocation texture, Subject subject, Optional<ResourceLocation> set) {

    public enum Kind {
        PLAYER("player"),
        PLAYER_FACE("player_face"),
        PLAYER_CAPE("player_cape"),
        ENTITY("entity"),
        HELD_ITEM("held_item"),
        OFFHAND_ITEM("offhand_item");

        private final String keyword;
        private final ResourceLocation sentinel;

        Kind(String keyword) {
            this.keyword = keyword;
            this.sentinel = Apoli.id("dynamic_texture/" + keyword);
        }

        public String keyword() {
            return keyword;
        }

        public ResourceLocation sentinel() {
            return sentinel;
        }

        public boolean isItem() {
            return this == HELD_ITEM || this == OFFHAND_ITEM;
        }
    }

    public enum Subject implements StringRepresentable {
        SELF("@s"),
        VIEWER("@p");

        private final String name;

        Subject(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final String SENTINEL_PREFIX = "dynamic_texture/";

    public static final Codec<ResourceLocation> ID_CODEC =
        Codec.STRING.comapFlatMap(TextureRef::parseId, ResourceLocation::toString);

    private record Raw(String texture, String selector, Optional<ResourceLocation> set) {}

    private static final MapCodec<Raw> RAW = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("texture").forGetter(Raw::texture),
        Codec.STRING.optionalFieldOf("selector", "@s").forGetter(Raw::selector),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("set", IdCodecs.ID).forGetter(Raw::set)
    ).apply(i, Raw::new));

    public static final MapCodec<TextureRef> MAP_CODEC = RAW.flatXmap(TextureRef::fromRaw, TextureRef::toRaw);

    public static final Codec<TextureRef> CODEC = Codec.either(Codec.STRING, MAP_CODEC.codec()).comapFlatMap(
        either -> either.map(
            string -> parseId(string).map(id -> new TextureRef(id, Subject.SELF, Optional.empty())),
            DataResult::success),
        ref -> ref.subject == Subject.SELF && ref.set.isEmpty()
            ? Either.left(ref.texture.toString())
            : Either.right(ref));

    @Nullable
    public static Kind kindOf(@Nullable ResourceLocation id) {
        if (id == null || !Apoli.MOD_ID.equals(id.getNamespace())) return null;
        String path = id.getPath();
        if (!path.startsWith(SENTINEL_PREFIX)) return null;
        String keyword = path.substring(SENTINEL_PREFIX.length());
        for (Kind kind : Kind.values()) {
            if (kind.keyword.equals(keyword)) return kind;
        }
        return null;
    }

    public static boolean isDynamic(@Nullable ResourceLocation id) {
        return kindOf(id) != null;
    }

    @Nullable
    public Kind kind() {
        return kindOf(texture);
    }

    private static DataResult<ResourceLocation> parseId(String raw) {
        Kind keyword = keyword(raw);
        if (keyword != null) return DataResult.success(keyword.sentinel());
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        return parsed != null
            ? DataResult.success(parsed)
            : DataResult.error(() -> "Not a texture id or a dynamic texture keyword: " + raw);
    }

    @Nullable
    private static Kind keyword(String raw) {
        String normalised = raw.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (normalised) {
            case "player", "skin", "player_skin" -> Kind.PLAYER;
            case "player_face", "face", "head" -> Kind.PLAYER_FACE;
            case "player_cape", "cape", "cloak" -> Kind.PLAYER_CAPE;
            case "entity", "entity_texture" -> Kind.ENTITY;
            case "held_item", "item", "main_hand", "mainhand" -> Kind.HELD_ITEM;
            case "offhand_item", "off_hand", "offhand" -> Kind.OFFHAND_ITEM;
            default -> null;
        };
    }

    private static DataResult<TextureRef> fromRaw(Raw raw) {
        DataResult<ResourceLocation> id = parseId(raw.texture());
        if (id.error().isPresent()) return DataResult.error(() -> id.error().get().message());
        Subject subject = subject(raw.selector());
        if (subject == null) {
            return DataResult.error(() -> "Unsupported selector '" + raw.selector()
                + "'; textures resolve on the client, so only @s (the power holder) and @p (the viewing player) work");
        }
        return DataResult.success(new TextureRef(id.result().get(), subject, raw.set()));
    }

    private static DataResult<Raw> toRaw(TextureRef ref) {
        return DataResult.success(new Raw(ref.texture.toString(), ref.subject.getSerializedName(), ref.set));
    }

    @Nullable
    private static Subject subject(String selector) {
        return switch (selector.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "@s", "self", "holder" -> Subject.SELF;
            case "@p", "viewer", "client", "@a" -> Subject.VIEWER;
            default -> null;
        };
    }
}
