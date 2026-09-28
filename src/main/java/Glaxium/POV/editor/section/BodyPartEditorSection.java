package Glaxium.POV.editor.section;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.tracks.TrackCatalog;
import mchorse.bbs_mod.film.replays.tracks.TrackDescriptor;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.Pair;

public final class BodyPartEditorSection implements PovEditorSection {
   public static final BodyPartEditorSection INSTANCE = new BodyPartEditorSection();

   private BodyPartEditorSection() {
   }

   @Override
   public void fillSheets(UIPovEditor editor, boolean resetView) {
      Replay replay = editor.getReplay();
      if (replay != null) {
         if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedHandData hand = access.bbsPov$getHand();
            ModelForm root = this.root(hand);
            if (hand != null && root != null) {
               RecordedHandData.ensureOverlays(root);
               this.enableBoneTracks(root);
               String selected = editor.getSelectedBodyPart();
               List<TrackDescriptor> descriptors;
               if (selected != null && !selected.isBlank()) {
                  descriptors = TrackCatalog.forPart(root, hand.bodyPartTracks, selected);
               } else {
                  descriptors = new ArrayList<>();
                  collectAllPartDescriptors(root, root, hand.bodyPartTracks, descriptors);
               }

               List<UIKeyframeSheet> sheets = new ArrayList<>();
               UIReplaysEditorUtils.buildSheets(descriptors, sheets);

               for (UIKeyframeSheet sheet : sheets) {
                  editor.addPendingSheet(sheet);
               }
            }
         }
      }
   }

   private void enableBoneTracks(Form form) {
      if (form != null) {
         if (form instanceof ModelForm modelForm) {
            modelForm.boneTracks.set(true);
         }

         for (BodyPart part : form.parts.getAllTyped()) {
            if (part != null && part.getForm() != null) {
               this.enableBoneTracks(part.getForm());
            }
         }
      }
   }

   public static void collectAllPartDescriptors(Form root, Form current, FormProperties properties, List<TrackDescriptor> out) {
      if (current != null) {
         for (BodyPart part : current.parts.getAllTyped()) {
            if (part != null && part.getForm() != null) {
               out.addAll(TrackCatalog.forPart(root, properties, FormUtils.getPath(part.getForm())));
               collectAllPartDescriptors(root, part.getForm(), properties, out);
            }
         }
      }
   }

   public boolean pick(UIPovEditor editor, UIContext context) {
      if (context.mouseButton != 0) {
         return false;
      } else {
         String bone = PovHandPicking.getPickedBone();
         Form pickedForm = PovHandPicking.getPickedForm();
         if (pickedForm == null) {
            int pickedIndex = PovHandPicking.getPickedBodyPart();
            if (pickedIndex < 0) {
               return false;
            }

            Replay replay = editor.getReplay();
            if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access)) {
               return false;
            }

            RecordedHandData var16 = access.bbsPov$getHand();
            ModelForm rRoot = this.root(var16);
            if (var16 == null || rRoot == null) {
               return false;
            }

            List<BodyPart> parts = rRoot.parts.getAllTyped();
            if (pickedIndex >= parts.size()) {
               return false;
            }

            BodyPart pickedPart = parts.get(pickedIndex);
            if (pickedPart != null) {
               pickedForm = pickedPart.getForm();
            }
         }

         if (pickedForm == null) {
            return false;
         } else {
            Film film = (Film)editor.getFilmPanel().getData();
            if (film != null) {
               Form root = FormUtils.getRoot(pickedForm);

               for (Replay r : film.replays.getList()) {
                  if (r != null && r.form.get() != null) {
                     Form rRootx = FormUtils.getRoot((Form)r.form.get());
                     if (r.form.get() == root || rRootx == root) {
                        if (editor.getReplay() != r && editor.getFilmPanel().replayEditor != null) {
                           editor.getFilmPanel().replayEditor.setReplay(r);
                        }
                        break;
                     }
                  }
               }
            }

            String formPath = FormUtils.getPath(pickedForm);
            if (formPath != null && !formPath.isBlank() && !formPath.equals(editor.getSelectedBodyPart())) {
               editor.selectBodyPart(formPath);
            }

            UIReplaysEditorUtils.pickForm(editor.keyframeEditor, editor.getFilmPanel(), pickedForm, bone, false);
            return true;
         }
      }
   }

   public ModelForm root(RecordedHandData hand) {
      if (hand != null && hand.baseForm.get() != null) {
         Form root = FormUtils.getRoot((Form)hand.baseForm.get());
         if (root instanceof ModelForm) {
            return (ModelForm)root;
         }
      }

      UIFilmPanel panel = PovReplaySettings.getFilmPanel();
      if (panel != null && panel.replayEditor != null && panel.replayEditor.getReplay() != null) {
         Form replayForm = (Form)panel.replayEditor.getReplay().form.get();
         if (replayForm != null) {
            Form root = FormUtils.getRoot(replayForm);
            if (root instanceof ModelForm) {
               return (ModelForm)root;
            }
         }
      }

      return null;
   }

   public String gizmoBone(UIPovEditor editor) {
      Pair<String, ?> selected = editor.keyframeEditor.getBone();
      String partPath = editor.getSelectedBodyPart();
      if (selected != null && selected.a != null && !((String)selected.a).isBlank()) {
         String bone = (String)selected.a;
         if (partPath != null && !partPath.isBlank()) {
            if (bone.equals(partPath) || bone.startsWith(partPath + "/")) {
               return bone;
            }

            if (!bone.contains("/")) {
               return partPath + "/" + bone;
            }
         }

         return bone;
      } else {
         return partPath != null && !partPath.isBlank() ? partPath : null;
      }
   }
}
