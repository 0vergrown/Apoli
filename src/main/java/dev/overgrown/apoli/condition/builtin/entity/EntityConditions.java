package dev.overgrown.apoli.condition.builtin.entity;
import dev.overgrown.apoli.compat.voicechat.VoiceDisabledCondition;
import dev.overgrown.apoli.compat.voicechat.VoiceListenersCondition;
import dev.overgrown.apoli.compat.voicechat.VoiceLoudnessCondition;
import dev.overgrown.apoli.compat.voicechat.VoiceSpeakingCondition;
import dev.overgrown.apoli.compat.voicechat.VoiceWhisperingCondition;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.accessory.condition.entity.AccessoryEquippedCountCondition;
import dev.overgrown.apoli.compat.accessory.condition.entity.AccessorySlotCountCondition;
import dev.overgrown.apoli.compat.hardcorerevival.condition.KnockedOutCondition;
import dev.overgrown.apoli.compat.walkers.condition.ShapeConditions;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class EntityConditions {
    private EntityConditions() {}

    public static void register() {
        ConditionTypes.ENTITY.register(
            Apoli.id("attack_charge"),
            new AttackChargeCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("spam_attack"))
                .addTypeAlias(Apoli.id("attack_cooldown"))
                .build()
        );
        ConditionTypes.ENTITY.register(Apoli.id("entity_type"), new EntityTypeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("has_command_tag"), new HasCommandTagCondition());
        ConditionTypes.ENTITY.register(Apoli.id("difficulty"), new DifficultyCondition());
        ConditionTypes.ENTITY.register(Apoli.id("invisible"), new InvisibleCondition());
        ConditionTypes.ENTITY.register(Apoli.id("player_model_type"), new PlayerModelTypeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("sneaking"), new SneakingCondition());
        ConditionTypes.ENTITY.register(
            Apoli.id("key_pressed"),
            new KeyPressedCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("key_held"))
                .addTypeAlias(Apoli.id("held_key"))
                .build()
        );
        ConditionTypes.ENTITY.register(Apoli.id("status_effect"), new StatusEffectCondition());
        ConditionTypes.ENTITY.register(
            Apoli.id("entity_set_size"),
            new EntitySetSizeCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("set_size")).build()
        );
        ConditionTypes.ENTITY.register(Apoli.id("resource"), new ResourceCondition());
        ConditionTypes.ENTITY.register(
            Apoli.id("in_team"),
            new InTeamCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("team")).build()
        );
        ConditionTypes.ENTITY.register(Apoli.id("living"), new LivingCondition());
        ConditionTypes.ENTITY.register(Apoli.id("passenger"), new PassengerCondition());
        ConditionTypes.ENTITY.register(Apoli.id("riding"), new RidingCondition());
        ConditionTypes.ENTITY.register(Apoli.id("equipped_item"), new EquippedItemCondition());
        ConditionTypes.ENTITY.register(Apoli.id("climbing"), SimpleFlagConditions.climbing());
        ConditionTypes.ENTITY.register(Apoli.id("collided_horizontally"), SimpleFlagConditions.collidedHoriz());
        ConditionTypes.ENTITY.register(Apoli.id("creative_flying"), SimpleFlagConditions.creativeFly());
        ConditionTypes.ENTITY.register(Apoli.id("daytime"), SimpleFlagConditions.daytime());
        ConditionTypes.ENTITY.register(Apoli.id("exists"), SimpleFlagConditions.exists());
        ConditionTypes.ENTITY.register(Apoli.id("exposed_to_sky"), SimpleFlagConditions.exposedToSky());
        ConditionTypes.ENTITY.register(Apoli.id("exposed_to_sun"), SimpleFlagConditions.exposedToSun());
        ConditionTypes.ENTITY.register(Apoli.id("fall_flying"), SimpleFlagConditions.fallFlying());
        ConditionTypes.ENTITY.register(Apoli.id("glowing"), SimpleFlagConditions.glowing());
        ConditionTypes.ENTITY.register(Apoli.id("in_rain"), SimpleFlagConditions.inRain());
        ConditionTypes.ENTITY.register(Apoli.id("in_snow"), SimpleFlagConditions.inSnow());
        ConditionTypes.ENTITY.register(Apoli.id("in_thunderstorm"), SimpleFlagConditions.inThunder());
        ConditionTypes.ENTITY.register(Apoli.id("on_fire"), SimpleFlagConditions.onFire());
        ConditionTypes.ENTITY.register(Apoli.id("sprinting"), SimpleFlagConditions.sprinting());
        ConditionTypes.ENTITY.register(Apoli.id("swimming"), SimpleFlagConditions.swimming());
        ConditionTypes.ENTITY.register(Apoli.id("tamed"), SimpleFlagConditions.tamed());
        ConditionTypes.ENTITY.register(Apoli.id("using_effective_tool"), SimpleFlagConditions.usingEffectiveTool());
        ConditionTypes.ENTITY.register(Apoli.id("air"), new AirCondition());
        ConditionTypes.ENTITY.register(Apoli.id("attribute"), new AttributeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("damage_would_kill"), new DamageWouldKillCondition());
        ConditionTypes.ENTITY.register(Apoli.id("brightness"), new BrightnessCondition());
        ConditionTypes.ENTITY.register(Apoli.id("command"), new CommandCondition());
        ConditionTypes.ENTITY.register(Apoli.id("enchantment"), new EnchantmentCondition());
        ConditionTypes.ENTITY.register(Apoli.id("fall_distance"), new FallDistanceCondition());
        ConditionTypes.ENTITY.register(Apoli.id("fluid_height"), new FluidHeightCondition());
        ConditionTypes.ENTITY.register(Apoli.id("health"), new HealthCondition());
        ConditionTypes.ENTITY.register(Apoli.id("relative_health"), new RelativeHealthCondition());
        ConditionTypes.ENTITY.register(Apoli.id("time_of_day"), new TimeOfDayCondition());

        ConditionTypes.ENTITY.register(Apoli.id("hunger"), new HungerCondition(HungerCondition.Kind.FOOD));
        ConditionTypes.ENTITY.register(Apoli.id("food_level"), new HungerCondition(HungerCondition.Kind.FOOD));
        ConditionTypes.ENTITY.register(Apoli.id("saturation_level"), new HungerCondition(HungerCondition.Kind.SATURATION));

        ConditionTypes.ENTITY.register(Apoli.id("xp"), new XpCondition(XpCondition.Unit.LEVELS));
        ConditionTypes.ENTITY.register(Apoli.id("xp_levels"), new XpCondition(XpCondition.Unit.LEVELS));
        ConditionTypes.ENTITY.register(Apoli.id("xp_points"), new XpCondition(XpCondition.Unit.POINTS));

        ConditionTypes.ENTITY.register(Apoli.id("advancement"), new AdvancementCondition());
        ConditionTypes.ENTITY.register(Apoli.id("ability"), new AbilityCondition());
        ConditionTypes.ENTITY.register(Apoli.id("dimension"), new DimensionCondition());
        ConditionTypes.ENTITY.register(Apoli.id("entity_group"), new EntityGroupCondition());
        ConditionTypes.ENTITY.register(Apoli.id("gamemode"), new GamemodeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("in_tag"), new InTagCondition());
        ConditionTypes.ENTITY.register(Apoli.id("nbt"), new NbtCondition());
        ConditionTypes.ENTITY.register(Apoli.id("predicate"), new PredicateCondition());
        ConditionTypes.ENTITY.register(Apoli.id("submerged_in"), new SubmergedInCondition());
        ConditionTypes.ENTITY.register(Apoli.id("moving"), new MovingCondition());
        ConditionTypes.ENTITY.register(Apoli.id("using_item"), new UsingItemCondition());
        ConditionTypes.ENTITY.register(Apoli.id("biome"), new BiomeCondition());

        ConditionTypes.ENTITY.register(Apoli.id("in_block"), new InBlockCondition());
        ConditionTypes.ENTITY.register(Apoli.id("in_block_anywhere"), new InBlockAnywhereCondition());
        ConditionTypes.ENTITY.register(Apoli.id("on_block"), new OnBlockCondition());
        ConditionTypes.ENTITY.register(Apoli.id("block_collision"), new BlockCollisionCondition());
        ConditionTypes.ENTITY.register(Apoli.id("block_in_radius"), new BlockInRadiusCondition());
        ConditionTypes.ENTITY.register(Apoli.id("entity_in_radius"), new EntityInRadiusCondition());

        ConditionTypes.ENTITY.register(Apoli.id("power"), new PowerCondition());
        ConditionTypes.ENTITY.register(Apoli.id("power_active"), new PowerActiveCondition());
        ConditionTypes.ENTITY.register(Apoli.id("power_type"), new PowerTypeCondition());

        ConditionTypes.ENTITY.register(Apoli.id("scoreboard"), new ScoreboardCondition());
        ConditionTypes.ENTITY.register(Apoli.id("stat"), new StatCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("stats"))
                .addTypeAlias(Apoli.id("status"))
                .addTypeAlias(Apoli.id("statistic"))
                .renameField("status", "stat")
                .build());
        ConditionTypes.ENTITY.register(Apoli.id("distance_from_coordinates"), new DistanceFromCoordinatesCondition());

        ConditionTypes.ENTITY.register(Apoli.id("passenger_recursive"), new PassengerRecursiveCondition());
        ConditionTypes.ENTITY.register(Apoli.id("riding_recursive"), new RidingRecursiveCondition());
        ConditionTypes.ENTITY.register(Apoli.id("riding_root"), new RidingRootCondition());

        ConditionTypes.ENTITY.register(Apoli.id("elytra_flight_possible"), new ElytraFlightPossibleCondition());
        ConditionTypes.ENTITY.register(Apoli.id("inventory"), new InventoryCondition());
        ConditionTypes.ENTITY.register(Apoli.id("raycast"), new RaycastEntityCondition());

        ConditionTypes.ENTITY.register(Apoli.id("voice_speaking"), new VoiceSpeakingCondition());
        ConditionTypes.ENTITY.register(
            Apoli.id("voice_disabled"),
            new VoiceDisabledCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("voice_muted")).build()
        );
        ConditionTypes.ENTITY.register(Apoli.id("voice_loudness"), new VoiceLoudnessCondition());
        ConditionTypes.ENTITY.register(Apoli.id("voice_listeners"), new VoiceListenersCondition());
        ConditionTypes.ENTITY.register(Apoli.id("voice_whispering"), new VoiceWhisperingCondition());

        ConditionTypes.ENTITY.register(Apoli.id("attached_to_rope"), new AttachedToRopeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("disguised"), new DisguisedCondition());

        if (ModCompat.anyAccessory()) {
            ConditionTypes.ENTITY.register(Apoli.id("accessory_equipped_count"), new AccessoryEquippedCountCondition(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("equipped_trinket_count")).build());
            ConditionTypes.ENTITY.register(Apoli.id("accessory_slot_count"), new AccessorySlotCountCondition(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("trinket_slot_count")).build());
        }

        if (ModCompat.HARDCORE_REVIVAL) {
            ConditionTypes.ENTITY.register(Apoli.id("knocked_out"), new KnockedOutCondition());
        }

        ConditionTypes.ENTITY.register(Apoli.id("script"), new ScriptCondition());

        if (ModCompat.WALKERS) {
            ConditionTypes.ENTITY.register(Apoli.id("can_use_shape_ability"), new ShapeConditions.CanUseShapeAbility());
            ConditionTypes.ENTITY.register(Apoli.id("has_shape_ability"), new ShapeConditions.HasShapeAbility());
            ConditionTypes.ENTITY.register(Apoli.id("shape_ability_cooldown"), new ShapeConditions.ShapeAbilityCooldown());
            ConditionTypes.ENTITY.register(
                Apoli.id("shape_condition"),
                new ShapeConditions.Shape(),
                dev.overgrown.apoli.alias.AliasingOptions.builder()
                    .addTypeAlias(Apoli.id("shape"))
                    .addTypeAlias("shappoli:shape_condition")
                    .addTypeAlias("shappoli:shape")
                    .build()
            );
        }

        ConditionTypes.ENTITY.register(Apoli.id("send_condition"),
            new dev.overgrown.apoli.condition.builtin.meta.SendConditionMeta.Entity(),
            AliasingOptions.builder().addTypeAlias("shappoli:send_condition").build());
    }
}
