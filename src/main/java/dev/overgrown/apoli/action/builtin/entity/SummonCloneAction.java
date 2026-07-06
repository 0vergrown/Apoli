package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.entity.summon.Summons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class SummonCloneAction implements ActionType<EntityCtx, SummonCloneAction.Cfg> {

    public record Cfg(
        boolean canAttack,
        boolean canSit,
        boolean followOwner,
        boolean inheritEquipment,
        boolean inheritEnchantments,
        boolean slim,
        Optional<ResourceLocation> wideTexture,
        Optional<ResourceLocation> slimTexture,
        int maxLifeTicks,
        Optional<ResourceLocation> summonId,
        List<ResourceLocation> powers,
        Optional<BiEntityAction> bientityAction
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("can_attack", true).forGetter(Cfg::canAttack),
            Codec.BOOL.optionalFieldOf("can_sit", true).forGetter(Cfg::canSit),
            Codec.BOOL.optionalFieldOf("follow_owner", true).forGetter(Cfg::followOwner),
            Codec.BOOL.optionalFieldOf("inherit_equipment", true).forGetter(Cfg::inheritEquipment),
            Codec.BOOL.optionalFieldOf("inherit_enchantments", true).forGetter(Cfg::inheritEnchantments),
            Codec.BOOL.optionalFieldOf("slim", false).forGetter(Cfg::slim),
            ResourceLocation.CODEC.optionalFieldOf("wide_texture").forGetter(Cfg::wideTexture),
            ResourceLocation.CODEC.optionalFieldOf("slim_texture").forGetter(Cfg::slimTexture),
            Codec.INT.optionalFieldOf("max_life_ticks", 1200).forGetter(Cfg::maxLifeTicks),
            ResourceLocation.CODEC.optionalFieldOf("summon_id").forGetter(Cfg::summonId),
            ResourceLocation.CODEC.listOf().optionalFieldOf("powers", List.of()).forGetter(Cfg::powers),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Cfg::bientityAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player owner) || !(ctx.level() instanceof ServerLevel level)) return;

        CloneEntity clone = ApoliEntities.cloneType().create(level);
        if (clone == null) return;

        clone.setOwner(owner);
        clone.setCanAttack(cfg.canAttack);
        clone.setCanSit(cfg.canSit);
        clone.setFollowOwner(cfg.followOwner);
        clone.setSlim(cfg.slim);
        cfg.wideTexture.ifPresent(clone::setWideTexture);
        cfg.slimTexture.ifPresent(clone::setSlimTexture);
        clone.setMaxLifeTime(cfg.maxLifeTicks);
        cfg.summonId.ifPresent(clone::setSummonId);
        clone.setCustomName(owner.getName());
        clone.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYHeadRot(), 0);

        if (cfg.inheritEquipment) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack equipped = owner.getItemBySlot(slot);
                if (equipped.isEmpty()) continue;
                ItemStack copy = cfg.inheritEnchantments
                    ? equipped.copy()
                    : new ItemStack(equipped.getItem(), equipped.getCount());
                clone.setDropChance(slot, 0f);
                clone.setItemSlot(slot, copy);
            }
        }

        level.addFreshEntity(clone);
        clone.updateWeaponGoals();
        Summons.grantPowers(clone, cfg.powers);
        cfg.bientityAction.ifPresent(action -> action.run(new BiEntityCtx(owner, clone, level)));
    }
}
