package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartFolderSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {IUIKeyframeGraph.class},
   remap = false
)
public interface IUIKeyframeGraphBodyPartFolderMixin {
   @Inject(
      method = {"addKeyframeManually"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$blockBodyPartFolderKeys(UIKeyframeSheet sheet, float tick, Object value, CallbackInfoReturnable<Keyframe> info) {
      if (sheet instanceof PovBodyPartFolderSheet) {
         info.setReturnValue(null);
      }
   }
}
