package wemppy.bbs_pov.client.clips.ui;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import wemppy.bbs_pov.clips.MouseTrackerClip;

public class UIMouseTrackerClip extends UIClip<MouseTrackerClip>
{
    public UIButton editKeyframes;
    public UIKeyframeEditor keyframes;

    public UIToggle liveTracking;
    public UIToggle visible;
    public UIToggle clicking;
    public UITrackpad x;
    public UITrackpad y;

    public UIMouseTrackerClip(MouseTrackerClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            if (this.editor instanceof UIClipsPanel panel && this.clip.getParent() instanceof Clips clips)
            {
                UIReplaysEditor.renderRuler(context, this.keyframes.view, panel, clips, this.clip.tick.get());
            }
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("mouse_tracker_keyframes");

        UIKeyframeSheet sheetX = new UIKeyframeSheet("mouse_x", IKey.raw("Cursor X"), Colors.RED, this.clip.xChannel, null).icon(Icons.MOVE_RIGHT);
        UIKeyframeSheet sheetY = new UIKeyframeSheet("mouse_y", IKey.raw("Cursor Y"), Colors.GREEN, this.clip.yChannel, null).icon(Icons.MOVE_UP);
        UIKeyframeSheet sheetVis = new UIKeyframeSheet("mouse_vis", IKey.raw("Visible"), Colors.CYAN, this.clip.visibleChannel, this.clip.visible).icon(Icons.VISIBLE);
        UIKeyframeSheet sheetClick = new UIKeyframeSheet("mouse_click", IKey.raw("Clicking"), Colors.YELLOW, this.clip.clickingChannel, this.clip.clicking).icon(Icons.POINTER);

        this.keyframes.view.addSheet(sheetX);
        this.keyframes.view.addSheet(sheetY);
        this.keyframes.view.addSheet(sheetVis);
        this.keyframes.view.addSheet(sheetClick);

        this.editKeyframes = new UIButton(IKey.raw("Edit Keyframes"), (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.editKeyframes.keys().register(Keys.FORMS_EDIT, () -> this.editKeyframes.clickItself());

        this.liveTracking = this.toggle(IKey.raw("Live Mouse Tracking"), this.clip.liveTracking);
        this.visible = this.toggle(IKey.raw("Cursor Visible"), this.clip.visible);
        this.clicking = this.toggle(IKey.raw("Clicking"), this.clip.clicking);
        this.x = this.trackpad(this.clip.x);
        this.y = this.trackpad(this.clip.y);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Mouse Tracking"), this.liveTracking, this.visible, this.clicking, this.x, this.y));
        this.panels.add(this.section(IKey.raw("Keyframes"), this.editKeyframes));
    }
}
