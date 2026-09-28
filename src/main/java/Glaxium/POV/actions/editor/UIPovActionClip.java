package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.PovActionClip;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;

public class UIPovActionClip<T extends PovActionClip> extends UIClip<T> {
   public UIPovActionClip(T clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   protected void registerUI() {
      super.registerUI();
   }

   protected void registerPanels() {
      super.registerPanels();
   }

   protected void addEnvelopes() {
   }

   public void fillData() {
      super.fillData();
   }
}
