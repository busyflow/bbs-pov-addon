package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIHotbarIntegerKeyframeFactory extends UIKeyframeFactory<Integer> {
   private final UITrackpad value;

   public UIHotbarIntegerKeyframeFactory(UITrackValue<Integer> track, UIKeyframes editor) {
      super(track, editor);
      this.value = new UITrackpad(this::setValue);
      this.value.integer();
      this.value.setValue((double)track.getValue().intValue());
      this.scroll.add(this.value);
   }

   private void setValue(double value) {
      this.setValue((int) value);
   }

   public void update() {
      super.update();
      this.value.setValue((double)this.track.getValue().intValue());
   }
}
