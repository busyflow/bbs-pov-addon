package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.List;

/** BBS's native Transform keyframe panel reduced to the 2D controls used by a HUD. */
public final class UIHotbarTransformKeyframeFactory extends UITransformKeyframeFactory
{
    public UIHotbarTransformKeyframeFactory(Keyframe<Transform> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        List<IUIElement> rows = List.copyOf(this.transform.getChildren());

        /* HUD layout is strictly two-dimensional with a single Z rotation slider.
         * Row layout in UIPropTransform:
         * - Row 0: Space / Parent button (removed)
         * - Row 1: Translation (tx, ty - tz removed)
         * - Row 2: Scale (sx, sy - sz removed)
         * - Row 3: Rotation (iconR, rz - rx and ry removed)
         */
        this.transform.tz.removeFromParent();
        this.transform.sz.removeFromParent();
        this.transform.rx.removeFromParent();
        this.transform.ry.removeFromParent();

        if (rows.size() > 3 && rows.get(3) instanceof UIElement rotateRowElement)
        {
            if (!rotateRowElement.getChildren().isEmpty())
            {
                ((UIElement) rotateRowElement.getChildren().get(0)).setEnabled(false);
            }
        }

        if (!rows.isEmpty())
        {
            this.transform.remove((UIElement) rows.get(0));
        }

        for (int i = 4; i < rows.size(); i++)
        {
            this.transform.remove((UIElement) rows.get(i));
        }

        this.transform.h(48);

        if (this.transform instanceof IUIPropTransform2DLayout layout)
        {
            layout.bbsPov$set2DLayout(true);
        }
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}
