package Glaxium.POV.editor;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.editor.UIPovActionPanels;
import Glaxium.POV.actions.timeline.PovActionTimelineFactory;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.section.ActionsEditorSection;
import Glaxium.POV.editor.section.BodyPartEditorSection;
import Glaxium.POV.editor.section.HandEditorSection;
import Glaxium.POV.editor.section.HudEditorSection;
import Glaxium.POV.editor.section.PovEditorSection;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.UIReplaysListPanelPovAccess;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.tracks.TrackCatalog;
import mchorse.bbs_mod.film.replays.tracks.TrackDescriptor;
import mchorse.bbs_mod.film.replays.tracks.TrackKind;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.forms.editors.UIForms;
import mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.items.FoldState;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.PoseTransform;

public final class UIPovEditor extends UIElement {
   private final UIFilmPanel filmPanel;
   private final UIElement partHeader;
   private final UIIcon hotbarTab;
   private final UIIcon handTab;
   private final UIIcon bodyPartTab;
   private final UIIcon actionsTab;
   private final PovActionTimelineFactory actionTimelineFactory = new PovActionTimelineFactory();
   public final UIClipsPanel actionTimeline;
   public final UIKeyframeEditor keyframeEditor;
   private final ModelForm handEditorForm = new ModelForm();
   private Film film;
   private Replay replay;
   private UIPovEditor.Section section = UIPovEditor.Section.HOTBAR;
   private UIPovEditor.Section previousSection = UIPovEditor.Section.HOTBAR;
   private String selectedBodyPart = "";
   private int bodyPartSignature = Integer.MIN_VALUE;
   private final FoldState<String> expandedTabs = new FoldState();
   private final List<UIKeyframeSheet> pendingSheets = new ArrayList<>();

   public UIPovEditor(UIFilmPanel filmPanel) {
      this.filmPanel = filmPanel;
      UIPovActionPanels.register();
      this.partHeader = new UIElement() {
         protected boolean subMouseClicked(UIContext context) {
            return this.area.isInside(context);
         }
      };
      this.partHeader.relative(this).x(20).y(0).w(120).h(21);
      this.partHeader.add(new UIRenderable(context -> this.partHeader.area.render(context.batcher, BBSSettings.baseSurface())));
      UILabel partName = new UILabel(this::getSelectedPartName).color(-5592406, false).labelAnchor(0.0F, 0.5F);
      partName.relative(this.partHeader).x(5).y(0).w(1.0F, -10).h(1.0F);
      partName.tooltip(() -> L10n.lang("bbs.ui.film.replays.selected_body_part").format(new Object[]{this.getSelectedPartName()}).get());
      this.partHeader.add(partName);
      this.hotbarTab = new UIIcon(Icons.HOTBAR, button -> this.setSection(UIPovEditor.Section.HOTBAR));
      this.hotbarTab.relative(this).xy(0, 0).wh(20, 20);
      this.hotbarTab.tooltip(IKey.constant("Hotbar"), Direction.RIGHT);
      this.handTab = new UIIcon(Icons.LIMB, button -> this.setSection(UIPovEditor.Section.HAND));
      this.handTab.relative(this).xy(0, 20).wh(20, 20);
      this.handTab.tooltip(IKey.constant("Hand"), Direction.RIGHT);
      this.bodyPartTab = new UIIcon(Icons.BLOCK, button -> {
         if (this.section == UIPovEditor.Section.BODY_PART) {
            this.selectBodyPart("");
            if (this.filmPanel.replayEditor != null) {
               this.filmPanel.replayEditor.selectBodyPart("");
            }
         } else {
            this.setSection(UIPovEditor.Section.BODY_PART);
         }
      });
      this.bodyPartTab.relative(this).xy(0, 0).wh(20, 20);
      this.bodyPartTab.tooltip(IKey.constant("BodyPart"), Direction.RIGHT);
      this.bodyPartTab.setVisible(false);
      this.actionsTab = new UIIcon(Icons.ACTION, button -> this.setSection(UIPovEditor.Section.ACTIONS));
      this.actionsTab.relative(this).xy(0, 40).wh(20, 20);
      this.actionsTab.tooltip(IKey.constant("POV Actions"), Direction.RIGHT);
      this.actionTimeline = new UIClipsPanel(this.filmPanel, this.actionTimelineFactory);
      this.actionTimeline.target(this.filmPanel.editArea);
      this.actionTimeline.relative(this).x(20).y(0).w(1.0F, -20).h(1.0F);
      this.keyframeEditor = new UIKeyframeEditor(consumer -> new UIFilmKeyframes(this.filmPanel.cameraEditor, consumer).absolute());
      this.keyframeEditor.target(this.filmPanel.editArea);
      this.keyframeEditor.relative(this).x(20).y(0).w(1.0F, -20).h(1.0F);
      this.keyframeEditor.view.duration(this::getDuration);
      this.keyframeEditor.view.getDopeSheet().setExpanded(this.expandedTabs);
      this.keyframeEditor.view.getDopeSheet().setEmptyState(UIKeys.KEYFRAMES_EMPTY_FILTERED, UIKeys.KEYFRAMES_EMPTY_FILTERED_HINT);
      this.keyframeEditor.view.context(menu -> {
         if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet) {
            menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, this::openTrackFilter);
         }
      });
      this.keyframeEditor.setUndoId("pov_keyframe_editor");
      this.add(new IUIElement[]{this.hotbarTab, this.handTab, this.bodyPartTab, this.actionsTab, this.actionTimeline, this.keyframeEditor, this.partHeader});
      this.setSection(UIPovEditor.Section.HOTBAR);
   }

   public void selectBodyPart(String path) {
      if (path == null) {
         path = "";
      }

      if (!this.selectedBodyPart.equals(path) || !path.isEmpty() && this.section != UIPovEditor.Section.BODY_PART) {
         this.selectedBodyPart = path;
         if (this.filmPanel != null && this.filmPanel.replayEditor != null) {
            if (this.filmPanel.replayEditor.getReplay() != null) {
               this.replay = this.filmPanel.replayEditor.getReplay();
            }

            if (this.filmPanel.replayEditor.replaysList != null) {
               this.filmPanel.replayEditor.replaysList.setBodyPartsReplay(this.replay, path);
               this.filmPanel.replayEditor.replaysList.bodyParts.setCurrentPath(path);
               this.filmPanel.replayEditor.replaysList.resize();
            }
         }

         if (!path.isEmpty()) {
            if (this.section != UIPovEditor.Section.BODY_PART) {
               this.previousSection = this.section;
            }

            this.hotbarTab.setVisible(false);
            this.handTab.setVisible(false);
            this.actionsTab.setVisible(false);
            this.bodyPartTab.setVisible(true);
            this.bodyPartTab.relative(this).xy(0, 0).wh(20, 20);
            this.bodyPartTab.resize();
            this.filmPanel.editArea.removeAll();
            if (this.section == UIPovEditor.Section.BODY_PART) {
               this.refreshSheets(false);
            } else {
               this.setSection(UIPovEditor.Section.BODY_PART);
            }
         } else {
            this.bodyPartTab.setVisible(false);
            this.hotbarTab.setVisible(true);
            this.handTab.setVisible(true);
            this.actionsTab.setVisible(true);
            this.hotbarTab.relative(this).xy(0, 0).wh(20, 20);
            this.handTab.relative(this).xy(0, 20).wh(20, 20);
            this.actionsTab.relative(this).xy(0, 40).wh(20, 20);
            this.hotbarTab.resize();
            this.handTab.resize();
            this.actionsTab.resize();
            this.filmPanel.editArea.removeAll();
            UIPovEditor.Section target = this.previousSection == UIPovEditor.Section.BODY_PART ? UIPovEditor.Section.HAND : this.previousSection;
            if (this.section == target) {
               this.refreshSheets(false);
            } else {
               this.setSection(target);
            }
         }
      }
   }

   public String getSelectedBodyPart() {
      return this.selectedBodyPart;
   }

   public void setFilm(Film film) {
      this.film = film;
      this.replay = film == null ? null : this.filmPanel.replayEditor.getReplay();
      if (this.replay == null) {
         this.selectBodyPart("");
      }

      if (this.section == UIPovEditor.Section.ACTIONS) {
         this.actionTimeline.setClips(this.getActions());
      } else {
         this.refreshSheets(true);
      }
   }

   public void reloadActions() {
      if (this.actionTimeline.clips != null) {
         this.actionTimeline.clips.clearSelection();
      }

      this.actionTimeline.pickClip(null);
      if (this.section == UIPovEditor.Section.ACTIONS) {
         this.actionTimeline.setClips(this.getActions());
      }
   }

   public Replay getReplay() {
      return this.replay;
   }

   public UIFilmPanel getFilmPanel() {
      return this.filmPanel;
   }

   public ModelForm getHandEditorForm() {
      return this.handEditorForm;
   }

   public FoldState<String> getExpandedTabs() {
      return this.expandedTabs;
   }

   public boolean isHandSection() {
      return this.isVisible() && this.section == UIPovEditor.Section.HAND;
   }

   public boolean isBodyPartSection() {
      return this.isVisible() && this.section == UIPovEditor.Section.BODY_PART;
   }

   public boolean isPoseGizmoSection() {
      return this.isHandSection() || this.isBodyPartSection();
   }

   public boolean pickViewport(UIContext context, Area viewport) {
      return (PovHandPicking.getPickedBodyPart() >= 0 || this.isBodyPartSection()) && BodyPartEditorSection.INSTANCE.pick(this, context)
         ? true
         : this.pickHand(context, viewport);
   }

   public boolean pickHand(UIContext context, Area viewport) {
      return HandEditorSection.INSTANCE.pick(this, context, viewport);
   }

   public UIPropTransform getHandGizmoTransform() {
      if (!this.isPoseGizmoSection()) {
         return null;
      } else {
         UIPropTransform transform = UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor);
         String bone = this.getGizmoBone();
         return transform != null && transform.getTransform() != null && bone != null && !bone.isBlank() ? transform : null;
      }
   }

   public String getGizmoBone() {
      if (this.isBodyPartSection() || this.selectedBodyPart != null && !this.selectedBodyPart.isBlank()) {
         String bodyPartBone = BodyPartEditorSection.INSTANCE.gizmoBone(this);
         if (bodyPartBone != null && !bodyPartBone.isBlank()) {
            return bodyPartBone;
         }
      }

      Pair<String, ?> selected = this.keyframeEditor.getBone();
      if (selected != null && selected.a != null && !((String)selected.a).isBlank()) {
         return (String)selected.a;
      } else {
         return this.selectedBodyPart != null && !this.selectedBodyPart.isBlank() ? this.selectedBodyPart : null;
      }
   }

   public void expandTrackById(String id) {
      this.expandedTabs.set(id, true);
      this.keyframeEditor.view.getDopeSheet().resize();
   }

   public void expandPoseTrack(KeyframeChannel<?> poseChannel) {
      IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();

      for (UIKeyframeSheet sheet : graph.getSheets()) {
         if (sheet.channel == poseChannel) {
            this.expandedTabs.set(sheet.id, true);
            this.keyframeEditor.view.getDopeSheet().resize();
            return;
         }
      }
   }

   public void selectClosestKeyframe(KeyframeChannel<?> channel) {
      if (channel != null && !channel.isEmpty()) {
         IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();
         UIKeyframeSheet poseSheet = null;

         for (UIKeyframeSheet sheet : graph.getSheets()) {
            if (sheet.channel == channel) {
               poseSheet = sheet;
               break;
            }
         }

         if (poseSheet != null) {
            Keyframe<?> closest = null;
            float distance = Float.POSITIVE_INFINITY;
            int cursor = this.filmPanel.getCursor();

            for (Keyframe<?> keyframe : channel.getKeyframes()) {
               float keyDistance = Math.abs(keyframe.getTick() - (float)cursor);
               if (keyDistance < distance) {
                  distance = keyDistance;
                  closest = keyframe;
               }
            }

            if (closest != null) {
               graph.clearSelection();
               poseSheet.selection.add(closest);
               graph.pickKeyframe(closest);
               this.filmPanel.setCursor((int)closest.getTick());
            }
         }
      }
   }

   public void setTimelineVisible(boolean visible) {
      this.keyframeEditor.setTimelineVisible(visible);
      this.actionTimeline.setTimelineVisible(visible);
   }

   public void setPropertiesVisible(boolean visible) {
      this.keyframeEditor.setPropertiesVisible(visible);
      this.actionTimeline.setPropertiesVisible(visible);
   }

   private void syncReplaySelection() {
      Replay selected = null;
      if (this.film != null) {
         boolean povEditMode = this.filmPanel.getController().getPovMode() == 6;
         if (!povEditMode) {
            selected = PovCameraClips.resolveReplay(this.film, PovCameraClips.resolve(this.film, (float)this.filmPanel.getCursor()));
         }

         if (selected == null) {
            selected = this.filmPanel.replayEditor.getReplay();
         }
      }

      if (selected != this.replay) {
         this.replay = selected;
         if (this.replay == null) {
            this.selectBodyPart("");
         }

         if (this.section == UIPovEditor.Section.ACTIONS) {
            this.actionTimeline.setClips(this.getActions());
         } else {
            this.refreshSheets(false);
         }
      }
   }

   private void setSection(UIPovEditor.Section section) {
      if (this.section != section) {
         this.rememberExpandedTabs();
         this.section = section;
         this.hotbarTab.active(section == UIPovEditor.Section.HOTBAR);
         this.handTab.active(section == UIPovEditor.Section.HAND);
         this.bodyPartTab.active(section == UIPovEditor.Section.BODY_PART);
         this.actionsTab.active(section == UIPovEditor.Section.ACTIONS);
         this.actionTimeline.setVisible(section == UIPovEditor.Section.ACTIONS && this.replay != null);
         this.keyframeEditor.setVisible(section != UIPovEditor.Section.ACTIONS && this.replay != null);
         this.filmPanel.editArea.removeAll();
         if (this.actionTimeline.clips != null) {
            this.actionTimeline.clips.clearSelection();
         }

         this.actionTimeline.pickClip(null);
         if (section == UIPovEditor.Section.ACTIONS) {
            this.actionTimeline.setClips(this.getActions());
         } else {
            this.actionTimeline.setClips(null);
            this.refreshSheets(false);
         }
      }
   }

   public void addPendingSheet(UIKeyframeSheet sheet) {
      if (sheet != null) {
         this.pendingSheets.add(sheet);
      }
   }

   public void refreshSheets(boolean resetView) {
      this.keyframeEditor.view.removeAllSheets();
      this.pendingSheets.clear();
      if (this.replay != null) {
         this.currentSection().fillSheets(this, resetView);
         Set<String> disabled = this.getDisabledTracks();
         this.pendingSheets.removeIf(sheetx -> isSheetDisabled(sheetx, disabled));
         UIReplaysEditorUtils.pruneTree(this.pendingSheets);

         for (UIKeyframeSheet sheet : this.pendingSheets) {
            this.keyframeEditor.view.addSheet(sheet);
         }

         this.keyframeEditor.view.getDopeSheet().setExpanded(this.expandedTabs);
         if (resetView) {
            this.keyframeEditor.view.resetView();
         }

         this.keyframeEditor.view.getDopeSheet().getYAxis().clamp();
      }
   }

   public void openTrackFilter() {
      this.openTrackFilter(this.section);
   }

   public void openTrackFilter(UIPovEditor.Section targetSection) {
      if (this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHandData var9 = access.bbsPov$getHand();
         RecordedHudData hud = access.bbsPov$getHud();
         LinkedHashSet keys = new LinkedHashSet();
         HashMap keyToColor = new HashMap();
         Set<String> disabled;
         if (targetSection == UIPovEditor.Section.HOTBAR) {
            if (hud == null) {
               return;
            }

            disabled = (Set<String>)hud.disabledTracks.get();
            collectHudTrackKeys(hud, keys, keyToColor);
         } else if (targetSection == UIPovEditor.Section.HAND) {
            if (var9 == null) {
               return;
            }

            disabled = (Set<String>)var9.disabledTracks.get();
            collectHandTrackKeys(this, var9, keys, keyToColor);
         } else {
            if (targetSection != UIPovEditor.Section.BODY_PART) {
               return;
            }

            if (var9 == null) {
               return;
            }

            ModelForm root = BodyPartEditorSection.INSTANCE.root(var9);
            if (root == null) {
               return;
            }

            disabled = (Set<String>)var9.disabledTracks.get();
            collectBodyPartTrackKeys(root, var9, this.selectedBodyPart, keys, keyToColor);
         }

         UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(disabled, keys, keyToColor);
         UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);
         panel.onClose(e -> {
            if (targetSection == UIPovEditor.Section.HOTBAR && hud != null) {
               hud.disabledTracks.set(disabled);
            } else if (var9 != null) {
               var9.disabledTracks.set(disabled);
            }

            this.refreshSheets(false);
         });
      }
   }

   public static boolean isSheetDisabled(UIKeyframeSheet sheet, Set<String> disabled) {
      if (sheet == null) {
         return false;
      } else {
         String key = getSheetFilterKey(sheet);
         String lowerKey = key.toLowerCase();
         if (disabled != null && !disabled.isEmpty()) {
            for (String s : disabled) {
               String lowerS = s.toLowerCase();
               if (key.equals(s) || lowerKey.equals(lowerS) || sheet.id.equals(s) || sheet.id.endsWith("/" + s)) {
                  return true;
               }
            }

            boolean isPoseDisabled = disabled.contains("Pose") || disabled.contains("pose") || disabled.contains("pov_hand_pose");
            if (isPoseDisabled
               && (
                  sheet.id.startsWith("pose_overlay")
                     || sheet.id.contains("pose_overlay")
                     || sheet.isBoneTrack
                     || sheet.id.startsWith("bone:")
                     || sheet.id.contains("/bone:")
                     || sheet.descriptor != null && sheet.descriptor.kind() == TrackKind.BONE
               )) {
               return true;
            }

            boolean isTransformDisabled = disabled.contains("Transform")
               || disabled.contains("transform")
               || disabled.contains("camera_offset")
               || disabled.contains("Camera Offset");
            if (isTransformDisabled
               && (key.equalsIgnoreCase("camera offset") || sheet.id.startsWith("transform_overlay") || sheet.id.contains("transform_overlay"))) {
               return true;
            }
         }

         Form owner = UIReplaysEditor.getSheetForm(sheet);
         if (owner != null) {
            Set<String> ownerDisabled = (Set<String>)owner.disabledTracks.get();
            if (ownerDisabled != null && !ownerDisabled.isEmpty()) {
               if (ownerDisabled.contains("*") || ownerDisabled.contains(key) || ownerDisabled.contains(lowerKey) || ownerDisabled.contains(sheet.id)) {
                  return true;
               }

               if ((ownerDisabled.contains("pose") || ownerDisabled.contains("Pose"))
                  && (
                     sheet.id.startsWith("pose_overlay")
                        || sheet.id.contains("pose_overlay")
                        || sheet.isBoneTrack
                        || sheet.id.startsWith("bone:")
                        || sheet.id.contains("/bone:")
                        || sheet.descriptor != null && sheet.descriptor.kind() == TrackKind.BONE
                  )) {
                  return true;
               }

               if ((ownerDisabled.contains("transform") || ownerDisabled.contains("Transform") || ownerDisabled.contains("camera_offset"))
                  && (key.equalsIgnoreCase("camera offset") || sheet.id.startsWith("transform_overlay") || sheet.id.contains("transform_overlay"))) {
                  return true;
               }
            }
         }

         for (UIKeyframeSheet p = sheet.parent; p != null; p = p.parent) {
            if (isSheetDisabled(p, disabled)) {
               return true;
            }
         }

         return false;
      }
   }

   public Set<String> getDisabledTracks() {
      if (this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHandData var4 = access.bbsPov$getHand();
         RecordedHudData hud = access.bbsPov$getHud();
         if (this.section == UIPovEditor.Section.HOTBAR) {
            return hud != null ? (Set)hud.disabledTracks.get() : Collections.emptySet();
         } else if (this.section == UIPovEditor.Section.HAND) {
            return var4 != null ? (Set)var4.disabledTracks.get() : Collections.emptySet();
         } else if (this.section == UIPovEditor.Section.BODY_PART) {
            return var4 != null ? (Set)var4.disabledTracks.get() : Collections.emptySet();
         } else {
            return Collections.emptySet();
         }
      } else {
         return Collections.emptySet();
      }
   }

   public static String getSheetFilterKey(UIKeyframeSheet sheet) {
      if (sheet == null) {
         return "";
      } else if (sheet.descriptor != null) {
         return sheet.descriptor.filterKey();
      } else if (sheet.isBoneTrack) {
         return sheet.title != null ? sheet.title.get() : sheet.id;
      } else {
         return sheet.title != null ? sheet.title.get() : sheet.getFilterKey();
      }
   }

   public static void addTrackKey(String key, int color, Set<String> keys, Map<String, Integer> keyToColor) {
      keys.add(key);
      keyToColor.put(key, color);
   }

   public static void collectHandTrackKeys(UIPovEditor editor, RecordedHandData hand, Set<String> keys, Map<String, Integer> keyToColor) {
      addTrackKey("Visible", UIReplaysEditor.getColor("visible"), keys, keyToColor);
      addTrackKey("Model", UIReplaysEditor.getColor("model"), keys, keyToColor);
      addTrackKey("Texture", UIReplaysEditor.getColor("texture"), keys, keyToColor);
      addTrackKey("Color", UIReplaysEditor.getColor("color"), keys, keyToColor);
      addTrackKey("Color Overlay", UIReplaysEditor.getColor("color_overlay"), keys, keyToColor);
      addTrackKey("Camera Offset", UIReplaysEditor.getColor("transform"), keys, keyToColor);
      addTrackKey("Pose", UIReplaysEditor.getColor("pose"), keys, keyToColor);
      if ((Boolean)BBSSettings.recordingOverlays.get()) {
         addTrackKey("pose_overlay", UIReplaysEditor.getColor("pose_overlay"), keys, keyToColor);
         int additional = (Integer)BBSSettings.recordingPoseOverlays.get();

         for (int k = 0; k < additional; k++) {
            addTrackKey("pose_overlay" + k, UIReplaysEditor.getColor("pose_overlay" + k), keys, keyToColor);
         }
      }

      if (editor != null) {
         ModelForm handForm = editor.getHandEditorForm();
         ModelInstance model = ModelFormRenderer.getModel(handForm);
         if (model == null || model.getModel() == null) {
            ModelForm baseForm = HandEditorSection.INSTANCE.baseModelForm(editor, hand);
            if (baseForm != null) {
               model = ModelFormRenderer.getModel(baseForm);
            }
         }

         if ((model == null || model.getModel() == null) && BBSModClient.getModels() != null) {
            model = BBSModClient.getModels().getModel(HandEditorSection.INSTANCE.currentModel(editor, hand));
         }

         HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);
         int colorIdx = 0;

         for (String bone : handBones.depths().keySet()) {
            addTrackKey(bone, UIKeyframeEditor.COLORS[colorIdx++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
         }
      }

      addTrackKey("Item Pose", UIReplaysEditor.getColor("pose"), keys, keyToColor);
      addTrackKey("Right Hand Visible", UIReplaysEditor.getColor("visible"), keys, keyToColor);
      addTrackKey("Left Hand Visible", UIReplaysEditor.getColor("visible"), keys, keyToColor);
      int handColor = 0;
      addTrackKey("Off Hand Item", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Swinging Hand", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Swing Progress", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Main Equip", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Offhand Equip", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Active Use Hand", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Active Use Item", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Show Particles", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Use Time", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Bob Phase", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Bob Strength", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Render Yaw", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Render Pitch", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Left-handed Main Arm", UIKeyframeEditor.COLORS[handColor++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
   }

   public static void collectHudTrackKeys(RecordedHudData hud, Set<String> keys, Map<String, Integer> keyToColor) {
      int c = 0;
      addTrackKey("Layout", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Visible", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Status Bars", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Crosshair", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Cursor Layout", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Cursor Visible", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Cursor Item", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Selected Slot", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);

      for (int i = 1; i <= 9; i++) {
         addTrackKey("Slot " + i, UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      }

      addTrackKey("Inventory Slots", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Offhand", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Health", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Previous Health", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Health Flash", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Health Container", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Absorption", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Absorption Container", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Heart Type", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Hardcore", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Heart Regeneration", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Armor", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Hunger", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Hunger Effect", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Mount Health", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Mount Health Container", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Air", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Experience", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Experience Level", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
      addTrackKey("Golden Heart Flash", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
   }

   public static void collectBodyPartTrackKeys(ModelForm root, RecordedHandData hand, String selectedPart, Set<String> keys, Map<String, Integer> keyToColor) {
      List<TrackDescriptor> descriptors;
      if (selectedPart != null && !selectedPart.isBlank()) {
         descriptors = TrackCatalog.forPart(root, hand.bodyPartTracks, selectedPart);
      } else {
         descriptors = new ArrayList<>();
         BodyPartEditorSection.collectAllPartDescriptors(root, root, hand.bodyPartTracks, descriptors);
      }

      for (TrackDescriptor track : descriptors) {
         keys.add(track.filterKey());
         keyToColor.put(track.filterKey(), track.color());
      }
   }

   private static int computeHierarchySignature(Form form) {
      if (form == null) {
         return 0;
      } else {
         int sig = System.identityHashCode(form) ^ form.getDisplayName().hashCode();

         for (BodyPart part : form.parts.getAllTyped()) {
            if (part != null) {
               sig = 31 * sig + part.getId().hashCode();
               if (part.getForm() != null) {
                  sig = 31 * sig + computeHierarchySignature(part.getForm());
               }
            }
         }

         return sig;
      }
   }

   private PovEditorSection currentSection() {
      return (PovEditorSection)(switch (this.section) {
         case HAND -> HandEditorSection.INSTANCE;
         case BODY_PART -> BodyPartEditorSection.INSTANCE;
         case ACTIONS -> ActionsEditorSection.INSTANCE;
         default -> HudEditorSection.INSTANCE;
      });
   }

   public RecordedPovActions getActions() {
      return this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access ? access.bbsPov$getActions() : null;
   }

   private void rememberExpandedTabs() {
   }

   public void reloadHandModel() {
      HandEditorSection.INSTANCE.reloadModel(this);
   }

   public String getCurrentHandModel(RecordedHandData hand) {
      return HandEditorSection.INSTANCE.currentModel(this, hand);
   }

   public Link getCurrentHandTexture(RecordedHandData hand) {
      return HandEditorSection.INSTANCE.currentTexture(this, hand);
   }

   public ModelForm getPovBaseModelForm(RecordedHandData hand) {
      return HandEditorSection.INSTANCE.baseModelForm(this, hand);
   }

   public UIKeyframeSheet createBoneSheet(String sheetId, String bone, KeyframeChannel<PoseTransform> channel, int colorIndex) {
      return HandEditorSection.INSTANCE.createBoneSheet(this, sheetId, bone, channel, colorIndex);
   }

   public int addSheet(String title, KeyframeChannel<?> channel, Icon icon, int colorIndex) {
      return this.addSheet(title, channel, icon, colorIndex, null);
   }

   public int addSheet(String title, KeyframeChannel<?> channel, Icon icon, int colorIndex, BaseValueBasic property, Supplier<Object> seed) {
      this.createSheet(title, channel, icon, colorIndex, property, seed);
      return colorIndex + 1;
   }

   public int addSheet(String title, KeyframeChannel<?> channel, Icon icon, int colorIndex, Supplier<Object> seed) {
      this.createSheet(title, channel, icon, colorIndex, null, seed);
      return colorIndex + 1;
   }

   public UIKeyframeSheet createSheet(String title, KeyframeChannel<?> channel, Icon icon, int colorIndex, BaseValueBasic property, Supplier<Object> seed) {
      int color = UIKeyframeEditor.COLORS[colorIndex % UIKeyframeEditor.COLORS.length];
      UIKeyframeSheet sheet = new UIKeyframeSheet(channel.getId(), IKey.constant(title), color, channel, property);
      sheet.icon(icon);
      if (seed != null) {
         sheet.seed(seed);
      }

      this.addPendingSheet(sheet);
      return sheet;
   }

   public UIKeyframeSheet createSheetWithColor(String title, KeyframeChannel<?> channel, Icon icon, int color, BaseValueBasic property, Supplier<Object> seed) {
      UIKeyframeSheet sheet = new UIKeyframeSheet(channel.getId(), IKey.constant(title), color, channel, property);
      sheet.icon(icon);
      if (seed != null) {
         sheet.seed(seed);
      }

      this.addPendingSheet(sheet);
      return sheet;
   }

   public UIKeyframeSheet addSheetWithColor(String title, KeyframeChannel<?> channel, Icon icon, int color, BaseValueBasic property, Supplier<Object> seed) {
      return this.createSheetWithColor(title, channel, icon, color, property, seed);
   }

   public UIKeyframeSheet addSheetWithColor(String title, KeyframeChannel<?> channel, Icon icon, int color, Supplier<Object> seed) {
      return this.createSheetWithColor(title, channel, icon, color, null, seed);
   }

   public UIKeyframeSheet addSheetWithColor(String title, KeyframeChannel<?> channel, Icon icon, int color) {
      return this.createSheetWithColor(title, channel, icon, color, null, null);
   }

   private int getDuration() {
      return this.film == null ? 1 : Math.max(1, this.film.camera.calculateDuration());
   }

   public int getReplayTick() {
      return this.replay == null ? this.filmPanel.getCursor() : this.replay.getTick(this.filmPanel.getCursor());
   }

   public void render(UIContext context) {
      this.syncReplaySelection();
      RecordedHandData hand = this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access ? access.bbsPov$getHand() : null;
      ModelForm root = BodyPartEditorSection.INSTANCE.root(hand);
      int signature = computeHierarchySignature(root);
      if (!this.selectedBodyPart.isEmpty()) {
         boolean partExists = root != null && FormUtils.getForm(root, this.selectedBodyPart) != null && FormUtils.getForm(root, this.selectedBodyPart) != root;
         if (!partExists) {
            this.selectBodyPart("");
         }
      }

      if (signature != this.bodyPartSignature) {
         this.bodyPartSignature = signature;
         if (this.filmPanel != null && this.filmPanel.replayEditor != null && this.filmPanel.replayEditor.replaysList != null) {
            this.filmPanel.replayEditor.replaysList.setBodyPartsReplay(this.replay, this.selectedBodyPart);
            this.filmPanel.replayEditor.replaysList.resize();
         }

         if (this.section == UIPovEditor.Section.BODY_PART) {
            this.refreshSheets(false);
         }
      }
      UIIcon activeTab = switch (this.section) {
         case HAND -> this.handTab;
         case BODY_PART -> this.bodyPartTab;
         case ACTIONS -> this.actionsTab;
         default -> this.hotbarTab;
      };
      if (activeTab != null && activeTab.isVisible()) {
         context.batcher.highlight(activeTab.area, Direction.RIGHT);
      }

      if (this.isPoseGizmoSection()) {
         UIPropTransform transform = UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor);
         if (transform != null) {
            transform.hotkeyDrag(
               () -> UIReplaysEditorUtils.buildFilmGizmoDrag(
                     this.filmPanel, this.filmPanel.getCamera(), this.filmPanel.preview.getViewport(), transform, context.getTransition()
                  )
            );
            transform.worldTransform(output -> HandEditorSection.INSTANCE.getWorldMatrix(this, output));
         }
      }

      this.keyframeEditor.setVisible(this.section != UIPovEditor.Section.ACTIONS && this.replay != null);
      this.actionTimeline.setVisible(this.section == UIPovEditor.Section.ACTIONS && this.replay != null);
      this.partHeader
         .setVisible(
            this.replay != null
               && this.keyframeEditor != null
               && this.keyframeEditor.isVisible()
               && this.keyframeEditor.view.getGraph() == this.keyframeEditor.view.getDopeSheet()
               && !this.keyframeEditor.view.getDopeSheet().getSheets().isEmpty()
         );
      if (this.partHeader.isVisible()) {
         int labelWidth = Math.min(this.keyframeEditor.view.getLabelWidth(), this.keyframeEditor.view.area.w);
         if (this.partHeader.area.w != labelWidth) {
            this.partHeader.w(labelWidth);
            this.partHeader.resize();
         }
      }

      super.render(context);
   }

   public String getSelectedPartName() {
      if (this.replay == null) {
         return "-";
      } else if (this.selectedBodyPart != null && !this.selectedBodyPart.isEmpty()) {
         if (this.filmPanel != null && this.filmPanel.replayEditor != null && this.filmPanel.replayEditor.replaysList != null) {
            UIReplaysListPanel replaysList = this.filmPanel.replayEditor.replaysList;
            if (replaysList.bodyParts != null) {
               for (FormEntry entry : replaysList.bodyParts.getList()) {
                  if (this.selectedBodyPart.equals(entry.getPath())) {
                     return entry.toString();
                  }
               }
            }

            if (replaysList instanceof UIReplaysListPanelPovAccess access) {
               UIForms povParts = access.bbsPov$getPovBodyParts();
               if (povParts != null) {
                  for (FormEntry entryx : povParts.getList()) {
                     if (this.selectedBodyPart.equals(entryx.getPath())) {
                        return entryx.toString();
                     }
                  }
               }
            }
         }

         RecordedHandData hand = this.replay.keyframes instanceof ReplayKeyframesPovAccess accessx ? accessx.bbsPov$getHand() : null;
         ModelForm root = BodyPartEditorSection.INSTANCE.root(hand);
         if (root != null) {
            Form form = FormUtils.getForm(root, this.selectedBodyPart);
            if (form != null) {
               return form.getDisplayName();
            }
         }

         if (this.replay.form.get() != null) {
            Form form = FormUtils.getForm((Form)this.replay.form.get(), this.selectedBodyPart);
            if (form != null) {
               return form.getDisplayName();
            }
         }

         return this.selectedBodyPart;
      } else {
         RecordedHandData handx = this.replay.keyframes instanceof ReplayKeyframesPovAccess accessxx ? accessxx.bbsPov$getHand() : null;
         if (handx != null && handx.baseForm.get() != null) {
            return ((Form)handx.baseForm.get()).getDisplayName();
         } else {
            return this.replay.form.get() != null ? ((Form)this.replay.form.get()).getDisplayName() : "-";
         }
      }
   }

   public static enum Section {
      HOTBAR,
      HAND,
      BODY_PART,
      ACTIONS;
   }
}
