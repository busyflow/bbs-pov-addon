package Glaxium.POV.hud.editor;

import java.util.function.DoubleFunction;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIConstrainedNumericKeyframeFactory<T extends Number> extends UIKeyframeFactory<T> {
   private final UITrackpad value;
   private final double minimum;
   private final double maximum;
   private final DoubleFunction<T> converter;

   public UIConstrainedNumericKeyframeFactory(
      UITrackValue<T> track, UIKeyframes editor, double minimum, double maximum, boolean integer, DoubleFunction<T> converter
   ) {
      super(track, editor);
      this.minimum = minimum;
      this.maximum = maximum;
      this.converter = converter;
      this.value = new UITrackpad(this::setConstrainedValue);
      this.value.limit(minimum, maximum, integer);
      this.value.setValue(((Number)track.getValue()).doubleValue());
      this.scroll.add(this.value);
   }

   private void setConstrainedValue(double value) {
      this.setValue(this.converter.apply(Math.max(this.minimum, Math.min(this.maximum, value))));
   }

   public void update() {
      super.update();
      this.value.setValue(((Number)this.track.getValue()).doubleValue());
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      super.render(context);
   }
}
