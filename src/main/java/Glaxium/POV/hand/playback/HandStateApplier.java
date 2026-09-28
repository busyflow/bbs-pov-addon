package Glaxium.POV.hand.playback;

import Glaxium.POV.bodypart.PovBodyPartPlayback;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.HandBoneUtils;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

public final class HandStateApplier {
   private HandStateApplier() {
   }

   public static void inheritReplayModel(RecordedHandData data, HandState state, Replay replay, UIFilmPanel panel) {
      ModelForm baseModelForm = null;
      if (data != null && data.baseForm.get() != null && FormUtils.getRoot((Form)data.baseForm.get()) instanceof ModelForm mf) {
         baseModelForm = mf;
      }

      if (baseModelForm == null) {
         IEntity entity = null;
         if (panel != null) {
            Film film = (Film)panel.getData();
            int selector = PovCameraClips.indexOfReplay(film, replay);
            if (selector >= 0) {
               entity = (IEntity)panel.getController().getEntities().get(selector);
            }
         }

         Form source = entity == null ? (Form)replay.form.get() : entity.getForm();
         if ((source == null ? null : FormUtils.getRoot(source)) instanceof ModelForm mf) {
            baseModelForm = mf;
         }
      }

      if (baseModelForm == null) {
         if (state.model == null || state.model.isBlank()) {
            state.model = "player/steve";
         }
      } else {
         if (state.model == null || state.model.isBlank()) {
            String model = (String)baseModelForm.model.get();
            if (model != null && !model.isBlank()) {
               state.model = model;
            } else {
               state.model = "player/steve";
            }
         }

         if (state.texture == null) {
            state.texture = state.model.equals(baseModelForm.model.get()) ? (Link)baseModelForm.texture.get() : null;
         }

         if (state.color == null && baseModelForm.color.get() != null) {
            state.color = ((Color)baseModelForm.color.get()).copy();
         }

         if (state.colorOverlay == null && baseModelForm.overlayColor.get() != null) {
            state.colorOverlay = ((Color)baseModelForm.overlayColor.get()).copy();
         }

         if (baseModelForm.pose.get() != null && state.pose.transforms.isEmpty()) {
            state.pose.copy((Pose)baseModelForm.pose.get());
         }
      }
   }

   public static void applyForm(ModelForm form, HandState state, RecordedHandData data, float tick) {
      RecordedHandData.ensureOverlays(form);
      form.visible.set(true);
      form.model.set(state.model != null && !state.model.isBlank() ? state.model : "player/steve");
      form.texture.set(state.texture);
      form.color.set(state.color != null ? state.color.copy() : Color.white());
      form.overlayColor.set(state.colorOverlay != null ? state.colorOverlay.copy() : new Color(1.0F, 1.0F, 1.0F, 0.0F));
      String rightBone = "right_arm";
      String leftBone = "left_arm";
      if (FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer modelRenderer) {
         ModelInstance model = modelRenderer.getModel();
         if (model == null || model.getModel() == null || HandBoneUtils.collect(model).depths().isEmpty()) {
            form.model.set("player/steve");
            form.texture.set(RecordedHandData.DEFAULT_TEXTURE);
            if (FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer fallbackRenderer) {
               model = fallbackRenderer.getModel();
            }
         }

         if (model != null) {
            rightBone = getBone(model.getFpMain(), rightBone);
            leftBone = getBone(model.getFpOffhand(), leftBone);
         }
      }

      Pose pose = state.pose.copy();
      if (data != null && !data.rightPose.isEmpty()) {
         pose.transforms.put(rightBone, copyPose(state.rightPose));
      }

      if (data != null && !data.leftPose.isEmpty()) {
         pose.transforms.put(leftBone, copyPose(state.leftPose));
      }

      form.pose.set(pose);
      if (!UIPovHandEditor.isActive()) {
         data.applyPoseTracks(form, tick);
         if (data != null && data.baseForm.get() != null && FormUtils.getRoot((Form)data.baseForm.get()) instanceof ModelForm mf) {
            PovBodyPartPlayback.sync(form, mf);
         }

         data.applyBodyPartTracks(form, tick);
         PovBodyPartPlayback.syncHoverFrame(form);
      } else {
         Form preview = UIPovHandEditor.getActive() != null ? UIPovHandEditor.getActive().getPreviewForm() : null;
         if (preview != null && FormUtils.getRoot(preview) instanceof ModelForm mf) {
            PovBodyPartPlayback.sync(form, mf);
         }
      }
   }

   private static String getBone(ArmorSlot slot, String fallback) {
      return slot != null && slot.group != null && !slot.group.isBlank() ? slot.group : fallback;
   }

   public static HandState createDefaultHandEditorState(RecordedHandData data, Replay replay) {
      HandState state = new HandState();
      state.visible = true;
      state.rightHandVisible = true;
      state.leftHandVisible = true;
      String model = null;
      Link texture = null;
      Form activeForm = UIPovHandEditor.getActive() != null ? UIPovHandEditor.getActive().getPreviewForm() : null;
      if (activeForm == null && data != null) {
         activeForm = (Form)data.baseForm.get();
      }

      if (activeForm != null && FormUtils.getRoot(activeForm) instanceof ModelForm mf) {
         model = (String)mf.model.get();
         texture = (Link)mf.texture.get();
         if (mf.color.get() != null) {
            state.color = ((Color)mf.color.get()).copy();
         }

         if (mf.overlayColor.get() != null) {
            state.colorOverlay = ((Color)mf.overlayColor.get()).copy();
         }

         if (mf.pose.get() != null) {
            state.pose.copy((Pose)mf.pose.get());
         }
      }

      if ((model == null || model.isBlank())
         && replay != null
         && replay.form.get() != null
         && FormUtils.getRoot((Form)replay.form.get()) instanceof ModelForm mf) {
         model = (String)mf.model.get();
         texture = (Link)mf.texture.get();
         if (state.color == null && mf.color.get() != null) {
            state.color = ((Color)mf.color.get()).copy();
         }

         if (state.colorOverlay == null && mf.overlayColor.get() != null) {
            state.colorOverlay = ((Color)mf.overlayColor.get()).copy();
         }
      }

      if (model == null || model.isBlank()) {
         model = "player/steve";
         texture = RecordedHandData.DEFAULT_TEXTURE;
      }

      state.model = model;
      state.texture = texture;
      return state;
   }

   private static PoseTransform copyPose(PoseTransform source) {
      PoseTransform copy = new PoseTransform();
      copy.copy(source);
      return copy;
   }
}
