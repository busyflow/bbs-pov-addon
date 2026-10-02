package Glaxium.POV.camera.clip;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;

/** Inspector for the POV camera timeline clip. */
public class UIPovCameraClip extends UIClip<PovCameraClip>
{
    public UIButton selector;
    public UIToggle hands;
    public UIToggle hud;
    public UIToggle crosshair;
    public UIToggle actions;
    public UIToggle screenEffects;
    public UIToggle cursor;
    public UIToggle cameraShake;
    public UIToggle headLook;
    public UIToggle hardcoreLook;
    public UIToggle blockOutline;
    public UIButton perspective;
    public UITrackpad fov;

    public UIPovCameraClip(PovCameraClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.selector = new UIButton(IKey.constant("Replay Source"), (button) -> {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);

            if (panel != null)
            {
                mchorse.bbs_mod.film.Film film = (mchorse.bbs_mod.film.Film) panel.getData();
                java.util.List<mchorse.bbs_mod.film.replays.Replay> replays = film != null ? film.replays.getList() : java.util.List.of();
                int currentIndex = this.clip.selector.get();
                String currentId = currentIndex >= 0 && currentIndex < replays.size() ? replays.get(currentIndex).getId() : "";

                UIAnchorKeyframeFactory.displayActors(
                    this.getContext(),
                    panel.getController().getEntities(),
                    currentId,
                    (selectedId) -> {
                        int foundIndex = -1;
                        for (int i = 0; i < replays.size(); i++)
                        {
                            if (replays.get(i).getId().equals(selectedId))
                            {
                                foundIndex = i;
                                break;
                            }
                        }
                        int finalIndex = foundIndex;
                        this.editor.editMultiple(this.clip.selector, (value) -> value.set(finalIndex));
                    });
            }
        });
        this.selector.tooltip(IKey.constant("Choose the replay that supplies this POV clip"));

        this.hands = new UIToggle(IKey.constant("Hands"), (toggle) ->
            this.editor.editMultiple(this.clip.hands, (value) -> value.set(toggle.getValue())));
        this.hands.tooltip(IKey.constant("Render this replay's POV hands. Click-selection and hand gizmos still require POV Camera Mode."));
        this.hud = new UIToggle(IKey.constant("HUD"), (toggle) ->
            this.editor.editMultiple(this.clip.hud, (value) -> value.set(toggle.getValue())));
        this.crosshair = new UIToggle(IKey.constant("Crosshair"), (toggle) ->
            this.editor.editMultiple(this.clip.crosshair, (value) -> value.set(toggle.getValue())));
        this.crosshair.tooltip(IKey.constant("Render the replay's recorded crosshair independently from the HUD"));
        this.actions = new UIToggle(IKey.constant("POV Actions"), (toggle) ->
            this.editor.editMultiple(this.clip.actions, (value) -> value.set(toggle.getValue())));
        this.screenEffects = new UIToggle(IKey.constant("Screen Effects"), (toggle) ->
            this.editor.editMultiple(this.clip.screenEffects, (value) -> value.set(toggle.getValue())));
        this.screenEffects.tooltip(IKey.constant("Render recorded screen effects (vignettes, spyglass, etc.)"));
        this.cursor = new UIToggle(IKey.constant("Cursor"), (toggle) ->
            this.editor.editMultiple(this.clip.cursor, (value) -> value.set(toggle.getValue())));
        this.cursor.tooltip(IKey.constant("Render the recorded GUI cursor and its hover behavior"));
        this.cameraShake = new UIToggle(IKey.constant("Camera Shake"), (toggle) ->
            this.editor.editMultiple(this.clip.cameraShake, (value) -> value.set(toggle.getValue())));
        this.cameraShake.tooltip(IKey.constant("Apply baked Camera Shake action clips while this POV camera clip is active"));
        this.headLook = new UIToggle(IKey.constant("Head Look"), (toggle) ->
        {
            this.editor.editMultiple(this.clip.headLook, (value) -> value.set(toggle.getValue()));

            if (this.hardcoreLook != null)
            {
                this.hardcoreLook.setEnabled(toggle.getValue());
            }
            if (this.blockOutline != null)
            {
                this.blockOutline.setEnabled(toggle.getValue());
            }
            if (this.fov != null)
            {
                this.fov.setEnabled(toggle.getValue());
            }
            if (this.perspective != null)
            {
                this.perspective.setEnabled(toggle.getValue());
            }
        });
        this.headLook.tooltip(IKey.constant("Use the replay's recorded head position and look direction for the camera"));

        this.hardcoreLook = new UIToggle(IKey.constant("Hardcore Look"), (toggle) ->
            this.editor.editMultiple(this.clip.hardcoreLook, (value) -> value.set(toggle.getValue())));
        this.hardcoreLook.tooltip(IKey.constant("Follow the actual head bone position and rotation from pose, transform, and overlay keyframes"));

        this.blockOutline = new UIToggle(IKey.constant("Block Outline"), (toggle) ->
            this.editor.editMultiple(this.clip.blockOutline, (value) -> value.set(toggle.getValue())));
        this.blockOutline.tooltip(IKey.constant("Show block outline when looking at a block in POV camera mode"));

        this.perspective = new UIButton(IKey.constant(PovCameraClip.getPerspectiveLabel(this.clip.perspective.get())), (button) -> {
            int next = PovCameraClip.nextPerspective(this.clip.perspective.get());
            boolean firstPerson = next == PovCameraClip.VIEW_FIRST_PERSON;

            this.editor.editMultiple(this.clip.perspective, (value) -> value.set(next));
            this.editor.editMultiple(this.clip.hands, (value) -> value.set(firstPerson));
            this.editor.editMultiple(this.clip.crosshair, (value) -> value.set(firstPerson));

            this.updatePerspectiveButton();
            this.hands.setValue(firstPerson);
            this.crosshair.setValue(firstPerson);
        });
        this.perspective.tooltip(IKey.constant("Cycle camera view: First Person, Third Person (Back), Third Person (Front)"));

        this.fov = new UITrackpad((value) ->
            this.editor.editMultiple(this.clip.fov, (field) -> field.set(value.floatValue())));
        this.fov.limit(1D, 180D);
        this.fov.tooltip(IKey.constant("Camera field of view"));
    }

    private void updatePerspectiveButton()
    {
        if (this.perspective != null)
        {
            this.perspective.label = IKey.constant(PovCameraClip.getPerspectiveLabel(this.clip.perspective.get()));
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Replay Source"),
            this.selector));
        this.panels.add(this.section(
            IKey.constant("POV Output"),
            this.hands,
            this.hud,
            this.crosshair,
            this.actions,
            this.screenEffects,
            this.cursor,
            this.cameraShake));
        this.panels.add(this.section(
            IKey.constant("Camera"),
            this.headLook,
            this.hardcoreLook,
            this.blockOutline,
            this.perspective,
            this.fov));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.hands.setValue(this.clip.hands.get());
        this.hud.setValue(this.clip.hud.get());
        this.crosshair.setValue(this.clip.crosshair.get());
        this.actions.setValue(this.clip.actions.get());
        this.screenEffects.setValue(this.clip.screenEffects.get());
        this.cursor.setValue(this.clip.cursor.get());
        this.cameraShake.setValue(this.clip.cameraShake.get());
        this.headLook.setValue(this.clip.headLook.get());
        this.hardcoreLook.setValue(this.clip.hardcoreLook.get());
        this.hardcoreLook.setEnabled(this.clip.headLook.get());
        this.blockOutline.setValue(this.clip.blockOutline.get());
        this.blockOutline.setEnabled(this.clip.headLook.get());
        this.updatePerspectiveButton();
        this.perspective.setEnabled(this.clip.headLook.get());
        this.fov.setValue(this.clip.fov.get());
        this.fov.setEnabled(this.clip.headLook.get());
    }
}
