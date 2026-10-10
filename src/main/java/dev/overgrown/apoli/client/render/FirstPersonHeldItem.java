package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public final class FirstPersonHeldItem {
    private FirstPersonHeldItem() {}

    private static final Quaternionf GRIP_PITCH = Axis.XP.rotationDegrees(-90.0F);
    private static final Quaternionf GRIP_YAW = Axis.YP.rotationDegrees(180.0F);

    @Nullable
    private static ItemInHandRenderer renderer;
    private static ItemStack stack = ItemStack.EMPTY;
    @Nullable
    private static HumanoidArm arm;
    private static boolean drawn;
    private static boolean drawing;

    public static boolean attaches(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty() || player.isAutoSpinAttack()) return false;
        return !player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || player.getUsedItemHand() != hand;
    }

    public static void begin(ItemInHandRenderer renderer, ItemStack stack, HumanoidArm arm) {
        FirstPersonHeldItem.renderer = renderer;
        FirstPersonHeldItem.stack = stack;
        FirstPersonHeldItem.arm = arm;
        drawn = false;
    }

    public static boolean end() {
        boolean result = drawn;
        renderer = null;
        stack = ItemStack.EMPTY;
        arm = null;
        drawn = false;
        return result;
    }

    public static boolean drawing() {
        return drawing;
    }

    public static void render(PlayerModel<?> model, ModelPart rendered, AbstractClientPlayer player, PoseStack pose,
                              MultiBufferSource buffers, int light) {
        ItemInHandRenderer itemRenderer = renderer;
        if (itemRenderer == null || drawn) return;
        HumanoidArm side = rendered == model.rightArm ? HumanoidArm.RIGHT
            : rendered == model.leftArm ? HumanoidArm.LEFT : null;
        if (side == null || side != arm) return;
        boolean left = side == HumanoidArm.LEFT;
        drawn = true;
        drawing = true;
        pose.pushPose();
        try {
            model.translateToHand(side, pose);
            pose.mulPose(GRIP_PITCH);
            pose.mulPose(GRIP_YAW);
            pose.translate((left ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
            itemRenderer.renderItem(player, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, pose, buffers, light);
        } finally {
            pose.popPose();
            drawing = false;
        }
    }
}
