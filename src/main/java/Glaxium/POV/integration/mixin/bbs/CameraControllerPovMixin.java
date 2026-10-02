package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.playback.PovPlaybackContext;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the POV Hand Editor is active, disable BBS film camera overrides so
 * Minecraft uses the local player's normal first-person camera and head rotation.
 */
@Mixin(value = CameraController.class, remap = false)
public class CameraControllerPovMixin
{
    @Inject(method = "getCurrent", at = @At("HEAD"), cancellable = true)
    private void bbsPov$noControllerInHandEditor(CallbackInfoReturnable<ICameraController> info)
    {
        if (UIPovHandEditor.isActive())
        {
            info.setReturnValue(null);
        }
    }

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void bbsPov$noSetupInHandEditor(Camera camera, float tickDelta, CallbackInfo info)
    {
        if (UIPovHandEditor.isActive())
        {
            info.cancel();
        }
    }

    @Inject(method = "getFOV", at = @At("RETURN"), cancellable = true)
    private void bbsPov$applySpyglassZoom(CallbackInfoReturnable<Double> info)
    {
        if (PovPlaybackContext.getActive() == null && !Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            return;
        }

        float tickDelta = net.minecraft.client.MinecraftClient.getInstance().getTickDelta();
        float zoomMultiplier = Glaxium.POV.actions.screeneffect.render.PovSpyglassZoomHelper.resolveZoomMultiplier(tickDelta);
        if (zoomMultiplier > 0.0001F && Math.abs(zoomMultiplier - 1.0F) > 0.0001F)
        {
            info.setReturnValue(info.getReturnValueD() * (double) zoomMultiplier);
        }
    }
}
