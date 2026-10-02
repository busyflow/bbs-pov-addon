package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 1500)
public abstract class GameRendererPovMixin
{
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$suppressNightVision(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> info)
    {
        float povNv = Glaxium.POV.actions.screeneffect.render.PovNightVisionHelper.resolveNightVisionStrength(tickDelta);
        if (povNv >= 0.0F)
        {
            info.setReturnValue(povNv);
            return;
        }

        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            info.setReturnValue(0.0F);
        }
    }

    @org.spongepowered.asm.mixin.Shadow private int ticks;

    @Inject(method = "showFloatingItem", at = @At("HEAD"))
    private void bbsPov$onShowFloatingItem(net.minecraft.item.ItemStack floatingItem, CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean flipped = false;

        if (client.player != null && floatingItem != null)
        {
            boolean isOffHand = client.player.getOffHandStack().isOf(floatingItem.getItem());
            flipped = (client.player.getMainArm() == net.minecraft.util.Arm.LEFT && !isOffHand)
                   || (client.player.getMainArm() == net.minecraft.util.Arm.RIGHT && isOffHand);
        }

        Glaxium.POV.actions.screeneffect.recording.ScreenEffectRecorder.onFloatingItem(floatingItem, flipped);
    }

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    private void bbsPov$suppressFloatingItem(int scaledWidth, int scaledHeight, float tickDelta, CallbackInfo info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            info.cancel();
        }
    }

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;loadProjectionMatrix(Lorg/joml/Matrix4f;)V"
        )
    )
    private void bbsPov$applyNauseaWorldWobble(float tickDelta, long limitTime, net.minecraft.client.util.math.MatrixStack matrixStack, CallbackInfo info)
    {
        Glaxium.POV.actions.screeneffect.render.PovNauseaApplier.apply(matrixStack, tickDelta, this.ticks);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bbsPov$applySpyglassZoom(CallbackInfoReturnable<Double> info)
    {
        if (PovPlaybackContext.getActive() == null && !Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            return;
        }

        mchorse.bbs_mod.camera.controller.CameraController controller = mchorse.bbs_mod.BBSModClient.getCameraController();
        if (controller != null && controller.getCurrent() != null)
        {
            // Already multiplied in CameraController.getFOV()
            return;
        }

        float tickDelta = MinecraftClient.getInstance().getTickDelta();
        float zoomMultiplier = Glaxium.POV.actions.screeneffect.render.PovSpyglassZoomHelper.resolveZoomMultiplier(tickDelta);
        if (zoomMultiplier > 0.0001F && Math.abs(zoomMultiplier - 1.0F) > 0.0001F)
        {
            info.setReturnValue(info.getReturnValueD() * (double) zoomMultiplier);
        }
    }
}
