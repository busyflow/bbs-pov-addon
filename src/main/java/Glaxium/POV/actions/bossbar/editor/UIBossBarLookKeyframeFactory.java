package Glaxium.POV.actions.bossbar.editor;

import Glaxium.POV.actions.bossbar.BossBarLooks;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBossBarLookKeyframeFactory extends UIKeyframeFactory<String> {
   private final UIBossBarLookKeyframeFactory.Mode mode;
   private final UIButton button;
   private String lastValue;

   public UIBossBarLookKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor, UIBossBarLookKeyframeFactory.Mode mode) {
      super(keyframe, editor);
      this.mode = mode;
      this.lastValue = (String)keyframe.getValue();
      this.button = new UIButton(IKey.constant(this.getButtonLabel()), this::onButtonClicked);
      this.scroll.add(this.button);
   }

   private String getButtonLabel() {
      String val = (String)this.keyframe.getValue();
      return this.mode == UIBossBarLookKeyframeFactory.Mode.COLOR ? "Color: " + BossBarLooks.displayColor(val) : "Style: " + BossBarLooks.displayStyle(val);
   }

   private void updateButtonLabel() {
      this.button.label = IKey.constant(this.getButtonLabel());
   }

   private void onButtonClicked(UIButton button) {
      String current = (String)this.keyframe.getValue();
      String next = this.mode == UIBossBarLookKeyframeFactory.Mode.COLOR ? BossBarLooks.nextColor(current) : BossBarLooks.nextStyle(current);
      this.setValue(next);
      this.lastValue = next;
      this.updateButtonLabel();
   }

   public void update() {
      super.update();
      String val = (String)this.keyframe.getValue();
      if (val == null ? this.lastValue != null : !val.equals(this.lastValue)) {
         this.lastValue = val;
         this.updateButtonLabel();
      }
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      super.render(context);
   }

   public static enum Mode {
      COLOR,
      STYLE;
   }
}
