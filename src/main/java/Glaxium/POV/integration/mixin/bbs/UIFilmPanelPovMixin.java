package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.RecorderPovAccess;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.integration.access.bbs.UIReplayPropertiesPovAccess;
import Glaxium.POV.integration.access.bbs.UIReplaysListPanelPovAccess;
import Glaxium.POV.recording.ActiveRecordingRange;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UIFilmPanel.class},
   remap = false
)
public abstract class UIFilmPanelPovMixin implements UIFilmPanelPovAccess {
   @Shadow
   public UIElement main;
   @Shadow
   public UIClipsPanel cameraEditor;
   @Shadow
   private List<UIElement> panels;
   @Shadow
   private UIElement selectedMainEditorPanel;
   @Unique
   private UIPovEditor bbsPov$editor;
   @Unique
   private UIIcon bbsPov$openEditor;
   @Unique
   private UIKeyframeEditor bbsPov$replayKeyframeEditor;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void bbsPov$installEditor(UIDashboard dashboard, CallbackInfo info) {
      UIFilmPanel panel = (UIFilmPanel)(Object)this;
      this.bbsPov$editor = new UIPovEditor(panel);
      this.bbsPov$replayKeyframeEditor = panel.replayEditor.keyframeEditor;
      this.bbsPov$editor.full(this.cameraEditor);
      this.bbsPov$editor.setVisible(false);
      this.bbsPov$openEditor = new UIIcon(Icons.LOOKING, button -> panel.showPanel(this.bbsPov$editor));
      this.bbsPov$openEditor.tooltip(IKey.constant("Open POV Editor"), Direction.BOTTOM);
      this.bbsPov$openEditor.setEnabled(false);
      this.main.add(this.bbsPov$editor);
      this.panels.add(this.bbsPov$editor);
      panel.actions().editor(this.bbsPov$openEditor, () -> this.bbsPov$editor != null && this.bbsPov$editor.isVisible());
   }

   @Inject(
      method = {"fillData(Lmchorse/bbs_mod/film/Film;)V"},
      at = {@At("TAIL")}
   )
   private void bbsPov$fillEditor(Film film, CallbackInfo info) {
      if (this.bbsPov$editor != null) {
         this.bbsPov$openEditor.setEnabled(film != null);
         this.bbsPov$editor.setFilm(film);
         if (film != null) {
            PovCameraClips.ensureLegacyClip(film);
         }
      }
   }

   @Inject(
      method = {"applyRecordedKeyframes"},
      at = {@At("HEAD")}
   )
   private void bbsPov$closeRecordedPovTracks(Recorder recorder, Film film, CallbackInfo info) {
      boolean var10000;
      label141: {
         if (recorder instanceof RecorderPovAccess access && access.bbsPov$isOutside()) {
            var10000 = true;
            break label141;
         }

         var10000 = false;
      }

      boolean isOutside = var10000;
      if (isOutside) {
         ActiveRecordingRange.set(recorder.initialTick, recorder.tick);
      } else {
         ActiveRecordingRange.clear();
      }

      if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHudData hud = access.bbsPov$getHud();
         if (hud != null && hud.hasRecordedData()) {
            hud.addRecordingEndKeyframes(recorder.keyframes, recorder.tick);
         }

         RecordedHandData hand = access.bbsPov$getHand();
         if (hand != null && hand.hasRecordedData()) {
            hand.addRecordingEndKeyframes(recorder.keyframes, recorder.tick);
         }

         if (film != null && film.replays.getList().size() > recorder.exception) {
            Replay replay = (Replay)film.replays.getList().get(recorder.exception);
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess targetAccess) {
               RecordedHudData targetHud = targetAccess.bbsPov$getHud();
               RecordedPovActions recActions = access.bbsPov$getActions();
               RecordedPovActions targetActions = targetAccess.bbsPov$getActions();
               if (isOutside) {
                  if (targetHud != null) {
                     targetHud.trimCursorForRecordingRange((float)recorder.initialTick, (float)recorder.tick);
                  }

                  if (recActions != null && targetActions != null) {
                     targetActions.trimForRecordingRange(recorder.initialTick, recorder.tick);
                     if (PovSettings.isBakeAnyActions()) {
                        for (Clip clip : recActions.get()) {
                           if (clip instanceof GuiPovActionClip guiClip) {
                              guiClip.ensureBakingBounds();
                           } else if (clip instanceof CameraShakePovActionClip shakeClip) {
                              shakeClip.ensureBakingBounds();
                           }
                        }

                        for (PovActionClip recorded : recActions.takeSessionClips()) {
                           if (recorded instanceof ToastPovActionClip toastClip) {
                              toastClip.trimToRecording(recorder.tick);
                           }

                           targetActions.addClip(recorded.copy());
                        }
                     }

                     targetActions.sync();
                  }
               } else {
                  if (targetHud != null) {
                     targetHud.cursorLayout.removeAll();
                     targetHud.cursorVisible.removeAll();
                     targetHud.cursorItem.removeAll();
                  }

                  if (targetActions != null) {
                     targetActions.clearAll();
                     if (recActions != null && PovSettings.isBakeAnyActions()) {
                        for (Clip clipx : recActions.get()) {
                           if (clipx instanceof GuiPovActionClip guiClip) {
                              guiClip.ensureBakingBounds();
                           } else if (clipx instanceof CameraShakePovActionClip shakeClip) {
                              shakeClip.ensureBakingBounds();
                           }
                        }

                        for (PovActionClip recorded : recActions.takeSessionClips()) {
                           if (recorded instanceof ToastPovActionClip toastClip) {
                              toastClip.trimToRecording(recorder.tick);
                           }

                           targetActions.addClip(recorded.copy());
                        }
                     }

                     targetActions.sync();
                  }

                  for (KeyframeChannel<?> channel : replay.keyframes.getChannels()) {
                     if (!bbsPov$isAuthoredChannel(channel)) {
                        channel.removeAll();
                     }
                  }
               }

               if (this.bbsPov$editor != null) {
                  this.bbsPov$editor.reloadActions();
               }
            }
         }
      }
   }

   @Inject(
      method = {"applyRecordedKeyframes"},
      at = {@At("TAIL")}
   )
   private void bbsPov$clearActiveRecordingRange(Recorder recorder, Film film, CallbackInfo info) {
      boolean var10000;
      label26: {
         ActiveRecordingRange.clear();
         if (recorder instanceof RecorderPovAccess access && access.bbsPov$isOutside()) {
            var10000 = true;
            break label26;
         }

         var10000 = false;
      }

      boolean isOutside = var10000;
      if (!isOutside && film != null && film.replays.getList().size() > recorder.exception) {
         Replay replay = (Replay)film.replays.getList().get(recorder.exception);
         if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess targetAccess) {
            RecordedHudData targetHud = targetAccess.bbsPov$getHud();
            if (targetHud != null) {
               targetHud.ensureNativeSlotDefaults(replay.keyframes);
            }
         }
      }
   }

   @Unique
   private static boolean bbsPov$isAuthoredChannel(KeyframeChannel<?> channel) {
      String id = channel.getId();
      return id == null
         ? false
         : id.equals("hotbar_absorption_flash")
            || id.equals("hotbar_layout")
            || id.equals("hotbar_visible")
            || id.equals("hotbar_status_bars_visible")
            || id.equals("hotbar_crosshair")
            || id.equals("pov_hand_visible")
            || id.equals("pov_hand_model")
            || id.equals("pov_hand_texture")
            || id.equals("pov_hand_color")
            || id.equals("pov_hand_camera_offset")
            || id.equals("pov_hand_pose")
            || id.equals("pov_hand_item_pose")
            || id.equals("pov_hand_right_hand_visible")
            || id.equals("pov_hand_left_hand_visible")
            || id.equals("pov_hand_right_pose")
            || id.equals("pov_hand_left_pose")
            || id.equals("pov_hand_main_arm");
   }

   @Inject(
      method = {"applyRecordedKeyframes"},
      at = {@At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/film/replays/ReplayKeyframes;compressItemChannels()V",
         shift = Shift.AFTER
      )}
   )
   private void bbsPov$closeNativeItemTracksAfterCompression(Recorder recorder, Film film, CallbackInfo info) {
      if (recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHudData hud = access.bbsPov$getHud();
         if (hud != null) {
            hud.addNativeItemBoundaryKeyframes(recorder.keyframes, recorder.initialTick, recorder.tick);
         }
      }
   }

   @Inject(
      method = {"updateMainEditorVisibility"},
      at = {@At("TAIL")}
   )
   private void bbsPov$updateEditorVisibility(boolean enabled, CallbackInfo info) {
      if (this.bbsPov$editor != null) {
         boolean active = enabled && this.selectedMainEditorPanel == this.bbsPov$editor;
         this.bbsPov$editor.setVisible(active);
         this.bbsPov$editor.setTimelineVisible(active);
         this.bbsPov$editor.setPropertiesVisible(active);
         this.bbsPov$openEditor.active(active);
         this.bbsPov$syncNativeKeyframeEditor(active);
         this.bbsPov$syncPovMode(active);
      }
   }

   @Inject(
      method = {"showPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;)V"},
      at = {@At("TAIL")}
   )
   private void bbsPov$updateButton(UIElement panel, CallbackInfo info) {
      boolean povActive = panel == this.bbsPov$editor;
      if (this.bbsPov$openEditor != null) {
         this.bbsPov$openEditor.active(povActive);
      }

      this.bbsPov$syncNativeKeyframeEditor(povActive);
      this.bbsPov$syncPovMode(povActive);
   }

   @Inject(
      method = {"showPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;)V"},
      at = {@At("HEAD")}
   )
   private void bbsPov$stopGizmoBeforeEditorSwitch(UIElement panel, CallbackInfo info) {
      UIFilmPanel filmPanel = (UIFilmPanel)(Object)this;
      if (this.bbsPov$editor != null && (panel == this.bbsPov$editor || this.bbsPov$editor.isVisible())) {
         filmPanel.getController().stopGizmoInteraction();
         Gizmo.INSTANCE.stop();
      }
   }

   @Unique
   private void bbsPov$syncNativeKeyframeEditor(boolean povActive) {
      if (this.bbsPov$editor != null && this.bbsPov$replayKeyframeEditor != null) {
         UIFilmPanel panel = (UIFilmPanel)(Object)this;
         panel.replayEditor.keyframeEditor = povActive ? this.bbsPov$editor.keyframeEditor : this.bbsPov$replayKeyframeEditor;
      }
   }

   @Override
   public UIPovEditor bbsPov$getEditor() {
      return this.bbsPov$editor;
   }

   @Override
   public boolean bbsPov$isPovActive() {
      return this.bbsPov$editor != null && this.selectedMainEditorPanel == this.bbsPov$editor && this.bbsPov$editor.isVisible();
   }

   @Unique
   private void bbsPov$syncPovMode(boolean povActive) {
      UIFilmPanel panel = (UIFilmPanel)(Object)this;
      if (panel.replayEditor != null) {
         if (panel.replayEditor.replaysList instanceof UIReplaysListPanelPovAccess access) {
            access.bbsPov$setPovMode(povActive);
         }

         if (panel.replayEditor.replayProperties instanceof UIReplayPropertiesPovAccess access) {
            access.bbsPov$setPovMode(povActive);
         }
      }
   }
}
