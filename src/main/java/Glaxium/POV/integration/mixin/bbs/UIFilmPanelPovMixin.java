package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.integration.access.bbs.RecorderPovAccess;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.recording.ActiveRecordingRange;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Installs POV Editor as a third native Film editor beside Camera and Replay. */
@Mixin(value = UIFilmPanel.class, remap = false)
public abstract class UIFilmPanelPovMixin implements UIFilmPanelPovAccess
{
    @Shadow public UIElement main;
    @Shadow public UIClipsPanel cameraEditor;
    @Shadow private List<UIElement> panels;
    @Shadow private UIElement selectedMainEditorPanel;

    @Unique private UIPovEditor bbsPov$editor;
    @Unique private UIIcon bbsPov$openEditor;
    @Unique private UIKeyframeEditor bbsPov$replayKeyframeEditor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsPov$installEditor(UIDashboard dashboard, CallbackInfo info)
    {
        UIFilmPanel panel = (UIFilmPanel) (Object) this;

        this.bbsPov$editor = new UIPovEditor(panel);
        this.bbsPov$replayKeyframeEditor = panel.replayEditor.keyframeEditor;
        this.bbsPov$editor.full(this.cameraEditor);
        this.bbsPov$editor.setVisible(false);

        this.bbsPov$openEditor = new UIIcon(Icons.LOOKING, (button) -> panel.showPanel(this.bbsPov$editor));
        this.bbsPov$openEditor.tooltip(IKey.constant("Open POV Editor"), Direction.BOTTOM);
        this.bbsPov$openEditor.setEnabled(false);

        this.main.add(this.bbsPov$editor);
        this.panels.add(this.bbsPov$editor);
        panel.actions().editor(this.bbsPov$openEditor, () -> this.bbsPov$editor != null && this.bbsPov$editor.isVisible());
    }

    @Inject(method = "fillData(Lmchorse/bbs_mod/film/Film;)V", at = @At("TAIL"))
    private void bbsPov$fillEditor(Film film, CallbackInfo info)
    {
        if (this.bbsPov$editor == null)
        {
            return;
        }

        this.bbsPov$openEditor.setEnabled(film != null);
        this.bbsPov$editor.setFilm(film);
        if (film != null)
        {
            Glaxium.POV.camera.clip.PovCameraClips.ensureLegacyClip(film);
        }
    }

    @Inject(method = "applyRecordedKeyframes", at = @At("HEAD"))
    private void bbsPov$closeRecordedPovTracks(Recorder recorder, Film film, CallbackInfo info)
    {
        boolean isOutside = recorder instanceof RecorderPovAccess access && access.bbsPov$isOutside();

        if (isOutside)
        {
            ActiveRecordingRange.set(recorder.initialTick, recorder.tick);
        }
        else
        {
            ActiveRecordingRange.clear();
        }

        if (recorder.keyframes instanceof ReplayKeyframesPovAccess access)
        {
            RecordedHudData hud = access.bbsPov$getHud();

            if (hud != null && hud.hasRecordedData())
            {
                hud.addRecordingEndKeyframes(recorder.keyframes, recorder.tick);
            }

            RecordedHandData hand = access.bbsPov$getHand();

            if (hand != null && hand.hasRecordedData())
            {
                hand.addRecordingEndKeyframes(recorder.keyframes, recorder.tick);
            }

            if (film != null && film.replays.getList().size() > recorder.exception)
            {
                Replay replay = film.replays.getList().get(recorder.exception);
                if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess targetAccess)
                {
                    RecordedHudData targetHud = targetAccess.bbsPov$getHud();
                    RecordedPovActions recActions = access.bbsPov$getActions();
                    RecordedPovActions targetActions = targetAccess.bbsPov$getActions();

                    if (isOutside)
                    {
                        if (targetHud != null)
                        {
                            targetHud.trimCursorForRecordingRange((float) recorder.initialTick, (float) recorder.tick);
                        }

                        if (recActions != null && targetActions != null)
                        {
                            targetActions.trimForRecordingRange(recorder.initialTick, recorder.tick);
                            if (Glaxium.POV.config.PovSettings.isBakeAnyActions())
                            {
                                for (Clip clip : recActions.get())
                                {
                                    if (clip instanceof GuiPovActionClip guiClip)
                                    {
                                        guiClip.ensureBakingBounds();
                                    }
                                    else if (clip instanceof CameraShakePovActionClip shakeClip)
                                    {
                                        shakeClip.ensureBakingBounds();
                                    }
                                }

                                for (PovActionClip recorded : recActions.takeSessionClips())
                                {
                                    if (recorded instanceof Glaxium.POV.actions.clip.ToastPovActionClip toastClip)
                                    {
                                        toastClip.trimToRecording(recorder.tick);
                                    }
                                    targetActions.addClip(recorded.copy());
                                }
                            }
                            targetActions.sync();
                        }
                    }
                    else
                    {
                        if (targetHud != null)
                        {
                            targetHud.cursorLayout.removeAll();
                            targetHud.cursorVisible.removeAll();
                            targetHud.cursorItem.removeAll();
                        }

                        if (targetActions != null)
                        {
                            targetActions.clearAll();
                            if (recActions != null && Glaxium.POV.config.PovSettings.isBakeAnyActions())
                            {
                                for (Clip clip : recActions.get())
                                {
                                    if (clip instanceof GuiPovActionClip guiClip)
                                    {
                                        guiClip.ensureBakingBounds();
                                    }
                                    else if (clip instanceof CameraShakePovActionClip shakeClip)
                                    {
                                        shakeClip.ensureBakingBounds();
                                    }
                                }

                                for (PovActionClip recorded : recActions.takeSessionClips())
                                {
                                    if (recorded instanceof Glaxium.POV.actions.clip.ToastPovActionClip toastClip)
                                    {
                                        toastClip.trimToRecording(recorder.tick);
                                    }
                                    targetActions.addClip(recorded.copy());
                                }
                            }
                            targetActions.sync();
                        }

                        for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                        {
                            if (!bbsPov$isAuthoredChannel(channel))
                            {
                                channel.removeAll();
                            }
                        }
                    }

                    if (this.bbsPov$editor != null)
                    {
                        this.bbsPov$editor.reloadActions();
                    }
                }
            }
        }
    }

    @Inject(method = "applyRecordedKeyframes", at = @At("TAIL"))
    private void bbsPov$clearActiveRecordingRange(Recorder recorder, Film film, CallbackInfo info)
    {
        ActiveRecordingRange.clear();

        boolean isOutside = recorder instanceof RecorderPovAccess access && access.bbsPov$isOutside();
        if (!isOutside && film != null && film.replays.getList().size() > recorder.exception)
        {
            Replay replay = film.replays.getList().get(recorder.exception);
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess targetAccess)
            {
                RecordedHudData targetHud = targetAccess.bbsPov$getHud();
                if (targetHud != null)
                {
                    targetHud.ensureNativeSlotDefaults(replay.keyframes);
                }
            }
        }
    }

    @Unique
    private static boolean bbsPov$isAuthoredChannel(KeyframeChannel<?> channel)
    {
        String id = channel.getId();
        if (id == null)
        {
            return false;
        }

        return id.equals("hotbar_absorption_flash")
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
        method = "applyRecordedKeyframes",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/film/replays/ReplayKeyframes;compressItemChannels()V",
            shift = At.Shift.AFTER))
    private void bbsPov$closeNativeItemTracksAfterCompression(
        Recorder recorder,
        Film film,
        CallbackInfo info)
    {
        if (recorder.keyframes instanceof ReplayKeyframesPovAccess access)
        {
            RecordedHudData hud = access.bbsPov$getHud();

            if (hud != null)
            {
                hud.addNativeItemBoundaryKeyframes(
                    recorder.keyframes,
                    recorder.initialTick,
                    recorder.tick);
            }
        }
    }

    @Inject(method = "updateMainEditorVisibility", at = @At("TAIL"))
    private void bbsPov$updateEditorVisibility(boolean enabled, CallbackInfo info)
    {
        /* UIFilmPanel invokes this from its constructor before our RETURN injection runs. */
        if (this.bbsPov$editor == null)
        {
            return;
        }

        boolean active = enabled && this.selectedMainEditorPanel == this.bbsPov$editor;

        this.bbsPov$editor.setVisible(active);
        this.bbsPov$editor.setTimelineVisible(active);
        this.bbsPov$editor.setPropertiesVisible(active);
        this.bbsPov$openEditor.active(active);
        this.bbsPov$syncNativeKeyframeEditor(active);
        this.bbsPov$syncPovMode(active);
    }

    @Inject(method = "showPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;)V", at = @At("TAIL"))
    private void bbsPov$updateButton(UIElement panel, CallbackInfo info)
    {
        boolean povActive = panel == this.bbsPov$editor;

        if (this.bbsPov$openEditor != null)
        {
            this.bbsPov$openEditor.active(povActive);
        }

        this.bbsPov$syncNativeKeyframeEditor(povActive);
        this.bbsPov$syncPovMode(povActive);
    }

    @Inject(method = "showPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;)V", at = @At("HEAD"))
    private void bbsPov$stopGizmoBeforeEditorSwitch(UIElement panel, CallbackInfo info)
    {
        UIFilmPanel filmPanel = (UIFilmPanel) (Object) this;

        if (this.bbsPov$editor != null
            && (panel == this.bbsPov$editor || this.bbsPov$editor.isVisible()))
        {
            filmPanel.getController().stopGizmoInteraction();
            Gizmo.INSTANCE.stop();
        }
    }

    /** BBS's Film controller, gizmo drag builder and bone world provider all read
     * replayEditor.keyframeEditor directly. Point that native integration seam at
     * the POV editor only while its panel is active, then restore it unchanged. */
    @Unique
    private void bbsPov$syncNativeKeyframeEditor(boolean povActive)
    {
        if (this.bbsPov$editor == null || this.bbsPov$replayKeyframeEditor == null)
        {
            return;
        }

        UIFilmPanel panel = (UIFilmPanel) (Object) this;
        panel.replayEditor.keyframeEditor = povActive
            ? this.bbsPov$editor.keyframeEditor
            : this.bbsPov$replayKeyframeEditor;
    }

    @Override
    public UIPovEditor bbsPov$getEditor()
    {
        return this.bbsPov$editor;
    }

    @Override
    public boolean bbsPov$isPovActive()
    {
        return this.bbsPov$editor != null && this.selectedMainEditorPanel == this.bbsPov$editor && this.bbsPov$editor.isVisible();
    }

    @Unique
    private void bbsPov$syncPovMode(boolean povActive)
    {
        UIFilmPanel panel = (UIFilmPanel) (Object) this;

        if (panel.replayEditor != null)
        {
            if (panel.replayEditor.replaysList instanceof Glaxium.POV.integration.access.bbs.UIReplaysListPanelPovAccess access)
            {
                access.bbsPov$setPovMode(povActive);
            }

            if (panel.replayEditor.replayProperties instanceof Glaxium.POV.integration.access.bbs.UIReplayPropertiesPovAccess access)
            {
                access.bbsPov$setPovMode(povActive);
            }
        }
    }


    @Inject(method = "togglePlayback", at = @At("TAIL"))
    private void bbsPov$shiftPovTimelineOnPlay(CallbackInfo info)
    {
        UIFilmPanel panel = (UIFilmPanel) (Object) this;
        if (panel.isRunning() && this.bbsPov$editor != null && this.bbsPov$isPovActive())
        {
            if (this.bbsPov$editor.actionTimeline != null && this.bbsPov$editor.actionTimeline.clips != null)
            {
                this.bbsPov$editor.actionTimeline.clips.getXAxis().shiftIntoMiddle(panel.getCursor());
            }
            if (this.bbsPov$editor.keyframeEditor != null && this.bbsPov$editor.keyframeEditor.view != null)
            {
                this.bbsPov$editor.keyframeEditor.view.getXAxis().shiftIntoMiddle(panel.getCursor());
            }
        }
    }
}
