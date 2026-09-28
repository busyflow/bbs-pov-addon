package Glaxium.POV.bodypart;

import Glaxium.POV.hand.editor.PovHandPicking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;

public final class PovBodyPartPlayback {
   private static final Map<ModelForm, BaseType> LAST_SYNCED_PARTS = new WeakHashMap<>();
   private static final Map<ModelForm, Map<String, PovBodyPartPlayback.BodyPartFrame>> BODY_PART_FRAMES = new WeakHashMap<>();

   private PovBodyPartPlayback() {
   }

   public static void syncHoverFrame(ModelForm root) {
      if (!PovHandPicking.isStencilPass()) {
         Map<String, PovBodyPartPlayback.BodyPartFrame> frame = new HashMap<>();
         captureFrame(root, "", frame);
         BODY_PART_FRAMES.put(root, frame);
      } else {
         Map<String, PovBodyPartPlayback.BodyPartFrame> frame = BODY_PART_FRAMES.get(root);
         if (frame != null) {
            for (Entry<String, PovBodyPartPlayback.BodyPartFrame> entry : frame.entrySet()) {
               Form target = FormUtils.getForm(root, entry.getKey());
               PovBodyPartPlayback.BodyPartFrame saved = entry.getValue();
               if (target != null) {
                  target.transform.setRuntimeValue(saved.transform.copy());
                  target.transformOverlay.setRuntimeValue(saved.transformOverlay.copy());

                  for (int j = 0; j < Math.min(target.additionalTransforms.size(), saved.additionalTransforms.size()); j++) {
                     ((ValueTransform)target.additionalTransforms.get(j)).setRuntimeValue(saved.additionalTransforms.get(j).copy());
                  }

                  if (target instanceof ModelForm) {
                     ModelForm model = (ModelForm)target;
                     if (saved.pose != null) {
                        model.pose.setRuntimeValue(saved.pose.copy());
                     }

                     if (saved.poseOverlay != null) {
                        model.poseOverlay.setRuntimeValue(saved.poseOverlay.copy());
                     }

                     for (int j = 0; j < Math.min(model.additionalOverlays.size(), saved.additionalPoses.size()); j++) {
                        ((ValuePose)model.additionalOverlays.get(j)).setRuntimeValue(saved.additionalPoses.get(j).copy());
                     }
                  }
               }
            }
         }
      }
   }

   private static void captureFrame(Form root, String prefix, Map<String, PovBodyPartPlayback.BodyPartFrame> frame) {
      int index = 0;

      for (BodyPart part : root.parts.getAllTyped()) {
         Form child = part.getForm();
         String path = prefix.isEmpty() ? Integer.toString(index) : prefix + "/" + index;
         if (child != null) {
            List<Transform> addT = new ArrayList<>();

            for (ValueTransform t : child.additionalTransforms) {
               addT.add(((Transform)t.get()).copy());
            }

            Pose p = null;
            Pose pOverlay = null;
            List<Pose> addP = new ArrayList<>();
            if (child instanceof ModelForm model) {
               p = ((Pose)model.pose.get()).copy();
               pOverlay = ((Pose)model.poseOverlay.get()).copy();

               for (ValuePose o : model.additionalOverlays) {
                  addP.add(((Pose)o.get()).copy());
               }
            }

            frame.put(
               path,
               new PovBodyPartPlayback.BodyPartFrame(
                  ((Transform)child.transform.get()).copy(), ((Transform)child.transformOverlay.get()).copy(), addT, p, pOverlay, addP
               )
            );
            captureFrame(child, path, frame);
         }

         index++;
      }
   }

   public static void sync(ModelForm target, ModelForm source) {
      BaseType sourceData = source.parts.toData();
      BaseType last = LAST_SYNCED_PARTS.get(target);
      if (last == null || !BaseType.equals(last, sourceData)) {
         LAST_SYNCED_PARTS.put(target, sourceData);
         target.parts.fromData(sourceData);
      }
   }

   public static void render(ModelForm povForm, int light) {
      if (!PovHandPicking.isStencilPass()) {
         render(povForm, light, null);
      } else {
         render(povForm, light, PovHandPicking.getStencilMap());
      }
   }

   public static void render(ModelForm povForm, int light, StencilMap stencilMap) {
      if (povForm != null && !povForm.parts.getAllTyped().isEmpty()) {
         if (FormUtilsClient.getRenderer(povForm) instanceof PovBodyPartRenderer renderer) {
            renderer.bbsPov$renderBodyParts(light, stencilMap);
         }
      }
   }

   private static record BodyPartFrame(
      Transform transform, Transform transformOverlay, List<Transform> additionalTransforms, Pose pose, Pose poseOverlay, List<Pose> additionalPoses
   ) {
   }
}
