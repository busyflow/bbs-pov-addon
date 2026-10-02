package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.utils.UITimelinePanel;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIKeyframeEditor.class},
   remap = false
)
public abstract class UIKeyframeEditorPovMixin extends UITimelinePanel {
   @Shadow
   public UIKeyframeFactory<?> editor;

   @Inject(
      method = {"pickKeyframe"},
      at = {@At("TAIL")}
   )
   private void bbsPov$dynamicallyAdjustViewWidth(Keyframe keyframe, CallbackInfo info) {
      UIKeyframeEditor self = (UIKeyframeEditor)(Object)this;
      if (self.view != null) {
         if (this.target == null) {
            if (this.editor != null) {
               self.view.w(1.0F, -140);
            } else {
               self.view.w(1.0F);
            }

            self.resize();
         } else {
            self.view.w(1.0F);
            self.resize();
         }
      }
   }

   @Inject(
      method = {"getBone"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getParentPoseBone(CallbackInfoReturnable<Pair<String, TransformSpace>> info) {
      if (this.editor instanceof UIPoseKeyframeFactory poseFactory) {
         mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet sheet = poseFactory.getSheet();
         String channel = sheet != null && sheet.channel != null ? sheet.channel.getId() : "";
         if ("pov_hand_pose".equals(channel) || "pov_hand_item_pose".equals(channel)) {
            String bone = (String)poseFactory.poseEditor.groups.list.getCurrentFirst();
            if (bone != null && !bone.isBlank()) {
               info.setReturnValue(new Pair(bone, poseFactory.poseEditor.transform.getSpace()));
            }
         }
      }
   }
}
