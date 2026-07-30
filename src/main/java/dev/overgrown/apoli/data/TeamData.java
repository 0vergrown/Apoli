package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record TeamData(
    Optional<String> name,
    Optional<Boolean> friendlyFire,
    Optional<Boolean> seeFriendlyInvisibles,
    Optional<Team.Visibility> nametagVisibility,
    Optional<Team.Visibility> deathMessageVisibility,
    Optional<Team.CollisionRule> collisionRule,
    Optional<ChatFormatting> color
) {

    public static final Codec<Team.Visibility> VISIBILITY = enumCodec(
        Team.Visibility.values(), v -> v.name, Team.Visibility::byName, "nametag visibility");

    public static final Codec<Team.CollisionRule> COLLISION_RULE = enumCodec(
        Team.CollisionRule.values(), r -> r.name, Team.CollisionRule::byName, "collision rule");

    public static final Codec<ChatFormatting> COLOR = Codec.STRING.comapFlatMap(
        raw -> {
            ChatFormatting formatting = ChatFormatting.getByName(raw);
            return formatting == null
                ? DataResult.error(() -> "Unknown team color: " + raw)
                : DataResult.success(formatting);
        },
        ChatFormatting::getName);

    private static final Map<String, String> LEGACY_FIELDS = Map.of(
        "friendlyFire", "friendly_fire",
        "showFriendlyInvisibles", "see_friendly_invisibles",
        "seeFriendlyInvisibles", "see_friendly_invisibles",
        "nametagVisibility", "nametag_visibility",
        "nameTagVisibility", "nametag_visibility",
        "deathMessageVisibility", "death_message_visibility",
        "collisionRule", "collision_rule");

    private static final MapCodec<TeamData> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(TeamData::name),
            Codec.BOOL.optionalFieldOf("friendly_fire").forGetter(TeamData::friendlyFire),
            Codec.BOOL.optionalFieldOf("see_friendly_invisibles").forGetter(TeamData::seeFriendlyInvisibles),
            VISIBILITY.optionalFieldOf("nametag_visibility").forGetter(TeamData::nametagVisibility),
            VISIBILITY.optionalFieldOf("death_message_visibility").forGetter(TeamData::deathMessageVisibility),
            COLLISION_RULE.optionalFieldOf("collision_rule").forGetter(TeamData::collisionRule),
            COLOR.optionalFieldOf("color").forGetter(TeamData::color)
        ).apply(i, TeamData::new)),
        LEGACY_FIELDS);

    public static final Codec<TeamData> CODEC = Codec.either(Codec.STRING, MAP_CODEC.codec()).xmap(
        either -> either.map(TeamData::named, data -> data),
        data -> data.isNameOnly() ? Either.left(data.name().orElseThrow()) : Either.right(data));

    public static final Codec<List<TeamData>> LIST_OR_SINGLE = Codec.either(CODEC, Codec.list(CODEC)).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    public static TeamData named(String name) {
        return new TeamData(Optional.of(name), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean isNameOnly() {
        return name.isPresent() && friendlyFire.isEmpty() && seeFriendlyInvisibles.isEmpty()
            && nametagVisibility.isEmpty() && deathMessageVisibility.isEmpty()
            && collisionRule.isEmpty() && color.isEmpty();
    }

    public boolean isEmpty() {
        return name.isEmpty() && friendlyFire.isEmpty() && seeFriendlyInvisibles.isEmpty()
            && nametagVisibility.isEmpty() && deathMessageVisibility.isEmpty()
            && collisionRule.isEmpty() && color.isEmpty();
    }

    public boolean matches(@Nullable Team team) {
        if (team == null) return false;
        if (isEmpty()) return true;
        if (name.isPresent() && !name.get().equals(team.getName())) return false;
        if (friendlyFire.isPresent() && friendlyFire.get() != team.isAllowFriendlyFire()) return false;
        if (seeFriendlyInvisibles.isPresent() && seeFriendlyInvisibles.get() != team.canSeeFriendlyInvisibles()) return false;
        if (nametagVisibility.isPresent() && nametagVisibility.get() != team.getNameTagVisibility()) return false;
        if (deathMessageVisibility.isPresent() && deathMessageVisibility.get() != team.getDeathMessageVisibility()) return false;
        if (collisionRule.isPresent() && collisionRule.get() != team.getCollisionRule()) return false;
        return color.isEmpty() || color.get() == team.getColor();
    }

    public boolean matches(@Nullable Entity entity) {
        return entity != null && matches(entity.getTeam());
    }

    public void applyTo(PlayerTeam team) {
        friendlyFire.ifPresent(team::setAllowFriendlyFire);
        seeFriendlyInvisibles.ifPresent(team::setSeeFriendlyInvisibles);
        nametagVisibility.ifPresent(team::setNameTagVisibility);
        deathMessageVisibility.ifPresent(team::setDeathMessageVisibility);
        collisionRule.ifPresent(team::setCollisionRule);
        color.ifPresent(team::setColor);
    }

    private static <E extends Enum<E>> Codec<E> enumCodec(E[] values, java.util.function.Function<E, String> naming,
                                                          java.util.function.Function<String, E> byName, String label) {
        return Codec.STRING.comapFlatMap(raw -> {
            E direct = byName.apply(raw);
            if (direct != null) return DataResult.success(direct);
            String normalised = raw.replace("_", "").toLowerCase(Locale.ROOT);
            for (E value : values) {
                if (naming.apply(value).toLowerCase(Locale.ROOT).equals(normalised)) return DataResult.success(value);
            }
            return DataResult.error(() -> "Unknown " + label + ": " + raw);
        }, naming);
    }
}
