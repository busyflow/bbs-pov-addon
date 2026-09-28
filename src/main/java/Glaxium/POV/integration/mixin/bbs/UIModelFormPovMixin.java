package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.render.PovHandMatrices;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIForm.class},
   remap = false
)
public abstract class UIModelFormPovMixin {
   @Shadow
   public Form form;

   @Shadow
   public abstract UIPoseEditor getPoseEditor();

   @Shadow
   public abstract TransformSpace getGizmoSpace();

   @Inject(
      method = {"getOrigin(FLjava/lang/String;Lmchorse/bbs_mod/ui/framework/elements/input/drag/TransformSpace;)Lorg/joml/Matrix4f;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getHandOrigin(float transition, String path, TransformSpace space, CallbackInfoReturnable<Matrix4f> info) {
      if (UIPovHandEditor.isActive()) {
         UIPovHandEditor editor = UIPovHandEditor.getActive();
         if (editor == null) {
            return;
         }

         Form currentForm = this.form;
         Form root = editor.getRootForm();
         boolean placesOnOwnFrame = space != null && space.placesOnOwnFrame();
         if (currentForm == root) {
            UIPoseEditor poseEditor = this.getPoseEditor();
            String bone = poseEditor != null ? (String)poseEditor.groups.list.getCurrentFirst() : null;
            if (bone != null && !bone.isEmpty()) {
               Matrix4f matrix = placesOnOwnFrame ? PovHandMatrices.getFull(bone) : PovHandMatrices.getOrigin(bone);
               if (matrix != null) {
                  info.setReturnValue(new Matrix4f(matrix));
               }
            }
         } else {
            BodyPart part = UIPovHandEditor.findBodyPart(currentForm);
            if (part != null) {
               UIPoseEditor poseEditor = this.getPoseEditor();
               String bone = poseEditor != null ? (String)poseEditor.groups.list.getCurrentFirst() : null;
               int partIndex = UIPovHandEditor.findBodyPartIndex(currentForm);
               String formPath = FormUtils.getPath(currentForm);
               String key = (
                     formPath != null && !formPath.isEmpty()
                        ? formPath
                        : String.valueOf(partIndex >= 0 ? partIndex : (part.getId() != null ? part.getId() : ""))
                  )
                  + (bone != null && !bone.isEmpty() ? "/" + bone : "");
               String fallbackKey1 = (partIndex >= 0 ? String.valueOf(partIndex) : "") + (bone != null && !bone.isEmpty() ? "/" + bone : "");
               String fallbackKey2 = (part.getId() != null ? part.getId() : "") + (bone != null && !bone.isEmpty() ? "/" + bone : "");
               Matrix4f matrix = PovHandMatrices.getForSpace(key, space);
               if (matrix == null) {
                  matrix = PovHandMatrices.getForSpace(fallbackKey1, space);
               }

               if (matrix == null) {
                  matrix = PovHandMatrices.getForSpace(fallbackKey2, space);
               }

               if (matrix == null) {
                  matrix = placesOnOwnFrame ? PovHandMatrices.getFull(key) : PovHandMatrices.getOrigin(key);
                  if (matrix == null) {
                     matrix = placesOnOwnFrame ? PovHandMatrices.getFull(fallbackKey1) : PovHandMatrices.getOrigin(fallbackKey1);
                  }

                  if (matrix == null) {
                     matrix = placesOnOwnFrame ? PovHandMatrices.getFull(fallbackKey2) : PovHandMatrices.getOrigin(fallbackKey2);
                  }
               }

               label117:
               if (matrix == null) {
                  Matrix4f bodyPartBase = UIPovHandEditor.getBodyPartBase(part);
                  if (currentForm instanceof ModelForm modelForm && bone != null && !bone.isEmpty()) {
                     matrix = UIPovHandEditor.getBodyPartBoneMatrix(modelForm, bone, placesOnOwnFrame, bodyPartBase);
                     break label117;
                  }

                  matrix = placesOnOwnFrame ? bodyPartBase : UIPovHandEditor.getBodyPartParent(part);
               }

               if (matrix != null) {
                  info.setReturnValue(new Matrix4f(matrix));
               }
            }
         }
      }
   }
}
