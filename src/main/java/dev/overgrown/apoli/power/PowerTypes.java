package dev.overgrown.apoli.power;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.accessory.power.ActionOnAccessoryChangePower;
import dev.overgrown.apoli.compat.accessory.power.ModifyAccessorySlotsPower;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryEquipPower;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryUnequipPower;
import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnKnockoutPower;
import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnRevivePower;
import dev.overgrown.apoli.compat.icarus.WingsPower;
import dev.overgrown.apoli.power.builtin.ActionOnBlockBreakPower;
import dev.overgrown.apoli.power.builtin.ActionOnCollisionPower;
import dev.overgrown.apoli.power.builtin.ReplaceSoundEmissionPower;
import dev.overgrown.apoli.power.builtin.ReplaceSoundReceptionPower;
import dev.overgrown.apoli.power.builtin.ActionOnBlockPlacePower;
import dev.overgrown.apoli.power.builtin.ActionOnBlockUsePower;
import dev.overgrown.apoli.power.builtin.ActionOnCallbackPower;
import dev.overgrown.apoli.power.builtin.ActionOnDeathPower;
import dev.overgrown.apoli.power.builtin.ActionOnHitPower;
import dev.overgrown.apoli.power.builtin.ActionOnItemPickupPower;
import dev.overgrown.apoli.power.builtin.ActionOnItemUsePower;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOnKeySequencePower;
import dev.overgrown.apoli.power.builtin.ActionOnKillPower;
import dev.overgrown.apoli.power.builtin.ActionOnLandPower;
import dev.overgrown.apoli.power.builtin.ActionOnUsePower;
import dev.overgrown.apoli.power.builtin.ActionOnWakeUpPower;
import dev.overgrown.apoli.power.builtin.ActionOverTimePower;
import dev.overgrown.apoli.power.builtin.ActionWhenHitPower;
import dev.overgrown.apoli.power.builtin.AttributePower;
import dev.overgrown.apoli.power.builtin.ClimbingPower;
import dev.overgrown.apoli.power.builtin.CooldownPower;
import dev.overgrown.apoli.power.builtin.CreativeFlightPower;
import dev.overgrown.apoli.power.builtin.DisableRegenPower;
import dev.overgrown.apoli.power.builtin.DisableSlotPower;
import dev.overgrown.apoli.power.builtin.EdibleItemPower;
import dev.overgrown.apoli.power.builtin.EffectImmunityPower;
import dev.overgrown.apoli.power.builtin.ElytraFlightPower;
import dev.overgrown.apoli.power.builtin.EntityGlowPower;
import dev.overgrown.apoli.power.builtin.FunctionPower;
import dev.overgrown.apoli.power.builtin.ModifyTypeTagPower;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import dev.overgrown.apoli.power.builtin.ExhaustPower;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.FreezePower;
import dev.overgrown.apoli.power.builtin.GameEventListenerPower;
import dev.overgrown.apoli.power.builtin.GroundedPower;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import dev.overgrown.apoli.power.builtin.InvulnerabilityPower;
import dev.overgrown.apoli.power.builtin.ItemOnItemPower;
import dev.overgrown.apoli.power.builtin.KeepInventoryPower;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import dev.overgrown.apoli.power.builtin.ModifyModelPartsPower;
import dev.overgrown.apoli.power.builtin.ModifyBlockRenderPower;
import dev.overgrown.apoli.power.builtin.ModifyCursorSpeedPower;
import dev.overgrown.apoli.power.builtin.ModifyBreakSpeedPower;
import dev.overgrown.apoli.power.builtin.ModifyCraftingPower;
import dev.overgrown.apoli.power.builtin.ModifyDamagePower;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelPower;
import dev.overgrown.apoli.power.builtin.ModifyGrindstonePower;
import dev.overgrown.apoli.power.builtin.ModifyExhaustionPower;
import dev.overgrown.apoli.power.builtin.ModifyFallingPower;
import dev.overgrown.apoli.power.builtin.ModifyHarvestPower;
import dev.overgrown.apoli.power.builtin.ModifyHealingPower;
import dev.overgrown.apoli.power.builtin.ModifyBlockStuckSpeedPower;
import dev.overgrown.apoli.power.builtin.ModifyJumpPower;
import dev.overgrown.apoli.power.builtin.ScareMobsPower;
import dev.overgrown.apoli.power.builtin.WaterBreathingPower;
import dev.overgrown.apoli.power.builtin.ModifyLabelRenderPower;
import dev.overgrown.apoli.power.builtin.ModifyPlayerModelPower;
import dev.overgrown.apoli.power.builtin.EmissivePower;
import dev.overgrown.apoli.power.builtin.ShowBothArmsPower;
import dev.overgrown.apoli.power.builtin.ModifyPlayerSpawnPower;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamagePower;
import dev.overgrown.apoli.power.builtin.ModifySlipperinessPower;
import dev.overgrown.apoli.power.builtin.ModifySwimSpeedPower;
import dev.overgrown.apoli.power.builtin.ModifyVelocityPower;
import dev.overgrown.apoli.power.builtin.ModifyXpGainPower;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.apoli.power.builtin.OverlayPower;
import dev.overgrown.apoli.power.builtin.ParticlePower;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import dev.overgrown.apoli.power.builtin.PosePower;
import dev.overgrown.apoli.power.builtin.PreventBlockPlacePower;
import dev.overgrown.apoli.power.builtin.PreventBlockSelectionPower;
import dev.overgrown.apoli.power.builtin.PreventBlockUsePower;
import dev.overgrown.apoli.power.builtin.PreventDeathPower;
import dev.overgrown.apoli.power.builtin.PreventElytraFlightPower;
import dev.overgrown.apoli.power.builtin.PreventEntityCollisionPower;
import dev.overgrown.apoli.power.builtin.PreventEntityRenderPower;
import dev.overgrown.apoli.power.builtin.PreventFeatureRenderPower;
import dev.overgrown.apoli.power.builtin.PreventPowersPower;
import dev.overgrown.apoli.power.builtin.PreventGameEventPower;
import dev.overgrown.apoli.power.builtin.PreventItemPickupPower;
import dev.overgrown.apoli.power.builtin.PreventItemUsePower;
import dev.overgrown.apoli.power.builtin.PreventSleepPower;
import dev.overgrown.apoli.power.builtin.PreventSprintingPower;
import dev.overgrown.apoli.power.builtin.PreventUsePower;
import dev.overgrown.apoli.power.builtin.RecipePower;
import dev.overgrown.apoli.power.builtin.ReplaceLootTablePower;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import dev.overgrown.apoli.power.builtin.RestrictArmorPower;
import dev.overgrown.apoli.power.builtin.ShaderPower;
import dev.overgrown.apoli.power.builtin.ShakingPower;
import dev.overgrown.apoli.power.builtin.SimplePower;
import dev.overgrown.apoli.power.builtin.StackingStatusEffectPower;
import dev.overgrown.apoli.power.builtin.StartingEquipmentPower;
import dev.overgrown.apoli.power.builtin.SwimmingPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import dev.overgrown.apoli.power.builtin.TooltipPower;
import dev.overgrown.apoli.power.builtin.WalkOnFluidPower;


public final class PowerTypes {
    public static final ActionOnKeyPressPower ACTION_ON_KEY_PRESS = new ActionOnKeyPressPower();
    public static final ActionOnUsePower ACTION_ON_USE = new ActionOnUsePower();
    public static final ActionOnHitPower ACTION_ON_HIT = new ActionOnHitPower();
    public static final ActionWhenHitPower ACTION_WHEN_HIT = new ActionWhenHitPower();
    public static final ModifyDamagePower MODIFY_DAMAGE = new ModifyDamagePower();

    private PowerTypes() {}

    public static void bootstrap() {
        PowerTypeRegistry.register(Apoli.id("creative_flight"), new CreativeFlightPower());
        PowerTypeRegistry.register(Apoli.id("action_over_time"), new ActionOverTimePower());
        PowerTypeRegistry.register(
            Apoli.id("action_on_key_press"),
            ACTION_ON_KEY_PRESS,
            AliasingOptions.builder().addTypeAlias(Apoli.id("active_self")).build()
        );
        PowerTypeRegistry.register(
            Apoli.id("action_on_key_sequence"),
            new ActionOnKeySequencePower(),
            AliasingOptions.builder().addTypeAlias("sync:action_on_key_sequence").build()
        );
        PowerTypeRegistry.register(Apoli.id("entity_set"), new EntitySetPower());
        PowerTypeRegistry.register(Apoli.id("modify_type_tag"), new ModifyTypeTagPower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("entity_group")).build());
        PowerTypeRegistry.register(Apoli.id("function"), new FunctionPower());
        PowerTypeRegistry.register(Apoli.id("multiple"), new MultiplePower());
        PowerTypeRegistry.register(Apoli.id("resource"), new ResourcePower());
        PowerTypeRegistry.register(Apoli.id("cooldown"), new CooldownPower());
        PowerTypeRegistry.register(
            Apoli.id("attribute"),
            new AttributePower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("conditioned_attribute")).build()
        );
        PowerTypeRegistry.register(
            Apoli.id("action_on_use"),
            ACTION_ON_USE,
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("action_on_entity_use"))
                .addTypeAlias(Apoli.id("action_on_being_used"))
                .build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("action_on_being_used"), targetUsed(true));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("action_on_entity_use"), targetUsed(false));

        PowerTypeRegistry.register(
            Apoli.id("modify_damage"),
            MODIFY_DAMAGE,
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("modify_damage_dealt"))
                .addTypeAlias(Apoli.id("modify_damage_taken"))
                .build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("modify_damage_dealt"), targetUsed(false));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("modify_damage_taken"), targetUsed(true));

        PowerTypeRegistry.register(
            Apoli.id("action_on_hit"),
            ACTION_ON_HIT,
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("self_action_on_hit"))
                .addTypeAlias(Apoli.id("target_action_on_hit"))
                .build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("self_action_on_hit"), entityActionTarget("self"));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("target_action_on_hit"), entityActionTarget("target"));

        PowerTypeRegistry.register(
            Apoli.id("action_when_hit"),
            ACTION_WHEN_HIT,
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("self_action_when_hit"))
                .addTypeAlias(Apoli.id("attacker_action_when_hit"))
                .addTypeAlias(Apoli.id("action_when_damage_taken"))
                .build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("self_action_when_hit"), entityActionTarget("self"));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("action_when_damage_taken"), entityActionTarget("self"));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("attacker_action_when_hit"), entityActionTarget("attacker"));

        PowerTypeRegistry.register(Apoli.id("simple"), new SimplePower());
        PowerTypeRegistry.register(Apoli.id("disable_regen"), new DisableRegenPower());
        PowerTypeRegistry.register(Apoli.id("invulnerability"), new InvulnerabilityPower());
        PowerTypeRegistry.register(Apoli.id("climbing"), new ClimbingPower());
        PowerTypeRegistry.register(Apoli.id("grounded"), new GroundedPower());
        PowerTypeRegistry.register(Apoli.id("restrict_armor"), new RestrictArmorPower());
        PowerTypeRegistry.register(
            Apoli.id("disable_slot"),
            new DisableSlotPower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("restrict_slot")).build()
        );
        PowerTypeRegistry.register(
            Apoli.id("entity_glow"),
            new EntityGlowPower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("self_glow")).build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("self_glow"), selfGlowTarget(true));

        PowerTypeRegistry.register(Apoli.id("elytra_flight"), new ElytraFlightPower());
        PowerTypeRegistry.register(Apoli.id("invisibility"), new InvisibilityPower());
        PowerTypeRegistry.register(Apoli.id("swimming"), new SwimmingPower());
        PowerTypeRegistry.register(Apoli.id("walk_on_fluid"), new WalkOnFluidPower());
        PowerTypeRegistry.register(Apoli.id("phasing"), new PhasingPower());

        PowerTypeRegistry.register(
            Apoli.id("modify_cursor_speed"),
            new ModifyCursorSpeedPower(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("modify_mouse_sensitivity"))
                .addTypeAlias(Apoli.id("modify_mouse_speed"))
                .addTypeAlias(Apoli.id("modify_look_sensitivity"))
                .build()
        );

        PowerTypeRegistry.register(Apoli.id("modify_block_render"), new ModifyBlockRenderPower());
        PowerTypeRegistry.register(Apoli.id("modify_break_speed"), new ModifyBreakSpeedPower());
        PowerTypeRegistry.register(Apoli.id("modify_enchantment_level"), new ModifyEnchantmentLevelPower());
        PowerTypeRegistry.register(Apoli.id("modify_exhaustion"), new ModifyExhaustionPower());
        PowerTypeRegistry.register(Apoli.id("modify_falling"), new ModifyFallingPower());
        PowerTypeRegistry.register(Apoli.id("modify_harvest"), new ModifyHarvestPower());
        PowerTypeRegistry.register(Apoli.id("modify_healing"), new ModifyHealingPower());
        PowerTypeRegistry.register(Apoli.id("modify_jump"), new ModifyJumpPower());
        PowerTypeRegistry.register(Apoli.id("modify_block_stuck_speed"), new ModifyBlockStuckSpeedPower());
        PowerTypeRegistry.register(Apoli.id("scare_mobs"), new ScareMobsPower());
        PowerTypeRegistry.register(Apoli.id("water_breathing"), new WaterBreathingPower());
        PowerTypeRegistry.register(Apoli.id("modify_player_spawn"), new ModifyPlayerSpawnPower());
        PowerTypeRegistry.register(Apoli.id("modify_projectile_damage"), new ModifyProjectileDamagePower());
        PowerTypeRegistry.register(Apoli.id("modify_slipperiness"), new ModifySlipperinessPower());
        PowerTypeRegistry.register(Apoli.id("modify_swim_speed"), new ModifySwimSpeedPower());
        PowerTypeRegistry.register(Apoli.id("modify_velocity"), new ModifyVelocityPower());
        PowerTypeRegistry.register(Apoli.id("modify_xp_gain"), new ModifyXpGainPower());

        PowerTypeRegistry.register(
            Apoli.id("prevent_use"),
            new PreventUsePower(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("prevent_entity_use"))
                .addTypeAlias(Apoli.id("prevent_being_used"))
                .build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("prevent_entity_use"), PreventUsePower.targetUsed(false));
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("prevent_being_used"), PreventUsePower.targetUsed(true));

        PowerTypeRegistry.register(Apoli.id("prevent_block_place"), new PreventBlockPlacePower());
        PowerTypeRegistry.register(Apoli.id("prevent_block_selection"), new PreventBlockSelectionPower());
        PowerTypeRegistry.register(Apoli.id("prevent_block_use"), new PreventBlockUsePower());
        PowerTypeRegistry.register(Apoli.id("prevent_death"), new PreventDeathPower());
        PowerTypeRegistry.register(Apoli.id("prevent_elytra_flight"), new PreventElytraFlightPower());
        PowerTypeRegistry.register(Apoli.id("prevent_entity_collision"), new PreventEntityCollisionPower());
        PowerTypeRegistry.register(Apoli.id("prevent_entity_render"), new PreventEntityRenderPower());
        PowerTypeRegistry.register(Apoli.id("prevent_feature_render"), new PreventFeatureRenderPower());
        PowerTypeRegistry.register(Apoli.id("prevent_game_event"), new PreventGameEventPower());
        PowerTypeRegistry.register(Apoli.id("prevent_item_pickup"), new PreventItemPickupPower());
        PowerTypeRegistry.register(Apoli.id("prevent_item_use"), new PreventItemUsePower());
        PowerTypeRegistry.register(Apoli.id("prevent_sleep"), new PreventSleepPower());
        PowerTypeRegistry.register(Apoli.id("prevent_sprinting"), new PreventSprintingPower());

        PowerTypeRegistry.register(Apoli.id("action_on_block_break"), new ActionOnBlockBreakPower());
        PowerTypeRegistry.register(Apoli.id("action_on_block_place"), new ActionOnBlockPlacePower());
        PowerTypeRegistry.register(Apoli.id("action_on_block_use"), new ActionOnBlockUsePower());
        PowerTypeRegistry.register(Apoli.id("action_on_callback"), new ActionOnCallbackPower());
        PowerTypeRegistry.register(Apoli.id("action_on_death"), new ActionOnDeathPower());
        PowerTypeRegistry.register(
            Apoli.id("action_on_kill"),
            new ActionOnKillPower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("self_action_on_kill")).build()
        );
        PowerTypeRegistry.registerAliasDefaults(Apoli.id("self_action_on_kill"), entityActionTarget("self"));
        PowerTypeRegistry.register(Apoli.id("action_on_item_pickup"), new ActionOnItemPickupPower());
        PowerTypeRegistry.register(Apoli.id("action_on_item_use"), new ActionOnItemUsePower());
        PowerTypeRegistry.register(Apoli.id("action_on_land"), new ActionOnLandPower());
        PowerTypeRegistry.register(Apoli.id("action_on_collision"), new ActionOnCollisionPower());
        PowerTypeRegistry.register(Apoli.id("replace_sound_emission"), new ReplaceSoundEmissionPower());
        PowerTypeRegistry.register(Apoli.id("replace_sound_reception"), new ReplaceSoundReceptionPower());
        PowerTypeRegistry.register(Apoli.id("action_on_wake_up"), new ActionOnWakeUpPower());

        PowerTypeRegistry.register(Apoli.id("freeze"), new FreezePower());
        PowerTypeRegistry.register(Apoli.id("shaking"), new ShakingPower());

        PowerTypeRegistry.register(Apoli.id("effect_immunity"), new EffectImmunityPower());
        PowerTypeRegistry.register(Apoli.id("exhaust"), new ExhaustPower());
        PowerTypeRegistry.register(
            Apoli.id("ignore_fluid"),
            new IgnoreFluidPower(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("ignore_water")).build()
        );
        PowerTypeRegistry.register(Apoli.id("keep_inventory"), new KeepInventoryPower());
        PowerTypeRegistry.register(Apoli.id("starting_equipment"), new StartingEquipmentPower());
        PowerTypeRegistry.register(Apoli.id("replace_loot_table"), new ReplaceLootTablePower());
        PowerTypeRegistry.register(Apoli.id("stacking_status_effect"), new StackingStatusEffectPower());
        PowerTypeRegistry.register(Apoli.id("toggle"), new TogglePower());
        PowerTypeRegistry.register(Apoli.id("prevent_powers"), new PreventPowersPower());

        PowerTypeRegistry.register(Apoli.id("model_color"), new ModelColorPower());
        PowerTypeRegistry.register(Apoli.id("modify_model_parts"), new ModifyModelPartsPower());
        PowerTypeRegistry.register(Apoli.id("custom_model_render"), new dev.overgrown.apoli.power.builtin.CustomModelRenderPower());
        PowerTypeRegistry.register(Apoli.id("action_on_speak"), new dev.overgrown.apoli.compat.voicechat.ActionOnSpeakPower());
        PowerTypeRegistry.register(Apoli.id("action_on_reply"), new dev.overgrown.apoli.compat.voicechat.ActionOnReplyPower());
        PowerTypeRegistry.register(Apoli.id("action_on_sending_message"), new dev.overgrown.apoli.compat.voicechat.ActionOnSendingMessagePower());
        PowerTypeRegistry.register(Apoli.id("action_on_speech"), new dev.overgrown.apoli.compat.voicechat.ActionOnSpeechPower());
        PowerTypeRegistry.register(Apoli.id("overlay"), new OverlayPower());
        PowerTypeRegistry.register(Apoli.id("shader"), new ShaderPower());
        PowerTypeRegistry.register(Apoli.id("tooltip"), new TooltipPower());
        PowerTypeRegistry.register(Apoli.id("particle"), new ParticlePower());
        PowerTypeRegistry.register(Apoli.id("pose"), new PosePower());
        PowerTypeRegistry.register(Apoli.id("modify_player_model"), new ModifyPlayerModelPower());
        PowerTypeRegistry.register(Apoli.id("show_both_arms"), new ShowBothArmsPower());
        PowerTypeRegistry.register(Apoli.id("emissive"), new EmissivePower());
        PowerTypeRegistry.register(Apoli.id("modify_label_render"), new ModifyLabelRenderPower(),
            dev.overgrown.apoli.alias.AliasingOptions.builder()
                .addTypeAlias("sync:modify_label_render")
                .build());

        PowerTypeRegistry.register(Apoli.id("inventory"), new InventoryPower());
        PowerTypeRegistry.register(Apoli.id("recipe"), new RecipePower());
        PowerTypeRegistry.register(Apoli.id("modify_crafting"), new ModifyCraftingPower());
        PowerTypeRegistry.register(Apoli.id("modify_grindstone"), new ModifyGrindstonePower());
        PowerTypeRegistry.register(Apoli.id("item_on_item"), new ItemOnItemPower());
        PowerTypeRegistry.register(Apoli.id("edible_item"), new EdibleItemPower());

        PowerTypeRegistry.register(Apoli.id("fire_projectile"), new FireProjectilePower());
        PowerTypeRegistry.register(Apoli.id("game_event_listener"), new GameEventListenerPower());

        if (ModCompat.ICARUS) {
            PowerTypeRegistry.register(Apoli.id("wings"), new WingsPower());
        }

        if (ModCompat.anyAccessory()) {
            PowerTypeRegistry.register(Apoli.id("action_on_accessory_change"), new ActionOnAccessoryChangePower(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("action_on_trinket_change")).build());
            PowerTypeRegistry.register(Apoli.id("prevent_accessory_equip"), new PreventAccessoryEquipPower(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("prevent_trinket_equip")).build());
            PowerTypeRegistry.register(Apoli.id("prevent_accessory_unequip"), new PreventAccessoryUnequipPower(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("prevent_trinket_unequip")).build());
            PowerTypeRegistry.register(Apoli.id("modify_accessory_slots"), new ModifyAccessorySlotsPower(),
                AliasingOptions.builder()
                    .addTypeAlias(Apoli.id("modify_trinket_slot"))
                    .addTypeAlias(Apoli.id("modify_trinket_slots"))
                    .build());
        }

        if (ModCompat.HARDCORE_REVIVAL) {
            PowerTypeRegistry.register(Apoli.id("action_on_knockout"), new ActionOnKnockoutPower());
            PowerTypeRegistry.register(Apoli.id("action_on_revive"), new ActionOnRevivePower());
        }
    }

    private static JsonObject selfGlowTarget(boolean value) {
        JsonObject obj = new JsonObject();
        obj.add("self_glow_target", new JsonPrimitive(value));
        return obj;
    }

    private static JsonObject targetUsed(boolean value) {
        JsonObject obj = new JsonObject();
        obj.add("target_used", new JsonPrimitive(value));
        return obj;
    }

    private static JsonObject entityActionTarget(String side) {
        JsonObject obj = new JsonObject();
        obj.add("entity_action_target", new JsonPrimitive(side));
        return obj;
    }
}
