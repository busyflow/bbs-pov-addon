package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.playback.PovPlaybackContext;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Unique;

/** Held items are not hand bones and must not occupy the hand selection mask. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererPovPickingMixin
{
    /** Iris passes entity lighting instead of the detached Film camera's light. */
    @ModifyVariable(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int bbsPov$cameraLight(int original)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && PovHandPlayback.isHandActive(client.getTickDelta()))
        {
            return net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(
                client.world, client.gameRenderer.getCamera().getBlockPos());
        }
        return original;
    }

    @ModifyVariable(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0)
    private float bbsPov$lockRenderItemTickDelta(float originalTickDelta)
    {
        if (PovHandPlayback.isActive())
        {
            return PovHandPlayback.getCurrentItemRenderTickDelta(originalTickDelta);
        }
        return originalTickDelta;
    }

    @ModifyVariable(
        method = "renderFirstPersonItem",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0)
    private float bbsPov$lockRenderFirstPersonItemTickDelta(float originalTickDelta)
    {
        if (PovHandPlayback.isActive())
        {
            return PovHandPlayback.getCurrentItemRenderTickDelta(originalTickDelta);
        }
        return originalTickDelta;
    }

    @Unique private boolean bbsPov$startedPlayback;
    @Unique private boolean bbsPov$itemPosePushed;

    /**
     * When Iris shaders are active, Iris invokes HeldItemRenderer.renderItem() directly
     * from MixinLevelRenderer without going through GameRenderer.renderHand().
     * This hook ensures that the replay's sampled POV state, keyframed hotbar items,
     * swing progress, transforms, and model form are active during the renderItem call.
     */
    @Inject(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("HEAD"),
        cancellable = true)
    private void bbsPov$beginItemPlayback(
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider.Immediate consumers,
        net.minecraft.client.network.ClientPlayerEntity player,
        int light,
        CallbackInfo info)
    {
        if (!PovHandPlayback.isActive())
        {
            this.bbsPov$startedPlayback = PovHandPlayback.begin(tickDelta);

            if (this.bbsPov$startedPlayback)
            {
                matrices.push();
                matrices.loadIdentity();
                PovHandPlayback.applyTransforms(matrices);
            }
            else if (PovPlaybackContext.getActive() != null
                || Glaxium.POV.replay.PovReplaySettings.getFilmPanel() != null
                || mchorse.bbs_mod.BBSModClient.getCameraController().getCurrent() != null
                || MinecraftClient.getInstance().currentScreen instanceof mchorse.bbs_mod.ui.framework.UIScreen)
            {
                info.cancel();
            }
        }
    }

    @Inject(
        method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At("RETURN"))
    private void bbsPov$endItemPlayback(
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider.Immediate consumers,
        net.minecraft.client.network.ClientPlayerEntity player,
        int light,
        CallbackInfo info)
    {
        if (PovHandPlayback.isActive())
        {
            try
            {
                consumers.draw();
                if (PovHandPicking.isStencilPass())
                {
                    PovHandPlayback.renderBodyPartsForPicking(light, PovHandPicking.getStencilMap());
                }
                else
                {
                    PovHandPlayback.renderBodyParts(light);
                }
            }
            catch (Exception ignored)
            {
            }
        }

        if (this.bbsPov$startedPlayback)
        {
            try
            {
                PovHandPlayback.popTransforms(matrices);
                matrices.pop();
                Glaxium.POV.hand.editor.PovHandGizmo.captureVisual();
                PovHandPlayback.end();
            }
            finally
            {
                this.bbsPov$startedPlayback = false;
            }
        }
    }

    @ModifyVariable(
        method = "renderFirstPersonItem",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 2)
    private float bbsPov$overrideSwingProgress(
        float originalSwingProgress,
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand)
    {
        if (PovHandPlayback.isActive())
        {
            Glaxium.POV.hand.HandState state = PovHandPlayback.getActiveState();
            if (state != null)
            {
                Arm arm = (hand == Hand.MAIN_HAND) ? state.mainArm : state.mainArm.getOpposite();
                return arm == Arm.RIGHT ? state.rightSwingProgress : state.leftSwingProgress;
            }
        }
        return originalSwingProgress;
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At("HEAD"))
    private void bbsPov$beforeRenderFirstPersonItem(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        if (PovHandPlayback.isActive())
        {
            matrices.push();
            PovHandPlayback.applyHandAnimations(matrices, hand);
        }
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At("RETURN"))
    private void bbsPov$afterRenderFirstPersonItem(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        if (PovHandPlayback.isActive())
        {
            matrices.pop();
        }
    }

    /** Vanilla omits an empty logical off hand. Render that bare arm when its
     * editor visibility track requests it; non-empty held items keep the entire
     * vanilla item pipeline. */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void bbsPov$renderEmptyOffHand(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        if (!PovHandPlayback.isActive()
            || hand != Hand.OFF_HAND
            || !item.isEmpty()
            || player.isInvisible())
        {
            return;
        }

        Arm arm = player.getMainArm().getOpposite();

        if (PovHandPlayback.shouldRenderArm(arm))
        {
            ((HeldItemRendererPovAccessor) this).bbsPov$renderArmHoldingItem(
                matrices,
                consumers,
                light,
                equipProgress,
                swingProgress,
                arm);
            info.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingSpyglass()Z"))
    private boolean bbsPov$neverHideSpyglassHand(AbstractClientPlayerEntity player)
    {
        if (PovHandPlayback.isActive())
        {
            return false;
        }
        return player.isUsingSpyglass();
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"))
    private net.minecraft.util.UseAction bbsPov$overrideUseAction(ItemStack stack)
    {
        net.minecraft.util.UseAction original = stack.getUseAction();
        if (PovHandPlayback.isActive())
        {
            Glaxium.POV.hand.HandState state = PovHandPlayback.getActiveState();
            if (state != null && state.usingItem && original == net.minecraft.util.UseAction.NONE)
            {
                return net.minecraft.util.UseAction.EAT;
            }
        }
        return original;
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "applyEatOrDrinkTransformation",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxUseTime()I"))
    private int bbsPov$overrideMaxUseTime(ItemStack stack)
    {
        int original = stack.getMaxUseTime();
        if (PovHandPlayback.isActive() && original <= 0)
        {
            return 32;
        }
        return original;
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"))
    private void bbsPov$applyItemPose(
        LivingEntity entity,
        ItemStack stack,
        ModelTransformationMode mode,
        boolean leftHanded,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        this.bbsPov$itemPosePushed = false;

        if (!PovHandPlayback.isActive() || PovHandPicking.isStencilPass())
        {
            return;
        }

        String bone = PovHandPlayback.getItemBone(leftHanded);
        PoseTransform transform = PovHandPlayback.getItemPose(leftHanded);
        Matrix4f origin = new Matrix4f(matrices.peek().getPositionMatrix());

        matrices.push();
        PovItemPose.apply(matrices, transform);
        this.bbsPov$itemPosePushed = true;
        PovHandMatrices.captureItem(
            bone,
            origin,
            matrices.peek().getPositionMatrix());

        /* Build the same baked-model transform Minecraft applies immediately
         * afterward, then retain its projected unit cube for screen picking. */
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = client.getItemRenderer().getModel(
            stack,
            entity.getWorld(),
            entity,
            entity.getId());
        MatrixStack itemBounds = new MatrixStack();
        MatrixStackUtils.multiply(itemBounds, matrices.peek().getPositionMatrix());
        model.getTransformation().getTransformation(mode).apply(leftHanded, itemBounds);
        itemBounds.translate(-0.5F, -0.5F, -0.5F);
        PovHandPicking.captureItemBounds(bone, itemBounds.peek().getPositionMatrix());
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN"))
    private void bbsPov$restoreItemPose(
        LivingEntity entity,
        ItemStack stack,
        ModelTransformationMode mode,
        boolean leftHanded,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        if (this.bbsPov$itemPosePushed)
        {
            matrices.pop();
            this.bbsPov$itemPosePushed = false;
        }
    }

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        cancellable = true)
    private void bbsPov$hideItemFromHandStencil(
        LivingEntity entity,
        ItemStack stack,
        ModelTransformationMode mode,
        boolean leftHanded,
        MatrixStack matrices,
        VertexConsumerProvider consumers,
        int light,
        CallbackInfo info)
    {
        if (PovHandPicking.isStencilPass())
        {
            info.cancel();
        }
    }
}
