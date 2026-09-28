package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.recording.ScreenEffectRecorder;
import Glaxium.POV.actions.screeneffect.render.PovNauseaApplier;
import Glaxium.POV.actions.screeneffect.render.PovNightVisionHelper;
import Glaxium.POV.actions.screeneffect.render.PovSpyglassZoomHelper;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.utils.PovEffectSuppression;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {GameRenderer.class},
   priority = 1500
)
public abstract class GameRendererPovMixin {
   @Shadow
   private int ticks;

   @Inject(
      method = {"getNightVisionStrength"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$suppressNightVision(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> info) {
      float povNv = PovNightVisionHelper.resolveNightVisionStrength(tickDelta);
      if (povNv >= 0.0F) {
         info.setReturnValue(povNv);
      } else {
         if (PovEffectSuppression.isBbsActive()) {
            info.setReturnValue(0.0F);
         }
      }
   }

   @Inject(
      method = {"showFloatingItem"},
      at = {@At("HEAD")}
   )
   private void bbsPov$onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
      MinecraftClient client = MinecraftClient.getInstance();
      boolean flipped = false;
      if (client.player != null && floatingItem != null) {
         boolean isOffHand = client.player.getOffHandStack().isOf(floatingItem.getItem());
         flipped = client.player.getMainArm() == Arm.LEFT && !isOffHand || client.player.getMainArm() == Arm.RIGHT && isOffHand;
      }

      ScreenEffectRecorder.onFloatingItem(floatingItem, flipped);
   }

   @Inject(
      method = {"renderFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$suppressFloatingItem(int scaledWidth, int scaledHeight, float tickDelta, CallbackInfo info) {
      if (PovEffectSuppression.isBbsActive()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;loadProjectionMatrix(Lorg/joml/Matrix4f;)V"
      )}
   )
   private void bbsPov$applyNauseaWorldWobble(float tickDelta, long limitTime, MatrixStack matrixStack, CallbackInfo info) {
      PovNauseaApplier.apply(matrixStack, tickDelta, this.ticks);
   }

   @Inject(
      method = {"getFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$applySpyglassZoom(CallbackInfoReturnable<Double> info) {
      if (PovPlaybackContext.getActive() != null || PovEffectSuppression.isBbsActive()) {
         CameraController controller = BBSModClient.getCameraController();
         if (controller == null || controller.getCurrent() == null) {
            float tickDelta = MinecraftClient.getInstance().getTickDelta();
            float zoomMultiplier = PovSpyglassZoomHelper.resolveZoomMultiplier(tickDelta);
            if (zoomMultiplier > 1.0E-4F && Math.abs(zoomMultiplier - 1.0F) > 1.0E-4F) {
               info.setReturnValue(info.getReturnValueD() * (double)zoomMultiplier);
            }
         }
      }
   }
}
