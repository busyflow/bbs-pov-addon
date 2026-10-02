package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.playback.PovHandPlayback;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {FormRenderer.class},
   remap = false
)
public abstract class FormRendererPovMixin {
   @Shadow
   public Form form;

   @Inject(
      method = {
         "applyTransforms(Lnet/minecraft/class_4587;ZF)V",
         "applyTransforms(Lnet/minecraft/client/util/math/MatrixStack;ZF)V"
      },
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void bbsPov$suppressFormTransforms(MatrixStack stack, boolean origin, float transition, CallbackInfo info) {
      if (PovHandPlayback.isSuppressFormTransform(this.form)) {
         info.cancel();
      }
   }

   @Inject(
      method = {"applyTransforms(Lorg/joml/Matrix4f;F)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void bbsPov$suppressMatrixTransforms(Matrix4f matrix, float transition, CallbackInfo info) {
      if (PovHandPlayback.isSuppressFormTransform(this.form)) {
         info.cancel();
      }
   }
}
