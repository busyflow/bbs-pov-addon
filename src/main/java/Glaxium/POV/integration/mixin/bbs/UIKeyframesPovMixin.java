package Glaxium.POV.integration.mixin.bbs;

import java.util.function.Consumer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UIKeyframes.class},
   remap = false
)
public abstract class UIKeyframesPovMixin {
   @Shadow
   private Consumer<Keyframe> callback;
   @Shadow
   private IUIKeyframeGraph currentGraph;
   @Unique
   private boolean bbsPov$hadSelection;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void bbsPov$autoCloseEditorOnDeselect(UIContext context, CallbackInfo info) {
      if (this.currentGraph != null) {
         boolean hasSelected = this.currentGraph.getSelected() != null;
         if (!hasSelected && this.bbsPov$hadSelection && this.callback != null) {
            this.callback.accept(null);
         }

         this.bbsPov$hadSelection = hasSelected;
      }
   }
}
