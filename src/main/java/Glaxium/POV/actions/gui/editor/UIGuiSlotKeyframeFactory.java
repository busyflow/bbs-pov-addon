package Glaxium.POV.actions.gui.editor;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIGuiSlotKeyframeFactory extends UIKeyframeFactory<Boolean> {
   private final UIGuiSlotEditor slotEditor;

   public UIGuiSlotKeyframeFactory(UITrackValue<Boolean> track, UIKeyframes editor) {
      super(track, editor);
      this.scroll.removeAll();
      UIKeyframeSheet sheet = track.sheet;
      float tick = editor != null ? editor.getTick() : 0.0F;
      String sheetId = sheet == null ? "" : sheet.id;
      UIFilmPanel filmPanel = PovReplaySettings.getFilmPanel();
      IUIClipsDelegate clipsDelegate = filmPanel != null ? filmPanel.cameraEditor : null;
      BaseValue parentChannel = sheet != null ? sheet.channel : null;
      GuiPovActionClip guiClip = null;
      if (parentChannel != null && parentChannel.getParent() instanceof GuiPovActionClip clip) {
         guiClip = clip;
      } else if (filmPanel != null && filmPanel.cameraEditor != null && filmPanel.cameraEditor.getClip() instanceof GuiPovActionClip clip) {
         guiClip = clip;
      }

      if (!"crafting_grid".equals(sheetId) && !"gui_slots".equals(sheetId)) {
         Replay replay;
         label72: {
            replay = null;
            if (filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null) {
               replay = access.bbsPov$getEditor().getReplay();
               break label72;
            }

            if (filmPanel != null && filmPanel.replayEditor != null) {
               replay = filmPanel.replayEditor.getReplay();
            }
         }

         RecordedHudData hud = null;
         if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            hud = access.bbsPov$getHud();
         }

         if (hud != null && filmPanel != null) {
            this.slotEditor = new UIGuiSlotEditor(null, null);
            this.slotEditor.configure(hud.inventory, hud.inventoryAnchor, tick);
            this.scroll.add(this.slotEditor);
         } else {
            this.slotEditor = null;
         }
      } else if (guiClip != null) {
         String guiId = guiClip.state.isEmpty() ? "inventory" : (String)guiClip.state.get(0).getValue();
         UIGuiSlotEditor.Mode mode = "crafting_grid".equals(sheetId) ? UIGuiSlotEditor.Mode.CRAFTING : UIGuiSlotEditor.Mode.GUI;
         this.slotEditor = new UIGuiSlotEditor(guiClip, clipsDelegate);
         this.slotEditor.configure(guiId, mode, tick);
         this.scroll.add(this.slotEditor);
      } else {
         this.slotEditor = null;
      }
   }
}
