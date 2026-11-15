package Glaxium.POV.actions.gui.editor;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/** Keyframe factory displaying standard controls (Tick, Duration, Easing) at top, followed by the slot grid. */
public class UIGuiSlotKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private final UIGuiSlotEditor slotEditor;

    public UIGuiSlotKeyframeFactory(Keyframe<Boolean> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        // Hide all default keyframe settings (tick, duration, interp, etc.) so only the slots show
        this.scroll.removeAll();

        UIKeyframeSheet sheet = editor != null && editor.getGraph() != null
            ? editor.getGraph().getSheet(keyframe)
            : (editor != null && editor.getDopeSheet() != null ? editor.getDopeSheet().getSheet(keyframe) : null);
        String sheetId = sheet == null ? "" : sheet.id;

        UIFilmPanel filmPanel = PovReplaySettings.getFilmPanel();
        IUIClipsDelegate clipsDelegate = filmPanel != null ? filmPanel.cameraEditor : null;

        mchorse.bbs_mod.settings.values.base.BaseValue parentChannel = keyframe.getParent();
        GuiPovActionClip guiClip = null;
        if (parentChannel != null && parentChannel.getParent() instanceof GuiPovActionClip clip)
        {
            guiClip = clip;
        }
        else if (filmPanel != null && filmPanel.cameraEditor != null && filmPanel.cameraEditor.getClip() instanceof GuiPovActionClip clip)
        {
            guiClip = clip;
        }

        if ("crafting_grid".equals(sheetId) || "gui_slots".equals(sheetId))
        {
            if (guiClip != null)
            {
                String guiId = guiClip.state.isEmpty() ? "inventory" : guiClip.state.get(0).getValue();
                UIGuiSlotEditor.Mode mode = "crafting_grid".equals(sheetId)
                    ? UIGuiSlotEditor.Mode.CRAFTING
                    : UIGuiSlotEditor.Mode.GUI;

                this.slotEditor = new UIGuiSlotEditor(guiClip, clipsDelegate);
                this.slotEditor.configure(guiId, mode, keyframe.getTick());
                this.scroll.add((IUIElement) this.slotEditor);
            }
            else
            {
                this.slotEditor = null;
            }
        }
        else
        {
            Replay replay = null;
            if (filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null)
            {
                replay = access.bbsPov$getEditor().getReplay();
            }
            else if (filmPanel != null && filmPanel.replayEditor != null)
            {
                replay = filmPanel.replayEditor.getReplay();
            }

            RecordedHudData hud = null;
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access)
            {
                hud = access.bbsPov$getHud();
            }

            if (hud != null && filmPanel != null)
            {
                this.slotEditor = new UIGuiSlotEditor(null, null);
                this.slotEditor.configure(hud.inventory, hud.inventoryAnchor, keyframe.getTick());
                this.scroll.add((IUIElement) this.slotEditor);
            }
            else
            {
                this.slotEditor = null;
            }
        }
    }
}
