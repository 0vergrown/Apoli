package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.apoli.entity.LabelManager;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModifyLabelRenderPower extends PowerType<ModifyLabelRenderPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_label_render");

    public enum LabelMode implements StringRepresentable {
        DEFAULT("default"),
        HIDE_PARTIALLY("hide_partially"),
        HIDE_COMPLETELY("hide_completely");

        public static final Codec<LabelMode> CODEC = StringRepresentable.fromEnum(LabelMode::values);
        private final String name;

        LabelMode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(
        Optional<Component> text,
        LabelMode renderMode,
        int tickRate,
        int priority,
        Optional<EntityCondition> entityCondition,
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityAction> beforeParseAction,
        Optional<EntityAction> afterParseAction,
        boolean overrideChatName,
        boolean overrideTabName
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TextComponent.CODEC.optionalFieldOf("text").forGetter(Config::text),
            LabelMode.CODEC.optionalFieldOf("render_mode", LabelMode.DEFAULT).forGetter(Config::renderMode),
            Codec.INT.optionalFieldOf("tick_rate", 20).forGetter(Config::tickRate),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority),
            EntityCondition.CODEC.optionalFieldOf("entity_condition").forGetter(Config::entityCondition),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            EntityAction.CODEC.optionalFieldOf("before_parse_action").forGetter(Config::beforeParseAction),
            EntityAction.CODEC.optionalFieldOf("after_parse_action").forGetter(Config::afterParseAction),
            Codec.BOOL.optionalFieldOf("override_chat_name", false).forGetter(Config::overrideChatName),
            Codec.BOOL.optionalFieldOf("override_tab_name", false).forGetter(Config::overrideTabName)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        int rate = Math.max(1, cfg.tickRate);

        if (owner.tickCount % rate != Math.floorMod(powerId.hashCode() + owner.getId(), rate)) return;

        Power power = ApoliPowers.get(powerId);
        boolean active = power != null
            && (power.condition().isEmpty() || power.condition().get().test(new EntityCtx(owner, level)));

        boolean changed;
        if (!active) {
            changed = LabelManager.setText(owner, powerId, null);
        } else {
            EntityCtx ctx = new EntityCtx(owner, level);
            cfg.beforeParseAction.ifPresent(a -> a.run(ctx));
            Component resolved = resolve(cfg, owner);
            changed = LabelManager.setText(owner, powerId, resolved);
            if (changed) {
                cfg.afterParseAction.ifPresent(a -> a.run(ctx));
            }
        }

        if (changed) {
            boolean namesChanged = recomputeNameOverrides(owner);
            LabelManager.broadcast(owner);
            if (namesChanged && owner instanceof ServerPlayer player) {
                refreshTabName(player);
            }
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity owner = holder.owner();
        if (owner == null || owner.level().isClientSide() || holder.hasPower(powerId)) return;
        if (LabelManager.setText(owner, powerId, null)) {
            boolean namesChanged = recomputeNameOverrides(owner);
            LabelManager.broadcast(owner);
            if (namesChanged && owner instanceof ServerPlayer player) {
                refreshTabName(player);
            }
        }
    }

    private static Component resolve(Config cfg, LivingEntity owner) {
        if (cfg.text.isEmpty()) return Component.empty();
        try {
            return ComponentUtils.updateForEntity(
                owner.createCommandSourceStack().withPermission(2).withSuppressedOutput(),
                cfg.text.get(), owner, 0);
        } catch (Exception e) {
            return cfg.text.get();
        }
    }

    private static boolean recomputeNameOverrides(LivingEntity owner) {
        if (!(owner instanceof ServerPlayer)) return false;
        Component chatName = null;
        Component tabName = null;
        int chatPriority = Integer.MIN_VALUE;
        int tabPriority = Integer.MIN_VALUE;
        Map<ResourceLocation, Component> texts = LabelManager.textsFor(owner.getUUID());
        for (Map.Entry<ResourceLocation, Component> entry : texts.entrySet()) {
            Power power = ApoliPowers.get(entry.getKey());
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            Component text = entry.getValue();
            if (text.getString().isEmpty()) continue;
            if (cfg.overrideChatName() && cfg.priority() > chatPriority) {
                chatName = text;
                chatPriority = cfg.priority();
            }
            if (cfg.overrideTabName() && cfg.priority() > tabPriority) {
                tabName = text;
                tabPriority = cfg.priority();
            }
        }
        return LabelManager.setNameOverrides(owner, chatName, chatPriority, tabName, tabPriority);
    }

    public static void refreshTabName(ServerPlayer player) {
        player.serverLevel().getServer().getPlayerList().broadcastAll(
            new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player)));
    }

    public static void onEntityGone(Entity entity) {
        LabelManager.onEntityGone(entity.getUUID());
    }
}
