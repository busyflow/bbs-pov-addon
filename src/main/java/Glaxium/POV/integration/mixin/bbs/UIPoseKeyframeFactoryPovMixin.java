package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.hand.editor.HandBoneUtils;
import java.util.LinkedHashSet;
import java.util.Set;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory.UIPoseFactoryEditor;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UIPoseKeyframeFactory.class},
   remap = false
)
public class UIPoseKeyframeFactoryPovMixin {
   @Shadow
   public UIPoseFactoryEditor poseEditor;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void bbsPov$filterHandBones(Keyframe<Pose> keyframe, UIKeyframes editor, CallbackInfo info) {
      UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
      if (sheet != null && "pov_hand_item_pose".equals(sheet.channel.getId())) {
         this.poseEditor.setPose((Pose)keyframe.getValue(), "");
         this.poseEditor.fillGroups(PovItemPose.BONES, false);
      } else if (sheet != null && sheet.form instanceof ModelForm form) {
         if (sheet.id == null || !sheet.id.contains("/")) {
            String channelId = sheet.channel != null ? sheet.channel.getId() : "";
            String sheetId = sheet.id != null ? sheet.id : "";
            boolean isHandPose = "pov_hand_pose".equals(channelId)
               || "pose".equals(sheetId)
               || "pose_overlay".equals(sheetId)
               || sheetId.matches("^pose_overlay\\d+$")
               || channelId.startsWith("pov_hand_pose_overlay")
               || sheet.property != null
                  && (sheet.property == form.pose || sheet.property == form.poseOverlay || form.additionalOverlays.contains(sheet.property));
            if (isHandPose) {
               if (FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer renderer) {
                  ModelInstance model = renderer.getModel();
                  HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);
                  if (model != null && !handBones.depths().isEmpty()) {
                     Set<String> hidden = new LinkedHashSet<>();

                     for (String bone : model.getModel().getGroupKeysInHierarchyOrder()) {
                        if (!handBones.contains(bone)) {
                           hidden.add(bone);
                        }
                     }

                     this.poseEditor.fillGroups(model.getModel(), model.getFlippedParts(), false, hidden);
                     return;
                  }

                  return;
               }
            }
         }
      }
   }
}
