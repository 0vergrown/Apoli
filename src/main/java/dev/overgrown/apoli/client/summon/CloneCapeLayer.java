package dev.overgrown.apoli.client.summon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overgrown.apoli.access.BodyEditFollow;
import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import dev.overgrown.apoli.entity.summon.CloneEntity;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.PreventFeatureRenderPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class CloneCapeLayer extends RenderLayer<CloneEntity, CloneModel> {
    private static final String[] CAPE = {"cape"};

    public CloneCapeLayer(RenderLayerParent<CloneEntity, CloneModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, CloneEntity clone, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (clone.isInvisible() || clone.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        ResourceLocation cape = capeTexture(clone);
        if (cape == null) return;
        pose.pushPose();
        if (this.getParentModel() instanceof BodyEditFollow follow) follow.apoli$followBodyEdit(pose);
        pose.translate(0.0F, 0.0F, 0.125F);
        double dx = Mth.lerp(partialTick, clone.xCloakO, clone.xCloak) - Mth.lerp(partialTick, clone.xo, clone.getX());
        double dy = Mth.lerp(partialTick, clone.yCloakO, clone.yCloak) - Mth.lerp(partialTick, clone.yo, clone.getY());
        double dz = Mth.lerp(partialTick, clone.zCloakO, clone.zCloak) - Mth.lerp(partialTick, clone.zo, clone.getZ());
        float body = Mth.rotLerp(partialTick, clone.yBodyRotO, clone.yBodyRot);
        double sin = Mth.sin(body * Mth.DEG_TO_RAD);
        double cos = -Mth.cos(body * Mth.DEG_TO_RAD);
        float lift = Mth.clamp((float) dy * 10.0F, -6.0F, 32.0F);
        float swing = Mth.clamp((float) (dx * sin + dz * cos) * 100.0F, 0.0F, 150.0F);
        float sway = Mth.clamp((float) (dx * cos - dz * sin) * 100.0F, -20.0F, 20.0F);
        float bob = Mth.lerp(partialTick, clone.oBob, clone.bob);
        lift += Mth.sin(Mth.lerp(partialTick, clone.walkDistO, clone.walkDist) * 6.0F) * 32.0F * bob;
        if (clone.isCrouching()) lift += 25.0F;
        pose.mulPose(Axis.XP.rotationDegrees(6.0F + swing / 2.0F + lift));
        pose.mulPose(Axis.ZP.rotationDegrees(sway / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - sway / 2.0F));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(cape));
        this.getParentModel().renderCloak(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Nullable
    private static ResourceLocation capeTexture(CloneEntity clone) {
        PlayerSkin disguise = ClientDisguiseManager.playerSkin(clone.getId());
        if (disguise != null) return disguise.capeTexture();
        if (clone.getCustomTexture(false) != null) return null;
        if (!(clone.getOwner() instanceof AbstractClientPlayer owner)) return null;
        if (!owner.isModelPartShown(PlayerModelPart.CAPE) || CustomModelRenderPower.shouldHideCape(owner)) return null;
        if (PreventFeatureRenderPower.prevents(owner, CAPE, Minecraft.getInstance().player)) return null;
        return owner.getSkin().capeTexture();
    }
}
