package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.editor.HandBoneHierarchy;
import Glaxium.POV.hand.editor.HandBoneUtils;
import java.util.Collection;
import mchorse.bbs_mod.cubic.IBoneHierarchy;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.utils.bones.UIBonePickerContextMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
   value = {UIBonePickerContextMenu.class},
   remap = false
)
public class UIBonePickerContextMenuPovMixin {
   @ModifyVariable(
      method = {"bones"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private IBoneHierarchy bbsPov$filterModel(IBoneHierarchy model) {
      if (UIPovHandEditor.isActive() && model != null) {
         UIPovHandEditor editor = UIPovHandEditor.getActive();
         if (editor != null && editor.getRootForm() instanceof ModelForm rootForm) {
            ModelInstance instance = ModelFormRenderer.getModel(rootForm);
            if (instance != null && instance.getModel() == model) {
               HandBoneUtils.HandBones handBones = HandBoneUtils.collect(instance);
               return new HandBoneHierarchy(model, handBones);
            }
         }
      }

      return model;
   }

   @ModifyVariable(
      method = {"bones"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Collection<String> bbsPov$filterHandBones(Collection<String> disabled, IBoneHierarchy model) {
      return UIPovHandEditor.isActive() ? null : disabled;
   }
}
