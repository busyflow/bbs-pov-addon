package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.screeneffect.render.PovSpyglassZoomHelper;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.utils.PovEffectSuppression;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {CameraController.class},
   remap = false
)
public class CameraControllerPovMixin {
   @Inject(
      method = {"getCurrent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$noControllerInHandEditor(CallbackInfoReturnable<ICameraController> info) {
      if (UIPovHandEditor.isActive()) {
         info.setReturnValue(null);
      }
   }

   @Inject(
      method = {"setup"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$noSetupInHandEditor(Camera camera, float tickDelta, CallbackInfo info) {
      if (UIPovHandEditor.isActive()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"getFOV"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$applySpyglassZoom(CallbackInfoReturnable<Double> info) {
      if (PovPlaybackContext.getActive() != null || PovEffectSuppression.isBbsActive()) {
         float tickDelta = MinecraftClient.getInstance().getTickDelta();
         float zoomMultiplier = PovSpyglassZoomHelper.resolveZoomMultiplier(tickDelta);
         if (zoomMultiplier > 1.0E-4F && Math.abs(zoomMultiplier - 1.0F) > 1.0E-4F) {
            info.setReturnValue(info.getReturnValueD() * (double)zoomMultiplier);
         }
      }
   }
}
