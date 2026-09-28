package Glaxium.POV.hud.editor;

import java.util.List;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;

public final class UIHotbarTransformKeyframeFactory extends UITransformKeyframeFactory {
   public UIHotbarTransformKeyframeFactory(Keyframe<Transform> keyframe, UIKeyframes editor) {
      super(keyframe, editor);
      List<IUIElement> rows = List.copyOf(this.transform.getChildren());
      this.transform.tz.removeFromParent();
      this.transform.sz.removeFromParent();
      if (!rows.isEmpty()) {
         this.transform.remove((UIElement)rows.get(0));
      }

      for (int i = 3; i < rows.size(); i++) {
         this.transform.remove((UIElement)rows.get(i));
      }

      this.transform.h(32);
      if (this.transform instanceof IUIPropTransform2DLayout layout) {
         layout.bbsPov$set2DLayout(true);
      }
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      super.render(context);
   }
}
