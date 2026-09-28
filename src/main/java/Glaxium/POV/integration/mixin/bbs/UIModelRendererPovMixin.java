package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.render.PovHandMatrices;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIModelRenderer.class},
   remap = false
)
public class UIModelRendererPovMixin {
   @Inject(
      method = {"setupPosition"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$setupPosition(CallbackInfo info) {
      if (UIPovHandEditor.isActive()) {
         UIModelRenderer self = (UIModelRenderer)(Object)this;
         self.camera.position.set(0.0, 0.0, 0.0);
         info.cancel();
      }
   }

   @Inject(
      method = {"getSceneAxes"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getPovSceneAxes(CallbackInfoReturnable<Matrix3f> info) {
      if (UIPovHandEditor.isActive()) {
         UIPovHandEditor editor = UIPovHandEditor.getActive();
         if (editor != null
            && editor.getFormEditor() != null
            && editor.getFormEditor().editor instanceof UIModelForm modelForm
            && modelForm.modelPanel != null
            && modelForm.modelPanel.poseEditor != null) {
            if (modelForm.form == editor.getRootForm()) {
               String bone = (String)modelForm.modelPanel.poseEditor.groups.list.getCurrentFirst();
               if (bone != null && !bone.isEmpty()) {
                  Matrix3f globalBasis = PovHandMatrices.getGlobalBasis(bone);
                  if (globalBasis != null) {
                     info.setReturnValue(new Matrix3f(globalBasis));
                  }
               }
            } else {
               BodyPart part = UIPovHandEditor.findBodyPart(modelForm.form);
               if (part != null) {
                  Matrix4f bodyPartBase = UIPovHandEditor.getBodyPartBase(part);
                  info.setReturnValue(bodyPartBase.get3x3(new Matrix3f()));
               }
            }
         }
      }
   }
}
