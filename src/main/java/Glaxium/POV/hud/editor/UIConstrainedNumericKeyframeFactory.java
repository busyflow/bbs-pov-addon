package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.DoubleFunction;

/** A single meaningful value control for bounded POV channels. It deliberately
 * omits Bezier handle fields on enum, discrete and strictly bounded state. */
public class UIConstrainedNumericKeyframeFactory<T extends Number> extends UIKeyframeFactory<T>
{
    private final UITrackpad value;
    private final double minimum;
    private final double maximum;
    private final DoubleFunction<T> converter;

    public UIConstrainedNumericKeyframeFactory(
        Keyframe<T> keyframe,
        UIKeyframes editor,
        double minimum,
        double maximum,
        boolean integer,
        DoubleFunction<T> converter)
    {
        super(keyframe, editor);

        this.minimum = minimum;
        this.maximum = maximum;
        this.converter = converter;
        this.value = new UITrackpad(this::setConstrainedValue);
        this.value.limit(minimum, maximum, integer);
        this.value.setValue(keyframe.getValue().doubleValue());
        this.scroll.add(this.value);
    }

    private void setConstrainedValue(double value)
    {
        this.setValue(this.converter.apply(Math.max(this.minimum, Math.min(this.maximum, value))));
    }

    @Override
    public void update()
    {
        super.update();
        this.value.setValue(this.keyframe.getValue().doubleValue());
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}
