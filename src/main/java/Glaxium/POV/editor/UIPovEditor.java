package Glaxium.POV.editor;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.timeline.PovActionTimelineFactory;
import Glaxium.POV.actions.editor.UIPovActionPanels;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.ui.framework.elements.input.items.FoldState;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.editor.section.ActionsEditorSection;
import Glaxium.POV.editor.section.BodyPartEditorSection;
import Glaxium.POV.editor.section.HandEditorSection;
import Glaxium.POV.editor.section.HudEditorSection;
import Glaxium.POV.editor.section.PovEditorSection;
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
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import Glaxium.POV.integration.access.bbs.UIReplaysListPanelPovAccess;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.UIForms;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.renderers.TimelineRulerRenderer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** A Film editor dedicated to POV Hotbar, Hand and native Actions clips. */
public final class UIPovEditor extends UIElement
{
    public enum Section
    {
        HOTBAR,
        HAND,
        BODY_PART,
        ACTIONS
    }

    private final UIFilmPanel filmPanel;
    private final UIElement partHeader;
    private final UIIcon hotbarTab;
    private final UIIcon handTab;
    private final UIIcon bodyPartTab;
    private final UIIcon actionsTab;
    private final PovActionTimelineFactory actionTimelineFactory = new PovActionTimelineFactory();
    public final UIClipsPanel actionTimeline;
    public final UIKeyframeEditor keyframeEditor;
    /** Supplies BBS's native Pose editor with the active hand model and its bone list. */
    private final ModelForm handEditorForm = new ModelForm();

    private Film film;
    private Replay replay;
    private Section section = Section.HOTBAR;
    private Section previousSection = Section.HOTBAR;
    private String selectedBodyPart = "";
    private int bodyPartSignature = Integer.MIN_VALUE;
    /** Folder state belongs to each category, not to the transient sheet list. */
    private final FoldState<String> expandedTabs = new FoldState<>();
    private final List<UIKeyframeSheet> pendingSheets = new ArrayList<>();

    public UIPovEditor(UIFilmPanel filmPanel)
    {
        this.filmPanel = filmPanel;
        UIPovActionPanels.register();

        this.partHeader = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                return this.area.isInside(context);
            }
        };
        this.partHeader.relative(this).x(20).y(0).w(120).h(TimelineRulerRenderer.RULER_BLOCK_HEIGHT);
        this.partHeader.add(new UIRenderable((context) ->
        {
            this.partHeader.area.render(context.batcher, BBSSettings.baseSurface());
        }));
        UILabel partName = new UILabel(this::getSelectedPartName).color(0xffaaaaaa, false).labelAnchor(0F, 0.5F);
        partName.relative(this.partHeader).x(5).y(0).w(1F, -10).h(1F);
        partName.tooltip(() -> L10n.lang("bbs.ui.film.replays.selected_body_part").format(this.getSelectedPartName()).get());
        this.partHeader.add(partName);

        this.hotbarTab = new UIIcon(Icons.HOTBAR, (button) -> this.setSection(Section.HOTBAR));
        this.hotbarTab.relative(this).xy(0, 0).wh(20, 20);
        this.hotbarTab.tooltip(IKey.constant("Hotbar"), Direction.RIGHT);

        this.handTab = new UIIcon(Icons.LIMB, (button) -> this.setSection(Section.HAND));
        this.handTab.relative(this).xy(0, 20).wh(20, 20);
        this.handTab.tooltip(IKey.constant("Hand"), Direction.RIGHT);

        this.bodyPartTab = new UIIcon(Icons.BLOCK, (button) -> {
            if (this.section == Section.BODY_PART)
            {
                this.selectBodyPart("");
                if (this.filmPanel.replayEditor != null)
                {
                    this.filmPanel.replayEditor.selectBodyPart("");
                }
            }
            else
            {
                this.setSection(Section.BODY_PART);
            }
        });
        this.bodyPartTab.relative(this).xy(0, 0).wh(20, 20);
        this.bodyPartTab.tooltip(IKey.constant("BodyPart"), Direction.RIGHT);
        this.bodyPartTab.setVisible(false);

        this.actionsTab = new UIIcon(Icons.ACTION, (button) -> this.setSection(Section.ACTIONS));
        this.actionsTab.relative(this).xy(0, 40).wh(20, 20);
        this.actionsTab.tooltip(IKey.constant("POV Actions"), Direction.RIGHT);

        this.actionTimeline = new UIClipsPanel(this.filmPanel, this.actionTimelineFactory);
        this.actionTimeline.target(this.filmPanel.editArea);
        this.actionTimeline.relative(this).x(20).y(0).w(1F, -20).h(1F);

        this.keyframeEditor = new UIKeyframeEditor((consumer) ->
            new UIFilmKeyframes(this.filmPanel.cameraEditor, consumer).absolute());
        this.keyframeEditor.target(this.filmPanel.editArea);
        this.keyframeEditor.relative(this).x(20).y(0).w(1F, -20).h(1F);
        this.keyframeEditor.view.duration(this::getDuration);
        this.keyframeEditor.view.getDopeSheet().setExpanded(this.expandedTabs);
        this.keyframeEditor.view.getDopeSheet().setEmptyState(UIKeys.KEYFRAMES_EMPTY_FILTERED, UIKeys.KEYFRAMES_EMPTY_FILTERED_HINT);
        this.keyframeEditor.view.context(menu ->
        {
            if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet)
            {
                menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, this::openTrackFilter);
            }
        });
        this.keyframeEditor.setUndoId("pov_keyframe_editor");

        this.add(this.hotbarTab, this.handTab, this.bodyPartTab, this.actionsTab,
            this.actionTimeline, this.keyframeEditor, this.partHeader);
        this.setSection(Section.HOTBAR);
    }

    public void selectBodyPart(String path)
    {
        if (path == null)
        {
            path = "";
        }

        if (this.selectedBodyPart.equals(path) && (path.isEmpty() || this.section == Section.BODY_PART))
        {
            return;
        }

        this.selectedBodyPart = path;

        if (this.filmPanel != null && this.filmPanel.replayEditor != null)
        {
            if (this.filmPanel.replayEditor.getReplay() != null)
            {
                this.replay = this.filmPanel.replayEditor.getReplay();
            }
            if (this.filmPanel.replayEditor.replaysList != null)
            {
                this.filmPanel.replayEditor.replaysList.setBodyPartsReplay(this.replay, path);
                this.filmPanel.replayEditor.replaysList.bodyParts.setCurrentPath(path);
                this.filmPanel.replayEditor.replaysList.resize();
            }
        }

        if (!path.isEmpty())
        {
            if (this.section != Section.BODY_PART)
            {
                this.previousSection = this.section;
            }

            this.hotbarTab.setVisible(false);
            this.handTab.setVisible(false);
            this.actionsTab.setVisible(false);

            this.bodyPartTab.setVisible(true);
            this.bodyPartTab.relative(this).xy(0, 0).wh(20, 20);
            this.bodyPartTab.resize();

            this.filmPanel.editArea.removeAll();
            if (this.section == Section.BODY_PART)
            {
                this.refreshSheets(false);
            }
            else
            {
                this.setSection(Section.BODY_PART);
            }
        }
        else
        {
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
            Section target = this.previousSection == Section.BODY_PART ? Section.HAND : this.previousSection;
            if (this.section == target)
            {
                this.refreshSheets(false);
            }
            else
            {
                this.setSection(target);
            }
        }
    }

    public String getSelectedBodyPart()
    {
        return this.selectedBodyPart;
    }

    public void setFilm(Film film)
    {
        this.film = film;
        this.replay = film == null ? null : this.filmPanel.replayEditor.getReplay();
        if (this.replay == null)
        {
            this.selectBodyPart("");
        }

        if (this.section == Section.ACTIONS)
        {
            this.actionTimeline.setClips(this.getActions());
        }
        else
        {
            this.refreshSheets(true);
        }
    }

    public void reloadActions()
    {
        if (this.actionTimeline.clips != null)
        {
            this.actionTimeline.clips.clearSelection();
        }

        this.actionTimeline.pickClip(null);

        if (this.section == Section.ACTIONS)
        {
            this.actionTimeline.setClips(this.getActions());
        }
    }

    public Replay getReplay()
    {
        return this.replay;
    }

    public UIFilmPanel getFilmPanel()
    {
        return this.filmPanel;
    }

    public ModelForm getHandEditorForm()
    {
        return this.handEditorForm;
    }

    public FoldState<String> getExpandedTabs()
    {
        return this.expandedTabs;
    }

    public Section getSection()
    {
        return this.section;
    }

    public boolean isHandSection()
    {
        return this.isVisible() && this.section == Section.HAND;
    }

    public boolean isBodyPartSection()
    {
        return this.isVisible() && this.section == Section.BODY_PART;
    }

    public boolean isActionsSection()
    {
        return this.isVisible() && this.section == Section.ACTIONS;
    }

    public boolean isPoseGizmoSection()
    {
        return this.isHandSection() || this.isBodyPartSection();
    }

    public boolean pickViewport(UIContext context, Area viewport)
    {
        if (Glaxium.POV.hand.editor.PovHandPicking.getPickedBodyPart() >= 0 || this.isBodyPartSection())
        {
            if (BodyPartEditorSection.INSTANCE.pick(this, context))
            {
                return true;
            }
        }

        return this.pickHand(context, viewport);
    }

    public boolean pickHand(UIContext context, Area viewport)
    {
        return HandEditorSection.INSTANCE.pick(this, context, viewport);
    }

    /** A gizmo is valid only for an actually selected POV Pose transform. */
    public UIPropTransform getHandGizmoTransform()
    {
        if (!this.isPoseGizmoSection())
        {
            return null;
        }

        UIPropTransform transform = UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor);
        String bone = this.getGizmoBone();

        return transform != null && transform.getTransform() != null
            && bone != null && !bone.isBlank()
            ? transform
            : null;
    }

    public String getGizmoBone()
    {
        if (this.isBodyPartSection() || (this.selectedBodyPart != null && !this.selectedBodyPart.isBlank()))
        {
            String bodyPartBone = BodyPartEditorSection.INSTANCE.gizmoBone(this);
            if (bodyPartBone != null && !bodyPartBone.isBlank())
            {
                return bodyPartBone;
            }
        }

        Pair<String, ?> selected = this.keyframeEditor.getBone();

        if (selected != null && selected.a != null && !selected.a.isBlank())
        {
            return selected.a;
        }

        if (this.selectedBodyPart != null && !this.selectedBodyPart.isBlank())
        {
            return this.selectedBodyPart;
        }

        return null;
    }

    public void expandTrackById(String id)
    {
        this.expandedTabs.set(id, true);
        this.keyframeEditor.view.getDopeSheet().resize();
    }

    public void expandPoseTrack(KeyframeChannel<?> poseChannel)
    {
        IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();

        for (UIKeyframeSheet sheet : graph.getSheets())
        {
            if (sheet.channel == poseChannel)
            {
                this.expandedTabs.set(sheet.id, true);
                this.keyframeEditor.view.getDopeSheet().resize();
                return;
            }
        }
    }

    public void selectClosestKeyframe(KeyframeChannel<?> channel)
    {
        if (channel == null || channel.isEmpty())
        {
            return;
        }

        IUIKeyframeGraph graph = this.keyframeEditor.view.getGraph();
        UIKeyframeSheet poseSheet = null;

        for (UIKeyframeSheet sheet : graph.getSheets())
        {
            if (sheet.channel == channel)
            {
                poseSheet = sheet;
                break;
            }
        }

        if (poseSheet == null)
        {
            return;
        }

        Keyframe<?> closest = null;
        float distance = Float.POSITIVE_INFINITY;
        int cursor = this.filmPanel.getCursor();

        for (Keyframe<?> keyframe : channel.getKeyframes())
        {
            float keyDistance = Math.abs(keyframe.getTick() - cursor);

            if (keyDistance < distance)
            {
                distance = keyDistance;
                closest = keyframe;
            }
        }

        if (closest != null)
        {
            graph.clearSelection();
            poseSheet.selection.add(closest);
            graph.pickKeyframe(closest);
            this.filmPanel.setCursor((int) closest.getTick());
        }
    }

    public void setTimelineVisible(boolean visible)
    {
        this.keyframeEditor.setTimelineVisible(visible);
        this.actionTimeline.setTimelineVisible(visible);
    }

    public void setPropertiesVisible(boolean visible)
    {
        this.keyframeEditor.setPropertiesVisible(visible);
        this.actionTimeline.setPropertiesVisible(visible);
    }

    /**
     * In POV Camera Mode the editor follows the manually selected replay, which
     * is required for full hand picking/gizmo editing. In normal Camera Mode an
     * active POV Camera Clip becomes the editor source so manual keyframe value
     * changes update the same hand/HUD that is visible in the shot.
     */
    private void syncReplaySelection()
    {
        Replay selected = null;

        if (this.film != null)
        {
            boolean povEditMode = this.filmPanel.getController().getPovMode() == PovCameraMode.POV;

            if (!povEditMode)
            {
                selected = PovCameraClips.resolveReplay(
                    this.film,
                    PovCameraClips.resolve(this.film, this.filmPanel.getCursor()));
            }

            if (selected == null)
            {
                selected = this.filmPanel.replayEditor.getReplay();
            }
        }

        if (selected != this.replay)
        {
            this.replay = selected;

            if (this.replay == null)
            {
                this.selectBodyPart("");
            }

            if (this.section == Section.ACTIONS)
            {
                this.actionTimeline.setClips(this.getActions());
            }
            else
            {
                this.refreshSheets(false);
            }
        }
    }

    private void setSection(Section section)
    {
        if (this.section == section)
        {
            return;
        }

        this.rememberExpandedTabs();
        this.section = section;
        this.hotbarTab.active(section == Section.HOTBAR);
        this.handTab.active(section == Section.HAND);
        this.bodyPartTab.active(section == Section.BODY_PART);
        this.actionsTab.active(section == Section.ACTIONS);
        this.actionTimeline.setVisible(section == Section.ACTIONS && this.replay != null);
        this.keyframeEditor.setVisible(section != Section.ACTIONS && this.replay != null);
        this.filmPanel.editArea.removeAll();

        if (this.actionTimeline.clips != null)
        {
            this.actionTimeline.clips.clearSelection();
        }
        this.actionTimeline.pickClip(null);

        if (section == Section.ACTIONS)
        {
            this.actionTimeline.setClips(this.getActions());
        }
        else
        {
            this.actionTimeline.setClips(null);
            this.refreshSheets(false);
        }
    }

    public void addPendingSheet(UIKeyframeSheet sheet)
    {
        if (sheet != null)
        {
            this.pendingSheets.add(sheet);
        }
    }

    public void refreshSheets(boolean resetView)
    {
        /* Pose-tab expansion is kept on this.expandedTabs and restored by each
         * section's configurePoseTabs() call so toggling a row stays open across
         * replay switches and section changes. */
        this.keyframeEditor.view.removeAllSheets();
        this.pendingSheets.clear();

        if (this.replay == null)
        {
            return;
        }

        this.currentSection().fillSheets(this, resetView);

        Set<String> disabled = this.getDisabledTracks();
        this.pendingSheets.removeIf(sheet -> isSheetDisabled(sheet, disabled));

        UIReplaysEditorUtils.pruneTree(this.pendingSheets);

        for (UIKeyframeSheet sheet : this.pendingSheets)
        {
            this.keyframeEditor.view.addSheet(sheet);
        }

        this.keyframeEditor.view.getDopeSheet().setExpanded(this.expandedTabs);
        if (resetView)
        {
            this.keyframeEditor.view.resetView();
        }
        this.keyframeEditor.view.getDopeSheet().getYAxis().clamp();
    }

    public void openTrackFilter()
    {
        this.openTrackFilter(this.section);
    }

    public void openTrackFilter(Section targetSection)
    {
        if (this.replay == null || !(this.replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        RecordedHandData hand = access.bbsPov$getHand();
        RecordedHudData hud = access.bbsPov$getHud();

        Set<String> disabled;
        Set<String> keys = new LinkedHashSet<>();
        Map<String, Integer> keyToColor = new HashMap<>();

        if (targetSection == Section.HOTBAR)
        {
            if (hud == null) return;
            disabled = hud.disabledTracks.get();
            collectHudTrackKeys(hud, keys, keyToColor);
        }
        else if (targetSection == Section.HAND)
        {
            if (hand == null) return;
            disabled = hand.disabledTracks.get();
            collectHandTrackKeys(this, hand, keys, keyToColor);
        }
        else if (targetSection == Section.BODY_PART)
        {
            if (hand == null) return;
            ModelForm root = BodyPartEditorSection.INSTANCE.root(hand);
            if (root == null) return;
            disabled = hand.disabledTracks.get();
            collectBodyPartTrackKeys(root, hand, this.selectedBodyPart, keys, keyToColor);
        }
        else
        {
            return;
        }

        UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(disabled, keys, keyToColor);
        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);
        panel.onClose(e ->
        {
            if (targetSection == Section.HOTBAR && hud != null)
            {
                hud.disabledTracks.set(disabled);
            }
            else if (hand != null)
            {
                hand.disabledTracks.set(disabled);
            }
            this.refreshSheets(false);
        });
    }

    public static boolean isSheetDisabled(UIKeyframeSheet sheet, Set<String> disabled)
    {
        if (sheet == null)
        {
            return false;
        }

        String key = getSheetFilterKey(sheet);
        String lowerKey = key.toLowerCase();

        // 1. Direct match in keyframe editor disabled set
        if (disabled != null && !disabled.isEmpty())
        {
            for (String s : disabled)
            {
                String lowerS = s.toLowerCase();
                if (key.equals(s) || lowerKey.equals(lowerS) || sheet.id.equals(s) || sheet.id.endsWith("/" + s))
                {
                    return true;
                }
            }

            // If Pose is disabled in keyframe filter, also disable all pose overlays and bone tracks
            boolean isPoseDisabled = disabled.contains("Pose") || disabled.contains("pose") || disabled.contains("pov_hand_pose");
            if (isPoseDisabled)
            {
                if (sheet.id.startsWith("pose_overlay") || sheet.id.contains("pose_overlay")
                    || sheet.isBoneTrack || sheet.id.startsWith("bone:") || sheet.id.contains("/bone:")
                    || (sheet.descriptor != null && sheet.descriptor.kind() == TrackKind.BONE))
                {
                    return true;
                }
            }

            // If Transform is disabled in keyframe filter, also disable transform overlays
            boolean isTransformDisabled = disabled.contains("Transform") || disabled.contains("transform")
                || disabled.contains("camera_offset") || disabled.contains("Camera Offset");
            if (isTransformDisabled)
            {
                if (key.equalsIgnoreCase("camera offset") || sheet.id.startsWith("transform_overlay") || sheet.id.contains("transform_overlay"))
                {
                    return true;
                }
            }
        }

        // 2. Check owner form disabled tracks (hardcore form filter)
        Form owner = UIReplaysEditor.getSheetForm(sheet);
        if (owner != null)
        {
            Set<String> ownerDisabled = owner.disabledTracks.get();
            if (ownerDisabled != null && !ownerDisabled.isEmpty())
            {
                if (ownerDisabled.contains(Form.DISABLED_ALL)
                    || ownerDisabled.contains(key)
                    || ownerDisabled.contains(lowerKey)
                    || ownerDisabled.contains(sheet.id))
                {
                    return true;
                }

                if (ownerDisabled.contains("pose") || ownerDisabled.contains("Pose"))
                {
                    if (sheet.id.startsWith("pose_overlay") || sheet.id.contains("pose_overlay")
                        || sheet.isBoneTrack || sheet.id.startsWith("bone:") || sheet.id.contains("/bone:")
                        || (sheet.descriptor != null && sheet.descriptor.kind() == TrackKind.BONE))
                    {
                        return true;
                    }
                }

                if (ownerDisabled.contains("transform") || ownerDisabled.contains("Transform") || ownerDisabled.contains("camera_offset"))
                {
                    if (key.equalsIgnoreCase("camera offset") || sheet.id.startsWith("transform_overlay") || sheet.id.contains("transform_overlay"))
                    {
                        return true;
                    }
                }
            }
        }

        // 3. If parent is disabled, child is disabled
        UIKeyframeSheet p = sheet.parent;
        while (p != null)
        {
            if (isSheetDisabled(p, disabled))
            {
                return true;
            }
            p = p.parent;
        }

        return false;
    }

    public Set<String> getDisabledTracks()
    {
        if (this.replay == null || !(this.replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return Collections.emptySet();
        }

        RecordedHandData hand = access.bbsPov$getHand();
        RecordedHudData hud = access.bbsPov$getHud();

        if (this.section == Section.HOTBAR)
        {
            return hud != null ? hud.disabledTracks.get() : Collections.emptySet();
        }
        else if (this.section == Section.HAND)
        {
            return hand != null ? hand.disabledTracks.get() : Collections.emptySet();
        }
        else if (this.section == Section.BODY_PART)
        {
            return hand != null ? hand.disabledTracks.get() : Collections.emptySet();
        }

        return Collections.emptySet();
    }

    public static String getSheetFilterKey(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            return "";
        }
        if (sheet.descriptor != null)
        {
            return sheet.descriptor.filterKey();
        }
        if (sheet.isBoneTrack)
        {
            return sheet.title != null ? sheet.title.get() : sheet.id;
        }
        if (sheet.title != null)
        {
            return sheet.title.get();
        }
        return sheet.getFilterKey();
    }

    public static void addTrackKey(String key, int color, Set<String> keys, Map<String, Integer> keyToColor)
    {
        keys.add(key);
        keyToColor.put(key, color);
    }

    public static void collectHandTrackKeys(UIPovEditor editor, RecordedHandData hand, Set<String> keys, Map<String, Integer> keyToColor)
    {
        addTrackKey("Visible", UIReplaysEditor.getColor("visible"), keys, keyToColor);
        addTrackKey("Model", UIReplaysEditor.getColor("model"), keys, keyToColor);
        addTrackKey("Texture", UIReplaysEditor.getColor("texture"), keys, keyToColor);
        addTrackKey("Color", UIReplaysEditor.getColor("color"), keys, keyToColor);
        addTrackKey("Color Overlay", UIReplaysEditor.getColor("color_overlay"), keys, keyToColor);
        addTrackKey("Camera Offset", UIReplaysEditor.getColor("transform"), keys, keyToColor);
        addTrackKey("Pose", UIReplaysEditor.getColor("pose"), keys, keyToColor);

        if (BBSSettings.recordingOverlays.get())
        {
            addTrackKey("pose_overlay", UIReplaysEditor.getColor("pose_overlay"), keys, keyToColor);
            int additional = BBSSettings.recordingPoseOverlays.get();
            for (int k = 0; k < additional; k++)
            {
                addTrackKey("pose_overlay" + k, UIReplaysEditor.getColor("pose_overlay" + k), keys, keyToColor);
            }
        }

        if (editor != null)
        {
            ModelForm handForm = editor.getHandEditorForm();
            ModelInstance model = ModelFormRenderer.getModel(handForm);
            if (model == null || model.getModel() == null)
            {
                ModelForm baseForm = HandEditorSection.INSTANCE.baseModelForm(editor, hand);
                if (baseForm != null)
                {
                    model = ModelFormRenderer.getModel(baseForm);
                }
            }
            if (model == null || model.getModel() == null)
            {
                if (BBSModClient.getModels() != null)
                {
                    model = BBSModClient.getModels().getModel(HandEditorSection.INSTANCE.currentModel(editor, hand));
                }
            }

            HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);
            int colorIdx = 0;
            for (String bone : handBones.depths().keySet())
            {
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

    public static void collectHudTrackKeys(RecordedHudData hud, Set<String> keys, Map<String, Integer> keyToColor)
    {
        int c = 0;
        addTrackKey("Layout", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Slots", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Hearts", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Food", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("XP Bar", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Visible", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Status Bars", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Crosshair", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Cursor Layout", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Cursor Visible", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Cursor Item", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);
        addTrackKey("Selected Slot", UIKeyframeEditor.COLORS[c++ % UIKeyframeEditor.COLORS.length], keys, keyToColor);

        for (int i = 1; i <= 9; i++)
        {
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

    public static void collectBodyPartTrackKeys(ModelForm root, RecordedHandData hand, String selectedPart, Set<String> keys, Map<String, Integer> keyToColor)
    {
        List<TrackDescriptor> descriptors;
        if (selectedPart != null && !selectedPart.isBlank())
        {
            descriptors = TrackCatalog.forPart(root, hand.bodyPartTracks, selectedPart);
        }
        else
        {
            descriptors = new ArrayList<>();
            BodyPartEditorSection.collectAllPartDescriptors(root, root, hand.bodyPartTracks, descriptors);
        }

        for (TrackDescriptor track : descriptors)
        {
            keys.add(track.filterKey());
            keyToColor.put(track.filterKey(), track.color());
        }
    }

    private static int computeHierarchySignature(Form form)
    {
        if (form == null)
        {
            return 0;
        }

        int sig = System.identityHashCode(form) ^ form.getDisplayName().hashCode();
        for (BodyPart part : form.parts.getAllTyped())
        {
            if (part != null)
            {
                sig = 31 * sig + part.getId().hashCode();
                if (part.getForm() != null)
                {
                    sig = 31 * sig + computeHierarchySignature(part.getForm());
                }
            }
        }
        return sig;
    }

    private PovEditorSection currentSection()
    {
        return switch (this.section)
        {
            case HAND -> HandEditorSection.INSTANCE;
            case BODY_PART -> BodyPartEditorSection.INSTANCE;
            case ACTIONS -> ActionsEditorSection.INSTANCE;
            default -> HudEditorSection.INSTANCE;
        };
    }

    public RecordedPovActions getActions()
    {
        return this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access
            ? access.bbsPov$getActions()
            : null;
    }

    private void rememberExpandedTabs()
    {
    }

    public void reloadHandModel()
    {
        HandEditorSection.INSTANCE.reloadModel(this);
    }

    public String getCurrentHandModel(RecordedHandData hand)
    {
        return HandEditorSection.INSTANCE.currentModel(this, hand);
    }

    public Link getCurrentHandTexture(RecordedHandData hand)
    {
        return HandEditorSection.INSTANCE.currentTexture(this, hand);
    }

    public ModelForm getPovBaseModelForm(RecordedHandData hand)
    {
        return HandEditorSection.INSTANCE.baseModelForm(this, hand);
    }

    public UIKeyframeSheet createBoneSheet(
        String sheetId,
        String bone,
        KeyframeChannel<PoseTransform> channel,
        int colorIndex)
    {
        return HandEditorSection.INSTANCE.createBoneSheet(this, sheetId, bone, channel, colorIndex);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public int addSheet(String title, KeyframeChannel<?> channel, Icon icon, int colorIndex)
    {
        return this.addSheet(title, channel, icon, colorIndex, null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public int addSheet(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int colorIndex,
        BaseValueBasic property,
        Supplier<Object> seed)
    {
        this.createSheet(title, channel, icon, colorIndex, property, seed);

        return colorIndex + 1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public int addSheet(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int colorIndex,
        Supplier<Object> seed)
    {
        this.createSheet(title, channel, icon, colorIndex, null, seed);

        return colorIndex + 1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet createSheet(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int colorIndex,
        BaseValueBasic property,
        Supplier<Object> seed)
    {
        int color = UIKeyframeEditor.COLORS[colorIndex % UIKeyframeEditor.COLORS.length];
        UIKeyframeSheet sheet = new UIKeyframeSheet(
            channel.getId(), IKey.constant(title), color, (KeyframeChannel) channel, property);

        sheet.icon(icon);

        if (seed != null)
        {
            sheet.seed(seed);
        }

        this.addPendingSheet(sheet);

        return sheet;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet createSheetWithColor(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int color,
        BaseValueBasic property,
        Supplier<Object> seed)
    {
        UIKeyframeSheet sheet = new UIKeyframeSheet(
            channel.getId(), IKey.constant(title), color, (KeyframeChannel) channel, property);

        sheet.icon(icon);

        if (seed != null)
        {
            sheet.seed(seed);
        }

        this.addPendingSheet(sheet);

        return sheet;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet addSheetWithColor(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int color,
        BaseValueBasic property,
        Supplier<Object> seed)
    {
        return this.createSheetWithColor(title, channel, icon, color, property, seed);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet addSheetWithColor(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int color,
        Supplier<Object> seed)
    {
        return this.createSheetWithColor(title, channel, icon, color, null, seed);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet addSheetWithColor(
        String title,
        KeyframeChannel<?> channel,
        Icon icon,
        int color)
    {
        return this.createSheetWithColor(title, channel, icon, color, null, null);
    }

    private int getDuration()
    {
        /* Match Replay Editor: the active/highlighted range is the Film camera duration.
         * Actor keyframes beyond it stay visible, but don't extend the colored track bars. */
        return this.film == null ? 1 : Math.max(1, this.film.camera.calculateDuration());
    }

    public int getReplayTick()
    {
        return this.replay == null
            ? this.filmPanel.getCursor()
            : this.replay.getTick(this.filmPanel.getCursor());
    }

    @Override
    public void render(UIContext context)
    {
        this.syncReplaySelection();

        RecordedHandData hand = this.replay != null && this.replay.keyframes instanceof ReplayKeyframesPovAccess access
            ? access.bbsPov$getHand() : null;
        ModelForm root = BodyPartEditorSection.INSTANCE.root(hand);
        int signature = computeHierarchySignature(root);

        if (!this.selectedBodyPart.isEmpty())
        {
            boolean partExists = root != null
                && FormUtils.getForm(root, this.selectedBodyPart) != null
                && FormUtils.getForm(root, this.selectedBodyPart) != root;
            if (!partExists)
            {
                this.selectBodyPart("");
            }
        }

        if (signature != this.bodyPartSignature)
        {
            this.bodyPartSignature = signature;
            if (this.filmPanel != null && this.filmPanel.replayEditor != null && this.filmPanel.replayEditor.replaysList != null)
            {
                this.filmPanel.replayEditor.replaysList.setBodyPartsReplay(this.replay, this.selectedBodyPart);
                this.filmPanel.replayEditor.replaysList.resize();
            }
            if (this.section == Section.BODY_PART)
            {
                this.refreshSheets(false);
            }
        }

        /* Highlight the active section tab (Hotbar/Hand/Actions) with the same
         * accent used by BBS's native film editor tabs. The section tabs sit on
         * the editor's left edge, so the highlight tab points right (RIGHT). */
        UIIcon activeTab = switch (this.section)
        {
            case HAND -> this.handTab;
            case BODY_PART -> this.bodyPartTab;
            case ACTIONS -> this.actionsTab;
            default -> this.hotbarTab;
        };

        if (activeTab != null && activeTab.isVisible())
        {
            context.batcher.highlight(activeTab.area, Direction.RIGHT);
        }

        /* UIReplaysEditorUtils.configureFilmHotkeyDrag() only reads the native
         * Replay editor's keyframe editor. Wire this POV editor's transform to
         * the same BBS drag builder so enableMode() can retain its complete
         * configured G/R target cycle (screen/view, sphere and X/Y/Z). */
        if (this.isPoseGizmoSection())
        {
            UIPropTransform transform = UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor);

            if (transform != null)
            {
                transform.hotkeyDrag(() -> UIReplaysEditorUtils.buildFilmGizmoDrag(
                    this.filmPanel,
                    this.filmPanel.getCamera(),
                    this.filmPanel.preview.getViewport(),
                    transform,
                    context.getTransition()));
                transform.worldTransform((output) -> HandEditorSection.INSTANCE.getWorldMatrix(this, output));
            }
        }

        this.keyframeEditor.setVisible(this.section != Section.ACTIONS && this.replay != null);
        this.actionTimeline.setVisible(this.section == Section.ACTIONS && this.replay != null);

        this.partHeader.setVisible(this.replay != null
            && this.keyframeEditor != null
            && this.keyframeEditor.isVisible()
            && this.keyframeEditor.view.getGraph() == this.keyframeEditor.view.getDopeSheet()
            && !this.keyframeEditor.view.getDopeSheet().getSheets().isEmpty());

        if (this.partHeader.isVisible())
        {
            int labelWidth = Math.min(this.keyframeEditor.view.getLabelWidth(), this.keyframeEditor.view.area.w);

            if (this.partHeader.area.w != labelWidth)
            {
                this.partHeader.w(labelWidth);
                this.partHeader.resize();
            }
        }

        super.render(context);
    }

    public String getSelectedPartName()
    {
        if (this.replay == null)
        {
            return "-";
        }

        if (this.selectedBodyPart == null || this.selectedBodyPart.isEmpty())
        {
            RecordedHandData hand = this.replay.keyframes instanceof ReplayKeyframesPovAccess access
                ? access.bbsPov$getHand() : null;

            if (this.section == Section.HAND && hand != null)
            {
                String model = HandEditorSection.INSTANCE.currentModel(this, hand);
                if (model != null && !model.isBlank())
                {
                    return model;
                }
            }

            if (hand != null && hand.baseForm.get() != null)
            {
                Form root = FormUtils.getRoot(hand.baseForm.get());
                if (root instanceof ModelForm mf && mf.model.get() != null && !mf.model.get().isBlank())
                {
                    return root.getDisplayName();
                }
            }
            if (this.replay.form.get() != null)
            {
                return this.replay.form.get().getDisplayName();
            }
            return "-";
        }

        if (this.filmPanel != null && this.filmPanel.replayEditor != null && this.filmPanel.replayEditor.replaysList != null)
        {
            UIReplaysListPanel replaysList = this.filmPanel.replayEditor.replaysList;
            if (replaysList.bodyParts != null)
            {
                for (UIForms.FormEntry entry : replaysList.bodyParts.getList())
                {
                    if (this.selectedBodyPart.equals(entry.getPath()))
                    {
                        return entry.toString();
                    }
                }
            }

            if (replaysList instanceof UIReplaysListPanelPovAccess access)
            {
                UIForms povParts = access.bbsPov$getPovBodyParts();
                if (povParts != null)
                {
                    for (UIForms.FormEntry entry : povParts.getList())
                    {
                        if (this.selectedBodyPart.equals(entry.getPath()))
                        {
                            return entry.toString();
                        }
                    }
                }
            }
        }

        RecordedHandData hand = this.replay.keyframes instanceof ReplayKeyframesPovAccess access
            ? access.bbsPov$getHand() : null;
        ModelForm root = BodyPartEditorSection.INSTANCE.root(hand);
        if (root != null)
        {
            Form form = FormUtils.getForm(root, this.selectedBodyPart);
            if (form != null)
            {
                return form.getDisplayName();
            }
        }

        if (this.replay.form.get() != null)
        {
            Form form = FormUtils.getForm(this.replay.form.get(), this.selectedBodyPart);
            if (form != null)
            {
                return form.getDisplayName();
            }
        }

        return this.selectedBodyPart;
    }
}
