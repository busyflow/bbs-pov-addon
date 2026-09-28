package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.IGizmoDragFirstPerson;
import mchorse.bbs_mod.ui.model_editor.ModelSlotTarget;
import mchorse.bbs_mod.ui.model_editor.UIModelEditorRenderer;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIModelEditorRenderer.class},
   remap = false
)
public abstract class UIModelEditorRendererPovMixin {
   @Shadow
   private Matrix4f boneMatrix(ModelSlotTarget target) {
      return null;
   }

   @Inject(
      method = {"buildGizmoDrag"},
      at = {@At("RETURN")}
   )
   private void bbsPov$attachFirstPersonPivot(ModelSlotTarget target, CallbackInfoReturnable<GizmoDrag> info) {
      GizmoDrag drag = (GizmoDrag)info.getReturnValue();
      if (drag != null && target != null && target.kind() != null && target.kind().firstPerson) {
         Matrix4f bone = this.boneMatrix(target);
         if (bone != null) {
            ((IGizmoDragFirstPerson)drag).bbsPov$setRotationPivot(bone.getTranslation(new Vector3f()));
         }
      }
   }
}
