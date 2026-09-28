package Glaxium.POV.editor.section;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.tracks.TrackCatalog;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;

public final class HandEditorSection implements PovEditorSection {
   public static final HandEditorSection INSTANCE = new HandEditorSection();

   private HandEditorSection() {
   }

   @Override
   public void fillSheets(UIPovEditor editor, boolean resetView) {
      Replay replay = editor.getReplay();
      if (replay != null) {
         ModelForm handForm = editor.getHandEditorForm();
         if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedHandData hand = access.bbsPov$getHand();
            if (hand != null) {
               RecordedHandData.ensureOverlays(handForm);
               handForm.model.set(this.currentModel(editor, hand));
               handForm.texture.set(this.currentTexture(editor, hand));
               handForm.boneTracks.set(true);
               ModelForm baseForm = this.baseModelForm(editor, hand);
               if (!hand.color.isEmpty()) {
                  handForm.color.set(((Color)hand.color.interpolate((float)editor.getReplayTick(), Color.white())).copy());
               } else if (baseForm != null && baseForm.color.get() != null) {
                  handForm.color.set(((Color)baseForm.color.get()).copy());
               } else {
                  handForm.color.set(Color.white());
               }

               if (!hand.colorOverlay.isEmpty()) {
                  handForm.overlayColor.set(((Color)hand.colorOverlay.interpolate((float)editor.getReplayTick(), new Color(1.0F, 1.0F, 1.0F, 0.0F))).copy());
               } else if (baseForm != null && baseForm.overlayColor.get() != null) {
                  handForm.overlayColor.set(((Color)baseForm.overlayColor.get()).copy());
               } else {
                  handForm.overlayColor.set(new Color(1.0F, 1.0F, 1.0F, 0.0F));
               }

               if (baseForm != null && baseForm.pose.get() != null) {
                  handForm.pose.set(((Pose)baseForm.pose.get()).copy());
               } else {
                  handForm.pose.set(new Pose());
               }

               UIKeyframeSheet visibleSheet = editor.addSheetWithColor(
                  "Visible",
                  hand.visible,
                  Icons.VISIBLE,
                  UIReplaysEditor.getColor("visible"),
                  handForm.visible,
                  () -> hand.visible.interpolate((float)editor.getReplayTick(), true)
               );
               visibleSheet.form(handForm);
               UIKeyframeSheet modelSheet = editor.addSheetWithColor(
                  "Model", hand.model, Icons.MORPH, UIReplaysEditor.getColor("model"), handForm.model, () -> this.currentModel(editor, hand)
               );
               modelSheet.form(handForm);
               UIKeyframeSheet textureSheet = editor.addSheetWithColor(
                  "Texture", hand.texture, Icons.IMAGE, UIReplaysEditor.getColor("texture"), handForm.texture, () -> this.currentTexture(editor, hand)
               );
               textureSheet.form(handForm);
               UIKeyframeSheet colorSheet = editor.addSheetWithColor(
                  "Color", hand.color, Icons.COLOR, UIReplaysEditor.getColor("color"), handForm.color, () -> this.currentColor(editor, hand)
               );
               colorSheet.form(handForm);
               UIKeyframeSheet colorOverlaySheet = editor.createSheetWithColor(
                  "Color Overlay",
                  hand.colorOverlay,
                  Icons.COLOR,
                  UIReplaysEditor.getColor("color_overlay"),
                  handForm.overlayColor,
                  () -> TrackCatalog.opaqueOverlaySeed(this.currentColorOverlay(editor, hand))
               );
               colorOverlaySheet.form(handForm);
               UIKeyframeSheet cameraOffsetSheet = editor.addSheetWithColor(
                  "Camera Offset",
                  hand.cameraOffset,
                  Icons.LAYOUT,
                  UIReplaysEditor.getColor("transform"),
                  () -> ((Transform)hand.cameraOffset.interpolate((float)editor.getReplayTick(), new Transform())).copy()
               );
               cameraOffsetSheet.form(handForm);
               Pose defaultPose = handForm.pose.get() != null ? (Pose)handForm.pose.get() : new Pose();
               UIKeyframeSheet poseSheet = editor.createSheetWithColor(
                  "Pose",
                  hand.pose,
                  Icons.POSE,
                  UIReplaysEditor.getColor("pose"),
                  handForm.pose,
                  () -> ((Pose)hand.pose.interpolate((float)editor.getReplayTick(), defaultPose)).copy()
               );
               poseSheet.form(handForm);
               if ((Boolean)BBSSettings.recordingOverlays.get()) {
                  String overlayKey = "pose_overlay";
                  KeyframeChannel<Pose> overlayChannel = hand.getOrCreatePoseOverlay(handForm, overlayKey);
                  if (overlayChannel != null) {
                     UIKeyframeSheet overlaySheet = new UIKeyframeSheet(
                        overlayKey, IKey.constant(overlayKey), UIReplaysEditor.getColor(overlayKey), overlayChannel, handForm.poseOverlay
                     );
                     overlaySheet.icon(Icons.POSE).form(handForm);
                     overlaySheet.seed(() -> ((Pose)overlayChannel.interpolate((float)editor.getReplayTick(), new Pose())).copy());
                     overlaySheet.setParent(poseSheet);
                     editor.addPendingSheet(overlaySheet);
                  }

                  int additional = (Integer)BBSSettings.recordingPoseOverlays.get();

                  for (int k = 0; k < additional; k++) {
                     String addKey = "pose_overlay" + k;
                     KeyframeChannel<Pose> addChannel = hand.getOrCreatePoseOverlay(handForm, addKey);
                     BaseValueBasic property = FormUtils.getProperty(handForm, addKey);
                     if (addChannel != null && property != null) {
                        UIKeyframeSheet addSheet = new UIKeyframeSheet(addKey, IKey.constant(addKey), UIReplaysEditor.getColor(addKey), addChannel, property);
                        addSheet.icon(Icons.POSE).form(handForm);
                        addSheet.seed(() -> ((Pose)addChannel.interpolate((float)editor.getReplayTick(), new Pose())).copy());
                        addSheet.setParent(poseSheet);
                        editor.addPendingSheet(addSheet);
                     }
                  }
               }

               int color = 0;
               String currentHandModel = this.currentModel(editor, hand);
               handForm.model.set(currentHandModel);
               ModelInstance model = ModelFormRenderer.getModel(handForm);
               if ((model == null || model.getModel() == null) && baseForm != null) {
                  model = ModelFormRenderer.getModel(baseForm);
               }

               if ((model == null || model.getModel() == null) && BBSModClient.getModels() != null) {
                  model = BBSModClient.getModels().getModel(currentHandModel);
                  if (model == null || model.getModel() == null) {
                     model = BBSModClient.getModels().getModel("player/steve");
                  }
               }

               HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);
               Map<String, UIKeyframeSheet> handBoneSheets = new LinkedHashMap<>();

               for (Entry<String, Integer> entry : handBones.depths().entrySet()) {
                  String bone = entry.getKey();
                  KeyframeChannel<PoseTransform> channel = hand.getOrCreatePoseTrack(bone, handBones);
                  String sheetId = TrackId.bone("", bone).toKey();
                  UIKeyframeSheet boneSheet = this.createBoneSheet(editor, sheetId, bone, channel, color++);
                  if (model != null && model.getModel() != null) {
                     String parentBone = model.getModel().getParentGroupKey(bone);
                     UIKeyframeSheet parentSheet = handBoneSheets.get(parentBone);
                     boneSheet.setParent(parentSheet != null ? parentSheet : poseSheet);
                  } else {
                     boneSheet.setParent(poseSheet);
                  }

                  handBoneSheets.put(bone, boneSheet);
               }

               UIKeyframeSheet itemPoseSheet = editor.createSheetWithColor(
                  "Item Pose",
                  hand.itemPose,
                  Icons.BLOCK,
                  UIReplaysEditor.getColor("pose"),
                  handForm.pose,
                  () -> ((Pose)hand.itemPose.interpolate((float)editor.getReplayTick(), defaultPose)).copy()
               );
               itemPoseSheet.form(handForm);
               editor.addSheetWithColor(
                  "World Interaction",
                  hand.worldInteraction,
                  Icons.BLOCK,
                  UIReplaysEditor.getColor("world_interaction"),
                  () -> hand.worldInteraction.interpolate((float)editor.getReplayTick(), false)
               );
               editor.addSheetWithColor(
                  "Replay Interaction",
                  hand.replayInteraction,
                  Icons.FILM,
                  UIReplaysEditor.getColor("replay_interaction"),
                  () -> hand.replayInteraction.interpolate((float)editor.getReplayTick(), false)
               );
               editor.addSheetWithColor(
                  "Right Hand Visible",
                  hand.rightHandVisible,
                  Icons.VISIBLE,
                  UIReplaysEditor.getColor("visible"),
                  () -> hand.rightHandVisible.interpolate((float)editor.getReplayTick(), true)
               );
               editor.addSheetWithColor(
                  "Left Hand Visible",
                  hand.leftHandVisible,
                  Icons.VISIBLE,
                  UIReplaysEditor.getColor("visible"),
                  () -> hand.leftHandVisible.interpolate((float)editor.getReplayTick(), false)
               );
               color = editor.addSheet("Off Hand Item", replay.keyframes.offHand, Icons.BLOCK, color);
               color = editor.addSheet("Right Swing", hand.rightSwingProgress, Icons.MAIN_HANDLE, color);
               color = editor.addSheet("Left Swing", hand.leftSwingProgress, Icons.LEFT_HANDLE, color);
               color = editor.addSheet("Main Equip", hand.mainEquipProgress, Icons.MAIN_HANDLE, color);
               color = editor.addSheet("Offhand Equip", hand.offEquipProgress, Icons.LEFT_HANDLE, color);
               color = editor.addSheet("Active Use Hand", hand.activeHand, Icons.POINTER, color);
               color = editor.addSheet("Active Use Item", hand.activeItem, Icons.BLOCK, color);
               color = editor.addSheet("Show Particles", hand.showUseParticles, Icons.BUBBLE, color);
               color = editor.addSheet("Use Time", hand.useTime, Icons.TIME, color);
               color = editor.addSheet("Bob Phase", hand.bobPhase, Icons.CURVES, color);
               color = editor.addSheet("Bob Strength", hand.bobStrength, Icons.CURVES, color);
               color = editor.addSheet("Render Yaw", hand.renderYaw, Icons.SPHERE, color);
               color = editor.addSheet("Render Pitch", hand.renderPitch, Icons.SPHERE, color);
               editor.addSheet("Left-handed Main Arm", hand.mainArm, Icons.LIMB, color);
            }
         }
      }
   }

   public boolean pick(UIPovEditor editor, UIContext context, Area viewport) {
      if (editor.isVisible() && editor.isHandSection() && editor.getReplay() != null) {
         if (editor.getReplay().keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedHandData hand = access.bbsPov$getHand();
            if (hand == null) {
               return false;
            } else {
               String itemBone = context.mouseButton == 0 && !hand.itemPose.isEmpty() ? PovHandPicking.pickItem(context, viewport) : null;
               if (itemBone != null) {
                  editor.selectClosestKeyframe(hand.itemPose);
                  if (editor.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory) {
                     poseFactory.poseEditor.selectBone(itemBone);
                  }

                  return true;
               } else {
                  String bone = context.mouseButton == 0 ? PovHandPicking.getPickedBone() : null;
                  if (bone != null && this.hasPoseKeys(editor, hand, bone)) {
                     KeyframeChannel<Pose> activeOverlay = null;
                     int additional = (Integer)BBSSettings.recordingPoseOverlays.get();

                     for (int k = additional - 1; k >= 0; k--) {
                        KeyframeChannel<Pose> add = hand.poseTracks.get(TrackId.property("", "pose_overlay" + k));
                        if (add != null && !add.isEmpty()) {
                           activeOverlay = add;
                           break;
                        }
                     }

                     if (activeOverlay == null) {
                        KeyframeChannel<Pose> overlay = hand.poseTracks.get(TrackId.property("", "pose_overlay"));
                        if (overlay != null && !overlay.isEmpty()) {
                           activeOverlay = overlay;
                        }
                     }

                     KeyframeChannel<?> boneTrack = this.boneTrack(editor, hand, bone);
                     if (activeOverlay != null) {
                        editor.selectClosestKeyframe(activeOverlay);
                        if (editor.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory) {
                           poseFactory.poseEditor.selectBone(bone);
                        }
                     } else if (boneTrack != null && !boneTrack.isEmpty()) {
                        editor.expandPoseTrack(hand.pose);
                        editor.selectClosestKeyframe(boneTrack);
                     } else {
                        editor.selectClosestKeyframe(hand.pose);
                        if (editor.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory) {
                           poseFactory.poseEditor.selectBone(bone);
                        }
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void reloadModel(UIPovEditor editor) {
      if (editor.getReplay() != null && editor.getReplay().keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHandData hand = access.bbsPov$getHand();
         if (hand != null) {
            editor.getHandEditorForm().model.set(this.currentModel(editor, hand));
            editor.getHandEditorForm().texture.set(this.currentTexture(editor, hand));
            ModelForm baseForm = this.baseModelForm(editor, hand);
            if (baseForm != null && baseForm.pose.get() != null) {
               editor.getHandEditorForm().pose.set(((Pose)baseForm.pose.get()).copy());
            }
         }
      }

      if (editor.isHandSection()) {
         editor.refreshSheets(false);
      }
   }

   public String currentModel(UIPovEditor editor, RecordedHandData hand) {
      String model = (String)hand.model.interpolate((float)editor.getReplayTick(), null);
      if (model == null || model.isBlank()) {
         ModelForm baseForm = this.baseModelForm(editor, hand);
         if (baseForm != null) {
            model = (String)baseForm.model.get();
         }
      }

      return model != null && !model.isBlank() ? model : "player/steve";
   }

   public Link currentTexture(UIPovEditor editor, RecordedHandData hand) {
      Link authored = (Link)hand.texture.interpolate((float)editor.getReplayTick(), null);
      if (authored != null) {
         return authored;
      } else {
         ModelForm baseForm = this.baseModelForm(editor, hand);
         String selectedModel = this.currentModel(editor, hand);
         if (baseForm != null && selectedModel.equals(baseForm.model.get())) {
            Link baseTexture = (Link)baseForm.texture.get();
            if (baseTexture != null) {
               return baseTexture;
            }
         }

         editor.getHandEditorForm().model.set(this.currentModel(editor, hand));
         ModelInstance model = ModelFormRenderer.getModel(editor.getHandEditorForm());
         Link texture = model == null ? null : model.getTexture();
         return texture == null ? RecordedHandData.DEFAULT_TEXTURE : texture;
      }
   }

   public Color currentColor(UIPovEditor editor, RecordedHandData hand) {
      if (hand != null && !hand.color.isEmpty()) {
         Color authored = (Color)hand.color.interpolate((float)editor.getReplayTick(), null);
         if (authored != null) {
            return authored.copy();
         }
      }

      ModelForm baseForm = this.baseModelForm(editor, hand);
      return baseForm != null && baseForm.color.get() != null ? ((Color)baseForm.color.get()).copy() : Color.white();
   }

   public Color currentColorOverlay(UIPovEditor editor, RecordedHandData hand) {
      if (hand != null && !hand.colorOverlay.isEmpty()) {
         Color authored = (Color)hand.colorOverlay.interpolate((float)editor.getReplayTick(), null);
         if (authored != null) {
            return authored.copy();
         }
      }

      ModelForm baseForm = this.baseModelForm(editor, hand);
      return baseForm != null && baseForm.overlayColor.get() != null ? ((Color)baseForm.overlayColor.get()).copy() : new Color(1.0F, 1.0F, 1.0F, 0.0F);
   }

   public ModelForm baseModelForm(UIPovEditor editor, RecordedHandData hand) {
      if (hand != null && hand.baseForm.get() != null) {
         Form root = FormUtils.getRoot((Form)hand.baseForm.get());
         if (root instanceof ModelForm) {
            return (ModelForm)root;
         }
      }

      return this.replayModelForm(editor);
   }

   private ModelForm replayModelForm(UIPovEditor editor) {
      IEntity entity = editor.getFilmPanel().getController().getCurrentEntity();
      Form form = entity == null ? null : entity.getForm();
      if (form == null && editor.getReplay() != null) {
         form = (Form)editor.getReplay().form.get();
      }

      return (form == null ? null : FormUtils.getRoot(form)) instanceof ModelForm modelForm ? modelForm : null;
   }

   public UIKeyframeSheet createBoneSheet(UIPovEditor editor, String sheetId, String bone, KeyframeChannel<PoseTransform> channel, int colorIndex) {
      ModelForm handForm = editor.getHandEditorForm();
      int color = UIKeyframeEditor.COLORS[colorIndex % UIKeyframeEditor.COLORS.length];
      PoseTransform defaultBoneTransform = handForm.pose.get() != null && ((Pose)handForm.pose.get()).get(bone) != null
         ? copyPose(((Pose)handForm.pose.get()).get(bone))
         : new PoseTransform();
      ValueTransform property = new ValueTransform(sheetId, copyPose(defaultBoneTransform));
      UIKeyframeSheet sheet = new UIKeyframeSheet(sheetId, IKey.constant(bone), color, channel, property, true);
      sheet.icon(Icons.LIMB);
      sheet.form(handForm);
      sheet.seed(() -> copyPose((PoseTransform)channel.interpolate((float)editor.getReplayTick(), defaultBoneTransform)));
      editor.addPendingSheet(sheet);
      return sheet;
   }

   public boolean getWorldMatrix(UIPovEditor editor, Matrix4f output) {
      String selected = editor.getGizmoBone();
      Matrix4f matrix = selected == null ? null : PovHandMatrices.getFull(selected);
      if (matrix == null) {
         return false;
      } else {
         output.set(matrix);
         return true;
      }
   }

   private boolean hasPoseKeys(UIPovEditor editor, RecordedHandData hand, String bone) {
      KeyframeChannel<?> track = this.boneTrack(editor, hand, bone);
      if (hand.pose.isEmpty() && (track == null || track.isEmpty())) {
         for (KeyframeChannel<?> channel : hand.poseTracks.tracks.values()) {
            if (channel.getId().startsWith("pose_overlay") && !channel.isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private KeyframeChannel<?> boneTrack(UIPovEditor editor, RecordedHandData hand, String bone) {
      HandBoneUtils.HandBones bones = HandBoneUtils.collect(ModelFormRenderer.getModel(editor.getHandEditorForm()));
      if (bone.equals(bones.mainRoot())) {
         return hand.rightPose;
      } else {
         return bone.equals(bones.offRoot()) ? hand.leftPose : hand.poseTracks.get(TrackId.bone("", bone));
      }
   }

   private static PoseTransform copyPose(PoseTransform pose) {
      PoseTransform copy = new PoseTransform();
      copy.copy(pose);
      return copy;
   }
}
