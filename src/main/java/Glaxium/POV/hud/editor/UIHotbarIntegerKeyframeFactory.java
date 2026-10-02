package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIHotbarIntegerKeyframeFactory extends UIKeyframeFactory<Integer> {
   private UITrackpad value = new UITrackpad(this::setValue);
   private UIBezierHandles handles;

   public UIHotbarIntegerKeyframeFactory(UITrackValue<Integer> track, UIKeyframes editor) {
      super(track, editor);
      this.value.integer();
      this.value.setValue((double)track.getValue().intValue());
      Keyframe<?> kf = this.getKeyframe();
      if (kf != null) {
         this.handles = new UIBezierHandles(kf);
         this.scroll.add(new IUIElement[]{this.value, this.handles.createColumn()});
      } else {
         this.scroll.add(this.value);
      }
   }

   public void update() {
      super.update();
      this.value.setValue((double)this.track.getValue().intValue());
      if (this.handles != null) {
         this.handles.update();
      }
   }
}
